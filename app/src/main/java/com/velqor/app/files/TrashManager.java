package com.velqor.app.files;

import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class TrashManager {
    private static final String TRASH_DIR_NAME = ".velqor_trash";

    private TrashManager() {}

    public static File getTrashDirectory(Context context) {
        File trash = new File(context.getFilesDir(), TRASH_DIR_NAME);
        if (!trash.exists()) {
            trash.mkdirs();
        }
        return trash;
    }

    public static boolean moveToTrash(Context context, File target) {
        if (target == null || !target.exists()) return false;
        File trashDir = getTrashDirectory(context);
        String originalName = target.getName();
        long timestamp = System.currentTimeMillis();
        String trashName = timestamp + "_" + originalName;
        File dest = new File(trashDir, trashName);
        return target.renameTo(dest);
    }

    public static List<TrashItem> listTrashItems(Context context) {
        List<TrashItem> items = new ArrayList<>();
        File trashDir = getTrashDirectory(context);
        File[] files = trashDir.listFiles();
        if (files == null) return items;
        for (File f : files) {
            String name = f.getName();
            int idx = name.indexOf('_');
            if (idx > 0) {
                try {
                    long time = Long.parseLong(name.substring(0, idx));
                    String origName = name.substring(idx + 1);
                    items.add(new TrashItem(f, origName, time, f.length()));
                } catch (NumberFormatException ignored) {
                    items.add(new TrashItem(f, name, f.lastModified(), f.length()));
                }
            } else {
                items.add(new TrashItem(f, name, f.lastModified(), f.length()));
            }
        }
        return items;
    }

    public static boolean restoreItem(TrashItem item, File restoreDir) {
        if (item == null || !item.file.exists() || restoreDir == null) return false;
        if (!restoreDir.exists()) restoreDir.mkdirs();
        File dest = new File(restoreDir, item.originalName);
        if (dest.exists()) {
            dest = new File(restoreDir, "Restored_" + item.originalName);
        }
        return item.file.renameTo(dest);
    }

    public static void emptyTrash(Context context) {
        File trashDir = getTrashDirectory(context);
        File[] files = trashDir.listFiles();
        if (files != null) {
            for (File f : files) {
                FileRepository.deleteRecursive(f);
            }
        }
    }

    public static long getTrashSize(Context context) {
        File trashDir = getTrashDirectory(context);
        return calculateSize(trashDir);
    }

    private static long calculateSize(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    size += calculateSize(f);
                } else {
                    size += f.length();
                }
            }
        }
        return size;
    }

    public static class TrashItem {
        public final File file;
        public final String originalName;
        public final long deletedTimestamp;
        public final long size;

        public TrashItem(File file, String originalName, long deletedTimestamp, long size) {
            this.file = file;
            this.originalName = originalName;
            this.deletedTimestamp = deletedTimestamp;
            this.size = size;
        }
    }
}
