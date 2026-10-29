package com.velqor.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.velqor.app.R;
import com.velqor.app.tokens.ThemeTokens;

import java.util.ArrayList;
import java.util.List;

public class FloatingTabBar extends FrameLayout {

    public interface OnTabSelectedListener {
        void onTabSelected(String tabKey, int index);
    }

    private BlurBackdropView mBackdropView;
    private WaterDropView mWaterDropIndicator;
    private final List<View> mTabViews = new ArrayList<>();
    private final String[] mTabKeys = {"files", "archive", "settings"};
    private final int[] mOutlineIcons = {
            R.drawable.ic_tab_folder_outline,
            R.drawable.ic_tab_archive_outline,
            R.drawable.ic_tab_settings_outline
    };
    private final int[] mFilledIcons = {
            R.drawable.ic_tab_folder_filled,
            R.drawable.ic_tab_archive_filled,
            R.drawable.ic_tab_settings_filled
    };
    private final String[] mContentDescriptions = {
            "Tệp",
            "Nén",
            "Cài đặt"
    };

    private String mCurrentTab = "files";
    private int mCurrentIndex = 0;
    private OnTabSelectedListener mListener;

    public FloatingTabBar(Context context) {
        super(context);
        init();
    }

    private void init() {
        int h = ThemeTokens.dp(getContext(), ThemeTokens.HEIGHT_TAB_BAR);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));
        setClipChildren(false);
        setClipToPadding(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(ThemeTokens.dp(getContext(), ThemeTokens.ELEVATION_FLOATING_BAR));
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                            ThemeTokens.dp(getContext(), ThemeTokens.RADIUS_TAB_BAR));
                }
            });
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setOutlineAmbientShadowColor(0x20000000);
            setOutlineSpotShadowColor(0x26000000);
        }

        mBackdropView = new BlurBackdropView(getContext());
        mBackdropView.setCornerRadius(ThemeTokens.dp(getContext(), ThemeTokens.RADIUS_TAB_BAR));
        addView(mBackdropView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mWaterDropIndicator = new WaterDropView(getContext());
        int dropHeight = ThemeTokens.dp(getContext(), ThemeTokens.HEIGHT_WATER_DROP);
        FrameLayout.LayoutParams dropLp = new FrameLayout.LayoutParams(dropHeight, dropHeight);
        dropLp.gravity = Gravity.CENTER_VERTICAL;
        addView(mWaterDropIndicator, dropLp);

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mTabViews.clear();
        for (int i = 0; i < mTabKeys.length; i++) {
            View tabView = createTabItem(mOutlineIcons[i], mFilledIcons[i], mContentDescriptions[i], mTabKeys[i], i);
            mTabViews.add(tabView);
            row.addView(tabView);
        }
        addView(row);

        post(() -> {
            adjustIndicatorWidth(0);
            animateIndicatorToTab(0, false);
        });
    }

    public void setBackdropSource(View target) {
        if (mBackdropView != null) {
            mBackdropView.setBackdropSource(target);
        }
    }

    public void invalidateBackdrop() {
        if (mBackdropView != null) {
            mBackdropView.invalidate();
        }
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        mListener = listener;
    }

    public void selectTab(String tabKey) {
        for (int i = 0; i < mTabKeys.length; i++) {
            if (mTabKeys[i].equals(tabKey)) {
                if (!mCurrentTab.equals(tabKey)) {
                    mCurrentTab = tabKey;
                    mCurrentIndex = i;
                    animateIndicatorToTab(i, true);
                    updateTabSelectionState(true);
                }
                break;
            }
        }
    }

    private View createTabItem(int outlineIconRes, int filledIconRes, String contentDesc, String tabKey, int index) {
        FrameLayout item = new FrameLayout(getContext());
        item.setClipChildren(false);
        item.setClipToPadding(false);
        item.setContentDescription(contentDesc);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        item.setLayoutParams(lp);

        boolean isSelected = mCurrentTab.equals(tabKey);

        FrameLayout iconBox = new FrameLayout(getContext());
        int iconSize = ThemeTokens.dp(getContext(), 24);
        FrameLayout.LayoutParams boxLp = new FrameLayout.LayoutParams(iconSize, iconSize);
        boxLp.gravity = Gravity.CENTER;
        iconBox.setLayoutParams(boxLp);

        ImageView ivOutline = new ImageView(getContext());
        ivOutline.setImageResource(outlineIconRes);
        ivOutline.setColorFilter(ThemeTokens.getSecondaryLabelColor(getContext()));
        ivOutline.setAlpha(isSelected ? 0.0f : 1.0f);
        iconBox.addView(ivOutline, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView ivFilled = new ImageView(getContext());
        ivFilled.setImageResource(filledIconRes);
        ivFilled.setColorFilter(ThemeTokens.getAccentColor(getContext()));
        ivFilled.setAlpha(isSelected ? 1.0f : 0.0f);
        iconBox.addView(ivFilled, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        item.addView(iconBox);

        item.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (index == mCurrentIndex) {
                        mWaterDropIndicator.animate().scaleX(1.06f).scaleY(1.06f).setDuration(80).start();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (index == mCurrentIndex) {
                        mWaterDropIndicator.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                    }
                    break;
            }
            return false;
        });

        item.setOnClickListener(v -> {
            if (!mCurrentTab.equals(tabKey)) {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                mCurrentTab = tabKey;
                mCurrentIndex = index;
                animateIndicatorToTab(index, true);
                updateTabSelectionState(true);
                if (mListener != null) {
                    mListener.onTabSelected(tabKey, index);
                }
            }
        });

        return item;
    }

    public void updateTabSelectionState(boolean animated) {
        for (int i = 0; i < mTabViews.size(); i++) {
            FrameLayout item = (FrameLayout) mTabViews.get(i);
            String tabKey = mTabKeys[i];
            boolean isSelected = mCurrentTab.equals(tabKey);

            FrameLayout iconBox = (FrameLayout) item.getChildAt(0);
            ImageView ivOutline = (ImageView) iconBox.getChildAt(0);
            ImageView ivFilled = (ImageView) iconBox.getChildAt(1);

            float targetFilledAlpha = isSelected ? 1.0f : 0.0f;
            float targetOutlineAlpha = isSelected ? 0.0f : 1.0f;

            if (animated) {
                ivFilled.animate().alpha(targetFilledAlpha).setDuration(150).start();
                ivOutline.animate().alpha(targetOutlineAlpha).setDuration(150).start();
            } else {
                ivFilled.setAlpha(targetFilledAlpha);
                ivOutline.setAlpha(targetOutlineAlpha);
            }
        }
    }

    private void adjustIndicatorWidth(int index) {
        if (index >= 0 && index < mTabViews.size()) {
            View tab = mTabViews.get(index);
            if (tab.getWidth() > 0) {
                int dropW = tab.getWidth() - ThemeTokens.dp(getContext(), 8);
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mWaterDropIndicator.getLayoutParams();
                int dropH = ThemeTokens.dp(getContext(), ThemeTokens.HEIGHT_WATER_DROP);
                if (lp != null && (lp.width != dropW || lp.height != dropH)) {
                    lp.width = dropW;
                    lp.height = dropH;
                    mWaterDropIndicator.setLayoutParams(lp);
                }
            }
        }
    }

    public void animateIndicatorToTab(int index, boolean animate) {
        if (index < 0 || index >= mTabViews.size()) return;
        View targetTab = mTabViews.get(index);
        adjustIndicatorWidth(index);
        int targetX = targetTab.getLeft() + ThemeTokens.dp(getContext(), 4);

        if (animate) {
            mWaterDropIndicator.animate()
                    .translationX(targetX)
                    .scaleX(1.20f)
                    .scaleY(0.94f)
                    .setDuration(130)
                    .withEndAction(() -> {
                        mWaterDropIndicator.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(180)
                                .setInterpolator(new OvershootInterpolator(1.15f))
                                .start();
                    })
                    .start();

            FrameLayout iconBox = (FrameLayout) ((FrameLayout) targetTab).getChildAt(0);
            iconBox.animate().scaleX(1.15f).scaleY(1.15f).setDuration(100).withEndAction(() -> {
                iconBox.animate().scaleX(1.0f).scaleY(1.0f).setDuration(140).start();
            }).start();
        } else {
            mWaterDropIndicator.setTranslationX(targetX);
        }
    }

    private static class WaterDropView extends View {
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF mRect = new RectF();
        private final Path mClipPath = new Path();

        public WaterDropView(Context context) {
            super(context);
            mPaint.setStyle(Paint.Style.FILL);
            int accent = ThemeTokens.getAccentColor(context);
            int dropAlpha = ThemeTokens.isDarkMode(context) ? 0x24000000 : 0x1A000000;
            mPaint.setColor((accent & 0x00FFFFFF) | dropAlpha);

            mStrokePaint.setStyle(Paint.Style.STROKE);
            mStrokePaint.setStrokeWidth(Math.max(1, ThemeTokens.dp(context, 1) / 2));
            boolean isDark = ThemeTokens.isDarkMode(context);
            mStrokePaint.setColor(isDark ? ThemeTokens.COLOR_DROP_BORDER : 0x2E000000);

            mHighlightPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mRect.set(0, 0, w, h);
            float rad = h / 2f;
            mClipPath.reset();
            mClipPath.addRoundRect(mRect, rad, rad, Path.Direction.CW);

            if (w > 0 && h > 0) {
                LinearGradient hlShader = new LinearGradient(
                        0, 0, 0, h * 0.55f,
                        new int[]{0x1AFFFFFF, 0x00FFFFFF},
                        null,
                        Shader.TileMode.CLAMP
                );
                mHighlightPaint.setShader(hlShader);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float rad = getHeight() / 2f;

            canvas.save();
            canvas.clipPath(mClipPath);
            canvas.drawRect(mRect, mPaint);
            canvas.drawRect(0, 0, getWidth(), getHeight() * 0.55f, mHighlightPaint);
            canvas.restore();

            float half = mStrokePaint.getStrokeWidth() / 2f;
            RectF strokeRect = new RectF(half, half, getWidth() - half, getHeight() - half);
            canvas.drawRoundRect(strokeRect, rad - half, rad - half, mStrokePaint);
        }
    }
}
