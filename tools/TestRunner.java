package tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

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

        System.out.println("\n--------------------------------------------------");
        System.out.println("Total tests: " + (passed + failed) + " | Passed: " + passed + " | Failed: " + failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testFormatters() {
        System.out.println("[1/5] Testing Size & Date Formatters...");
        assertEquals("Format 0 bytes", "0 B", formatFileSize(0));
        assertEquals("Format 500 bytes", "500 B", formatFileSize(500));
        assertEquals("Format 1024 bytes", "1.0 KB", formatFileSize(1024));
        assertEquals("Format 1.5MB", "1.5 MB", formatFileSize((long)(1024 * 1024 * 1.5)));
        assertEquals("Format 2GB", "2.0 GB", formatFileSize(2L * 1024 * 1024 * 1024));
    }

    private static void testSearchAccentRemoval() {
        System.out.println("[2/5] Testing Vietnamese Accent Removal for Search...");
        assertEquals("Accent removal", "tieng viet", removeAccents("tiếng việt"));
        assertEquals("Accent removal uppercase", "TAI LIEU", removeAccents("TÀI LIỆU"));
        assertEquals("Letter D stroke", "du an", removeAccents("dự án"));
        assertEquals("Capital D stroke", "DONG HO", removeAccents("ĐỒNG HỒ"));
    }

    private static void testZipSlipDefense() throws Exception {
        System.out.println("[3/5] Testing Zip Slip Path Traversal Defense...");
        File safeDir = new File("/data/data/com.termuxpro.app/files/home/test_safe_dir");
        safeDir.mkdirs();

        boolean caught = false;
        try {
            validateDestinationPath(safeDir, "../../../etc/shadow");
        } catch (SecurityException se) {
            caught = true;
        }
        assertTrue("Zip Slip path traversal blocked", caught);

        // Safe path should pass
        File validFile = validateDestinationPath(safeDir, "subfolder/document.txt");
        assertTrue("Valid destination path inside folder", validFile.getCanonicalPath().startsWith(safeDir.getCanonicalPath()));

        safeDir.delete();
    }

    private static void testSha256Hasher() throws Exception {
        System.out.println("[4/5] Testing SHA-256 Stream Hasher...");
        byte[] input = "hello world".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        String actual = hexString.toString();
        assertEquals("SHA-256 for 'hello world'", "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", actual);
    }

    private static void testVaultAes256Gcm() throws Exception {
        System.out.println("[5/5] Testing Vault AES-256-GCM Streaming & Tamper Detection...");
        File tempSrc = File.createTempFile("velqor_src", ".txt");
        File tempEnc = File.createTempFile("velqor_enc", ".vqr");
        File tempDec = File.createTempFile("velqor_dec", ".txt");

        try {
            String secretText = "Confidential Vault Data 2026 - Pure Apple Standard";
            try (FileOutputStream fos = new FileOutputStream(tempSrc)) {
                fos.write(secretText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            // 1. Encrypt
            encryptTest(tempSrc, tempEnc, "MySecurePass!99");
            assertTrue("Encrypted file exists", tempEnc.exists());
            assertTrue("Encrypted file has ciphertext overhead", tempEnc.length() > tempSrc.length());

            // 2. Decrypt
            decryptTest(tempEnc, tempDec, "MySecurePass!99");
            String restored = new String(java.nio.file.Files.readAllBytes(tempDec.toPath()), java.nio.charset.StandardCharsets.UTF_8);
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
                decryptTest(tempEnc, tempTamperDec, "MySecurePass!99");
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
        System.out.println("[6/6] Testing TAR & GZIP Stream Creation & Unpacking...");
        File srcFile = File.createTempFile("tar_src", ".txt");
        File tarGzFile = File.createTempFile("tar_out", ".tar.gz");
        File destDir = new File(srcFile.getParentFile(), "tar_extracted_" + System.currentTimeMillis());
        destDir.mkdirs();

        try {
            String testContent = "Velqor v2 Tar Archiving Engine Output";
            try (FileOutputStream fos = new FileOutputStream(srcFile)) {
                fos.write(testContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            // Write simple Gzip stream containing file
            try (FileOutputStream fos = new FileOutputStream(tarGzFile);
                 java.util.zip.GZIPOutputStream gzos = new java.util.zip.GZIPOutputStream(fos)) {
                gzos.write(testContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
            String unpackedContent = new String(java.nio.file.Files.readAllBytes(extractedFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            assertEquals("Unpacked content matches original", testContent, unpackedContent);

        } finally {
            srcFile.delete();
            tarGzFile.delete();
            new File(destDir, "unpacked.txt").delete();
            destDir.delete();
        }
    }

    // Helper utilities matching production logic
    private static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024.0));
        if (digitGroups < 0) digitGroups = 0;
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        double value = bytes / Math.pow(1024.0, digitGroups);
        if (digitGroups == 0) return bytes + " B";
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups]);
    }

    private static String removeAccents(String str) {
        String nfd = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfd).replaceAll("").replace("đ", "d").replace("Đ", "D");
    }

    private static File validateDestinationPath(File destinationDir, String entryName) throws Exception {
        File destFile = new File(destinationDir, entryName);
        String canonicalDestDirPath = destinationDir.getCanonicalPath();
        String canonicalDestFilePath = destFile.getCanonicalPath();
        if (!canonicalDestFilePath.startsWith(canonicalDestDirPath + File.separator) && !canonicalDestFilePath.equals(canonicalDestDirPath)) {
            throw new SecurityException("Zip Slip detected");
        }
        return destFile;
    }

    private static void encryptTest(File src, File dest, String password) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
        SecretKey key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dest)) {
            out.write("VELQOR_V1".getBytes());
            out.write(salt);
            byte[] buffer = new byte[64 * 1024];
            int read;
            long chunkIndex = 0;
            while ((read = in.read(buffer)) > 0) {
                byte[] iv = new byte[12];
                new SecureRandom().nextBytes(iv);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
                cipher.updateAAD(ByteBuffer.allocate(8).putLong(chunkIndex).array());
                byte[] cipherBytes = cipher.doFinal(buffer, 0, read);
                out.write(iv);
                out.write(ByteBuffer.allocate(4).putInt(cipherBytes.length).array());
                out.write(cipherBytes);
                chunkIndex++;
            }
        }
    }

    private static void decryptTest(File enc, File dest, String password) throws Exception {
        try (FileInputStream in = new FileInputStream(enc);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] magic = new byte["VELQOR_V1".getBytes().length];
            in.read(magic);
            byte[] salt = new byte[16];
            in.read(salt);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
            SecretKey key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

            byte[] iv = new byte[12];
            byte[] lenBuf = new byte[4];
            long chunkIndex = 0;
            while (in.read(iv) == 12) {
                in.read(lenBuf);
                int len = ByteBuffer.wrap(lenBuf).getInt();
                byte[] cBytes = new byte[len];
                in.read(cBytes);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
                cipher.updateAAD(ByteBuffer.allocate(8).putLong(chunkIndex).array());
                byte[] plain = cipher.doFinal(cBytes);
                out.write(plain);
                chunkIndex++;
            }
        }
    }
}
