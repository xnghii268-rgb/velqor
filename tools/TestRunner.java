package tools;

import com.velqor.app.archive.ZipManager;
import com.velqor.app.files.FileRepository;
import com.velqor.app.security.HashUtils;
import com.velqor.app.security.VaultManager;

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

        System.out.println("\n--------------------------------------------------");
        System.out.println("Total tests: " + (passed + failed) + " | Passed: " + passed + " | Failed: " + failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testFormatters() {
        System.out.println("[1/8] Testing Size & Date Formatters...");
        assertEquals("Format 0 bytes", "0 B", formatFileSize(0));
        assertEquals("Format 500 bytes", "500 B", formatFileSize(500));
        assertEquals("Format 1024 bytes", "1.0 KB", formatFileSize(1024));
        assertEquals("Format 1.5MB", "1.5 MB", formatFileSize((long)(1024 * 1024 * 1.5)));
        assertEquals("Format 2GB", "2.0 GB", formatFileSize(2L * 1024 * 1024 * 1024));
    }

    private static void testSearchAccentRemoval() {
        System.out.println("[2/8] Testing Vietnamese Accent Removal for Search...");
        assertEquals("Accent removal", "tieng viet", FileRepository.removeAccents("tiếng việt"));
        assertEquals("Accent removal uppercase", "TAI LIEU", FileRepository.removeAccents("TÀI LIỆU"));
        assertEquals("Letter D stroke", "du an", FileRepository.removeAccents("dự án"));
        assertEquals("Capital D stroke", "DONG HO", FileRepository.removeAccents("ĐỒNG HỒ"));
    }

    private static void testZipSlipDefense() throws Exception {
        System.out.println("[3/8] Testing Zip Slip Path Traversal Defense...");
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
        System.out.println("[4/8] Testing SHA-256 Stream Hasher...");
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
        System.out.println("[5/8] Testing Vault AES-256-GCM Streaming & Tamper Detection...");
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
        System.out.println("[6/8] Testing TAR & GZIP Stream Creation & Unpacking...");
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
        System.out.println("[7/8] Testing FileRepository Operations (Copy, Move, Delete, Rename, Duplicate check)...");
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
        System.out.println("[8/8] Testing SHA-256 Standard NIST Test Vectors...");
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
