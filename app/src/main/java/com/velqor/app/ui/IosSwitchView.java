package com.velqor.app.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.velqor.app.tokens.ThemeTokens;

public class IosSwitchView extends View {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(IosSwitchView view, boolean isChecked);
    }

    private boolean mIsChecked = false;
    private float mThumbPosition = 0.0f;
    private ValueAnimator mAnimator;
    private OnCheckedChangeListener mListener;

    private final Paint mTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mThumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mTrackRect = new RectF();

    public IosSwitchView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);

        mTrackPaint.setStyle(Paint.Style.FILL);
        mThumbPaint.setStyle(Paint.Style.FILL);
        mThumbPaint.setColor(Color.WHITE);

        mShadowPaint.setStyle(Paint.Style.FILL);
        mShadowPaint.setColor(0x33000000);

        setOnClickListener(v -> toggle());
    }

    public void setChecked(boolean checked) {
        setChecked(checked, false);
    }

    public void setChecked(boolean checked, boolean animate) {
        if (mIsChecked == checked && mThumbPosition == (checked ? 1.0f : 0.0f)) return;
        mIsChecked = checked;
        float target = checked ? 1.0f : 0.0f;

        if (animate) {
            if (mAnimator != null) mAnimator.cancel();
            mAnimator = ValueAnimator.ofFloat(mThumbPosition, target);
            mAnimator.setDuration(220);
            mAnimator.setInterpolator(new DecelerateInterpolator());
            mAnimator.addUpdateListener(anim -> {
                mThumbPosition = (float) anim.getAnimatedValue();
                invalidate();
            });
            mAnimator.start();
        } else {
            mThumbPosition = target;
            invalidate();
        }
    }

    public boolean isChecked() {
        return mIsChecked;
    }

    public void toggle() {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        setChecked(!mIsChecked, true);
        if (mListener != null) {
            mListener.onCheckedChanged(this, mIsChecked);
        }
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = ThemeTokens.dp(getContext(), 51);
        int h = ThemeTokens.dp(getContext(), 31);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        mTrackRect.set(0, 0, w, h);
        float radius = h / 2f;

        int offColor = ThemeTokens.isDarkMode(getContext()) ? 0xFF39393D : 0xFFE5E5EA;
        int onColor = 0xFF30D158;

        int r = (int) (Color.red(offColor) + (Color.red(onColor) - Color.red(offColor)) * mThumbPosition);
        int g = (int) (Color.green(offColor) + (Color.green(onColor) - Color.green(offColor)) * mThumbPosition);
        int b = (int) (Color.blue(offColor) + (Color.blue(onColor) - Color.blue(offColor)) * mThumbPosition);
        mTrackPaint.setColor(Color.rgb(r, g, b));

        canvas.drawRoundRect(mTrackRect, radius, radius, mTrackPaint);

        float padding = ThemeTokens.dp(getContext(), 2);
        float thumbDiameter = h - padding * 2;
        float thumbRadius = thumbDiameter / 2f;

        float minX = padding + thumbRadius;
        float maxX = w - padding - thumbRadius;
        float thumbCenterX = minX + (maxX - minX) * mThumbPosition;
        float thumbCenterY = h / 2f;

        canvas.drawCircle(thumbCenterX, thumbCenterY + ThemeTokens.dp(getContext(), 1), thumbRadius, mShadowPaint);
        canvas.drawCircle(thumbCenterX, thumbCenterY, thumbRadius, mThumbPaint);
    }
}
