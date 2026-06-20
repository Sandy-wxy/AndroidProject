package com.example.focus_flow.feature.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.focus_flow.R;
import com.example.focus_flow.feature.tasks.TaskUi;

public class StatsLineChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();
    private final Path fillPath = new Path();
    private int[] values = new int[0];
    private String[] labels = new String[0];

    public StatsLineChartView(Context context) {
        super(context);
        init();
    }

    public StatsLineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setData(int[] values, String[] labels) {
        this.values = values == null ? new int[0] : values;
        this.labels = labels == null ? new String[0] : labels;
        invalidate();
    }

    private void init() {
        setMinimumHeight(TaskUi.dp(getContext(), 210));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                resolveSize(TaskUi.dp(getContext(), 220), heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = TaskUi.dp(getContext(), 22);
        float right = getWidth() - TaskUi.dp(getContext(), 18);
        float top = TaskUi.dp(getContext(), 28);
        float bottom = getHeight() - TaskUi.dp(getContext(), 42);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(TaskUi.dp(getContext(), 1));
        paint.setColor(getContext().getColor(R.color.glass_stroke));
        for (int i = 0; i < 4; i++) {
            float y = top + (bottom - top) * i / 3f;
            canvas.drawLine(left, y, right, y, paint);
        }

        int max = 0;
        for (int value : values) {
            max = Math.max(max, value);
        }
        if (values.length == 0 || max <= 0) {
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(TaskUi.dp(getContext(), 15));
            paint.setColor(getContext().getColor(R.color.text_secondary));
            canvas.drawText("暂无折线趋势数据", getWidth() / 2f, getHeight() / 2f, paint);
            drawLabels(canvas, left, right, bottom);
            return;
        }

        linePath.reset();
        fillPath.reset();
        float slot = values.length == 1 ? 0 : (right - left) / (values.length - 1f);
        for (int i = 0; i < values.length; i++) {
            float x = left + slot * i;
            float ratio = values[i] / (float) max;
            float y = bottom - ratio * (bottom - top);
            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, bottom);
                fillPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }
        fillPath.lineTo(right, bottom);
        fillPath.close();

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, top, 0, bottom,
                0x5538BDF8, 0x0538BDF8, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(TaskUi.dp(getContext(), 4));
        paint.setColor(getContext().getColor(R.color.focus_cyan));
        canvas.drawPath(linePath, paint);

        for (int i = 0; i < values.length; i++) {
            float x = left + slot * i;
            float y = bottom - values[i] / (float) max * (bottom - top);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(getContext().getColor(R.color.glass_card_strong));
            canvas.drawCircle(x, y, TaskUi.dp(getContext(), 6), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(TaskUi.dp(getContext(), 3));
            paint.setColor(getContext().getColor(R.color.focus_cyan));
            canvas.drawCircle(x, y, TaskUi.dp(getContext(), 5), paint);
        }
        drawLabels(canvas, left, right, bottom);
    }

    private void drawLabels(Canvas canvas, float left, float right, float bottom) {
        if (labels.length == 0) {
            return;
        }
        float slot = labels.length == 1 ? 0 : (right - left) / (labels.length - 1f);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(TaskUi.dp(getContext(), 11));
        paint.setColor(getContext().getColor(R.color.text_weak));
        for (int i = 0; i < labels.length; i++) {
            canvas.drawText(labels[i], left + slot * i,
                    bottom + TaskUi.dp(getContext(), 25), paint);
        }
    }
}
