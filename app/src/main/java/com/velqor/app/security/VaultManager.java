package com.velqor.app.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public final class VaultManager {
    private VaultManager() {}

    private static final String MAGIC = "VQR1";
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    public static void encryptAesGcm(File src, File dest, String password) throws Exception {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
        cipher.updateAAD(ByteBuffer.allocate(8).putLong(0).array());

        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dest)) {
            out.write(MAGIC.getBytes());
            out.write(salt);
            out.write(iv);

            byte[] buffer = new byte[64 * 1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                byte[] enc = cipher.update(buffer, 0, len);
                if (enc != null) out.write(enc);
            }
            byte[] finish = cipher.doFinal();
            if (finish != null) out.write(finish);
        }
    }

    public static void decryptAesGcm(File src, File dest, String password) throws Exception {
        try (FileInputStream in = new FileInputStream(src)) {
            byte[] magic = new byte[4];
            if (in.read(magic) != 4 || !new String(magic).equals(MAGIC)) {
                throw new IllegalArgumentException("Định dạng file không phải Vault VQR1 hợp lệ!");
            }

            byte[] salt = new byte[SALT_LENGTH];
            if (in.read(salt) != SALT_LENGTH) throw new IllegalArgumentException("Thiếu Salt bảo mật!");

            byte[] iv = new byte[IV_LENGTH];
            if (in.read(iv) != IV_LENGTH) throw new IllegalArgumentException("Thiếu IV bảo mật!");

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
            SecretKey key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(ByteBuffer.allocate(8).putLong(0).array());

            try (FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buffer = new byte[64 * 1024];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    byte[] dec = cipher.update(buffer, 0, len);
                    if (dec != null) out.write(dec);
                }
                byte[] finish = cipher.doFinal();
                if (finish != null) out.write(finish);
            }
        }
    }
}
