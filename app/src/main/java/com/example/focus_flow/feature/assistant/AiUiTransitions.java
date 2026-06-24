package com.example.focus_flow.feature.assistant;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public final class AiUiTransitions {
    // Tune text cross-fade here. Old text fades out while new text fades in.
    public static final long TEXT_CROSS_FADE_MS = 500L;
    // Tune container/list cross-fade here. This is the total visible overlap time.
    public static final long CONTAINER_CROSS_FADE_MS = 500L;

    private AiUiTransitions() {
    }

    public static void crossFadeText(TextView titleView, CharSequence title,
                                     TextView bodyView, CharSequence body,
                                     boolean animate) {
        if (titleView == null || bodyView == null) {
            return;
        }
        if (!animate) {
            titleView.animate().cancel();
            bodyView.animate().cancel();
            titleView.setText(title == null ? "" : title);
            bodyView.setText(body == null ? "" : body);
            titleView.setAlpha(1f);
            bodyView.setAlpha(1f);
            return;
        }
        crossFadeSingleText(titleView, title);
        crossFadeSingleText(bodyView, body);
    }

    private static void crossFadeSingleText(TextView targetView, CharSequence nextText) {
        targetView.animate().cancel();
        if (!(targetView.getParent() instanceof ViewGroup)
                || targetView.getWidth() <= 0 || targetView.getHeight() <= 0) {
            targetView.animate().alpha(0f).setDuration(TEXT_CROSS_FADE_MS / 2L).withEndAction(() -> {
                targetView.setText(nextText == null ? "" : nextText);
                targetView.animate().alpha(1f).setDuration(TEXT_CROSS_FADE_MS / 2L).start();
            }).start();
            return;
        }

        ViewGroup parent = (ViewGroup) targetView.getParent();
        TextView oldCopy = copyTextView(targetView);
        oldCopy.setAlpha(targetView.getAlpha());
        oldCopy.layout(0, 0, targetView.getWidth(), targetView.getHeight());
        oldCopy.setX(targetView.getX());
        oldCopy.setY(targetView.getY());
        parent.getOverlay().add(oldCopy);

        targetView.setText(nextText == null ? "" : nextText);
        targetView.setAlpha(0f);
        targetView.animate().alpha(1f).setDuration(TEXT_CROSS_FADE_MS).start();
        oldCopy.animate().alpha(0f).setDuration(TEXT_CROSS_FADE_MS).withEndAction(() -> {
            parent.getOverlay().remove(oldCopy);
            targetView.setAlpha(1f);
        }).start();
    }

    private static TextView copyTextView(TextView source) {
        TextView copy = new TextView(source.getContext());
        copy.setText(source.getText());
        copy.setTextColor(source.getTextColors());
        copy.setTextSize(TypedValue.COMPLEX_UNIT_PX, source.getTextSize());
        copy.setTypeface(source.getTypeface());
        copy.setGravity(source.getGravity());
        copy.setIncludeFontPadding(source.getIncludeFontPadding());
        if (source.getMaxLines() > 0) {
            copy.setMaxLines(source.getMaxLines());
        }
        TextUtils.TruncateAt ellipsize = source.getEllipsize();
        if (ellipsize != null) {
            copy.setEllipsize(ellipsize);
        }
        copy.setPadding(source.getPaddingLeft(), source.getPaddingTop(),
                source.getPaddingRight(), source.getPaddingBottom());
        return copy;
    }

    public static void crossFadeChildren(ViewGroup container, Runnable update, boolean animate) {
        crossFadeView(container, update, animate);
    }

    public static void crossFadeView(View view, Runnable update, boolean animate) {
        if (view == null || update == null) {
            return;
        }
        view.animate().cancel();
        if (!animate) {
            update.run();
            view.setAlpha(1f);
            return;
        }
        if (!(view.getParent() instanceof ViewGroup) || view.getWidth() <= 0 || view.getHeight() <= 0) {
            view.animate().alpha(0f).setDuration(CONTAINER_CROSS_FADE_MS / 2L).withEndAction(() -> {
                update.run();
                view.setAlpha(0f);
                view.animate().alpha(1f).setDuration(CONTAINER_CROSS_FADE_MS / 2L).start();
            }).start();
            return;
        }

        ViewGroup parent = (ViewGroup) view.getParent();
        BitmapDrawable oldSnapshot = snapshot(view);
        oldSnapshot.setBounds(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
        parent.getOverlay().add(oldSnapshot);
        update.run();
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(CONTAINER_CROSS_FADE_MS).start();

        ValueAnimator fadeOutOld = ValueAnimator.ofInt(255, 0);
        fadeOutOld.setDuration(CONTAINER_CROSS_FADE_MS);
        fadeOutOld.addUpdateListener(animation -> {
            oldSnapshot.setAlpha((Integer) animation.getAnimatedValue());
            parent.invalidate();
        });
        fadeOutOld.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                parent.getOverlay().remove(oldSnapshot);
                view.setAlpha(1f);
            }
        });
        fadeOutOld.start();
    }

    private static BitmapDrawable snapshot(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        BitmapDrawable drawable = new BitmapDrawable(view.getResources(), bitmap);
        drawable.setBounds(0, 0, view.getWidth(), view.getHeight());
        return drawable;
    }
}
