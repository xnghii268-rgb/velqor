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
import android.view.Choreographer;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.accessibility.AccessibilityNodeInfo;
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
    private LiquidCapsuleView mLiquidCapsule;
    private LinearLayout mTabsRow;
    private final List<FrameLayout> mTabViews = new ArrayList<>();
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

    private final SpringSolver mPositionSpring = new SpringSolver(1.0f, 220.0f, 20.0f);
    private final SpringSolver mScaleSpring = new SpringSolver(1.0f, 280.0f, 22.0f);
    private boolean mIsSpringActive = false;
    private long mLastFrameNanos = 0;
    private Choreographer.FrameCallback mFrameCallback;

    private VelocityTracker mVelocityTracker;
    private float mTouchDownX = 0f;
    private float mTouchDownY = 0f;
    private boolean mIsDragging = false;
    private int mLastHoveredIndex = 0;

    private final Paint mAmbientShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKeyShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBarRect = new RectF();

    public FloatingTabBar(Context context) {
        super(context);
        init();
    }

    private void init() {
        int h = ThemeTokens.dp(getContext(), ThemeTokens.HEIGHT_TAB_BAR);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));
        setClipChildren(false);
        setClipToPadding(false);

        mAmbientShadowPaint.setStyle(Paint.Style.FILL);
        mKeyShadowPaint.setStyle(Paint.Style.FILL);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                            ThemeTokens.dp(getContext(), ThemeTokens.RADIUS_TAB_BAR));
                }
            });
        }

        mBackdropView = new BlurBackdropView(getContext());
        mBackdropView.setCornerRadius(ThemeTokens.dp(getContext(), ThemeTokens.RADIUS_TAB_BAR));
        addView(mBackdropView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mLiquidCapsule = new LiquidCapsuleView(getContext());
        int dropHeight = ThemeTokens.dp(getContext(), 42);
        int dropWidth = ThemeTokens.dp(getContext(), 58);
        FrameLayout.LayoutParams dropLp = new FrameLayout.LayoutParams(dropWidth, dropHeight);
        dropLp.gravity = Gravity.CENTER_VERTICAL;
        addView(mLiquidCapsule, dropLp);

        mTabsRow = new LinearLayout(getContext());
        mTabsRow.setOrientation(LinearLayout.HORIZONTAL);
        mTabsRow.setGravity(Gravity.CENTER_VERTICAL);
        mTabsRow.setClipChildren(false);
        mTabsRow.setClipToPadding(false);
        mTabsRow.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mTabViews.clear();
        for (int i = 0; i < mTabKeys.length; i++) {
            FrameLayout tabView = createTabItem(mOutlineIcons[i], mFilledIcons[i], mContentDescriptions[i], mTabKeys[i], i);
            mTabViews.add(tabView);
            mTabsRow.addView(tabView);
        }
        addView(mTabsRow);

        mFrameCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                if (mLastFrameNanos == 0) {
                    mLastFrameNanos = frameTimeNanos;
                }
                float dt = (frameTimeNanos - mLastFrameNanos) / 1_000_000_000.0f;
                mLastFrameNanos = frameTimeNanos;

                float currentX = mPositionSpring.update(dt);
                float currentScale = mScaleSpring.update(dt);

                mLiquidCapsule.setTranslationX(currentX);

                float velocity = mPositionSpring.getVelocity();
                float stretch = Math.min(0.24f, Math.abs(velocity) / 3600.0f);
                float sx = currentScale * (1.0f + stretch);
                float sy = currentScale * (1.0f / (1.0f + stretch));
                mLiquidCapsule.setScaleX(sx);
                mLiquidCapsule.setScaleY(sy);

                mLiquidCapsule.setBridgeOffset(velocity * 0.008f);

                if (!mPositionSpring.isAtRest() || !mScaleSpring.isAtRest() || mIsDragging) {
                    Choreographer.getInstance().postFrameCallback(this);
                } else {
                    mIsSpringActive = false;
                    mLastFrameNanos = 0;
                    mLiquidCapsule.setScaleX(1.0f);
                    mLiquidCapsule.setScaleY(1.0f);
                    mLiquidCapsule.setBridgeOffset(0f);
                }
            }
        };

        mScaleSpring.setValue(1.0f);
        mScaleSpring.setTarget(1.0f);

        post(() -> {
            snapToTabInstant(0);
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mIsSpringActive) {
            Choreographer.getInstance().removeFrameCallback(mFrameCallback);
            mIsSpringActive = false;
        }
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
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
                    animateIndicatorToTab(i);
                    updateTabSelectionState(true);
                }
                break;
            }
        }
    }

    private void snapToTabInstant(int index) {
        if (index < 0 || index >= mTabViews.size()) return;
        View targetTab = mTabViews.get(index);
        int dropW = mLiquidCapsule.getWidth() > 0 ? mLiquidCapsule.getWidth() : ThemeTokens.dp(getContext(), 58);
        int targetX = targetTab.getLeft() + (targetTab.getWidth() - dropW) / 2;
        mPositionSpring.setValue(targetX);
        mPositionSpring.setTarget(targetX);
        mPositionSpring.setVelocity(0f);
        mLiquidCapsule.setTranslationX(targetX);
    }

    private void animateIndicatorToTab(int index) {
        if (index < 0 || index >= mTabViews.size()) return;
        View targetTab = mTabViews.get(index);
        int dropW = mLiquidCapsule.getWidth() > 0 ? mLiquidCapsule.getWidth() : ThemeTokens.dp(getContext(), 58);
        int targetX = targetTab.getLeft() + (targetTab.getWidth() - dropW) / 2;

        mPositionSpring.setTarget(targetX);
        startSpringLoop();

        FrameLayout iconBox = (FrameLayout) ((FrameLayout) targetTab).getChildAt(0);
        iconBox.animate()
                .scaleX(1.18f)
                .scaleY(1.18f)
                .setDuration(120)
                .withEndAction(() -> iconBox.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160).start())
                .start();
    }

    private void startSpringLoop() {
        if (!mIsSpringActive) {
            mIsSpringActive = true;
            mLastFrameNanos = 0;
            Choreographer.getInstance().postFrameCallback(mFrameCallback);
        }
    }

    private FrameLayout createTabItem(int outlineIconRes, int filledIconRes, String contentDesc, String tabKey, int index) {
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

        item.setOnClickListener(v -> {
            if (!mCurrentTab.equals(tabKey)) {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                mCurrentTab = tabKey;
                mCurrentIndex = index;
                animateIndicatorToTab(index);
                updateTabSelectionState(true);
                if (mListener != null) {
                    mListener.onTabSelected(tabKey, index);
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            item.setAccessibilityDelegate(new AccessibilityDelegate() {
                @Override
                public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName("android.widget.TabWidget");
                    info.setSelected(mCurrentTab.equals(tabKey));
                }
            });
        }

        return item;
    }

    public void updateTabSelectionState(boolean animated) {
        for (int i = 0; i < mTabViews.size(); i++) {
            FrameLayout item = mTabViews.get(i);
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

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownX = x;
                mTouchDownY = y;
                mIsDragging = false;
                mScaleSpring.setTarget(1.12f);
                startSpringLoop();
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(x - mTouchDownX);
                if (dx > ThemeTokens.dp(getContext(), 8)) {
                    mIsDragging = true;
                }
                if (mIsDragging) {
                    int dropW = mLiquidCapsule.getWidth() > 0 ? mLiquidCapsule.getWidth() : ThemeTokens.dp(getContext(), 58);
                    float targetIndicatorX = x - dropW / 2f;

                    float maxRange = getWidth() - dropW;
                    float clampedX = SpringSolver.applyRubberBand(targetIndicatorX, maxRange, getWidth() * 0.4f);

                    mPositionSpring.setValue(clampedX);
                    mPositionSpring.setTarget(clampedX);
                    startSpringLoop();

                    int hoveredIndex = computeHoveredTab(x);
                    if (hoveredIndex != mLastHoveredIndex && hoveredIndex >= 0 && hoveredIndex < mTabViews.size()) {
                        mLastHoveredIndex = hoveredIndex;
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                        lensZoomTab(hoveredIndex);
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mScaleSpring.setTarget(1.0f);
                if (mIsDragging) {
                    mVelocityTracker.computeCurrentVelocity(1000);
                    float vx = mVelocityTracker.getXVelocity();

                    float[] tabCenters = new float[mTabViews.size()];
                    int dropW = mLiquidCapsule.getWidth() > 0 ? mLiquidCapsule.getWidth() : ThemeTokens.dp(getContext(), 58);
                    for (int i = 0; i < mTabViews.size(); i++) {
                        View tv = mTabViews.get(i);
                        tabCenters[i] = tv.getLeft() + (tv.getWidth() - dropW) / 2f;
                    }

                    int snapIndex = SpringSolver.computeSnapIndex(mPositionSpring.getValue(), vx, tabCenters);
                    snapIndex = Math.max(0, Math.min(mTabKeys.length - 1, snapIndex));

                    mPositionSpring.setVelocity(vx);
                    mCurrentIndex = snapIndex;
                    mCurrentTab = mTabKeys[snapIndex];
                    animateIndicatorToTab(snapIndex);
                    updateTabSelectionState(true);

                    performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                    if (mListener != null) {
                        mListener.onTabSelected(mCurrentTab, snapIndex);
                    }
                } else {
                    int clickIndex = computeHoveredTab(x);
                    if (clickIndex >= 0 && clickIndex < mTabKeys.length) {
                        if (!mCurrentTab.equals(mTabKeys[clickIndex])) {
                            mCurrentTab = mTabKeys[clickIndex];
                            mCurrentIndex = clickIndex;
                            animateIndicatorToTab(clickIndex);
                            updateTabSelectionState(true);
                            if (mListener != null) {
                                mListener.onTabSelected(mCurrentTab, clickIndex);
                            }
                        }
                    }
                }
                mIsDragging = false;
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private int computeHoveredTab(float touchX) {
        for (int i = 0; i < mTabViews.size(); i++) {
            View tv = mTabViews.get(i);
            if (touchX >= tv.getLeft() && touchX <= tv.getRight()) {
                return i;
            }
        }
        return mCurrentIndex;
    }

    private void lensZoomTab(int index) {
        for (int i = 0; i < mTabViews.size(); i++) {
            FrameLayout item = mTabViews.get(i);
            FrameLayout iconBox = (FrameLayout) item.getChildAt(0);
            if (i == index) {
                iconBox.animate().scaleX(1.14f).scaleY(1.14f).setDuration(90).start();
            } else {
                iconBox.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
            }
        }
    }

    private static class LiquidCapsuleView extends View {
        private final Paint mBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mBottomGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mBridgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final RectF mRect = new RectF();
        private final Path mClipPath = new Path();
        private final Path mBridgePath = new Path();
        private float mBridgeOffset = 0f;

        public LiquidCapsuleView(Context context) {
            super(context);
            mBodyPaint.setStyle(Paint.Style.FILL);
            mHighlightPaint.setStyle(Paint.Style.FILL);
            mBottomGlowPaint.setStyle(Paint.Style.FILL);
            mBridgePaint.setStyle(Paint.Style.FILL);

            mStrokePaint.setStyle(Paint.Style.STROKE);
            float strokeW = Math.max(1f, ThemeTokens.dp(context, 1) * 0.75f);
            mStrokePaint.setStrokeWidth(strokeW);
        }

        public void setBridgeOffset(float offset) {
            this.mBridgeOffset = offset;
            invalidate();
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

                int bodyTopAlpha = isDark ? 0x48000000 : 0x36000000;
                int bodyBottomAlpha = isDark ? 0x1A000000 : 0x14000000;
                LinearGradient bodyShader = new LinearGradient(
                        0, 0, 0, h,
                        new int[]{(accent & 0x00FFFFFF) | bodyTopAlpha, (accent & 0x00FFFFFF) | bodyBottomAlpha},
                        null,
                        Shader.TileMode.CLAMP
                );
                mBodyPaint.setShader(bodyShader);
                mBridgePaint.setColor((accent & 0x00FFFFFF) | (isDark ? 0x2E000000 : 0x22000000));

                int hlTop = isDark ? 0x68FFFFFF : 0x4EFFFFFF;
                LinearGradient hlShader = new LinearGradient(
                        0, 0, 0, h * 0.45f,
                        new int[]{hlTop, 0x00FFFFFF},
                        null,
                        Shader.TileMode.CLAMP
                );
                mHighlightPaint.setShader(hlShader);

                int glowColor = (accent & 0x00FFFFFF) | (isDark ? 0x30000000 : 0x24000000);
                LinearGradient glowShader = new LinearGradient(
                        0, h * 0.62f, 0, h,
                        new int[]{0x00000000, glowColor},
                        null,
                        Shader.TileMode.CLAMP
                );
                mBottomGlowPaint.setShader(glowShader);

                int strokeTop = isDark ? 0x75FFFFFF : 0x48FFFFFF;
                int strokeBottom = isDark ? 0x1EFFFFFF : 0x14000000;
                LinearGradient strokeShader = new LinearGradient(
                        0, 0, 0, h,
                        new int[]{strokeTop, strokeBottom},
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

            if (Math.abs(mBridgeOffset) > 1.0f) {
                mBridgePath.reset();
                float cx = getWidth() / 2f;
                float cy = getHeight() / 2f;
                float tailX = cx - mBridgeOffset * 2.8f;
                mBridgePath.moveTo(cx, cy - rad * 0.65f);
                mBridgePath.quadTo(cx - mBridgeOffset * 0.8f, cy, tailX, cy - rad * 0.3f);
                mBridgePath.lineTo(tailX, cy + rad * 0.3f);
                mBridgePath.quadTo(cx - mBridgeOffset * 0.8f, cy, cx, cy + rad * 0.65f);
                mBridgePath.close();
                canvas.drawPath(mBridgePath, mBridgePaint);
            }

            canvas.save();
            canvas.clipPath(mClipPath);
            canvas.drawRect(mRect, mBodyPaint);
            canvas.drawRect(0, 0, getWidth(), getHeight() * 0.45f, mHighlightPaint);
            canvas.drawRect(0, getHeight() * 0.62f, getWidth(), getHeight(), mBottomGlowPaint);
            canvas.restore();

            float half = mStrokePaint.getStrokeWidth() / 2f;
            RectF strokeRect = new RectF(half, half, getWidth() - half, getHeight() - half);
            canvas.drawRoundRect(strokeRect, rad - half, rad - half, mStrokePaint);
        }
    }
}
