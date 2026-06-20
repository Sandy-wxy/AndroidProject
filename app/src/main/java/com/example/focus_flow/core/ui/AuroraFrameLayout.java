package com.example.focus_flow.core.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AuroraFrameLayout extends FrameLayout {
    public AuroraFrameLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public AuroraFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AuroraFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        AuroraBackgroundPainter.draw(getContext(), canvas, getWidth(), getHeight());
        super.onDraw(canvas);
    }
}
