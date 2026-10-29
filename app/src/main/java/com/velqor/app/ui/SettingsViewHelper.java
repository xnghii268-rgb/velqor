package com.velqor.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.velqor.app.R;
import com.velqor.app.tokens.ThemeTokens;

public final class SettingsViewHelper {

    private SettingsViewHelper() {}

    public static View createProfileCard(Context ctx, String appName, String version, View.OnClickListener onClick) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(ThemeTokens.dp(ctx, 18), ThemeTokens.dp(ctx, 16),
                ThemeTokens.dp(ctx, 18), ThemeTokens.dp(ctx, 16));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(ThemeTokens.dp(ctx, 24));
        int startColor = ThemeTokens.isDarkMode(ctx) ? 0xFF2C2C2E : 0xFFFFFFFF;
        int endColor = ThemeTokens.isDarkMode(ctx) ? 0xFF1C1C1E : 0xFFF2F2F7;
        bg.setColors(new int[]{startColor, endColor});
        bg.setOrientation(GradientDrawable.Orientation.TL_BR);
        bg.setStroke(Math.max(1, ThemeTokens.dp(ctx, 1) / 2), 0x2AFFFFFF);
        card.setBackground(bg);

        FrameLayout logoBox = new FrameLayout(ctx);
        int boxSize = ThemeTokens.dp(ctx, 48);
        logoBox.setLayoutParams(new LinearLayout.LayoutParams(boxSize, boxSize));

        GradientDrawable logoBg = new GradientDrawable();
        logoBg.setCornerRadius(ThemeTokens.dp(ctx, 12));
        int accent = ThemeTokens.getAccentColor(ctx);
        logoBg.setColors(new int[]{accent, 0xFF5856D6});
        logoBg.setOrientation(GradientDrawable.Orientation.TL_BR);
        logoBox.setBackground(logoBg);

        TextView letter = new TextView(ctx);
        letter.setText("V");
        letter.setTextColor(Color.WHITE);
        letter.setTextSize(24);
        letter.setTypeface(Typeface.DEFAULT_BOLD);
        letter.setGravity(Gravity.CENTER);
        logoBox.addView(letter, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        card.addView(logoBox);

        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = ThemeTokens.dp(ctx, 14);

        TextView title = new TextView(ctx);
        title.setText(appName);
        title.setTextColor(ThemeTokens.getLabelColor(ctx));
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        textCol.addView(title);

        TextView sub = new TextView(ctx);
        sub.setText(version + " · Apple Liquid Glass");
        sub.setTextColor(ThemeTokens.getSecondaryLabelColor(ctx));
        sub.setTextSize(13);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = ThemeTokens.dp(ctx, 2);
        textCol.addView(sub, slp);

        card.addView(textCol, tlp);

        ImageView chevron = new ImageView(ctx);
        chevron.setImageResource(R.drawable.ic_chevron_right);
        chevron.setColorFilter(ThemeTokens.getSecondaryLabelColor(ctx));
        chevron.setAlpha(0.4f);
        int cSize = ThemeTokens.dp(ctx, 18);
        card.addView(chevron, new LinearLayout.LayoutParams(cSize, cSize));

        attachTapSpringEffect(card);
        card.setOnClickListener(onClick);
        card.setContentDescription(appName + " " + version);
        return card;
    }

    public static TextView createGroupHeader(Context ctx, String title) {
        TextView tv = new TextView(ctx);
        tv.setText(title.toUpperCase());
        tv.setTextColor(ThemeTokens.getSecondaryLabelColor(ctx));
        tv.setTextSize(13);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(ThemeTokens.dp(ctx, 16), ThemeTokens.dp(ctx, 20),
                ThemeTokens.dp(ctx, 16), ThemeTokens.dp(ctx, 6));
        return tv;
    }

    public static LinearLayout createGroupCard(Context ctx) {
        LinearLayout group = new LinearLayout(ctx);
        group.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(ThemeTokens.dp(ctx, 20));
        bg.setColor(ThemeTokens.getActionGroupBgColor(ctx));
        bg.setStroke(Math.max(1, ThemeTokens.dp(ctx, 1) / 2), 0x1AFFFFFF);
        group.setBackground(bg);
        return group;
    }

    public static View createRow(Context ctx, int iconRes, int iconBgColor, String title, String value,
                                boolean showChevron, View.OnClickListener onClick, boolean showDivider) {
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(ThemeTokens.dp(ctx, 52));
        row.setPadding(ThemeTokens.dp(ctx, 16), ThemeTokens.dp(ctx, 10),
                ThemeTokens.dp(ctx, 16), ThemeTokens.dp(ctx, 10));

        FrameLayout iconBox = new FrameLayout(ctx);
        int bSize = ThemeTokens.dp(ctx, 30);
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(bSize, bSize));

        GradientDrawable ibBg = new GradientDrawable();
        ibBg.setCornerRadius(ThemeTokens.dp(ctx, 8));
        ibBg.setColor(iconBgColor);
        iconBox.setBackground(ibBg);

        ImageView iv = new ImageView(ctx);
        iv.setImageResource(iconRes);
        iv.setColorFilter(Color.WHITE);
        int iSize = ThemeTokens.dp(ctx, 18);
        FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(iSize, iSize);
        ivLp.gravity = Gravity.CENTER;
        iconBox.addView(iv, ivLp);

        row.addView(iconBox);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextColor(ThemeTokens.getLabelColor(ctx));
        tvTitle.setTextSize(17);
        tvTitle.setSingleLine(true);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = ThemeTokens.dp(ctx, 14);
        row.addView(tvTitle, tlp);

        if (value != null && !value.isEmpty()) {
            TextView tvVal = new TextView(ctx);
            tvVal.setText(value);
            tvVal.setTextColor(ThemeTokens.getSecondaryLabelColor(ctx));
            tvVal.setTextSize(15);
            tvVal.setSingleLine(true);
            LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            vlp.rightMargin = showChevron ? ThemeTokens.dp(ctx, 6) : 0;
            row.addView(tvVal, vlp);
        }

        if (showChevron) {
            ImageView chevron = new ImageView(ctx);
            chevron.setImageResource(R.drawable.ic_chevron_right);
            chevron.setColorFilter(ThemeTokens.getSecondaryLabelColor(ctx));
            chevron.setAlpha(0.35f);
            int cSize = ThemeTokens.dp(ctx, 16);
            row.addView(chevron, new LinearLayout.LayoutParams(cSize, cSize));
        }

        if (onClick != null) {
            attachTapSpringEffect(row);
            row.setOnClickListener(onClick);
        }

        container.addView(row);

        if (showDivider) {
            View div = new View(ctx);
            div.setBackgroundColor(ThemeTokens.getSeparatorInsetColor(ctx));
            int divH = Math.max(1, ThemeTokens.dp(ctx, 1) / 2);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, divH);
            dlp.leftMargin = ThemeTokens.dp(ctx, 58);
            div.setLayoutParams(dlp);
            container.addView(div);
        }

        return container;
    }

    public static View createSwitchRow(Context ctx, int iconRes, int iconBgColor, String title, boolean isChecked,
                                      IosSwitchView.OnCheckedChangeListener listener, boolean showDivider) {
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(ThemeTokens.dp(ctx, 52));
        row.setPadding(ThemeTokens.dp(ctx, 16), ThemeTokens.dp(ctx, 10),
                ThemeTokens.dp(ctx, 16), ThemeTokens.dp(ctx, 10));

        FrameLayout iconBox = new FrameLayout(ctx);
        int bSize = ThemeTokens.dp(ctx, 30);
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(bSize, bSize));

        GradientDrawable ibBg = new GradientDrawable();
        ibBg.setCornerRadius(ThemeTokens.dp(ctx, 8));
        ibBg.setColor(iconBgColor);
        iconBox.setBackground(ibBg);

        ImageView iv = new ImageView(ctx);
        iv.setImageResource(iconRes);
        iv.setColorFilter(Color.WHITE);
        int iSize = ThemeTokens.dp(ctx, 18);
        FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(iSize, iSize);
        ivLp.gravity = Gravity.CENTER;
        iconBox.addView(iv, ivLp);

        row.addView(iconBox);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextColor(ThemeTokens.getLabelColor(ctx));
        tvTitle.setTextSize(17);
        tvTitle.setSingleLine(true);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = ThemeTokens.dp(ctx, 14);
        row.addView(tvTitle, tlp);

        IosSwitchView switchView = new IosSwitchView(ctx);
        switchView.setChecked(isChecked);
        switchView.setOnCheckedChangeListener(listener);
        switchView.setContentDescription(title);
        row.addView(switchView);

        container.addView(row);

        if (showDivider) {
            View div = new View(ctx);
            div.setBackgroundColor(ThemeTokens.getSeparatorInsetColor(ctx));
            int divH = Math.max(1, ThemeTokens.dp(ctx, 1) / 2);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, divH);
            dlp.leftMargin = ThemeTokens.dp(ctx, 58);
            div.setLayoutParams(dlp);
            container.addView(div);
        }

        return container;
    }

    private static void attachTapSpringEffect(View v) {
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.98f).scaleY(0.98f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                    break;
            }
            return false;
        });
    }
}
