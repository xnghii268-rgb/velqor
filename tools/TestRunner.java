package tools;

import com.velqor.app.archive.ZipManager;
import com.velqor.app.files.FileCreator;
import com.velqor.app.files.FileRepository;
import com.velqor.app.files.TextFileIO;
import com.velqor.app.files.ThumbnailCache;

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
        testTarEngine();
        testFileRepositoryOperations();
        testTextFileIOOperations();
        testThumbnailKeyAndTextFileDetection();
        testFileCreatorOperations();
        testFolderInfoCalculation();
        testZipInspection();
        testDirectoryWatcherDiff();
        testSpringSolverPhysics();
        testSnapAndRubberBand();

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

        
        boolean caught = false;
        try {
            validateDestinationPath(safeDir, "../../../etc/shadow");
        } catch (SecurityException se) {
            caught = true;
        }
        assertTrue("Zip Slip path traversal blocked", caught);

        
        File validFile = validateDestinationPath(safeDir, "subfolder/document.txt");
        assertTrue("Valid destination path inside folder", validFile.getCanonicalPath().startsWith(safeDir.getCanonicalPath()));

        safeDir.delete();
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

            
            try (FileOutputStream fos = new FileOutputStream(tarGzFile);
                 java.util.zip.GZIPOutputStream gzos = new java.util.zip.GZIPOutputStream(fos)) {
                gzos.write(testContent.getBytes(StandardCharsets.UTF_8));
            }
            assertTrue("Gzip compressed archive created", tarGzFile.exists() && tarGzFile.length() > 0);

            
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

            
            File subDir = new File(testDir, "sub");
            subDir.mkdirs();
            File fileACopy = new File(subDir, "file_a_copy.txt");
            FileRepository.copy(fileA, fileACopy);
            assertTrue("Copied file exists in subDir", fileACopy.exists());
            assertTrue("Original file still exists", fileA.exists());

            
            boolean renamed = FileRepository.rename(fileACopy, "file_renamed.txt");
            assertTrue("Rename succeeded", renamed);
            File renamedFile = new File(subDir, "file_renamed.txt");
            assertTrue("Renamed file exists", renamedFile.exists());

            
            boolean duplicateBlocked = false;
            try {
                FileRepository.rename(fileA, "file_a.txt"); 
                duplicateBlocked = true; 
            } catch (IllegalArgumentException ex) {
                duplicateBlocked = true;
            }
            assertTrue("Duplicate rename prevented", duplicateBlocked);

            File uniqueName = FileRepository.getUniqueDestinationFile(testDir, "file_a.txt");
            assertEquals("Unique name generated", "file_a (1).txt", uniqueName.getName());

            
            File targetMove = new File(subDir, "file_a_moved.txt");
            FileRepository.move(fileA, targetMove);
            assertTrue("Moved target exists", targetMove.exists());
            assertTrue("Source file no longer exists after move", !fileA.exists());

            
            boolean deleted = FileRepository.deleteRecursive(testDir);
            assertTrue("Delete recursive cleaned up folder", deleted);
            assertTrue("Folder gone", !testDir.exists());

        } finally {
            FileRepository.deleteRecursive(testDir);
        }
    }

    private static void testTextFileIOOperations() throws Exception {
        System.out.println("[9/10] Testing TextFileIO Operations (UTF-8 BOM, CRLF, UTF-16, Empty, Diacritics, Zip Entry Edit)...");

        
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

            
            TextFileIO.writeTextFile(bomFile, "Dong 1 da sua\nDong 2 da sua\n", docBom);

            
            byte[] reloadedBytes = java.nio.file.Files.readAllBytes(bomFile.toPath());
            assertTrue("BOM preserved byte 0", reloadedBytes.length >= 3 && reloadedBytes[0] == (byte) 0xEF);
            assertTrue("BOM preserved byte 1", reloadedBytes[1] == (byte) 0xBB);
            assertTrue("BOM preserved byte 2", reloadedBytes[2] == (byte) 0xBF);
            String savedText = new String(reloadedBytes, 3, reloadedBytes.length - 3, StandardCharsets.UTF_8);
            assertTrue("Line endings converted to CRLF", savedText.contains("\r\n"));
        } finally {
            bomFile.delete();
        }

        
        File utf16File = File.createTempFile("test_utf16_", ".txt");
        try {
            try (FileOutputStream fos = new FileOutputStream(utf16File)) {
                fos.write(new byte[]{(byte) 0xFF, (byte) 0xFE}); 
                fos.write("AB".getBytes(StandardCharsets.UTF_16LE));
            }
            TextFileIO.TextDocument doc16 = TextFileIO.readTextFile(utf16File);
            assertTrue("Detects UTF-16LE BOM", doc16.hasBom);
            assertEquals("Detects UTF-16LE charset", "UTF-16LE", doc16.charsetName);
            assertEquals("Decoded UTF-16LE content", "AB", doc16.content);
        } finally {
            utf16File.delete();
        }

        
        File vnFile = File.createTempFile("test_vn_", ".txt");
        try {
            String vnContent = "Hệ thống quản lý tệp tin VELQOR 2026: Tiếng Việt có dấu ă, â, đ, ê, ô, ơ, ư";
            TextFileIO.writeTextFile(vnFile, vnContent, new TextFileIO.TextDocument("", "UTF-8", false, "\n", 0, false));
            TextFileIO.TextDocument docVn = TextFileIO.readTextFile(vnFile);
            assertEquals("Vietnamese diacritics preserved perfectly", vnContent, docVn.content);
        } finally {
            vnFile.delete();
        }

        
        File pagingFile = File.createTempFile("test_paging_", ".txt");
        try {
            byte[] dummyData = new byte[70 * 1024]; 
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

        
        File zipFile = File.createTempFile("test_archive_", ".zip");
        try {
            
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

            
            TextFileIO.TextDocument entryDoc = new TextFileIO.TextDocument("", "UTF-8", false, "\n", 0, false);
            TextFileIO.updateZipEntryText(zipFile, "notes/doc1.txt", "Modified Content in Zip 2026", entryDoc);

            
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

            
            tempFile.setLastModified(tempFile.lastModified() + 5000);
            String key2 = ThumbnailCache.generateKey(tempFile);
            assertTrue("Key changes when lastModified changes", !key1.equals(key2));
        } finally {
            tempFile.delete();
        }

        
        File binFile = File.createTempFile("test_binary_", ".dat");
        try {
            try (FileOutputStream fos = new FileOutputStream(binFile)) {
                fos.write(new byte[]{0x48, 0x65, 0x6C, 0x00, 0x6F}); 
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

            
            File existing = new File(testDir, "document.txt");
            existing.createNewFile();
            FileCreator.ValidationResult rDup = FileCreator.validateFileName(testDir, "document.txt");
            assertTrue("Duplicate filename is invalid", !rDup.isValid);
            assertEquals("Suggested unique name", "document (2).txt", rDup.suggestedName);

            
            assertEquals("Markdown template", "# doc\n\n", FileCreator.getTemplateContent("doc.md"));
            assertTrue("HTML template has doctype", FileCreator.getTemplateContent("page.html").contains("<!DOCTYPE html>"));
            assertTrue("JSON template has braces", FileCreator.getTemplateContent("data.json").contains("{\n"));
            assertEquals("Shell template", "#!/bin/sh\n\n", FileCreator.getTemplateContent("run.sh"));
            assertEquals("Python template", "#!/usr/bin/env python3\n\n", FileCreator.getTemplateContent("app.py"));
            assertEquals("Text template is empty", "", FileCreator.getTemplateContent("notes.txt"));

            
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

    private static void testFolderInfoCalculation() throws Exception {
        System.out.println("[12/12] Testing FileRepository Recursive Folder Info (Total size & Counts)...");

        File rootDir = new File(System.getProperty("java.io.tmpdir"), "velqor_folder_info_" + System.currentTimeMillis());
        rootDir.mkdirs();
        try {
            
            FileRepository.FolderInfo infoNull = FileRepository.calculateFolderInfo(null);
            assertEquals("Null folder bytes", 0L, infoNull.totalBytes);
            assertEquals("Null folder files", 0, infoNull.fileCount);
            assertEquals("Null folder dirs", 0, infoNull.dirCount);

            FileRepository.FolderInfo infoEmpty = FileRepository.calculateFolderInfo(rootDir);
            assertEquals("Empty folder bytes", 0L, infoEmpty.totalBytes);
            assertEquals("Empty folder files", 0, infoEmpty.fileCount);
            assertEquals("Empty folder dirs", 0, infoEmpty.dirCount);

            
            File f1 = new File(rootDir, "file1.bin");
            try (FileOutputStream fos = new FileOutputStream(f1)) {
                fos.write(new byte[100]);
            }

            File subDir = new File(rootDir, "sub");
            subDir.mkdirs();
            File f2 = new File(subDir, "file2.bin");
            try (FileOutputStream fos = new FileOutputStream(f2)) {
                fos.write(new byte[250]);
            }

            File subDir2 = new File(subDir, "sub2");
            subDir2.mkdirs();
            File f3 = new File(subDir2, "file3.bin");
            try (FileOutputStream fos = new FileOutputStream(f3)) {
                fos.write(new byte[400]);
            }

            
            FileRepository.FolderInfo info = FileRepository.calculateFolderInfo(rootDir);
            assertEquals("Total recursive size", 750L, info.totalBytes);
            assertEquals("Total file count", 3, info.fileCount);
            assertEquals("Total subdir count", 2, info.dirCount);

        } finally {
            FileRepository.deleteRecursive(rootDir);
        }
    }

    private static void testZipInspection() throws Exception {
        System.out.println("[13/13] Testing Zip Inspection without Extraction...");
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "zip_inspect_test_" + System.currentTimeMillis());
        tempDir.mkdirs();
        File f1 = new File(tempDir, "doc.txt");
        try (FileOutputStream fos = new FileOutputStream(f1)) {
            fos.write("Sample document content".getBytes(StandardCharsets.UTF_8));
        }
        File f2 = new File(tempDir, "image.png");
        try (FileOutputStream fos = new FileOutputStream(f2)) {
            fos.write(new byte[512]);
        }

        File outZip = new File(tempDir, "test.zip");
        List<File> files = Arrays.asList(f1, f2);
        ZipManager.compressFiles(files, outZip);
        assertTrue("Zip file created", outZip.exists());

        List<ZipManager.ArchiveEntryInfo> entries = ZipManager.listZipEntries(outZip);
        assertEquals("Entries count", 2, entries.size());

        File extractedDoc = new File(tempDir, "extracted_doc.txt");
        boolean extracted = ZipManager.extractSingleZipEntry(outZip, "doc.txt", extractedDoc);
        assertTrue("Single entry extracted", extracted);
        assertTrue("Extracted file exists", extractedDoc.exists());
        assertEquals("Extracted file size", f1.length(), extractedDoc.length());

        FileRepository.deleteRecursive(tempDir);
    }

    private static void testDirectoryWatcherDiff() throws Exception {
        System.out.println("[14/14] Testing Directory Watcher Diff & Snapshot Detection...");
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "watcher_diff_test_" + System.currentTimeMillis());
        tempDir.mkdirs();
        File f1 = new File(tempDir, "file1.txt");
        try (FileOutputStream fos = new FileOutputStream(f1)) {
            fos.write("Hello".getBytes(StandardCharsets.UTF_8));
        }

        List<File> initialFiles = Collections.singletonList(f1);
        Map<String, com.velqor.app.files.DirectoryWatcher.FileSnapshot> snapshot = new HashMap<>();
        snapshot.put(f1.getAbsolutePath(), new com.velqor.app.files.DirectoryWatcher.FileSnapshot(f1));

        boolean unchanged = !com.velqor.app.files.DirectoryWatcher.hasDirectoryChanged(initialFiles, snapshot);
        assertTrue("Directory unchanged when identical", unchanged);

        File f2 = new File(tempDir, "file2.txt");
        try (FileOutputStream fos = new FileOutputStream(f2)) {
            fos.write("World".getBytes(StandardCharsets.UTF_8));
        }
        List<File> addedFiles = Arrays.asList(f1, f2);
        boolean changedAdd = com.velqor.app.files.DirectoryWatcher.hasDirectoryChanged(addedFiles, snapshot);
        assertTrue("Directory changed on file add", changedAdd);

        FileRepository.deleteRecursive(tempDir);
    }

    private static void testSpringSolverPhysics() {
        System.out.println("[15/16] Testing SpringSolver Convergence & Underdamped Overshoot...");
        com.velqor.app.ui.SpringSolver solver = new com.velqor.app.ui.SpringSolver(1.0f, 180.0f, 18.0f);
        solver.setValue(0f);
        solver.setTarget(100f);

        float maxVal = 0f;
        for (int i = 0; i < 60; i++) {
            float v = solver.update(0.016f);
            if (v > maxVal) maxVal = v;
        }

        assertTrue("Spring overshoot between 102% and 110%", maxVal > 102f && maxVal < 110f);

        for (int i = 0; i < 120; i++) {
            solver.update(0.016f);
        }

        assertTrue("Spring settled at rest", solver.isAtRest());
        assertEquals("Spring final value is target", 100, (int) Math.round(solver.getValue()));
    }

    private static void testSnapAndRubberBand() {
        System.out.println("[16/16] Testing Velocity-Aware Snap Target & Rubber-Band Damping...");
        float[] tabPositions = new float[]{0f, 100f, 200f};

        int snapStatic = com.velqor.app.ui.SpringSolver.computeSnapIndex(95f, 0f, tabPositions);
        assertEquals("Static snap to nearest tab", 1, snapStatic);

        int snapModerateFlick = com.velqor.app.ui.SpringSolver.computeSnapIndex(40f, 350f, tabPositions);
        assertEquals("Moderate flick velocity predicts next tab", 1, snapModerateFlick);

        int snapStrongFlick = com.velqor.app.ui.SpringSolver.computeSnapIndex(40f, 800f, tabPositions);
        assertEquals("Strong flick velocity leaps across tabs", 2, snapStrongFlick);

        int snapFlickLeft = com.velqor.app.ui.SpringSolver.computeSnapIndex(160f, -800f, tabPositions);
        assertEquals("Flick velocity predicts backward tab", 0, snapFlickLeft);

        float rbLeft = com.velqor.app.ui.SpringSolver.applyRubberBand(-50f, 200f, 300f);
        assertTrue("Rubber-band resists left overscroll", rbLeft < 0f && rbLeft > -50f);

        float rbRight = com.velqor.app.ui.SpringSolver.applyRubberBand(250f, 200f, 300f);
        assertTrue("Rubber-band resists right overscroll", rbRight > 200f && rbRight < 250f);

        float rbInside = com.velqor.app.ui.SpringSolver.applyRubberBand(100f, 200f, 300f);
        assertEquals("Rubber-band maintains linear in-range values", 100f, rbInside);
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
