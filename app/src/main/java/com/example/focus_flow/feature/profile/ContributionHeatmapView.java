package com.example.focus_flow.feature.profile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.example.focus_flow.R;
import com.example.focus_flow.feature.tasks.TaskUi;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ContributionHeatmapView extends View {
    public interface OnDaySelectedListener {
        void onDaySelected(long dayStartMillis);
    }

    public static class DayContribution {
        public int completedTasks;
        public int focusSeconds;

        public int score() {
            return completedTasks * 25 + focusSeconds / 60;
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF cell = new RectF();
    private final Map<Long, DayContribution> contributions = new HashMap<>();
    private final int cellSize;
    private final int gap;
    private final int labelWidth;
    private final int monthHeight;
    private long firstDay;
    private long lastDay;
    private long selectedDay;
    private int weekCount = 53;
    private OnDaySelectedListener listener;

    public ContributionHeatmapView(Context context) {
        super(context);
        cellSize = TaskUi.dp(context, 12);
        gap = TaskUi.dp(context, 3);
        labelWidth = TaskUi.dp(context, 28);
        monthHeight = TaskUi.dp(context, 22);
        setClickable(true);
    }

    public void setData(long firstDay, long lastDay,
                        Map<Long, DayContribution> values, long selectedDay) {
        this.firstDay = normalize(firstDay);
        this.lastDay = normalize(lastDay);
        this.selectedDay = normalize(selectedDay);
        contributions.clear();
        if (values != null) {
            contributions.putAll(values);
        }
        weekCount = Math.max(1, (int) ((this.lastDay - this.firstDay) / DAY_MILLIS / 7L) + 1);
        requestLayout();
        invalidate();
    }

    public void setOnDaySelectedListener(OnDaySelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = labelWidth + weekCount * (cellSize + gap) + gap;
        int desiredHeight = monthHeight + 7 * (cellSize + gap) + TaskUi.dp(getContext(), 8);
        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawWeekdayLabels(canvas);
        drawMonthsAndCells(canvas);
    }

    private void drawWeekdayLabels(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(TaskUi.dp(getContext(), 9));
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(getContext().getColor(R.color.text_weak));
        String[] labels = {"", "一", "", "三", "", "五", ""};
        for (int row = 0; row < labels.length; row++) {
            if (labels[row].isEmpty()) {
                continue;
            }
            float y = monthHeight + row * (cellSize + gap) + cellSize * 0.82f;
            canvas.drawText(labels[row], 0, y, paint);
        }
    }

    private void drawMonthsAndCells(Canvas canvas) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(firstDay);
        SimpleDateFormat monthFormat = new SimpleDateFormat("M月", Locale.getDefault());
        int previousMonth = -1;
        long day = firstDay;
        while (day <= lastDay) {
            calendar.setTimeInMillis(day);
            int week = (int) ((day - firstDay) / DAY_MILLIS / 7L);
            int row = dayRow(calendar);
            float left = labelWidth + week * (cellSize + gap);
            float top = monthHeight + row * (cellSize + gap);

            int month = calendar.get(Calendar.MONTH);
            if (month != previousMonth && calendar.get(Calendar.DAY_OF_MONTH) <= 7) {
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(TaskUi.dp(getContext(), 10));
                paint.setTextAlign(Paint.Align.LEFT);
                paint.setColor(getContext().getColor(R.color.text_weak));
                canvas.drawText(monthFormat.format(new Date(day)), left, TaskUi.dp(getContext(), 12), paint);
                previousMonth = month;
            }

            cell.set(left, top, left + cellSize, top + cellSize);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colorFor(contributions.get(normalize(day))));
            canvas.drawRoundRect(cell, TaskUi.dp(getContext(), 2), TaskUi.dp(getContext(), 2), paint);
            if (normalize(day) == selectedDay) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(TaskUi.dp(getContext(), 2));
                paint.setColor(getContext().getColor(R.color.focus_purple));
                canvas.drawRoundRect(cell, TaskUi.dp(getContext(), 3), TaskUi.dp(getContext(), 3), paint);
            }
            day += DAY_MILLIS;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) {
            return true;
        }
        int week = (int) ((event.getX() - labelWidth) / (cellSize + gap));
        int row = (int) ((event.getY() - monthHeight) / (cellSize + gap));
        if (week < 0 || week >= weekCount || row < 0 || row > 6) {
            return true;
        }
        long day = firstDay + (week * 7L + row) * DAY_MILLIS;
        if (day > lastDay) {
            return true;
        }
        selectedDay = normalize(day);
        invalidate();
        performClick();
        if (listener != null) {
            listener.onDaySelected(selectedDay);
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private int dayRow(Calendar calendar) {
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek == Calendar.SUNDAY ? 6 : dayOfWeek - Calendar.MONDAY;
    }

    private int colorFor(DayContribution contribution) {
        if (contribution == null || contribution.score() <= 0) {
            return getContext().getColor(R.color.glass_stroke);
        }
        int score = contribution.score();
        if (score < 25) {
            return 0xFFBAE6D3;
        }
        if (score < 50) {
            return 0xFF65C89D;
        }
        if (score < 90) {
            return 0xFF1F9D70;
        }
        return 0xFF087451;
    }

    private long normalize(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;
}
