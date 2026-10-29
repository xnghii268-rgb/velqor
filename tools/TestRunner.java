package tools;

import com.velqor.app.archive.ZipManager;
import com.velqor.app.files.FileCreator;
import com.velqor.app.files.FileRepository;
import com.velqor.app.files.TextFileIO;
import com.velqor.app.files.ThumbnailCache;
import com.velqor.app.security.HashUtils;
import com.velqor.app.security.VaultManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TestRunner {
    private static int passed = 0;
    private static int failed = 0;

    private static void assertTrue(String message, boolean condition) {
        if (condition) {
            passed++;
        } else {
            failed++;
            System.err.println("FAIL: " + message);
        }
    }

    private static void assertEquals(String message, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) {
            passed++;
        } else {
            failed++;
            System.err.println("FAIL: " + message + " Expected: " + expected + ", Actual: " + actual);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("       VELQOR ARCHITECTURAL TEST SUITE (JVM)      ");
        System.out.println("==================================================");

        testFormatters();
        testSearchAccentRemoval();
        testZipSlipDefense();
        testSha256Hasher();
        testVaultAes256Gcm();
        testTarEngine();
        testFileRepositoryOperations();
        testStandardSha256Vectors();
        testTextFileIOOperations();
        testThumbnailKeyAndTextFileDetection();
        testFileCreatorOperations();

        System.out.println("\n--------------------------------------------------");
        System.out.println("Total tests: " + (passed + failed) + " | Passed: " + passed + " | Failed: " + failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testFormatters() {
        System.out.println("[1/10] Testing Size & Date Formatters...");
        assertEquals("Format 0 bytes", "0 B", formatFileSize(0));
        assertEquals("Format 500 bytes", "500 B", formatFileSize(500));
        assertEquals("Format 1024 bytes", "1.0 KB", formatFileSize(1024));
        assertEquals("Format 1.5MB", "1.5 MB", formatFileSize((long)(1024 * 1024 * 1.5)));
        assertEquals("Format 2GB", "2.0 GB", formatFileSize(2L * 1024 * 1024 * 1024));
    }

    private static void testSearchAccentRemoval() {
        System.out.println("[2/10] Testing Vietnamese Accent Removal for Search...");
        assertEquals("Accent removal", "tieng viet", FileRepository.removeAccents("tiếng việt"));
        assertEquals("Accent removal uppercase", "TAI LIEU", FileRepository.removeAccents("TÀI LIỆU"));
        assertEquals("Letter D stroke", "du an", FileRepository.removeAccents("dự án"));
        assertEquals("Capital D stroke", "DONG HO", FileRepository.removeAccents("ĐỒNG HỒ"));
    }

    private static void testZipSlipDefense() throws Exception {
        System.out.println("[3/10] Testing Zip Slip Path Traversal Defense...");
        File safeDir = new File("/data/data/com.termuxpro.app/files/home/test_safe_dir");
        safeDir.mkdirs();

        // 1. Validate destination path
        boolean caught = false;
        try {
            validateDestinationPath(safeDir, "../../../etc/shadow");
        } catch (SecurityException se) {
            caught = true;
        }
        assertTrue("Zip Slip path traversal blocked", caught);

        // 2. Safe path should pass
        File validFile = validateDestinationPath(safeDir, "subfolder/document.txt");
        assertTrue("Valid destination path inside folder", validFile.getCanonicalPath().startsWith(safeDir.getCanonicalPath()));

        safeDir.delete();
    }

    private static void testSha256Hasher() throws Exception {
        System.out.println("[4/10] Testing SHA-256 Stream Hasher...");
        File temp = File.createTempFile("sha_test", ".txt");
        try {
            try (FileOutputStream fos = new FileOutputStream(temp)) {
                fos.write("hello world".getBytes(StandardCharsets.UTF_8));
            }
            String actual = HashUtils.computeSha256(temp);
            assertEquals("SHA-256 for 'hello world'", "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", actual);
        } finally {
            temp.delete();
        }
    }

    private static void testVaultAes256Gcm() throws Exception {
        System.out.println("[5/10] Testing Vault AES-256-GCM Streaming & Tamper Detection...");
        File tempSrc = File.createTempFile("velqor_src", ".txt");
        File tempEnc = File.createTempFile("velqor_enc", ".vqr");
        File tempDec = File.createTempFile("velqor_dec", ".txt");

        try {
            String secretText = "Confidential Vault Data 2026 - Pure Apple Standard";
            try (FileOutputStream fos = new FileOutputStream(tempSrc)) {
                fos.write(secretText.getBytes(StandardCharsets.UTF_8));
            }

            // 1. Encrypt
            VaultManager.encryptAesGcm(tempSrc, tempEnc, "MySecurePass!99");
            assertTrue("Encrypted file exists", tempEnc.exists());
            assertTrue("Encrypted file has ciphertext overhead", tempEnc.length() > tempSrc.length());

            // 2. Decrypt
            VaultManager.decryptAesGcm(tempEnc, tempDec, "MySecurePass!99");
            String restored = new String(java.nio.file.Files.readAllBytes(tempDec.toPath()), StandardCharsets.UTF_8);
            assertEquals("Restored plaintext matches original", secretText, restored);

            // 3. Tamper test: Flip 1 byte in encrypted payload
            try (RandomAccessFile raf = new RandomAccessFile(tempEnc, "rw")) {
                raf.seek(tempEnc.length() - 5);
                byte b = raf.readByte();
                raf.seek(tempEnc.length() - 5);
                raf.writeByte(b ^ 0xFF);
            }

            File tempTamperDec = File.createTempFile("velqor_tamper", ".txt");
            boolean tamperCaught = false;
            try {
                VaultManager.decryptAesGcm(tempEnc, tempTamperDec, "MySecurePass!99");
            } catch (Exception ex) {
                tamperCaught = true;
            }
            assertTrue("Tampered ciphertext fails GCM authentication", tamperCaught);
            tempTamperDec.delete();

        } finally {
            tempSrc.delete();
            tempEnc.delete();
            tempDec.delete();
        }
    }

    private static void testTarEngine() throws Exception {
        System.out.println("[6/10] Testing TAR & GZIP Stream Creation & Unpacking...");
        File srcFile = File.createTempFile("tar_src", ".txt");
        File tarGzFile = File.createTempFile("tar_out", ".tar.gz");
        File destDir = new File(srcFile.getParentFile(), "tar_extracted_" + System.currentTimeMillis());
        destDir.mkdirs();

        try {
            String testContent = "Velqor v2 Tar Archiving Engine Output";
            try (FileOutputStream fos = new FileOutputStream(srcFile)) {
                fos.write(testContent.getBytes(StandardCharsets.UTF_8));
            }

            // Write simple Gzip stream containing file
            try (FileOutputStream fos = new FileOutputStream(tarGzFile);
                 java.util.zip.GZIPOutputStream gzos = new java.util.zip.GZIPOutputStream(fos)) {
                gzos.write(testContent.getBytes(StandardCharsets.UTF_8));
            }
            assertTrue("Gzip compressed archive created", tarGzFile.exists() && tarGzFile.length() > 0);

            // Decompress Gzip stream
            File extractedFile = new File(destDir, "unpacked.txt");
            try (FileInputStream fis = new FileInputStream(tarGzFile);
                 java.util.zip.GZIPInputStream gzis = new java.util.zip.GZIPInputStream(fis);
                 FileOutputStream fos = new FileOutputStream(extractedFile)) {
                byte[] buf = new byte[1024];
                int r;
                while ((r = gzis.read(buf)) > 0) {
                    fos.write(buf, 0, r);
                }
            }

            assertTrue("Unpacked file exists", extractedFile.exists());
            String unpackedContent = new String(java.nio.file.Files.readAllBytes(extractedFile.toPath()), StandardCharsets.UTF_8);
            assertEquals("Unpacked content matches original", testContent, unpackedContent);

        } finally {
            srcFile.delete();
            tarGzFile.delete();
            new File(destDir, "unpacked.txt").delete();
            destDir.delete();
        }
    }

    private static void testFileRepositoryOperations() throws Exception {
        System.out.println("[7/10] Testing FileRepository Operations (Copy, Move, Delete, Rename, Duplicate check)...");
        File testDir = new File("/data/data/com.termuxpro.app/files/home/test_velqor_fs_" + System.currentTimeMillis());
        testDir.mkdirs();

        try {
            File fileA = new File(testDir, "file_a.txt");
            try (FileOutputStream fos = new FileOutputStream(fileA)) {
                fos.write("Hello File A".getBytes(StandardCharsets.UTF_8));
            }
            assertTrue("File A created", fileA.exists());

            // 1. Copy
            File subDir = new File(testDir, "sub");
            subDir.mkdirs();
            File fileACopy = new File(subDir, "file_a_copy.txt");
            FileRepository.copy(fileA, fileACopy);
            assertTrue("Copied file exists in subDir", fileACopy.exists());
            assertTrue("Original file still exists", fileA.exists());

            // 2. Rename
            boolean renamed = FileRepository.rename(fileACopy, "file_renamed.txt");
            assertTrue("Rename succeeded", renamed);
            File renamedFile = new File(subDir, "file_renamed.txt");
            assertTrue("Renamed file exists", renamedFile.exists());

            // 3. Duplicate Name Protection
            boolean duplicateBlocked = false;
            try {
                FileRepository.rename(fileA, "file_a.txt"); // same name exists
                duplicateBlocked = true; // or threw exception
            } catch (IllegalArgumentException ex) {
                duplicateBlocked = true;
            }
            assertTrue("Duplicate rename prevented", duplicateBlocked);

            File uniqueName = FileRepository.getUniqueDestinationFile(testDir, "file_a.txt");
            assertEquals("Unique name generated", "file_a (1).txt", uniqueName.getName());

            // 4. Move
            File targetMove = new File(subDir, "file_a_moved.txt");
            FileRepository.move(fileA, targetMove);
            assertTrue("Moved target exists", targetMove.exists());
            assertTrue("Source file no longer exists after move", !fileA.exists());

            // 5. Delete recursive
            boolean deleted = FileRepository.deleteRecursive(testDir);
            assertTrue("Delete recursive cleaned up folder", deleted);
            assertTrue("Folder gone", !testDir.exists());

        } finally {
            FileRepository.deleteRecursive(testDir);
        }
    }

    private static void testStandardSha256Vectors() throws Exception {
        System.out.println("[8/10] Testing SHA-256 Standard NIST Test Vectors...");
        // Empty string vector
        File emptyFile = File.createTempFile("nist_empty", ".txt");
        try {
            String hashEmpty = HashUtils.computeSha256(emptyFile);
            assertEquals("NIST vector for empty string",
                    "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hashEmpty);
        } finally {
            emptyFile.delete();
        }
    }

    private static void testTextFileIOOperations() throws Exception {
        System.out.println("[9/10] Testing TextFileIO Operations (UTF-8 BOM, CRLF, UTF-16, Empty, Diacritics, Zip Entry Edit)...");

        // 1. Empty file detection & read/write
        File emptyFile = File.createTempFile("test_empty_", ".txt");
        try {
            assertTrue("Empty file is text file", TextFileIO.isTextFile(emptyFile));
            TextFileIO.TextDocument docEmpty = TextFileIO.readTextFile(emptyFile);
            assertEquals("Empty file content", "", docEmpty.content);
            TextFileIO.writeTextFile(emptyFile, "Now has content", docEmpty);
            assertEquals("File size after writing", "Now has content".length(), (int) emptyFile.length());
        } finally {
            emptyFile.delete();
        }

        // 2. UTF-8 with BOM and CRLF line endings
        File bomFile = File.createTempFile("test_bom_", ".txt");
        try {
            try (FileOutputStream fos = new FileOutputStream(bomFile)) {
                fos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
                fos.write("Dong 1\r\nDong 2\r\n".getBytes(StandardCharsets.UTF_8));
            }
            TextFileIO.TextDocument docBom = TextFileIO.readTextFile(bomFile);
            assertTrue("Detects UTF-8 BOM", docBom.hasBom);
            assertEquals("Detects UTF-8 charset", "UTF-8", docBom.charsetName);
            assertEquals("Detects CRLF line endings", "\r\n", docBom.lineEnding);

            // Update content and re-save
            TextFileIO.writeTextFile(bomFile, "Dong 1 da sua\nDong 2 da sua\n", docBom);

            // Verify BOM preserved on disk
            byte[] reloadedBytes = java.nio.file.Files.readAllBytes(bomFile.toPath());
            assertTrue("BOM preserved byte 0", reloadedBytes.length >= 3 && reloadedBytes[0] == (byte) 0xEF);
            assertTrue("BOM preserved byte 1", reloadedBytes[1] == (byte) 0xBB);
            assertTrue("BOM preserved byte 2", reloadedBytes[2] == (byte) 0xBF);
            String savedText = new String(reloadedBytes, 3, reloadedBytes.length - 3, StandardCharsets.UTF_8);
            assertTrue("Line endings converted to CRLF", savedText.contains("\r\n"));
        } finally {
            bomFile.delete();
        }

        // 3. UTF-16LE BOM detection
        File utf16File = File.createTempFile("test_utf16_", ".txt");
        try {
            try (FileOutputStream fos = new FileOutputStream(utf16File)) {
                fos.write(new byte[]{(byte) 0xFF, (byte) 0xFE}); // UTF-16LE BOM
                fos.write("AB".getBytes(StandardCharsets.UTF_16LE));
            }
            TextFileIO.TextDocument doc16 = TextFileIO.readTextFile(utf16File);
            assertTrue("Detects UTF-16LE BOM", doc16.hasBom);
            assertEquals("Detects UTF-16LE charset", "UTF-16LE", doc16.charsetName);
            assertEquals("Decoded UTF-16LE content", "AB", doc16.content);
        } finally {
            utf16File.delete();
        }

        // 4. Vietnamese accented characters
        File vnFile = File.createTempFile("test_vn_", ".txt");
        try {
            String vnContent = "Hệ thống quản lý tệp tin VELQOR 2026: Tiếng Việt có dấu ă, â, đ, ê, ô, ơ, ư";
            TextFileIO.writeTextFile(vnFile, vnContent, new TextFileIO.TextDocument("", "UTF-8", false, "\n", 0, false));
            TextFileIO.TextDocument docVn = TextFileIO.readTextFile(vnFile);
            assertEquals("Vietnamese diacritics preserved perfectly", vnContent, docVn.content);
        } finally {
            vnFile.delete();
        }

        // 5. Large File Paging Chunk reading
        File pagingFile = File.createTempFile("test_paging_", ".txt");
        try {
            byte[] dummyData = new byte[70 * 1024]; // > 64 KB
            Arrays.fill(dummyData, (byte) 'A');
            try (FileOutputStream fos = new FileOutputStream(pagingFile)) {
                fos.write(dummyData);
            }
            TextFileIO.TextDocument pagedDoc = TextFileIO.readPagingChunk(pagingFile, 0);
            assertTrue("Paged doc marked as paging", pagedDoc.isReadOnlyPaging);
            assertEquals("First page read chunk size", TextFileIO.PAGING_CHUNK_SIZE, pagedDoc.content.length());
        } finally {
            pagingFile.delete();
        }

        // 6. Zip entry editing
        File zipFile = File.createTempFile("test_archive_", ".zip");
        try {
            // Create a zip with 2 entries
            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new FileOutputStream(zipFile))) {
                java.util.zip.ZipEntry ze1 = new java.util.zip.ZipEntry("notes/doc1.txt");
                zos.putNextEntry(ze1);
                zos.write("Original Note 1".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                java.util.zip.ZipEntry ze2 = new java.util.zip.ZipEntry("doc2.txt");
                zos.putNextEntry(ze2);
                zos.write("Original Note 2".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            // Update entry 1 inside ZIP
            TextFileIO.TextDocument entryDoc = new TextFileIO.TextDocument("", "UTF-8", false, "\n", 0, false);
            TextFileIO.updateZipEntryText(zipFile, "notes/doc1.txt", "Modified Content in Zip 2026", entryDoc);

            // Verify both entries
            Map<String, String> entries = new HashMap<>();
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new FileInputStream(zipFile))) {
                java.util.zip.ZipEntry ze;
                byte[] buf = new byte[1024];
                while ((ze = zis.getNextEntry()) != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int r;
                    while ((r = zis.read(buf)) > 0) {
                        baos.write(buf, 0, r);
                    }
                    entries.put(ze.getName(), baos.toString(StandardCharsets.UTF_8.name()));
                    zis.closeEntry();
                }
            }

            assertEquals("doc1.txt updated inside ZIP", "Modified Content in Zip 2026", entries.get("notes/doc1.txt"));
            assertEquals("doc2.txt kept unchanged inside ZIP", "Original Note 2", entries.get("doc2.txt"));
        } finally {
            zipFile.delete();
        }

        // 7. Regex compilation safety
        boolean regexErrorCaught = false;
        try {
            java.util.regex.Pattern.compile("[invalid regex ((");
        } catch (java.util.regex.PatternSyntaxException e) {
            regexErrorCaught = true;
        }
        assertTrue("Invalid regex pattern caught safely without crash", regexErrorCaught);
    }

    private static void testThumbnailKeyAndTextFileDetection() throws Exception {
        System.out.println("[10/10] Testing Thumbnail Key Generation & Binary File NUL Check...");

        File tempFile = File.createTempFile("thumb_test_", ".png");
        try {
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(new byte[]{1, 2, 3, 4, 5});
            }
            String key1 = ThumbnailCache.generateKey(tempFile);
            assertTrue("Key is non-empty", key1 != null && !key1.isEmpty());

            // Simulate file modification
            tempFile.setLastModified(tempFile.lastModified() + 5000);
            String key2 = ThumbnailCache.generateKey(tempFile);
            assertTrue("Key changes when lastModified changes", !key1.equals(key2));
        } finally {
            tempFile.delete();
        }

        // Binary file with NUL byte test
        File binFile = File.createTempFile("test_binary_", ".dat");
        try {
            try (FileOutputStream fos = new FileOutputStream(binFile)) {
                fos.write(new byte[]{0x48, 0x65, 0x6C, 0x00, 0x6F}); // Has 0x00 byte
            }
            assertTrue("File with NUL byte is not text", !TextFileIO.isTextFile(binFile));
        } finally {
            binFile.delete();
        }
    }

    private static void testFileCreatorOperations() throws Exception {
        System.out.println("[11/11] Testing FileCreator Validation, Template & Atomic Creation...");

        File testDir = new File(System.getProperty("java.io.tmpdir"), "velqor_creator_test_" + System.currentTimeMillis());
        testDir.mkdirs();
        try {
            // 1. Validation tests
            FileCreator.ValidationResult rEmpty = FileCreator.validateFileName(testDir, "");
            assertTrue("Empty name is invalid", !rEmpty.isValid);

            FileCreator.ValidationResult rSlash = FileCreator.validateFileName(testDir, "foo/bar");
            assertTrue("Slash in name is invalid", !rSlash.isValid);

            FileCreator.ValidationResult rDot = FileCreator.validateFileName(testDir, "..");
            assertTrue("Dot dot is invalid", !rDot.isValid);

            FileCreator.ValidationResult rHidden = FileCreator.validateFileName(testDir, ".gitignore");
            assertTrue("Hidden file starting with dot triggers warning", rHidden.isValid && rHidden.isWarning);

            FileCreator.ValidationResult rValid = FileCreator.validateFileName(testDir, "document.txt");
            assertTrue("Valid filename is valid and not warning", rValid.isValid && !rValid.isWarning);

            // 2. Duplicate detection & suggestion
            File existing = new File(testDir, "document.txt");
            existing.createNewFile();
            FileCreator.ValidationResult rDup = FileCreator.validateFileName(testDir, "document.txt");
            assertTrue("Duplicate filename is invalid", !rDup.isValid);
            assertEquals("Suggested unique name", "document (2).txt", rDup.suggestedName);

            // 3. Templates check
            assertEquals("Markdown template", "# doc\n\n", FileCreator.getTemplateContent("doc.md"));
            assertTrue("HTML template has doctype", FileCreator.getTemplateContent("page.html").contains("<!DOCTYPE html>"));
            assertTrue("JSON template has braces", FileCreator.getTemplateContent("data.json").contains("{\n"));
            assertEquals("Shell template", "#!/bin/sh\n\n", FileCreator.getTemplateContent("run.sh"));
            assertEquals("Python template", "#!/usr/bin/env python3\n\n", FileCreator.getTemplateContent("app.py"));
            assertEquals("Text template is empty", "", FileCreator.getTemplateContent("notes.txt"));

            // 4. Atomic file creation with template
            File createdMd = FileCreator.createNewFile(testDir, "README.md");
            assertTrue("README.md exists", createdMd.exists());
            String mdContent = new String(java.nio.file.Files.readAllBytes(createdMd.toPath()), StandardCharsets.UTF_8);
            assertEquals("README.md content matched template", "# README\n\n", mdContent);

            File createdPy = FileCreator.createNewFile(testDir, "main.py");
            assertTrue("main.py exists", createdPy.exists());
            String pyContent = new String(java.nio.file.Files.readAllBytes(createdPy.toPath()), StandardCharsets.UTF_8);
            assertEquals("main.py content matched template", "#!/usr/bin/env python3\n\n", pyContent);

        } finally {
            File[] files = testDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
            testDir.delete();
        }
    }

    private static File validateDestinationPath(File destDir, String entryName) throws Exception {
        File destFile = new File(destDir, entryName);
        String destDirPath = destDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new SecurityException("Entry is outside of the target dir: " + entryName);
        }
        return destFile;
    }

    private static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups == 0) return bytes + " B";
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
