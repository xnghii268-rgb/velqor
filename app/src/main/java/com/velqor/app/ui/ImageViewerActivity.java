package com.velqor.app.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.velqor.app.R;
import com.velqor.app.VelqorFileProvider;
import com.velqor.app.files.FileRepository;
import com.velqor.app.tokens.ThemeTokens;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageViewerActivity extends Activity {

    public static final String EXTRA_PATH = "extra_path";

    private File mCurrentFile;
    private final List<File> mSiblingImages = new ArrayList<>();
    private int mCurrentIndex = 0;

    private FrameLayout mRoot;
    private TouchImageView mImageView;
    private LinearLayout mTopBar;
    private LinearLayout mBottomBar;
    private TextView mTitleText;
    private TextView mCountText;
    private boolean mBarsVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
        setupEdgeToEdge();

        String path = getIntent().getStringExtra(EXTRA_PATH);
        if (path == null) {
            finish();
            return;
        }

        mCurrentFile = new File(path);
        loadSiblingImages();

        mRoot = new FrameLayout(this);
        mRoot.setBackgroundColor(Color.BLACK);

        mImageView = new TouchImageView(this);
        mRoot.addView(mImageView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        
        mTopBar = createTopBar();
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        topLp.gravity = Gravity.TOP;
        mRoot.addView(mTopBar, topLp);

        
        mBottomBar = createBottomBar();
        FrameLayout.LayoutParams botLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        botLp.gravity = Gravity.BOTTOM;
        mRoot.addView(mBottomBar, botLp);

        setContentView(mRoot);

        loadCurrentImage();
    }

    private void setupEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);

            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                      | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                      | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void loadSiblingImages() {
        mSiblingImages.clear();
        File parent = mCurrentFile.getParentFile();
        if (parent != null) {
            File[] files = parent.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (isImageFile(f)) {
                        mSiblingImages.add(f);
                    }
                }
            }
        }
        if (mSiblingImages.isEmpty()) {
            mSiblingImages.add(mCurrentFile);
        }
        for (int i = 0; i < mSiblingImages.size(); i++) {
            if (mSiblingImages.get(i).getAbsolutePath().equals(mCurrentFile.getAbsolutePath())) {
                mCurrentIndex = i;
                break;
            }
        }
    }

    public static boolean isImageFile(File f) {
        if (f == null || f.isDirectory()) return false;
        String name = f.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
               name.endsWith(".webp") || name.endsWith(".gif") || name.endsWith(".bmp");
    }

    private LinearLayout createTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        int padTop = getStatusBarHeight() + ThemeTokens.dp(this, 8);
        bar.setPadding(ThemeTokens.dp(this, 16), padTop, ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 12));
        bar.setBackgroundColor(0xCC000000); 

        
        ImageView btnBack = new ImageView(this);
        btnBack.setImageResource(R.drawable.ic_chevron_right);
        btnBack.setRotation(180); 
        btnBack.setColorFilter(Color.WHITE);
        int btnSize = ThemeTokens.dp(this, 28);
        btnBack.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
        btnBack.setOnClickListener(v -> finish());
        bar.addView(btnBack);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.leftMargin = ThemeTokens.dp(this, 16);

        mTitleText = new TextView(this);
        mTitleText.setText(mCurrentFile.getName());
        mTitleText.setTextColor(Color.WHITE);
        mTitleText.setTextSize(16);
        mTitleText.setSingleLine(true);
        mTitleText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textCol.addView(mTitleText);

        mCountText = new TextView(this);
        mCountText.setText((mCurrentIndex + 1) + " / " + mSiblingImages.size());
        mCountText.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        mCountText.setTextSize(12);
        textCol.addView(mCountText);

        bar.addView(textCol, colLp);
        return bar;
    }

    private LinearLayout createBottomBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        int padBot = getNavBarHeight() + ThemeTokens.dp(this, 12);
        bar.setPadding(ThemeTokens.dp(this, 24), ThemeTokens.dp(this, 12), ThemeTokens.dp(this, 24), padBot);
        bar.setBackgroundColor(0xCC000000);

        
        ImageView btnShare = createBarBtn(R.drawable.ic_share, v -> shareCurrent());
        bar.addView(btnShare);

        View space1 = new View(this);
        bar.addView(space1, new LinearLayout.LayoutParams(0, 1, 1f));

        
        ImageView btnInfo = createBarBtn(R.drawable.ic_info, v -> showDetails());
        bar.addView(btnInfo);

        View space2 = new View(this);
        bar.addView(space2, new LinearLayout.LayoutParams(0, 1, 1f));

        
        ImageView btnDel = createBarBtn(R.drawable.ic_delete, v -> confirmDelete());
        btnDel.setColorFilter(ThemeTokens.COLOR_DANGER);
        bar.addView(btnDel);

        return bar;
    }

    private ImageView createBarBtn(int iconRes, View.OnClickListener listener) {
        ImageView iv = new ImageView(this);
        iv.setImageResource(iconRes);
        iv.setColorFilter(Color.WHITE);
        int sz = ThemeTokens.dp(this, 26);
        iv.setLayoutParams(new LinearLayout.LayoutParams(sz, sz));
        iv.setOnClickListener(listener);
        return iv;
    }

    private void toggleBars() {
        mBarsVisible = !mBarsVisible;
        int visibility = mBarsVisible ? View.VISIBLE : View.GONE;
        mTopBar.setVisibility(visibility);
        mBottomBar.setVisibility(visibility);
    }

    private void loadCurrentImage() {
        if (mCurrentIndex < 0 || mCurrentIndex >= mSiblingImages.size()) return;
        mCurrentFile = mSiblingImages.get(mCurrentIndex);
        mTitleText.setText(mCurrentFile.getName());
        mCountText.setText((mCurrentIndex + 1) + " / " + mSiblingImages.size());

        new Thread(() -> {
            try {
                Bitmap bm = decodeSampledBitmap(mCurrentFile.getAbsolutePath(), 2048, 2048);
                runOnUiThread(() -> {
                    if (bm != null) {
                        mImageView.setImageBitmap(bm);
                    } else {
                        Toast.makeText(this, "Không thể đọc tệp ảnh", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (OutOfMemoryError oom) {
                runOnUiThread(() -> Toast.makeText(this, "Ảnh quá lớn, tràn bộ nhớ", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private Bitmap decodeSampledBitmap(String path, int reqWidth, int reqHeight) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        if (bitmap == null) return null;

        
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotationAngle = 0;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;

            if (rotationAngle != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationAngle);
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (rotated != bitmap) {
                    bitmap.recycle();
                    bitmap = rotated;
                }
            }
        } catch (Exception ignored) {}

        return bitmap;
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

    private void shareCurrent() {
        try {
            Uri uri = VelqorFileProvider.getUriForFile(this, "com.velqor.app.fileprovider", mCurrentFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Chia sẻ ảnh"));
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi chia sẻ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDetails() {
        String info = mCurrentFile.getName() + "\n" +
                "Kích thước: " + FileRowView.formatSize(mCurrentFile.length()) + "\n" +
                "Đường dẫn: " + mCurrentFile.getAbsolutePath() + "\n" +
                "Ngày: " + FileRowView.formatItemDate(mCurrentFile.lastModified());
        Toast.makeText(this, info, Toast.LENGTH_LONG).show();
    }

    private void confirmDelete() {
        IosSheet.showDeleteConfirm(this, mCurrentFile, () -> {
            FileRepository.deleteRecursive(mCurrentFile);
            Toast.makeText(this, "Đã xóa ảnh", Toast.LENGTH_SHORT).show();
            mSiblingImages.remove(mCurrentIndex);
            if (mSiblingImages.isEmpty()) {
                finish();
            } else {
                if (mCurrentIndex >= mSiblingImages.size()) mCurrentIndex = mSiblingImages.size() - 1;
                loadCurrentImage();
            }
        });
    }

    private int getStatusBarHeight() {
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resId > 0 ? getResources().getDimensionPixelSize(resId) : ThemeTokens.dp(this, 24);
    }

    private int getNavBarHeight() {
        int resId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return resId > 0 ? getResources().getDimensionPixelSize(resId) : ThemeTokens.dp(this, 16);
    }

    
    
    
    private class TouchImageView extends View {
        private Bitmap mBitmap;
        private final Matrix mMatrix = new Matrix();
        private final Matrix mSavedMatrix = new Matrix();
        private float mScale = 1f;
        private float mFocusX = 0f;
        private float mFocusY = 0f;

        private float mDownX = 0f;
        private float mDownY = 0f;

        private ScaleGestureDetector mScaleDetector;
        private GestureDetector mGestureDetector;

        public TouchImageView(Context context) {
            super(context);
            initGestures();
        }

        private void initGestures() {
            mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    float factor = detector.getScaleFactor();
                    float newScale = mScale * factor;
                    if (newScale >= 0.8f && newScale <= 4.5f) {
                        mScale = newScale;
                        mMatrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
                        invalidate();
                    }
                    return true;
                }
            });

            mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    toggleBars();
                    return true;
                }

                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if (mScale > 1.2f) {
                        resetZoom();
                    } else {
                        mScale = 2.5f;
                        mMatrix.postScale(2.5f, 2.5f, e.getX(), e.getY());
                        invalidate();
                    }
                    return true;
                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                    if (mScale <= 1.05f && e1 != null && e2 != null) {
                        float dx = e2.getX() - e1.getX();
                        float dy = e2.getY() - e1.getY();

                        
                        if (dy > 200 && Math.abs(dy) > Math.abs(dx) * 1.5) {
                            finish();
                            return true;
                        }

                        
                        if (Math.abs(dx) > 150 && Math.abs(dx) > Math.abs(dy)) {
                            if (dx < 0 && mCurrentIndex < mSiblingImages.size() - 1) {
                                mCurrentIndex++;
                                resetZoom();
                                loadCurrentImage();
                                return true;
                            } else if (dx > 0 && mCurrentIndex > 0) {
                                mCurrentIndex--;
                                resetZoom();
                                loadCurrentImage();
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });
        }

        public void setImageBitmap(Bitmap bm) {
            mBitmap = bm;
            resetZoom();
        }

        public void resetZoom() {
            mScale = 1.0f;
            mMatrix.reset();
            fitBitmapToCenter();
            invalidate();
        }

        private void fitBitmapToCenter() {
            if (mBitmap == null || getWidth() == 0 || getHeight() == 0) return;
            float viewW = getWidth();
            float viewH = getHeight();
            float bmW = mBitmap.getWidth();
            float bmH = mBitmap.getHeight();

            float scale = Math.min(viewW / bmW, viewH / bmH);
            float dx = (viewW - bmW * scale) / 2f;
            float dy = (viewH - bmH * scale) / 2f;

            mMatrix.reset();
            mMatrix.postScale(scale, scale);
            mMatrix.postTranslate(dx, dy);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            fitBitmapToCenter();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            mScaleDetector.onTouchEvent(event);
            mGestureDetector.onTouchEvent(event);

            
            if (mScale > 1.05f) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        mDownX = event.getX();
                        mDownY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (event.getPointerCount() == 1) {
                            float dx = event.getX() - mDownX;
                            float dy = event.getY() - mDownY;
                            mMatrix.postTranslate(dx, dy);
                            mDownX = event.getX();
                            mDownY = event.getY();
                            invalidate();
                        }
                        break;
                }
            }
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (mBitmap != null && !mBitmap.isRecycled()) {
                canvas.drawBitmap(mBitmap, mMatrix, null);
            }
        }
    }
}
