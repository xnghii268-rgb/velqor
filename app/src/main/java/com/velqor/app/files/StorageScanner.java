package com.velqor.app.files;

import android.os.Environment;
import android.os.StatFs;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StorageScanner {

    public static class StorageBreakdown {
        public long totalBytes;
        public long freeBytes;
        public long usedBytes;
        public long imagesBytes;
        public long videosBytes;
        public long audioBytes;
        public long docsBytes;
        public long apksBytes;
        public long archivesBytes;
        public long otherBytes;
        public final List<File> largeFiles = new ArrayList<>();
        public final List<FileDuplicateGroup> duplicateGroups = new ArrayList<>();
    }

    public static class FileDuplicateGroup {
        public final long size;
        public final List<File> files = new ArrayList<>();

        public FileDuplicateGroup(long size) {
            this.size = size;
        }
    }

    private StorageScanner() {}

    public static StorageBreakdown scanStorage(File rootDir, int maxFilesToScan) {
        StorageBreakdown breakdown = new StorageBreakdown();
        if (rootDir == null) rootDir = Environment.getExternalStorageDirectory();

        try {
            StatFs stat = new StatFs(rootDir.getAbsolutePath());
            breakdown.totalBytes = stat.getTotalBytes();
            breakdown.freeBytes = stat.getAvailableBytes();
            breakdown.usedBytes = breakdown.totalBytes - breakdown.freeBytes;
        } catch (Throwable ignored) {
            breakdown.totalBytes = 64L * 1024 * 1024 * 1024;
            breakdown.freeBytes = 20L * 1024 * 1024 * 1024;
            breakdown.usedBytes = breakdown.totalBytes - breakdown.freeBytes;
        }

        Map<Long, List<File>> sizeMap = new HashMap<>();
        List<File> allFiles = new ArrayList<>();
        collectFiles(rootDir, allFiles, maxFilesToScan);

        long classifiedBytes = 0;
        for (File f : allFiles) {
            long len = f.length();
            if (len >= 50L * 1024 * 1024) {
                breakdown.largeFiles.add(f);
            }
            if (len > 1024 * 10) {
                List<File> bucket = sizeMap.get(len);
                if (bucket == null) {
                    bucket = new ArrayList<>();
                    sizeMap.put(len, bucket);
                }
                bucket.add(f);
            }

            String name = f.getName().toLowerCase();
            if (isImage(name)) {
                breakdown.imagesBytes += len;
                classifiedBytes += len;
            } else if (isVideo(name)) {
                breakdown.videosBytes += len;
                classifiedBytes += len;
            } else if (isAudio(name)) {
                breakdown.audioBytes += len;
                classifiedBytes += len;
            } else if (isDoc(name)) {
                breakdown.docsBytes += len;
                classifiedBytes += len;
            } else if (isApk(name)) {
                breakdown.apksBytes += len;
                classifiedBytes += len;
            } else if (isArchive(name)) {
                breakdown.archivesBytes += len;
                classifiedBytes += len;
            }
        }

        breakdown.otherBytes = Math.max(0, breakdown.usedBytes - classifiedBytes);

        Collections.sort(breakdown.largeFiles, (f1, f2) -> Long.compare(f2.length(), f1.length()));
        if (breakdown.largeFiles.size() > 50) {
            breakdown.largeFiles.subList(50, breakdown.largeFiles.size()).clear();
        }

        for (Map.Entry<Long, List<File>> entry : sizeMap.entrySet()) {
            List<File> list = entry.getValue();
            if (list.size() > 1) {
                Map<String, List<File>> hashBucket = new HashMap<>();
                for (File candidate : list) {
                    String prefixKey = readFirst8KBHash(candidate);
                    List<File> hList = hashBucket.get(prefixKey);
                    if (hList == null) {
                        hList = new ArrayList<>();
                        hashBucket.put(prefixKey, hList);
                    }
                    hList.add(candidate);
                }
                for (List<File> dups : hashBucket.values()) {
                    if (dups.size() > 1) {
                        FileDuplicateGroup g = new FileDuplicateGroup(entry.getKey());
                        g.files.addAll(dups);
                        breakdown.duplicateGroups.add(g);
                    }
                }
            }
        }

        return breakdown;
    }

    private static void collectFiles(File dir, List<File> result, int max) {
        if (dir == null || result.size() >= max) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (result.size() >= max) break;
            if (child.isDirectory()) {
                if (!child.getName().startsWith(".") && !child.getName().equals("Android")) {
                    collectFiles(child, result, max);
                }
            } else {
                result.add(child);
            }
        }
    }

    private static String readFirst8KBHash(File f) {
        byte[] buf = new byte[8192];
        int read = 0;
        try (InputStream is = new FileInputStream(f)) {
            read = is.read(buf);
        } catch (Throwable ignored) {}
        if (read <= 0) return "empty_" + f.length();
        return Arrays.hashCode(Arrays.copyOf(buf, read)) + "_" + read;
    }

    private static boolean isImage(String name) {
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
               name.endsWith(".webp") || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".heic");
    }

    private static boolean isVideo(String name) {
        return name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") ||
               name.endsWith(".mov") || name.endsWith(".webm") || name.endsWith(".flv");
    }

    private static boolean isAudio(String name) {
        return name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".wav") ||
               name.endsWith(".ogg") || name.endsWith(".m4a") || name.endsWith(".aac");
    }

    private static boolean isDoc(String name) {
        return name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx") ||
               name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".ppt") ||
               name.endsWith(".pptx") || name.endsWith(".txt") || name.endsWith(".epub");
    }

    private static boolean isApk(String name) {
        return name.endsWith(".apk") || name.endsWith(".xapk") || name.endsWith(".apks");
    }

    private static boolean isArchive(String name) {
        return name.endsWith(".zip") || name.endsWith(".tar") || name.endsWith(".gz") ||
               name.endsWith(".tgz") || name.endsWith(".7z") || name.endsWith(".rar") || name.endsWith(".bz2");
    }
}
