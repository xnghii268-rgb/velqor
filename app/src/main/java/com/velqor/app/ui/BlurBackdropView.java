package com.velqor.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
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
    private int mStrokeColor = ThemeTokens.COLOR_SPECULAR_RIM;
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
        mStrokeWidth = ThemeTokens.dp(getContext(), 1);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(mStrokeWidth);
        mStrokePaint.setColor(mStrokeColor);

        mPaint.setStyle(Paint.Style.FILL);
    }

    public void setCornerRadius(float radius) {
        mCornerRadius = radius;
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
        mClipPath.reset();
        mClipPath.addRoundRect(mRect, mCornerRadius, mCornerRadius, Path.Direction.CW);
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

                    // Glass tint overlay on top of blurred content (Subtle Apple frosted glass)
                    mPaint.setColor(0x331C1C1E); // Frosted dark glass tint
                    canvas.drawRect(mRect, mPaint);

                    blurredSuccessfully = true;
                }
            } catch (Throwable ignored) {
                blurredSuccessfully = false;
            } finally {
                mIsRenderingBackdrop = false;
            }
        }

        // Fallback for API < 31 or if render node fails: 96% alpha glass surface
        if (!blurredSuccessfully) {
            mPaint.setColor(ThemeTokens.COLOR_SURFACE_GLASS);
            canvas.drawRect(mRect, mPaint);
        }

        // Draw Specular Rim Stroke
        canvas.drawRoundRect(mRect, mCornerRadius, mCornerRadius, mStrokePaint);

        canvas.restore();
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
