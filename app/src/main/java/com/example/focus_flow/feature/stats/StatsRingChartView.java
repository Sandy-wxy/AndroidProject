package com.example.focus_flow.feature.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.focus_flow.R;
import com.example.focus_flow.feature.tasks.TaskUi;

import java.util.ArrayList;
import java.util.List;

public class StatsRingChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private List<Segment> segments = new ArrayList<>();

    public StatsRingChartView(Context context) {
        super(context);
        init();
    }

    public StatsRingChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setSegments(List<Segment> segments) {
        this.segments = segments == null ? new ArrayList<>() : segments;
        invalidate();
    }

    private void init() {
        setMinimumHeight(TaskUi.dp(getContext(), 210));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = TaskUi.dp(getContext(), 220);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height) - TaskUi.dp(getContext(), 44);
        int cx = width / 2;
        int cy = height / 2;
        oval.set(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f);

        float total = 0f;
        for (Segment segment : segments) {
            total += Math.max(0, segment.value);
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(TaskUi.dp(getContext(), 22));
        paint.setStrokeCap(Paint.Cap.ROUND);
        if (total <= 0f) {
            paint.setColor(getContext().getColor(R.color.glass_stroke));
            canvas.drawArc(oval, -90, 360, false, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(TaskUi.dp(getContext(), 15));
            paint.setColor(getContext().getColor(R.color.text_secondary));
            canvas.drawText("暂无科目数据", cx, cy + TaskUi.dp(getContext(), 5), paint);
            return;
        }

        float start = -90f;
        for (Segment segment : segments) {
            if (segment.value <= 0) {
                continue;
            }
            float sweep = 360f * segment.value / total;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(TaskUi.dp(getContext(), 22));
            paint.setColor(segment.color);
            canvas.drawArc(oval, start, sweep, false, paint);
            start += sweep;
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(TaskUi.dp(getContext(), 14));
        paint.setColor(getContext().getColor(R.color.text_weak));
        canvas.drawText("科目占比", cx, cy - TaskUi.dp(getContext(), 4), paint);
        paint.setTextSize(TaskUi.dp(getContext(), 18));
        paint.setColor(getContext().getColor(R.color.text_primary));
        canvas.drawText(formatMinutes((int) total), cx, cy + TaskUi.dp(getContext(), 22), paint);
    }

    private String formatMinutes(int seconds) {
        int minutes = Math.max(0, seconds) / 60;
        if (minutes >= 60) {
            return (minutes / 60) + "小时" + (minutes % 60) + "分";
        }
        return minutes + "分钟";
    }

    public static class Segment {
        public final String label;
        public final int value;
        public final int color;

        public Segment(String label, int value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }
}
