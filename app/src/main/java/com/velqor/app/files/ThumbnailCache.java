package com.velqor.app.files;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

public final class ThumbnailCache {
    private static ThumbnailCache sInstance;

    private final LruCache<String, Bitmap> mMemoryCache;
    private final File mDiskCacheDir;
    private static final long MAX_DISK_CACHE_BYTES = 50L * 1024 * 1024; 

    public static synchronized ThumbnailCache getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ThumbnailCache(context.getApplicationContext());
        }
        return sInstance;
    }

    private ThumbnailCache(Context context) {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8; 

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        mDiskCacheDir = new File(context.getCacheDir(), "thumbs");
        if (!mDiskCacheDir.exists()) {
            mDiskCacheDir.mkdirs();
        }
    }

    public static String generateKey(File file) {
        if (file == null) return "";
        return "v2_" + file.getAbsolutePath().hashCode() + "_" + file.lastModified() + "_" + file.length();
    }

    public Bitmap get(String key) {
        if (key == null || key.isEmpty()) return null;

        
        Bitmap bm = mMemoryCache.get(key);
        if (bm != null && !bm.isRecycled()) {
            return bm;
        }

        
        File diskFile = new File(mDiskCacheDir, key + ".thumb");
        if (diskFile.exists()) {
            try {
                bm = BitmapFactory.decodeFile(diskFile.getAbsolutePath());
                if (bm != null) {
                    mMemoryCache.put(key, bm);
                    return bm;
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public void put(String key, Bitmap bitmap) {
        if (key == null || bitmap == null || bitmap.isRecycled()) return;

        
        mMemoryCache.put(key, bitmap);

        
        try {
            File diskFile = new File(mDiskCacheDir, key + ".thumb");
            try (FileOutputStream fos = new FileOutputStream(diskFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 85, fos);
                fos.flush();
            }
            trimDiskCache();
        } catch (Throwable ignored) {}
    }

    public void clear() {
        mMemoryCache.evictAll();
        File[] files = mDiskCacheDir.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
    }

    public long getDiskCacheSize() {
        long size = 0;
        File[] files = mDiskCacheDir.listFiles();
        if (files != null) {
            for (File f : files) {
                size += f.length();
            }
        }
        return size;
    }

    private void trimDiskCache() {
        File[] files = mDiskCacheDir.listFiles();
        if (files == null) return;

        long total = 0;
        for (File f : files) total += f.length();

        if (total > MAX_DISK_CACHE_BYTES) {
            
            Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
            for (File f : files) {
                long len = f.length();
                if (f.delete()) {
                    total -= len;
                    if (total <= MAX_DISK_CACHE_BYTES * 0.8) break;
                }
            }
        }
    }
}
