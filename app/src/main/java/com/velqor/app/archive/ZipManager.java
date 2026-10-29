package com.velqor.app.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class ZipManager {
    private ZipManager() {}

    public static class ArchiveEntryInfo {
        public final String name;
        public final boolean isDirectory;
        public final long size;
        public final long compressedSize;
        public final long time;

        public ArchiveEntryInfo(String name, boolean isDirectory, long size, long compressedSize, long time) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.size = size;
            this.compressedSize = compressedSize;
            this.time = time;
        }
    }

    public static boolean isArchive(File f) {
        if (f == null || f.isDirectory()) return false;
        String name = f.getName().toLowerCase();
        return name.endsWith(".zip") || name.endsWith(".tar") || name.endsWith(".tar.gz") ||
               name.endsWith(".tgz") || name.endsWith(".7z") || name.endsWith(".rar");
    }

    public static List<ArchiveEntryInfo> listZipEntries(File zipFile) throws Exception {
        List<ArchiveEntryInfo> entries = new ArrayList<>();
        if (zipFile == null || !zipFile.exists()) return entries;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.add(new ArchiveEntryInfo(
                        entry.getName(),
                        entry.isDirectory(),
                        entry.getSize(),
                        entry.getCompressedSize(),
                        entry.getTime()
                ));
                zis.closeEntry();
            }
        }
        return entries;
    }

    public static boolean extractSingleZipEntry(File zipFile, String targetEntryName, File destFile) throws Exception {
        if (zipFile == null || !zipFile.exists() || targetEntryName == null || destFile == null) return false;
        File parent = destFile.getParentFile();
        if (parent != null) parent.mkdirs();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[32 * 1024];
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(targetEntryName)) {
                    try (FileOutputStream fos = new FileOutputStream(destFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    zis.closeEntry();
                    return true;
                }
                zis.closeEntry();
            }
        }
        return false;
    }

    public static void compressFiles(List<File> files, File outZipFile) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outZipFile))) {
            for (File f : files) {
                if (f != null && f.exists()) {
                    addToZip(zos, f, "");
                }
            }
        }
    }

    private static void addToZip(ZipOutputStream zos, File file, String base) throws Exception {
        String entryName = base.isEmpty() ? file.getName() : base + "/" + file.getName();
        if (file.isDirectory()) {
            zos.putNextEntry(new ZipEntry(entryName + "/"));
            zos.closeEntry();
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    addToZip(zos, child, entryName);
                }
            }
        } else {
            zos.putNextEntry(new ZipEntry(entryName));
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[32 * 1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
            }
            zos.closeEntry();
        }
    }

    public static void extractZip(File zipFile, File destDir) throws Exception {
        destDir.mkdirs();
        String destCanonical = destDir.getCanonicalPath();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[32 * 1024];
            while ((entry = zis.getNextEntry()) != null) {
                File destFile = new File(destDir, entry.getName());
                if (!destFile.getCanonicalPath().startsWith(destCanonical)) {
                    throw new SecurityException("Phát hiện lỗi bảo mật Zip Slip: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    destFile.mkdirs();
                } else {
                    File parent = destFile.getParentFile();
                    if (parent != null) parent.mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(destFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }
}
