package com.velqor.app.ui;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfRenderer;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Size;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;

import com.velqor.app.files.ThumbnailCache;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ThumbnailLoader {
    private static ThumbnailLoader sInstance;
    private final Context mContext;
    private final ThumbnailCache mCache;
    private final ExecutorService mExecutor;
    private final Handler mMainHandler;

    public static synchronized ThumbnailLoader getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ThumbnailLoader(context.getApplicationContext());
        }
        return sInstance;
    }

    private ThumbnailLoader(Context context) {
        mContext = context;
        mCache = ThumbnailCache.getInstance(context);
        mExecutor = Executors.newFixedThreadPool(3);
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    public void loadThumbnail(File file, ImageView targetView) {
        if (file == null || targetView == null) return;

        String key = ThumbnailCache.generateKey(file);
        targetView.setTag(key);

        // Fast memory/disk cache check
        Bitmap cached = mCache.get(key);
        if (cached != null) {
            targetView.setImageBitmap(cached);
            return;
        }

        // Asynchronous decode
        mExecutor.execute(() -> {
            Bitmap result = null;
            try {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".apk")) {
                    result = decodeApkIcon(file);
                } else if (isImageFile(name)) {
                    result = decodeSampledImage(file, 128, 128);
                } else if (isVideoFile(name)) {
                    result = decodeVideoThumbnail(file);
                } else if (isAudioFile(name)) {
                    result = decodeAudioThumbnail(file);
                } else if (name.endsWith(".pdf") && file.length() < 50L * 1024 * 1024) {
                    result = decodePdfThumbnail(file);
                }
            } catch (Throwable ignored) {
                result = null;
            }

            final Bitmap finalBitmap = result;
            if (finalBitmap != null) {
                mCache.put(key, finalBitmap);
                mMainHandler.post(() -> {
                    // Check tag to avoid race conditions when views are recycled
                    if (key.equals(targetView.getTag())) {
                        targetView.setImageBitmap(finalBitmap);
                        AlphaAnimation anim = new AlphaAnimation(0.2f, 1.0f);
                        anim.setDuration(150);
                        targetView.startAnimation(anim);
                    }
                });
            }
        });
    }

    private Bitmap decodeApkIcon(File file) {
        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo pi = pm.getPackageArchiveInfo(file.getAbsolutePath(), 0);
            if (pi != null) {
                pi.applicationInfo.sourceDir = file.getAbsolutePath();
                pi.applicationInfo.publicSourceDir = file.getAbsolutePath();
                Drawable iconDrawable = pi.applicationInfo.loadIcon(pm);
                if (iconDrawable != null) {
                    Bitmap bm = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bm);
                    iconDrawable.setBounds(0, 0, 96, 96);
                    iconDrawable.draw(canvas);
                    return bm;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private Bitmap decodeSampledImage(File file, int reqW, int reqH) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), opts);

            opts.inSampleSize = calculateInSampleSize(opts, reqW, reqH);
            opts.inJustDecodeBounds = false;

            Bitmap bm = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
            if (bm == null) return null;

            // Correct EXIF orientation
            try {
                ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int angle = 0;
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90) angle = 90;
                else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) angle = 180;
                else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) angle = 270;

                if (angle != 0) {
                    Matrix m = new Matrix();
                    m.postRotate(angle);
                    Bitmap rotated = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
                    if (rotated != bm) {
                        bm.recycle();
                        bm = rotated;
                    }
                }
            } catch (Throwable ignored) {}

            return bm;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Bitmap decodeVideoThumbnail(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                return ThumbnailUtils.createVideoThumbnail(file, new Size(128, 128), null);
            } catch (Throwable ignored) {}
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());
            Bitmap frame = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (frame != null) {
                return Bitmap.createScaledBitmap(frame, 128, 128, true);
            }
        } catch (Throwable ignored) {
        } finally {
            try {
                retriever.release();
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private Bitmap decodeAudioThumbnail(File file) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());
            byte[] picture = retriever.getEmbeddedPicture();
            if (picture != null) {
                return BitmapFactory.decodeByteArray(picture, 0, picture.length);
            }
        } catch (Throwable ignored) {
        } finally {
            try {
                retriever.release();
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private Bitmap decodePdfThumbnail(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ParcelFileDescriptor pfd = null;
            PdfRenderer renderer = null;
            PdfRenderer.Page page = null;
            try {
                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
                renderer = new PdfRenderer(pfd);
                if (renderer.getPageCount() > 0) {
                    page = renderer.openPage(0);
                    Bitmap bm = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
                    page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    return bm;
                }
            } catch (Throwable ignored) {
            } finally {
                if (page != null) {
                    try { page.close(); } catch (Throwable ignored) {}
                }
                if (renderer != null) {
                    try { renderer.close(); } catch (Throwable ignored) {}
                }
                if (pfd != null) {
                    try { pfd.close(); } catch (Throwable ignored) {}
                }
            }
        }
        return null;
    }

    private static boolean isImageFile(String name) {
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
               name.endsWith(".webp") || name.endsWith(".gif") || name.endsWith(".bmp");
    }

    private static boolean isVideoFile(String name) {
        return name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") ||
               name.endsWith(".mov") || name.endsWith(".3gp") || name.endsWith(".webm");
    }

    private static boolean isAudioFile(String name) {
        return name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".wav") ||
               name.endsWith(".ogg") || name.endsWith(".m4a");
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
