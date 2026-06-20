package com.example.focus_flow.feature.focus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import com.example.focus_flow.R;

class FocusCountdownView extends View {
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private FocusTimerSnapshot snapshot;

    FocusCountdownView(Context context) {
        super(context);
        ringPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    void setSnapshot(FocusTimerSnapshot snapshot) {
        this.snapshot = snapshot;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int available = MeasureSpec.getSize(widthMeasureSpec);
        int size = Math.min(available, dp(280));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int size = Math.min(getWidth(), getHeight());
        float stroke = dp(16);
        float radiusPadding = stroke / 2f + dp(4);
        arcBounds.set(radiusPadding, radiusPadding, size - radiusPadding, size - radiusPadding);

        ringPaint.setStrokeWidth(stroke);
        ringPaint.setColor(getContext().getColor(R.color.glass_stroke));
        canvas.drawArc(arcBounds, -90, 360, false, ringPaint);

        progressPaint.setStrokeWidth(stroke);
        progressPaint.setColor(getContext().getColor(R.color.focus_cyan));
        float sweep = snapshot == null ? 0 : snapshot.progress * 360f;
        canvas.drawArc(arcBounds, -90, sweep, false, progressPaint);

        textPaint.setColor(getContext().getColor(R.color.text_primary));
        textPaint.setTextSize(dp(34));
        String remaining = snapshot == null ? "00:00" : formatClock(snapshot.remainingSeconds);
        canvas.drawText(remaining, size / 2f, size / 2f + dp(6), textPaint);

        labelPaint.setColor(getContext().getColor(R.color.text_secondary));
        labelPaint.setTextSize(dp(14));
        String label = snapshot == null ? "待开始" : (snapshot.paused ? "已暂停" : "沉浸专注中");
        canvas.drawText(label, size / 2f, size / 2f + dp(42), labelPaint);
    }

    private String formatClock(int totalSeconds) {
        int minutes = Math.max(0, totalSeconds) / 60;
        int seconds = Math.max(0, totalSeconds) % 60;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
