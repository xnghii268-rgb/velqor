package com.velqor.app.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
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
import android.view.animation.LinearInterpolator;
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
    private ValueAnimator mIndicatorAnimator;

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
        int dropHeight = ThemeTokens.dp(getContext(), 42);
        int dropWidth = ThemeTokens.dp(getContext(), 58);
        FrameLayout.LayoutParams dropLp = new FrameLayout.LayoutParams(dropWidth, dropHeight);
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
                        mWaterDropIndicator.animate()
                                .scaleX(1.07f)
                                .scaleY(0.93f)
                                .setDuration(90)
                                .start();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (index == mCurrentIndex) {
                        mWaterDropIndicator.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(200)
                                .setInterpolator(new OvershootInterpolator(1.4f))
                                .start();
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
                ivFilled.animate().alpha(targetFilledAlpha).setDuration(160).start();
                ivOutline.animate().alpha(targetOutlineAlpha).setDuration(160).start();
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
                int dropW = Math.min(tab.getWidth() - ThemeTokens.dp(getContext(), 16), ThemeTokens.dp(getContext(), 58));
                int dropH = ThemeTokens.dp(getContext(), 42);
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mWaterDropIndicator.getLayoutParams();
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

        int dropW = mWaterDropIndicator.getWidth() > 0 ? mWaterDropIndicator.getWidth() : ThemeTokens.dp(getContext(), 58);
        int targetX = targetTab.getLeft() + (targetTab.getWidth() - dropW) / 2;

        if (mIndicatorAnimator != null && mIndicatorAnimator.isRunning()) {
            mIndicatorAnimator.cancel();
        }

        if (animate) {
            float startX = mWaterDropIndicator.getTranslationX();
            float distance = targetX - startX;

            if (Math.abs(distance) < 1.0f) {
                mWaterDropIndicator.setTranslationX(targetX);
                mWaterDropIndicator.setScaleX(1.0f);
                mWaterDropIndicator.setScaleY(1.0f);
                return;
            }

            mIndicatorAnimator = ValueAnimator.ofFloat(0f, 1f);
            mIndicatorAnimator.setDuration(290);
            mIndicatorAnimator.setInterpolator(new LinearInterpolator());
            mIndicatorAnimator.addUpdateListener(animation -> {
                float t = (Float) animation.getAnimatedValue();

                float easedProgress = t < 0.5f ? 4.0f * t * t * t : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3.0) / 2.0f;
                float currentX = startX + distance * easedProgress;
                mWaterDropIndicator.setTranslationX(currentX);

                float sx;
                float sy;
                if (t <= 0.62f) {
                    float flightPhase = t / 0.62f;
                    float stretchWave = (float) Math.sin(flightPhase * Math.PI);
                    float maxStretch = Math.min(0.25f, 0.12f + Math.abs(distance) / 1000f);
                    sx = 1.0f + maxStretch * stretchWave;
                    sy = 1.0f - (maxStretch * 0.50f) * stretchWave;
                } else {
                    float settlePhase = (t - 0.62f) / 0.38f;
                    float squashWave = (float) (Math.sin(settlePhase * Math.PI * 1.5) * Math.exp(-settlePhase * 3.2));
                    sx = 1.0f - 0.10f * squashWave;
                    sy = 1.0f + 0.07f * squashWave;
                }
                mWaterDropIndicator.setScaleX(sx);
                mWaterDropIndicator.setScaleY(sy);
            });

            mIndicatorAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mWaterDropIndicator.setTranslationX(targetX);
                    mWaterDropIndicator.setScaleX(1.0f);
                    mWaterDropIndicator.setScaleY(1.0f);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mWaterDropIndicator.setTranslationX(targetX);
                    mWaterDropIndicator.setScaleX(1.0f);
                    mWaterDropIndicator.setScaleY(1.0f);
                }
            });

            mIndicatorAnimator.start();

            FrameLayout iconBox = (FrameLayout) ((FrameLayout) targetTab).getChildAt(0);
            iconBox.animate()
                    .scaleX(1.16f)
                    .scaleY(1.16f)
                    .setDuration(120)
                    .withEndAction(() -> iconBox.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(170)
                            .setInterpolator(new OvershootInterpolator(1.6f))
                            .start())
                    .start();
        } else {
            mWaterDropIndicator.setTranslationX(targetX);
            mWaterDropIndicator.setScaleX(1.0f);
            mWaterDropIndicator.setScaleY(1.0f);
        }
    }

    private static class WaterDropView extends View {
        private final Paint mBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mBottomGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF mRect = new RectF();
        private final Path mClipPath = new Path();

        public WaterDropView(Context context) {
            super(context);
            mBodyPaint.setStyle(Paint.Style.FILL);
            mHighlightPaint.setStyle(Paint.Style.FILL);
            mBottomGlowPaint.setStyle(Paint.Style.FILL);

            mStrokePaint.setStyle(Paint.Style.STROKE);
            float strokeW = Math.max(1f, ThemeTokens.dp(context, 1) * 0.75f);
            mStrokePaint.setStrokeWidth(strokeW);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mRect.set(0, 0, w, h);
            float rad = h / 2f;
            mClipPath.reset();
            mClipPath.addRoundRect(mRect, rad, rad, Path.Direction.CW);

            if (w > 0 && h > 0) {
                Context ctx = getContext();
                int accent = ThemeTokens.getAccentColor(ctx);
                boolean isDark = ThemeTokens.isDarkMode(ctx);

                int bodyTopAlpha = isDark ? 0x38000000 : 0x2C000000;
                int bodyBottomAlpha = isDark ? 0x16000000 : 0x14000000;
                LinearGradient bodyShader = new LinearGradient(
                        0, 0, 0, h,
                        new int[]{ (accent & 0x00FFFFFF) | bodyTopAlpha, (accent & 0x00FFFFFF) | bodyBottomAlpha },
                        null,
                        Shader.TileMode.CLAMP
                );
                mBodyPaint.setShader(bodyShader);

                int hlTop = isDark ? 0x48FFFFFF : 0x36FFFFFF;
                LinearGradient hlShader = new LinearGradient(
                        0, 0, 0, h * 0.48f,
                        new int[]{ hlTop, 0x00FFFFFF },
                        null,
                        Shader.TileMode.CLAMP
                );
                mHighlightPaint.setShader(hlShader);

                int glowColor = (accent & 0x00FFFFFF) | (isDark ? 0x22000000 : 0x18000000);
                LinearGradient glowShader = new LinearGradient(
                        0, h * 0.65f, 0, h,
                        new int[]{ 0x00000000, glowColor },
                        null,
                        Shader.TileMode.CLAMP
                );
                mBottomGlowPaint.setShader(glowShader);

                int strokeTop = isDark ? 0x60FFFFFF : 0x38000000;
                int strokeBottom = isDark ? 0x18FFFFFF : 0x12000000;
                LinearGradient strokeShader = new LinearGradient(
                        0, 0, 0, h,
                        new int[]{ strokeTop, strokeBottom },
                        null,
                        Shader.TileMode.CLAMP
                );
                mStrokePaint.setShader(strokeShader);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float rad = getHeight() / 2f;

            canvas.save();
            canvas.clipPath(mClipPath);
            canvas.drawRect(mRect, mBodyPaint);
            canvas.drawRect(0, 0, getWidth(), getHeight() * 0.48f, mHighlightPaint);
            canvas.drawRect(0, getHeight() * 0.65f, getWidth(), getHeight(), mBottomGlowPaint);
            canvas.restore();

            float half = mStrokePaint.getStrokeWidth() / 2f;
            RectF strokeRect = new RectF(half, half, getWidth() - half, getHeight() - half);
            canvas.drawRoundRect(strokeRect, rad - half, rad - half, mStrokePaint);
        }
    }
}
