package com.example.focus_flow.feature.tasks;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageButton;

import com.example.focus_flow.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public final class TaskUi {
    private TaskUi() {
    }

    public static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static TextView text(Context context, String text, int sp, int color, int style) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setIncludeFontPadding(false);
        view.setLineSpacing(dp(context, 2), 1.0f);
        return view;
    }

    public static MaterialCardView glassCard(Context context) {
        MaterialCardView card = new MaterialCardView(context);
        card.setCardBackgroundColor(context.getColor(R.color.glass_card));
        card.setStrokeColor(context.getColor(R.color.glass_stroke));
        card.setStrokeWidth(dp(context, 1));
        card.setRadius(dp(context, 24));
        card.setCardElevation(dp(context, 3));
        card.setUseCompatPadding(true);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    public static LinearLayout vertical(Context context, int paddingDp) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, paddingDp);
        layout.setPadding(padding, padding, padding, padding);
        return layout;
    }

    public static LinearLayout horizontal(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    public static MaterialButton button(Context context, String text, boolean filled) {
        MaterialButton button = new MaterialButton(context);
        button.setText(text);
        button.setAllCaps(false);
        button.setCornerRadius(dp(context, 18));
        if (!filled) {
            button.setBackgroundColor(context.getColor(android.R.color.transparent));
            button.setStrokeWidth(dp(context, 1));
            button.setStrokeColorResource(R.color.glass_stroke);
            button.setTextColor(context.getColor(R.color.text_secondary));
        }
        return button;
    }

    public static AppCompatImageButton iconButton(Context context, int iconResId) {
        AppCompatImageButton button = new AppCompatImageButton(context);
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(context.getColor(R.color.glass_card_strong));
        background.setStroke(dp(context, 1), context.getColor(R.color.glass_stroke));
        button.setBackground(background);
        button.setImageResource(iconResId);
        button.setImageTintList(ColorStateList.valueOf(context.getColor(R.color.focus_cyan)));
        button.setScaleType(android.widget.ImageView.ScaleType.CENTER);
        button.setPadding(0, 0, 0, 0);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        return button;
    }

    public static View spacer(Context context, int heightDp) {
        View view = new View(context);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(context, heightDp)));
        return view;
    }
}
