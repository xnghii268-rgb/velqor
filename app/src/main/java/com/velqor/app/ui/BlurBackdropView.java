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
    private int mStrokeWidth = 2; // ~1dp
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mClipPath = new Path();
    private final RectF mRect = new RectF();

    // RenderNode for Hardware Blur (API 31+)
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

        // 1. Specular Rim Gradient: Pure neutral white (35% top-left -> 5% bottom-right), NO red/color tint
        if (w > 0 && h > 0) {
            LinearGradient rimShader = new LinearGradient(
                    0, 0, w, h,
                    new int[]{ThemeTokens.COLOR_RIM_START, ThemeTokens.COLOR_RIM_END},
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

        // API 31+ Hardware Accelerated Realtime Backdrop Blur
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

                    // Calculate coordinate offset from target view to this backdrop view
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

                    // 1. Base dark glass tint under blur (alpha ~0.75) to hide text behind
                    mPaint.setColor(0xC01C1C1E);
                    canvas.drawRect(mRect, mPaint);

                    // 2. Subtle frosted glass white tint overlay (alpha ~0.06)
                    mPaint.setColor(0x10FFFFFF);
                    canvas.drawRect(mRect, mPaint);

                    blurredSuccessfully = true;
                }
            } catch (Throwable ignored) {
                blurredSuccessfully = false;
            } finally {
                mIsRenderingBackdrop = false;
            }
        }

        // Fallback for API < 31 or if render node fails: 94-96% alpha glass surface
        if (!blurredSuccessfully) {
            mPaint.setColor(ThemeTokens.COLOR_SURFACE_GLASS);
            canvas.drawRect(mRect, mPaint);
        }

        canvas.restore();

        // 3. Draw Specular Rim Stroke cleanly without clipping (0.5-1dp, white alpha 0.35 -> 0.05)
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
