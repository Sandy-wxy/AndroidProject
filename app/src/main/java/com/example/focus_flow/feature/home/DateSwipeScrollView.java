package com.example.focus_flow.feature.home;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ScrollView;

public class DateSwipeScrollView extends ScrollView {
    public interface OnDateSwipeListener {
        void onSwipeDays(int days);
    }

    private final int touchSlop;
    private float downX;
    private float downY;
    private boolean horizontalSwipe;
    private OnDateSwipeListener listener;

    public DateSwipeScrollView(Context context) {
        super(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setOnDateSwipeListener(OnDateSwipeListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
            horizontalSwipe = false;
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && !horizontalSwipe) {
            float dx = event.getX() - downX;
            float dy = event.getY() - downY;
            if (Math.abs(dx) > touchSlop * 2f && Math.abs(dx) > Math.abs(dy) * 1.25f) {
                horizontalSwipe = true;
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);
                cancel.recycle();
                return true;
            }
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP && horizontalSwipe) {
            float dx = event.getX() - downX;
            if (Math.abs(dx) >= touchSlop * 8f && listener != null) {
                listener.onSwipeDays(dx < 0 ? 1 : -1);
            }
            horizontalSwipe = false;
            return true;
        } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            horizontalSwipe = false;
        }
        if (horizontalSwipe) {
            return true;
        }
        return super.dispatchTouchEvent(event);
    }
}
