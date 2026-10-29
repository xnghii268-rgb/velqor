package com.velqor.app.security;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

public final class HashUtils {
    private HashUtils() {}

    public static String computeSha256(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] buf = new byte[64 * 1024];
            int r;
            while ((r = fis.read(buf)) > 0) {
                md.update(buf, 0, r);
            }
        }
        return toHexString(md.digest());
    }

    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
