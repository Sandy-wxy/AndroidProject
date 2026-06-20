package com.example.focus_flow.core.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

public class AuroraCoordinatorLayout extends CoordinatorLayout {
    public AuroraCoordinatorLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public AuroraCoordinatorLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AuroraCoordinatorLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
    }

    @Override
    public void onDraw(Canvas canvas) {
        AuroraBackgroundPainter.draw(getContext(), canvas, getWidth(), getHeight());
        super.onDraw(canvas);
    }
}
