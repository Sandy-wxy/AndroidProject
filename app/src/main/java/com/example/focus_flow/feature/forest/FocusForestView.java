package com.example.focus_flow.feature.forest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.focus_flow.core.model.FocusSessionStatus;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.domain.forest.ForestEvolutionEngine;
import com.example.focus_flow.domain.forest.ForestState;
import com.example.focus_flow.domain.forest.ForestTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FocusForestView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<ForestTree> trees = new ArrayList<>();
    private ForestState state;

    public FocusForestView(Context context) {
        super(context);
    }

    public FocusForestView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setTrees(List<FocusSessionRecord> sessions) {
        setForestState(new ForestEvolutionEngine().evaluate(sessions, System.currentTimeMillis()));
    }

    public void setForestState(ForestState forestState) {
        state = forestState;
        trees.clear();
        if (forestState != null) {
            trees.addAll(forestState.trees);
        }
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        paint.setColor(backgroundColor());
        canvas.drawRoundRect(new RectF(0, 0, width, height), 36, 36, paint);
        drawSun(canvas, width * 0.82f, height * 0.17f);
        drawCloud(canvas, width * 0.22f, height * 0.19f);

        paint.setColor(Color.rgb(154, 224, 175));
        Path rearHill = new Path();
        rearHill.moveTo(0, height * 0.63f);
        rearHill.quadTo(width * 0.28f, height * 0.42f, width * 0.56f, height * 0.61f);
        rearHill.quadTo(width * 0.78f, height * 0.48f, width, height * 0.60f);
        rearHill.lineTo(width, height);
        rearHill.lineTo(0, height);
        rearHill.close();
        canvas.drawPath(rearHill, paint);

        paint.setColor(Color.rgb(78, 181, 119));
        Path frontHill = new Path();
        frontHill.moveTo(0, height * 0.74f);
        frontHill.quadTo(width * 0.32f, height * 0.55f, width * 0.62f, height * 0.76f);
        frontHill.quadTo(width * 0.84f, height * 0.63f, width, height * 0.72f);
        frontHill.lineTo(width, height);
        frontHill.lineTo(0, height);
        frontHill.close();
        canvas.drawPath(frontHill, paint);

        if (trees.isEmpty()) {
            drawSeedling(canvas, width * 0.5f, height * 0.76f, 1.3f);
            paint.setColor(Color.rgb(48, 92, 68));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(14));
            paint.setFakeBoldText(true);
            canvas.drawText("完成一次 25 分钟专注，种下第一棵树",
                    width * 0.5f, height * 0.92f, paint);
            paint.setFakeBoldText(false);
            return;
        }

        int visible = Math.min(30, trees.size());
        Random random = new Random(20260622L);
        for (int i = 0; i < visible; i++) {
            ForestTree tree = trees.get(trees.size() - visible + i);
            float x = width * (0.08f + random.nextFloat() * 0.84f);
            float depth = random.nextFloat();
            float y = height * (0.57f + depth * 0.31f);
            float scale = 0.55f + depth * 0.65f;
            drawForestTree(canvas, tree, x, y, scale);
        }
    }

    private int backgroundColor() {
        if (state == null || state.level == null) {
            return Color.rgb(222, 248, 239);
        }
        if (state.level.levelNumber >= 5) {
            return Color.rgb(214, 238, 250);
        }
        if (state.level.levelNumber >= 4) {
            return Color.rgb(220, 242, 225);
        }
        if (state.level.levelNumber >= 3) {
            return Color.rgb(222, 248, 239);
        }
        return Color.rgb(232, 248, 229);
    }

    private void drawForestTree(Canvas canvas, ForestTree tree, float x, float y, float scale) {
        float sizedScale = scale;
        if (tree.sizeTier == ForestTree.SizeTier.TALL) {
            sizedScale *= 1.12f;
        } else if (tree.sizeTier == ForestTree.SizeTier.GIANT) {
            sizedScale *= 1.28f;
        }
        switch (tree.species) {
            case PINE:
                drawPine(canvas, x, y, sizedScale);
                break;
            case BLOOM:
                drawBloomTree(canvas, x, y, sizedScale);
                break;
            case CYPRESS:
                drawCypress(canvas, x, y, sizedScale);
                break;
            case MAPLE:
                drawGoldenTree(canvas, x, y, sizedScale);
                break;
            case BAMBOO:
                drawBamboo(canvas, x, y, sizedScale);
                break;
            case BROADLEAF:
            default:
                drawTree(canvas, x, y, sizedScale);
                break;
        }
    }

    private void drawTree(Canvas canvas, float x, float y, float scale) {
        paint.setColor(Color.rgb(116, 78, 47));
        canvas.drawRoundRect(x - 4 * scale, y - 30 * scale,
                x + 4 * scale, y + 5 * scale, 4, 4, paint);
        paint.setColor(Color.rgb(35, 148, 91));
        canvas.drawCircle(x, y - 42 * scale, 20 * scale, paint);
        paint.setColor(Color.rgb(64, 185, 111));
        canvas.drawCircle(x - 13 * scale, y - 35 * scale, 13 * scale, paint);
        canvas.drawCircle(x + 14 * scale, y - 36 * scale, 14 * scale, paint);
    }

    private void drawPine(Canvas canvas, float x, float y, float scale) {
        paint.setColor(Color.rgb(101, 72, 43));
        canvas.drawRect(x - 4 * scale, y - 31 * scale, x + 4 * scale, y + 5 * scale, paint);
        paint.setColor(Color.rgb(18, 119, 77));
        triangle(canvas, x, y - 62 * scale, 24 * scale, 38 * scale);
        paint.setColor(Color.rgb(30, 151, 91));
        triangle(canvas, x, y - 48 * scale, 29 * scale, 42 * scale);
    }

    private void drawGoldenTree(Canvas canvas, float x, float y, float scale) {
        paint.setColor(Color.rgb(118, 77, 42));
        canvas.drawRoundRect(x - 5 * scale, y - 35 * scale,
                x + 5 * scale, y + 6 * scale, 4, 4, paint);
        paint.setColor(Color.rgb(244, 169, 48));
        canvas.drawCircle(x, y - 47 * scale, 23 * scale, paint);
        paint.setColor(Color.rgb(255, 197, 66));
        canvas.drawCircle(x - 16 * scale, y - 38 * scale, 13 * scale, paint);
        canvas.drawCircle(x + 16 * scale, y - 39 * scale, 14 * scale, paint);
    }

    private void drawBloomTree(Canvas canvas, float x, float y, float scale) {
        paint.setColor(Color.rgb(112, 73, 48));
        canvas.drawRoundRect(x - 4 * scale, y - 30 * scale,
                x + 4 * scale, y + 5 * scale, 4, 4, paint);
        paint.setColor(Color.rgb(245, 122, 169));
        canvas.drawCircle(x, y - 43 * scale, 19 * scale, paint);
        paint.setColor(Color.rgb(255, 168, 198));
        canvas.drawCircle(x - 14 * scale, y - 35 * scale, 12 * scale, paint);
        canvas.drawCircle(x + 14 * scale, y - 36 * scale, 12 * scale, paint);
    }

    private void drawCypress(Canvas canvas, float x, float y, float scale) {
        paint.setColor(Color.rgb(89, 67, 46));
        canvas.drawRect(x - 3 * scale, y - 34 * scale, x + 3 * scale, y + 5 * scale, paint);
        paint.setColor(Color.rgb(21, 109, 93));
        canvas.drawOval(new RectF(x - 13 * scale, y - 78 * scale,
                x + 13 * scale, y - 18 * scale), paint);
        paint.setColor(Color.rgb(34, 145, 119));
        canvas.drawOval(new RectF(x - 9 * scale, y - 68 * scale,
                x + 9 * scale, y - 28 * scale), paint);
    }

    private void drawBamboo(Canvas canvas, float x, float y, float scale) {
        paint.setColor(Color.rgb(51, 151, 78));
        canvas.drawRoundRect(x - 3 * scale, y - 62 * scale,
                x + 3 * scale, y + 4 * scale, 3, 3, paint);
        canvas.drawRoundRect(x + 8 * scale, y - 52 * scale,
                x + 14 * scale, y + 4 * scale, 3, 3, paint);
        paint.setColor(Color.rgb(77, 188, 105));
        canvas.drawOval(new RectF(x - 25 * scale, y - 58 * scale,
                x - 1 * scale, y - 42 * scale), paint);
        canvas.drawOval(new RectF(x + 10 * scale, y - 47 * scale,
                x + 36 * scale, y - 31 * scale), paint);
    }

    private void drawSeedling(Canvas canvas, float x, float y, float scale) {
        paint.setColor(Color.rgb(31, 128, 75));
        canvas.drawRoundRect(x - 2 * scale, y - 28 * scale,
                x + 2 * scale, y + 2 * scale, 3, 3, paint);
        paint.setColor(Color.rgb(50, 174, 95));
        canvas.drawOval(new RectF(x - 25 * scale, y - 34 * scale,
                x - 1 * scale, y - 18 * scale), paint);
        canvas.drawOval(new RectF(x + 1 * scale, y - 42 * scale,
                x + 27 * scale, y - 23 * scale), paint);
    }

    private void triangle(Canvas canvas, float x, float top, float halfWidth, float height) {
        Path path = new Path();
        path.moveTo(x, top);
        path.lineTo(x - halfWidth, top + height);
        path.lineTo(x + halfWidth, top + height);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawSun(Canvas canvas, float x, float y) {
        paint.setColor(Color.rgb(255, 205, 72));
        canvas.drawCircle(x, y, dp(23), paint);
    }

    private void drawCloud(Canvas canvas, float x, float y) {
        paint.setColor(Color.argb(225, 255, 255, 255));
        canvas.drawCircle(x - dp(22), y + dp(7), dp(17), paint);
        canvas.drawCircle(x, y, dp(24), paint);
        canvas.drawCircle(x + dp(27), y + dp(8), dp(16), paint);
        canvas.drawRoundRect(x - dp(40), y + dp(7), x + dp(43),
                y + dp(25), dp(12), dp(12), paint);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
