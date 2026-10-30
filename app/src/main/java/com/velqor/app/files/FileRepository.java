package com.velqor.app.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class FileRepository {
    private FileRepository() {}

    public interface OperationCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public static List<File> listFiles(File dir, boolean showHidden, int sortField, boolean sortAsc) {
        if (dir == null) return Collections.emptyList();
        if (!WirelessDebugManager.isAndroidDataPath(dir) && !dir.exists()) return Collections.emptyList();
        File[] filesArr;
        if (WirelessDebugManager.isAndroidDataPath(dir)) {
            filesArr = WirelessDebugManager.listAndroidDataArray(dir);
            if (filesArr == null || filesArr.length == 0) {
                filesArr = dir.listFiles();
            }
        } else {
            filesArr = dir.listFiles();
        }
        if (filesArr == null || filesArr.length == 0) return Collections.emptyList();

        List<File> files = new ArrayList<>();
        for (File f : filesArr) {
            if (showHidden || !f.getName().startsWith(".")) {
                files.add(f);
            }
        }

        Collections.sort(files, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;

            int res = 0;
            if (sortField == 0) {
                res = a.getName().compareToIgnoreCase(b.getName());
            } else if (sortField == 1) {
                res = Long.compare(a.lastModified(), b.lastModified());
            } else if (sortField == 2) {
                res = Long.compare(a.length(), b.length());
            } else if (sortField == 3) {
                res = getFileExtension(a).compareToIgnoreCase(getFileExtension(b));
            }
            return sortAsc ? res : -res;
        });

        return files;
    }

    public static String getFileExtension(File f) {
        if (f == null || f.isDirectory()) return "";
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1) : "";
    }

    public static File getUniqueDestinationFile(File parent, String desiredName) {
        File target = new File(parent, desiredName);
        if (!target.exists()) return target;

        String name = desiredName;
        String ext = "";
        int dot = desiredName.lastIndexOf('.');
        if (dot > 0) {
            name = desiredName.substring(0, dot);
            ext = desiredName.substring(dot);
        }

        int count = 1;
        while (target.exists()) {
            target = new File(parent, name + " (" + count + ")" + ext);
            count++;
        }
        return target;
    }

    public static boolean deleteRecursive(File f) {
        if (f == null) return true;
        if (WirelessDebugManager.isAndroidDataPath(f) && WirelessDebugManager.isShizukuPermissionGranted()) {
            return WirelessDebugManager.deleteViaShizuku(f);
        }
        if (!f.exists()) return true;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return f.delete();
    }

    public static boolean clearDirectoryContents(File dir) {
        if (dir == null) return false;
        if (WirelessDebugManager.isAndroidDataPath(dir) && WirelessDebugManager.isShizukuPermissionGranted()) {
            WirelessDebugManager.runShizukuShellCommand("rm -rf \"" + dir.getAbsolutePath() + "\"/*");
            return true;
        }
        if (!dir.exists() || !dir.isDirectory()) return false;
        File[] children = dir.listFiles();
        boolean success = true;
        if (children != null) {
            for (File child : children) {
                if (!deleteRecursive(child)) {
                    success = false;
                }
            }
        }
        return success;
    }

    public static void copy(File src, File dest) throws IOException {
        if (src == null || dest == null) return;
        if ((WirelessDebugManager.isAndroidDataPath(src) || WirelessDebugManager.isAndroidDataPath(dest)) &&
                WirelessDebugManager.isShizukuPermissionGranted()) {
            WirelessDebugManager.copyViaShizuku(src, dest);
            return;
        }
        if (src.isDirectory()) {
            dest.mkdirs();
            File[] children = src.listFiles();
            if (children != null) {
                for (File child : children) {
                    copy(child, new File(dest, child.getName()));
                }
            }
        } else {
            File parent = dest.getParentFile();
            if (parent != null) parent.mkdirs();
            try (FileInputStream in = new FileInputStream(src);
                 FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[64 * 1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    public static void move(File src, File dest) throws IOException {
        if (src == null || dest == null) return;
        if ((WirelessDebugManager.isAndroidDataPath(src) || WirelessDebugManager.isAndroidDataPath(dest)) &&
                WirelessDebugManager.isShizukuPermissionGranted()) {
            WirelessDebugManager.moveViaShizuku(src, dest);
            return;
        }
        if (src.renameTo(dest)) return;

        copy(src, dest);
        deleteRecursive(src);
    }

    public static boolean rename(File src, String newName) throws IllegalArgumentException {
        if (src == null) return false;
        if (WirelessDebugManager.isAndroidDataPath(src) && WirelessDebugManager.isShizukuPermissionGranted()) {
            return WirelessDebugManager.renameViaShizuku(src, newName);
        }
        if (!src.exists()) return false;
        File target = new File(src.getParentFile(), newName);
        if (target.exists()) {
            throw new IllegalArgumentException("Tên '" + newName + "' đã tồn tại trong thư mục này!");
        }
        return src.renameTo(target);
    }

    public static boolean createDirectory(File parent, String dirName) throws IllegalArgumentException {
        if (parent == null || dirName == null || dirName.isEmpty()) return false;
        if (WirelessDebugManager.isAndroidDataPath(parent) && WirelessDebugManager.isShizukuPermissionGranted()) {
            return WirelessDebugManager.createDirectoryViaShizuku(parent, dirName);
        }
        File target = new File(parent, dirName);
        if (target.exists()) {
            throw new IllegalArgumentException("Thư mục '" + dirName + "' đã tồn tại!");
        }
        return target.mkdirs();
    }

    public static String removeAccents(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("")
                .replace("đ", "d").replace("Đ", "D");
    }

    public static List<File> search(File root, String query, int maxResults) {
        List<File> results = new ArrayList<>();
        if (root == null || !root.exists() || query == null || query.trim().isEmpty()) {
            return results;
        }

        String q = removeAccents(query.trim().toLowerCase());
        searchRecursive(root, q, results, maxResults);
        return results;
    }

    private static void searchRecursive(File current, String query, List<File> results, int maxResults) {
        if (results.size() >= maxResults) return;
        File[] list;
        if (WirelessDebugManager.isAndroidDataPath(current)) {
            list = WirelessDebugManager.listAndroidDataArray(current);
        } else {
            list = current.listFiles();
        }
        if (list == null) return;

        for (File f : list) {
            if (results.size() >= maxResults) return;
            String normalizedName = removeAccents(f.getName().toLowerCase());
            if (normalizedName.contains(query)) {
                results.add(f);
            }
            if (f.isDirectory() && !WirelessDebugManager.isAndroidDataPath(f)) {
                searchRecursive(f, query, results, maxResults);
            }
        }
    }

    public static class FolderInfo {
        public long totalBytes = 0;
        public int fileCount = 0;
        public int dirCount = 0;
    }

    public static FolderInfo calculateFolderInfo(File dir) {
        FolderInfo info = new FolderInfo();
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return info;
        }
        calculateFolderInfoRecursive(dir, info);
        return info;
    }

    private static void calculateFolderInfoRecursive(File dir, FolderInfo info) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                info.dirCount++;
                calculateFolderInfoRecursive(child, info);
            } else {
                info.fileCount++;
                info.totalBytes += child.length();
            }
        }
    }
}
