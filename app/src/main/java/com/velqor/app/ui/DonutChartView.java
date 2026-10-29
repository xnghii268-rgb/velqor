package com.velqor.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

import com.velqor.app.tokens.ThemeTokens;

import java.util.ArrayList;
import java.util.List;

public class DonutChartView extends View {

    public static class Segment {
        public final String label;
        public final long value;
        public final int color;

        public Segment(String label, long value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBounds = new RectF();
    private final List<Segment> mSegments = new ArrayList<>();
    private long mTotal = 0;
    private String mCenterTitle = "";
    private String mCenterSubtitle = "";

    public DonutChartView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<Segment> segments, long total, String centerTitle, String centerSubtitle) {
        mSegments.clear();
        if (segments != null) {
            mSegments.addAll(segments);
        }
        mTotal = total;
        mCenterTitle = (centerTitle != null) ? centerTitle : "";
        mCenterSubtitle = (centerSubtitle != null) ? centerSubtitle : "";
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float strokeWidth = ThemeTokens.dp(getContext(), 18);
        mPaint.setStrokeWidth(strokeWidth);

        float halfStroke = strokeWidth / 2f;
        float padding = ThemeTokens.dp(getContext(), 16);
        float size = Math.min(w, h) - padding * 2 - strokeWidth;
        float left = (w - size) / 2f;
        float top = (h - size) / 2f;
        mBounds.set(left, top, left + size, top + size);

        mPaint.setColor(0x18FFFFFF);
        canvas.drawOval(mBounds, mPaint);

        if (mTotal > 0 && !mSegments.isEmpty()) {
            float startAngle = -90f;
            float gapAngle = 3f;
            for (Segment seg : mSegments) {
                if (seg.value <= 0) continue;
                float sweepAngle = (float) ((double) seg.value / mTotal * 360.0);
                if (sweepAngle > gapAngle) {
                    mPaint.setColor(seg.color);
                    canvas.drawArc(mBounds, startAngle + gapAngle / 2f, sweepAngle - gapAngle, false, mPaint);
                }
                startAngle += sweepAngle;
            }
        }

        float centerX = w / 2f;
        float centerY = h / 2f;

        mTextPaint.setColor(ThemeTokens.getLabelColor(getContext()));
        mTextPaint.setTextSize(ThemeTokens.dp(getContext(), 20));
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText(mCenterTitle, centerX, centerY - ThemeTokens.dp(getContext(), 2), mTextPaint);

        mTextPaint.setColor(ThemeTokens.getSecondaryLabelColor(getContext()));
        mTextPaint.setTextSize(ThemeTokens.dp(getContext(), 12));
        mTextPaint.setTypeface(Typeface.DEFAULT);
        canvas.drawText(mCenterSubtitle, centerX, centerY + ThemeTokens.dp(getContext(), 16), mTextPaint);
    }
}
