package com.velqor.app.files;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class TextFileIO {
    private TextFileIO() {}

    public static final long LARGE_FILE_THRESHOLD = 10L * 1024 * 1024; // 10 MB
    public static final long WARN_FILE_THRESHOLD = 1L * 1024 * 1024;   // 1 MB
    public static final int PAGING_CHUNK_SIZE = 64 * 1024;             // 64 KB per page

    private static final Set<String> TEXT_EXTENSIONS = new HashSet<>(Arrays.asList(
            "txt", "md", "json", "xml", "html", "htm", "css", "js", "java", "kt", "sh",
            "py", "ini", "conf", "properties", "log", "csv", "yml", "yaml", "gradle",
            "c", "cpp", "h", "sql", "svg", "bat", "cmd", "env", "gitattributes", "gitignore"
    ));

    public static class TextDocument {
        public String content;
        public String charsetName;
        public boolean hasBom;
        public String lineEnding; // "\r\n" or "\n"
        public long fileSize;
        public boolean isReadOnlyPaging;
        public int pageIndex;
        public long totalPages;

        public TextDocument(String content, String charsetName, boolean hasBom, String lineEnding, long fileSize, boolean isReadOnlyPaging) {
            this.content = content;
            this.charsetName = charsetName;
            this.hasBom = hasBom;
            this.lineEnding = lineEnding;
            this.fileSize = fileSize;
            this.isReadOnlyPaging = isReadOnlyPaging;
            this.pageIndex = 0;
            this.totalPages = Math.max(1, (fileSize + PAGING_CHUNK_SIZE - 1) / PAGING_CHUNK_SIZE);
        }
    }

    public static boolean isTextFile(File f) {
        if (f == null || !f.exists() || f.isDirectory()) return false;
        String name = f.getName().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            String ext = name.substring(dot + 1);
            if (TEXT_EXTENSIONS.contains(ext)) return true;
        }

        // Byte detection: inspect up to 8 KB for NUL bytes
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] buf = new byte[8192];
            int r = fis.read(buf);
            if (r <= 0) return true; // Empty file is editable text
            for (int i = 0; i < r; i++) {
                if (buf[i] == 0x00) return false; // Contains NUL byte -> binary file
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static TextDocument readTextFile(File f) throws IOException {
        long len = f.length();
        if (len > LARGE_FILE_THRESHOLD) {
            return readPagingChunk(f, 0);
        }

        byte[] rawBytes = new byte[(int) len];
        try (FileInputStream fis = new FileInputStream(f)) {
            int offset = 0;
            int r;
            while (offset < rawBytes.length && (r = fis.read(rawBytes, offset, rawBytes.length - offset)) >= 0) {
                offset += r;
            }
        }

        return parseBytesToDocument(rawBytes, len, false);
    }

    public static TextDocument readPagingChunk(File f, int pageIndex) throws IOException {
        long len = f.length();
        long startOffset = (long) pageIndex * PAGING_CHUNK_SIZE;
        if (startOffset >= len) startOffset = Math.max(0, len - PAGING_CHUNK_SIZE);

        int bytesToRead = (int) Math.min((long) PAGING_CHUNK_SIZE, len - startOffset);
        byte[] rawBytes = new byte[bytesToRead];

        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            raf.seek(startOffset);
            raf.readFully(rawBytes);
        }

        TextDocument doc = parseBytesToDocument(rawBytes, len, true);
        doc.pageIndex = pageIndex;
        return doc;
    }

    private static TextDocument parseBytesToDocument(byte[] rawBytes, long totalFileSize, boolean isPaging) {
        String charsetName = "UTF-8";
        boolean hasBom = false;
        int dataStart = 0;

        // Detect BOM
        if (rawBytes.length >= 3 && rawBytes[0] == (byte) 0xEF && rawBytes[1] == (byte) 0xBB && rawBytes[2] == (byte) 0xBF) {
            charsetName = "UTF-8";
            hasBom = true;
            dataStart = 3;
        } else if (rawBytes.length >= 2 && rawBytes[0] == (byte) 0xFF && rawBytes[1] == (byte) 0xFE) {
            charsetName = "UTF-16LE";
            hasBom = true;
            dataStart = 2;
        } else if (rawBytes.length >= 2 && rawBytes[0] == (byte) 0xFE && rawBytes[1] == (byte) 0xFF) {
            charsetName = "UTF-16BE";
            hasBom = true;
            dataStart = 2;
        } else {
            // Check if valid UTF-8
            if (!isValidUtf8(rawBytes)) {
                charsetName = "Windows-1252";
            }
        }

        Charset cs;
        try {
            cs = Charset.forName(charsetName);
        } catch (Exception e) {
            cs = StandardCharsets.UTF_8;
            charsetName = "UTF-8";
        }

        String content = new String(rawBytes, dataStart, rawBytes.length - dataStart, cs);

        // Detect line ending (\r\n or \n)
        String lineEnding = content.contains("\r\n") ? "\r\n" : "\n";

        return new TextDocument(content, charsetName, hasBom, lineEnding, totalFileSize, isPaging);
    }

    private static boolean isValidUtf8(byte[] input) {
        int i = 0;
        while (i < input.length) {
            int b = input[i] & 0xFF;
            if (b <= 0x7F) {
                i++;
            } else if ((b & 0xE0) == 0xC0) {
                if (i + 1 >= input.length || (input[i + 1] & 0xC0) != 0x80) return false;
                i += 2;
            } else if ((b & 0xF0) == 0xE0) {
                if (i + 2 >= input.length || (input[i + 1] & 0xC0) != 0x80 || (input[i + 2] & 0xC0) != 0x80) return false;
                i += 3;
            } else if ((b & 0xF8) == 0xF0) {
                if (i + 3 >= input.length || (input[i + 1] & 0xC0) != 0x80 || (input[i + 2] & 0xC0) != 0x80 || (input[i + 3] & 0xC0) != 0x80) return false;
                i += 4;
            } else {
                return false;
            }
        }
        return true;
    }

    public static void writeTextFile(File f, String text, TextDocument doc) throws IOException {
        if (f == null) throw new IOException("Tệp đích không hợp lệ");
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        // Atomic save: write to temporary file then rename
        File tempFile = new File(parent, f.getName() + ".tmp_velqor_" + System.currentTimeMillis());

        // Prepare line endings
        String lineEnding = (doc != null && doc.lineEnding != null) ? doc.lineEnding : "\n";
        String normalizedText = text.replace("\r\n", "\n");
        if ("\r\n".equals(lineEnding)) {
            normalizedText = normalizedText.replace("\n", "\r\n");
        }

        String charsetName = (doc != null && doc.charsetName != null) ? doc.charsetName : "UTF-8";
        Charset cs;
        try {
            cs = Charset.forName(charsetName);
        } catch (Exception e) {
            cs = StandardCharsets.UTF_8;
        }

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            // Write BOM if preserved
            if (doc != null && doc.hasBom) {
                if ("UTF-8".equalsIgnoreCase(charsetName)) {
                    fos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
                } else if ("UTF-16LE".equalsIgnoreCase(charsetName)) {
                    fos.write(new byte[]{(byte) 0xFF, (byte) 0xFE});
                } else if ("UTF-16BE".equalsIgnoreCase(charsetName)) {
                    fos.write(new byte[]{(byte) 0xFE, (byte) 0xFF});
                }
            }

            byte[] bodyBytes = normalizedText.getBytes(cs);
            fos.write(bodyBytes);
            fos.flush();
        }

        // Rename atomic replacement
        if (!tempFile.renameTo(f)) {
            // Fallback: Copy and delete
            FileRepository.copy(tempFile, f);
            tempFile.delete();
        }
    }

    public static void updateZipEntryText(File zipFile, String entryPath, String newContent, TextDocument doc) throws Exception {
        if (!zipFile.exists() || !zipFile.canWrite()) {
            throw new IOException("Không có quyền ghi vào tệp ZIP: " + zipFile.getName());
        }

        File tempZip = new File(zipFile.getParentFile(), zipFile.getName() + ".tmp_update_" + System.currentTimeMillis());

        // Normalize text content and encoding
        String lineEnding = (doc != null && doc.lineEnding != null) ? doc.lineEnding : "\n";
        String normalizedText = newContent.replace("\r\n", "\n");
        if ("\r\n".equals(lineEnding)) {
            normalizedText = normalizedText.replace("\n", "\r\n");
        }
        String charsetName = (doc != null && doc.charsetName != null) ? doc.charsetName : "UTF-8";
        Charset cs = Charset.forName(charsetName);

        ByteArrayOutputStream newEntryBytes = new ByteArrayOutputStream();
        if (doc != null && doc.hasBom) {
            if ("UTF-8".equalsIgnoreCase(charsetName)) {
                newEntryBytes.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            } else if ("UTF-16LE".equalsIgnoreCase(charsetName)) {
                newEntryBytes.write(new byte[]{(byte) 0xFF, (byte) 0xFE});
            } else if ("UTF-16BE".equalsIgnoreCase(charsetName)) {
                newEntryBytes.write(new byte[]{(byte) 0xFE, (byte) 0xFF});
            }
        }
        newEntryBytes.write(normalizedText.getBytes(cs));
        byte[] finalEntryPayload = newEntryBytes.toByteArray();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZip))) {

            ZipEntry entry;
            byte[] buf = new byte[32 * 1024];
            boolean updated = false;

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                ZipEntry newEntry = new ZipEntry(name);
                newEntry.setTime(System.currentTimeMillis());

                if (name.equals(entryPath)) {
                    zos.putNextEntry(newEntry);
                    zos.write(finalEntryPayload);
                    zos.closeEntry();
                    updated = true;
                } else {
                    zos.putNextEntry(newEntry);
                    int len;
                    while ((len = zis.read(buf)) > 0) {
                        zos.write(buf, 0, len);
                    }
                    zos.closeEntry();
                }
                zis.closeEntry();
            }

            if (!updated) {
                // If entry was not in zip, append it
                ZipEntry newEntry = new ZipEntry(entryPath);
                zos.putNextEntry(newEntry);
                zos.write(finalEntryPayload);
                zos.closeEntry();
            }
        }

        // Verify tempZip is readable before atomic swap
        try (ZipInputStream verifyZis = new ZipInputStream(new FileInputStream(tempZip))) {
            while (verifyZis.getNextEntry() != null) {
                verifyZis.closeEntry();
            }
        } catch (Exception verifyEx) {
            tempZip.delete();
            throw new IOException("Tệp ZIP cập nhật không toàn vẹn! Giữ nguyên tệp gốc.");
        }

        // Atomic swap
        if (!tempZip.renameTo(zipFile)) {
            FileRepository.copy(tempZip, zipFile);
            tempZip.delete();
        }
    }
}
