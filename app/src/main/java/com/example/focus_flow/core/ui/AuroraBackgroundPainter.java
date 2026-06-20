package com.example.focus_flow.core.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import com.example.focus_flow.R;

public final class AuroraBackgroundPainter {
    private AuroraBackgroundPainter() {
    }

    public static void draw(Context context, Canvas canvas, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setDither(true);

        paint.setShader(new LinearGradient(0, 0, width, height,
                new int[]{
                        context.getColor(R.color.app_bg_start),
                        context.getColor(R.color.app_bg_mid),
                        context.getColor(R.color.app_bg_end)
                },
                new float[]{0f, 0.52f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, paint);

        drawRibbon(context, canvas, paint, width, height, 0.10f,
                R.color.aurora_cyan, R.color.aurora_purple, 0.24f);
        drawRibbon(context, canvas, paint, width, height, 0.46f,
                R.color.aurora_mint, R.color.aurora_cyan, 0.20f);
        drawCornerGlow(context, canvas, paint, width, height);
        drawFrostTexture(context, canvas, paint, width, height);
    }

    private static void drawRibbon(Context context, Canvas canvas, Paint paint, int width, int height,
                                   float yBias, int startColor, int endColor, float thicknessRatio) {
        float thickness = height * thicknessRatio;
        float y = height * yBias;
        Path path = new Path();
        path.moveTo(-width * 0.18f, y + thickness * 0.30f);
        path.cubicTo(width * 0.20f, y - thickness * 0.65f,
                width * 0.48f, y + thickness * 0.85f,
                width * 1.18f, y - thickness * 0.15f);
        path.lineTo(width * 1.18f, y + thickness * 0.70f);
        path.cubicTo(width * 0.62f, y + thickness * 1.55f,
                width * 0.24f, y + thickness * 0.35f,
                -width * 0.18f, y + thickness * 1.05f);
        path.close();
        paint.setShader(new LinearGradient(0, y, width, y + thickness,
                context.getColor(startColor),
                context.getColor(endColor),
                Shader.TileMode.CLAMP));
        canvas.drawPath(path, paint);
        paint.setShader(null);
    }

    private static void drawCornerGlow(Context context, Canvas canvas, Paint paint, int width, int height) {
        paint.setShader(new RadialGradient(width * 0.92f, height * 0.08f, width * 0.62f,
                context.getColor(R.color.aurora_purple),
                android.graphics.Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawRect(new RectF(0, 0, width, height * 0.58f), paint);
        paint.setShader(new RadialGradient(width * 0.08f, height * 0.74f, width * 0.55f,
                context.getColor(R.color.aurora_cyan),
                android.graphics.Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawRect(new RectF(0, height * 0.34f, width, height), paint);
        paint.setShader(null);
    }

    private static void drawFrostTexture(Context context, Canvas canvas, Paint paint, int width, int height) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(context.getColor(R.color.frost_speck));
        for (int i = 0; i < 96; i++) {
            float x = width * (((i * 37) % 101) / 101f);
            float y = height * (((i * 53 + 17) % 113) / 113f);
            float radius = 0.7f + ((i % 4) * 0.25f);
            canvas.drawCircle(x, y, radius, paint);
        }
    }
}
