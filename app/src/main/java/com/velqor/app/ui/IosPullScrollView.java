package com.velqor.app.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.ScrollView;

import com.velqor.app.tokens.ThemeTokens;

public class IosPullScrollView extends ScrollView {

    public interface OnRefreshListener {
        void onRefresh();
    }

    private final int mTouchSlop;
    private final Paint mIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mArcRect = new RectF();

    private float mDownY;
    private float mDownX;
    private boolean mIsPulling = false;
    private boolean mHapticTriggered = false;
    private boolean mIsRefreshing = false;
    private float mPullOffset = 0f;
    private float mThreshold;
    private float mIndicatorTopBase;

    private ValueAnimator mAnimator;
    private OnRefreshListener mRefreshListener;

    public IosPullScrollView(Context context) {
        super(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
    }

    private void init() {
        mThreshold = ThemeTokens.dp(getContext(), 60);
        mIndicatorTopBase = ThemeTokens.dp(getContext(), 144);

        mIndicatorPaint.setStyle(Paint.Style.STROKE);
        mIndicatorPaint.setStrokeWidth(getContext().getResources().getDisplayMetrics().density * 2.5f);
        mIndicatorPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        mRefreshListener = listener;
    }

    public void setIndicatorTopBase(float topBase) {
        mIndicatorTopBase = topBase;
        invalidate();
    }

    public boolean isRefreshing() {
        return mIsRefreshing;
    }

    public void finishRefresh() {
        if (!mIsRefreshing && mPullOffset == 0f) return;
        mIsRefreshing = false;
        animateTo(0f, null);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = ev.getY();
                mDownX = ev.getX();
                mIsPulling = false;
                mHapticTriggered = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = ev.getY() - mDownY;
                float dx = ev.getX() - mDownX;
                if (getScrollY() == 0 && dy > mTouchSlop && dy > Math.abs(dx) && !mIsRefreshing) {
                    mIsPulling = true;
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mIsPulling) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    float dy = ev.getY() - mDownY;
                    if (dy <= 0) {
                        mPullOffset = 0f;
                        mIsPulling = false;
                        applyTranslation(0f);
                        return super.onTouchEvent(ev);
                    }
                    float maxPull = ThemeTokens.dp(getContext(), 120);
                    mPullOffset = Math.min(maxPull, dy * 0.45f);
                    applyTranslation(mPullOffset);

                    if (mPullOffset >= mThreshold) {
                        if (!mHapticTriggered) {
                            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                            mHapticTriggered = true;
                        }
                    } else {
                        mHapticTriggered = false;
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mIsPulling = false;
                    if (mPullOffset >= mThreshold && !mIsRefreshing) {
                        startRefreshing();
                    } else {
                        animateTo(0f, null);
                    }
                    return true;
            }
        }
        return super.onTouchEvent(ev);
    }

    private void startRefreshing() {
        mIsRefreshing = true;
        float refreshHeight = ThemeTokens.dp(getContext(), 48);
        animateTo(refreshHeight, () -> {
            if (mRefreshListener != null) {
                mRefreshListener.onRefresh();
            }
        });
    }

    private void animateTo(float target, Runnable onEnd) {
        if (mAnimator != null) {
            mAnimator.cancel();
        }
        mAnimator = ValueAnimator.ofFloat(mPullOffset, target);
        mAnimator.setDuration(220);
        mAnimator.setInterpolator(new DecelerateInterpolator());
        mAnimator.addUpdateListener(anim -> {
            mPullOffset = (float) anim.getAnimatedValue();
            applyTranslation(mPullOffset);
        });
        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onEnd != null) {
                    onEnd.run();
                }
            }
        });
        mAnimator.start();
    }

    private void applyTranslation(float offset) {
        View child = getChildAt(0);
        if (child != null) {
            child.setTranslationY(offset);
        }
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mPullOffset > 0f) {
            float centerX = getWidth() / 2f;
            float centerY = mIndicatorTopBase + mPullOffset / 2f;
            float radius = ThemeTokens.dp(getContext(), 10);
            mArcRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

            int accent = ThemeTokens.getAccentColor(getContext());
            mIndicatorPaint.setColor(accent);

            if (!mIsRefreshing) {
                float progress = Math.min(1.0f, mPullOffset / mThreshold);
                mIndicatorPaint.setAlpha((int) (255 * (0.25f + 0.75f * progress)));
                float sweep = 280f * progress;
                canvas.drawArc(mArcRect, -90, sweep, false, mIndicatorPaint);
            } else {
                mIndicatorPaint.setAlpha(255);
                float angle = (System.currentTimeMillis() % 1000) / 1000f * 360f;
                canvas.drawArc(mArcRect, angle, 270, false, mIndicatorPaint);
                postInvalidateOnAnimation();
            }
        }
    }
}
