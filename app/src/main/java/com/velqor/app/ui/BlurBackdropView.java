package com.velqor.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

import com.velqor.app.tokens.ThemeTokens;

public class BlurBackdropView extends View {
    private View mTargetView;
    private float mCornerRadius;
    private int mStrokeWidth = 2;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mClipPath = new Path();
    private final RectF mRect = new RectF();

    private Object mRenderNode;
    private Object mBlurEffect;
    private boolean mBlurInitialized = false;
    private boolean mIsRenderingBackdrop = false;

    public BlurBackdropView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mStrokeWidth = Math.max(1, ThemeTokens.dp(getContext(), 1));
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(mStrokeWidth);
        mPaint.setStyle(Paint.Style.FILL);
    }

    public void setCornerRadius(float radius) {
        mCornerRadius = radius;
        updateClipPath();
        invalidate();
    }

    public void setBackdropSource(View target) {
        mTargetView = target;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mRect.set(0, 0, w, h);
        updateClipPath();

        if (w > 0 && h > 0) {
            boolean isDark = ThemeTokens.isDarkMode(getContext());
            int rimStart = isDark ? ThemeTokens.COLOR_RIM_START : 0x4D000000;
            int rimEnd = isDark ? ThemeTokens.COLOR_RIM_END : 0x0A000000;
            LinearGradient rimShader = new LinearGradient(
                    0, 0, w, h,
                    new int[]{rimStart, rimEnd},
                    new float[]{0f, 1f},
                    Shader.TileMode.CLAMP
            );
            mStrokePaint.setShader(rimShader);
        }
    }

    private void updateClipPath() {
        if (mRect.width() > 0 && mRect.height() > 0) {
            mClipPath.reset();
            mClipPath.addRoundRect(mRect, mCornerRadius, mCornerRadius, Path.Direction.CW);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.clipPath(mClipPath);

        boolean blurredSuccessfully = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && canvas.isHardwareAccelerated() && mTargetView != null && !mIsRenderingBackdrop) {
            try {
                if (!mBlurInitialized) {
                    initApi31RenderNode();
                }

                if (mRenderNode != null && mBlurEffect != null) {
                    mIsRenderingBackdrop = true;
                    android.graphics.RenderNode node = (android.graphics.RenderNode) mRenderNode;
                    node.setPosition(0, 0, getWidth(), getHeight());

                    android.graphics.RecordingCanvas recordingCanvas = node.beginRecording();

                    int[] locTarget = new int[2];
                    int[] locThis = new int[2];
                    mTargetView.getLocationInWindow(locTarget);
                    getLocationInWindow(locThis);

                    int dx = locTarget[0] - locThis[0];
                    int dy = locTarget[1] - locThis[1];

                    recordingCanvas.translate(dx, dy);
                    mTargetView.draw(recordingCanvas);

                    node.endRecording();

                    canvas.drawRenderNode(node);

                    boolean isDark = ThemeTokens.isDarkMode(getContext());
                    mPaint.setColor(isDark ? 0xC01C1C1E : 0xC0FFFFFF);
                    canvas.drawRect(mRect, mPaint);

                    mPaint.setColor(isDark ? 0x10FFFFFF : 0x08000000);
                    canvas.drawRect(mRect, mPaint);

                    blurredSuccessfully = true;
                }
            } catch (Throwable ignored) {
                blurredSuccessfully = false;
            } finally {
                mIsRenderingBackdrop = false;
            }
        }

        if (!blurredSuccessfully) {
            mPaint.setColor(ThemeTokens.getSurfaceGlassColor(getContext()));
            canvas.drawRect(mRect, mPaint);
        }

        canvas.restore();

        float halfStroke = mStrokeWidth / 2f;
        RectF strokeRect = new RectF(halfStroke, halfStroke, getWidth() - halfStroke, getHeight() - halfStroke);
        canvas.drawRoundRect(strokeRect, mCornerRadius - halfStroke, mCornerRadius - halfStroke, mStrokePaint);
    }

    private void initApi31RenderNode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                android.graphics.RenderNode node = new android.graphics.RenderNode("velqor_liquid_glass");
                float blurRadiusPx = ThemeTokens.dp(getContext(), 24);
                android.graphics.RenderEffect effect = android.graphics.RenderEffect.createBlurEffect(
                        blurRadiusPx, blurRadiusPx, Shader.TileMode.CLAMP);
                node.setRenderEffect(effect);
                mRenderNode = node;
                mBlurEffect = effect;
                mBlurInitialized = true;
            } catch (Throwable e) {
                mBlurInitialized = false;
            }
        }
    }
}
