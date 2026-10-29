package com.velqor.app.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
    private final String[] mTabKeys = {"files", "archive", "vault", "hash", "settings"};
    private final int[] mOutlineIcons = {
            R.drawable.ic_tab_folder_outline,
            R.drawable.ic_tab_archive_outline,
            R.drawable.ic_tab_vault_outline,
            R.drawable.ic_tab_hash_outline,
            R.drawable.ic_tab_settings_outline
    };
    private final int[] mFilledIcons = {
            R.drawable.ic_tab_folder_filled,
            R.drawable.ic_tab_archive_filled,
            R.drawable.ic_tab_vault_filled,
            R.drawable.ic_tab_hash_filled,
            R.drawable.ic_tab_settings_filled
    };
    private final String[] mLabels = {"Tệp", "Nén", "Vault", "Hash", "Cài đặt"};

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

        // Neutral Soft Shadow (No red or color tinting)
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
            setOutlineAmbientShadowColor(0x20000000); // Neutral black 12%
            setOutlineSpotShadowColor(0x26000000);    // Neutral black 15%
        }

        // 1. Real Hardware Blur / Glass Fallback Backdrop View
        mBackdropView = new BlurBackdropView(getContext());
        mBackdropView.setCornerRadius(ThemeTokens.dp(getContext(), ThemeTokens.RADIUS_TAB_BAR));
        addView(mBackdropView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 2. Refined Lightweight Water-drop Indicator (Height 48dp, Radius 24dp, Centered Vertically)
        mWaterDropIndicator = new WaterDropView(getContext());
        int dropHeight = ThemeTokens.dp(getContext(), ThemeTokens.HEIGHT_WATER_DROP);
        FrameLayout.LayoutParams dropLp = new FrameLayout.LayoutParams(dropHeight, dropHeight);
        dropLp.gravity = Gravity.CENTER_VERTICAL;
        addView(mWaterDropIndicator, dropLp);

        // 3. 5 Tab Items in Horizontal Row
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mTabViews.clear();
        for (int i = 0; i < mTabKeys.length; i++) {
            View tabView = createTabItem(mOutlineIcons[i], mFilledIcons[i], mLabels[i], mTabKeys[i], i);
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

    private View createTabItem(int outlineIconRes, int filledIconRes, String label, String tabKey, int index) {
        LinearLayout item = new LinearLayout(getContext());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setClipChildren(false);
        item.setClipToPadding(false);
        item.setPadding(ThemeTokens.dp(getContext(), 2), ThemeTokens.dp(getContext(), 2),
                ThemeTokens.dp(getContext(), 2), ThemeTokens.dp(getContext(), 2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        item.setLayoutParams(lp);

        boolean isSelected = mCurrentTab.equals(tabKey);

        // Icon Container with Dual Outline + Filled Layers for 150ms Crossfade
        FrameLayout iconBox = new FrameLayout(getContext());
        int iconSize = ThemeTokens.dp(getContext(), 24);
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));

        ImageView ivOutline = new ImageView(getContext());
        ivOutline.setImageResource(outlineIconRes);
        ivOutline.setColorFilter(ThemeTokens.COLOR_SEC_LABEL);
        ivOutline.setAlpha(isSelected ? 0.0f : 1.0f);
        iconBox.addView(ivOutline, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView ivFilled = new ImageView(getContext());
        ivFilled.setImageResource(filledIconRes);
        ivFilled.setColorFilter(ThemeTokens.COLOR_ACCENT);
        ivFilled.setAlpha(isSelected ? 1.0f : 0.0f);
        iconBox.addView(ivFilled, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        item.addView(iconBox);

        TextView tv = new TextView(getContext());
        tv.setText(label);
        tv.setTextSize(10);
        tv.setTextColor(isSelected ? ThemeTokens.COLOR_ACCENT : ThemeTokens.COLOR_SEC_LABEL);
        tv.setTypeface(isSelected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textLp.topMargin = ThemeTokens.dp(getContext(), 2);
        tv.setLayoutParams(textLp);
        item.addView(tv);

        // Press / Tap feedback
        item.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (index == mCurrentIndex) {
                        mWaterDropIndicator.animate().scaleX(1.05f).scaleY(1.05f).setDuration(80).start();
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
            LinearLayout item = (LinearLayout) mTabViews.get(i);
            String tabKey = mTabKeys[i];
            boolean isSelected = mCurrentTab.equals(tabKey);

            FrameLayout iconBox = (FrameLayout) item.getChildAt(0);
            ImageView ivOutline = (ImageView) iconBox.getChildAt(0);
            ImageView ivFilled = (ImageView) iconBox.getChildAt(1);
            TextView tv = (TextView) item.getChildAt(1);

            float targetFilledAlpha = isSelected ? 1.0f : 0.0f;
            float targetOutlineAlpha = isSelected ? 0.0f : 1.0f;

            if (animated) {
                ivFilled.animate().alpha(targetFilledAlpha).setDuration(150).start();
                ivOutline.animate().alpha(targetOutlineAlpha).setDuration(150).start();
            } else {
                ivFilled.setAlpha(targetFilledAlpha);
                ivOutline.setAlpha(targetOutlineAlpha);
            }

            tv.setTextColor(isSelected ? ThemeTokens.COLOR_ACCENT : ThemeTokens.COLOR_SEC_LABEL);
            tv.setTypeface(isSelected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
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
            // Spring stretch (scaleX ~1.20, scaleY ~0.94) & snap
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

            // Light icon pop on selected tab
            FrameLayout iconBox = (FrameLayout) ((LinearLayout) targetTab).getChildAt(0);
            iconBox.animate().scaleX(1.15f).scaleY(1.15f).setDuration(100).withEndAction(() -> {
                iconBox.animate().scaleX(1.0f).scaleY(1.0f).setDuration(140).start();
            }).start();
        } else {
            mWaterDropIndicator.setTranslationX(targetX);
        }
    }

    // Refined Lightweight Water-drop Indicator
    private static class WaterDropView extends View {
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF mRect = new RectF();
        private final Path mClipPath = new Path();

        public WaterDropView(Context context) {
            super(context);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(ThemeTokens.COLOR_DROP_BG); // Accent alpha 0.14

            mStrokePaint.setStyle(Paint.Style.STROKE);
            mStrokePaint.setStrokeWidth(Math.max(1, ThemeTokens.dp(context, 1) / 2)); // 0.5dp
            mStrokePaint.setColor(ThemeTokens.COLOR_DROP_BORDER); // 25% white border

            mHighlightPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mRect.set(0, 0, w, h);
            float rad = h / 2f;
            mClipPath.reset();
            mClipPath.addRoundRect(mRect, rad, rad, Path.Direction.CW);

            // Subtle top highlight (alpha 0.10 -> 0)
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
            // 1. Accent translucent tint
            canvas.drawRect(mRect, mPaint);
            // 2. Top-half light highlight
            canvas.drawRect(0, 0, getWidth(), getHeight() * 0.55f, mHighlightPaint);
            canvas.restore();

            // 3. Thin 0.5dp white border
            float half = mStrokePaint.getStrokeWidth() / 2f;
            RectF strokeRect = new RectF(half, half, getWidth() - half, getHeight() - half);
            canvas.drawRoundRect(strokeRect, rad - half, rad - half, mStrokePaint);
        }
    }
}
