package com.velqor.app.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.velqor.app.tokens.ThemeTokens;

import java.io.File;

public final class IosSheet {
    private IosSheet() {}

    public interface InputValidator {
        String validate(String input);
    }

    public interface InputCallback {
        void onConfirm(String text);
    }

    public static Dialog createBaseDialog(Context ctx) {
        Dialog dialog = new Dialog(ctx, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    public static LinearLayout createSheetRoot(Context ctx, Dialog dialog) {
        int screenWidth = ctx.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = ctx.getResources().getDisplayMetrics().heightPixels;
        final int maxSheetHeight = (int) (screenHeight * 0.75f); // Max height 75% screen

        LinearLayout sheet = new LinearLayout(ctx) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int mode = MeasureSpec.getMode(heightMeasureSpec);
                int size = MeasureSpec.getSize(heightMeasureSpec);
                if (mode == MeasureSpec.UNSPECIFIED || size > maxSheetHeight) {
                    heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxSheetHeight, MeasureSpec.AT_MOST);
                }
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(ThemeTokens.dp(ctx, 16), ThemeTokens.dp(ctx, 10),
                ThemeTokens.dp(ctx, 16), ThemeTokens.dp(ctx, 24));
        sheet.setMinimumWidth(screenWidth);
        sheet.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadii(new float[]{
                ThemeTokens.dp(ctx, ThemeTokens.RADIUS_SHEET_TOP), ThemeTokens.dp(ctx, ThemeTokens.RADIUS_SHEET_TOP),
                ThemeTokens.dp(ctx, ThemeTokens.RADIUS_SHEET_TOP), ThemeTokens.dp(ctx, ThemeTokens.RADIUS_SHEET_TOP),
                0, 0, 0, 0});
        bg.setColor(ThemeTokens.COLOR_SURFACE_ELEVATED);
        sheet.setBackground(bg);

        // Grabber bar 36x5dp
        View grabber = new View(ctx);
        int grabberW = ThemeTokens.dp(ctx, 36);
        int grabberH = ThemeTokens.dp(ctx, 5);
        grabber.setLayoutParams(new LinearLayout.LayoutParams(grabberW, grabberH));
        GradientDrawable grabberBg = new GradientDrawable();
        grabberBg.setCornerRadius(ThemeTokens.dp(ctx, 3));
        grabberBg.setColor(ThemeTokens.COLOR_SEPARATOR);
        grabber.setBackground(grabberBg);
        LinearLayout.LayoutParams grabberLp = (LinearLayout.LayoutParams) grabber.getLayoutParams();
        grabberLp.gravity = Gravity.CENTER_HORIZONTAL;
        grabberLp.bottomMargin = ThemeTokens.dp(ctx, 14);
        sheet.addView(grabber);

        return sheet;
    }

    public static void show(Dialog dialog, View sheet) {
        show(dialog, sheet, false);
    }

    public static void show(Dialog dialog, View sheet, boolean hasInput) {
        Context ctx = sheet.getContext();
        FrameLayout root = new FrameLayout(ctx);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(ThemeTokens.COLOR_SCRIM_BLACK);
        root.setOnClickListener(v -> dialog.dismiss());

        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sheetLp.gravity = Gravity.BOTTOM;
        sheet.setOnClickListener(v -> {});
        root.addView(sheet, sheetLp);

        dialog.setContentView(root);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            window.setWindowAnimations(android.R.style.Animation_InputMethod);
            if (hasInput) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        }
        dialog.show();
    }

    public static LinearLayout createActionGroup(Context ctx) {
        LinearLayout group = new LinearLayout(ctx);
        group.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(ThemeTokens.dp(ctx, ThemeTokens.RADIUS_GROUP_LIST));
        bg.setColor(ThemeTokens.COLOR_ACTION_GROUP_BG);
        group.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = ThemeTokens.dp(ctx, 12);
        group.setLayoutParams(lp);
        return group;
    }

    public static View createActionRow(Context ctx, int iconResId, String title, boolean isDanger, View.OnClickListener listener, boolean hasDivider) {
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(ThemeTokens.dp(ctx, 16), 0, ThemeTokens.dp(ctx, 16), 0);
        int rowH = ThemeTokens.dp(ctx, ThemeTokens.HEIGHT_ACTION_ROW);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowH));

        ImageView iv = new ImageView(ctx);
        iv.setImageResource(iconResId);
        iv.setColorFilter(isDanger ? ThemeTokens.COLOR_DANGER : ThemeTokens.COLOR_LABEL);
        int iconSize = ThemeTokens.dp(ctx, ThemeTokens.SIZE_ICON_INSIDE);
        iv.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
        row.addView(iv);

        TextView tv = new TextView(ctx);
        tv.setText(title);
        tv.setTextSize(17);
        tv.setTextColor(isDanger ? ThemeTokens.COLOR_DANGER : ThemeTokens.COLOR_LABEL);
        tv.setTypeface(isDanger ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = ThemeTokens.dp(ctx, 14);
        row.addView(tv, tlp);

        row.setOnClickListener(listener);
        container.addView(row);

        if (hasDivider) {
            View div = new View(ctx);
            div.setBackgroundColor(ThemeTokens.COLOR_SEPARATOR_INSET);
            int divH = Math.max(1, ThemeTokens.dp(ctx, 1) / 2);
            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, divH);
            divLp.leftMargin = ThemeTokens.dp(ctx, 52);
            div.setLayoutParams(divLp);
            container.addView(div);
        }

        return container;
    }

    public static void showDeleteConfirm(Context ctx, File f, Runnable onConfirm) {
        Dialog dialog = createBaseDialog(ctx);
        LinearLayout sheet = createSheetRoot(ctx, dialog);

        LinearLayout card = createActionGroup(ctx);
        card.setPadding(ThemeTokens.dp(ctx, 16), ThemeTokens.dp(ctx, 16),
                ThemeTokens.dp(ctx, 16), ThemeTokens.dp(ctx, 16));
        card.setGravity(Gravity.CENTER);

        TextView title = new TextView(ctx);
        title.setText("Bạn có chắc chắn muốn xóa?");
        title.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        title.setTextSize(13);
        title.setGravity(Gravity.CENTER);
        card.addView(title);

        TextView fname = new TextView(ctx);
        fname.setText("'" + f.getName() + "'");
        fname.setTextColor(ThemeTokens.COLOR_LABEL);
        fname.setTextSize(15);
        fname.setTypeface(Typeface.DEFAULT_BOLD);
        fname.setGravity(Gravity.CENTER);
        fname.setPadding(0, ThemeTokens.dp(ctx, 4), 0, ThemeTokens.dp(ctx, 8));
        card.addView(fname);

        View div = new View(ctx);
        div.setBackgroundColor(ThemeTokens.COLOR_SEPARATOR_INSET);
        div.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, ThemeTokens.dp(ctx, 1) / 2)));
        card.addView(div);

        TextView btnDelete = new TextView(ctx);
        btnDelete.setText("Xóa");
        btnDelete.setTextColor(ThemeTokens.COLOR_DANGER);
        btnDelete.setTextSize(17);
        btnDelete.setTypeface(Typeface.DEFAULT_BOLD);
        btnDelete.setGravity(Gravity.CENTER);
        btnDelete.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(ctx, 48)));
        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            if (onConfirm != null) onConfirm.run();
        });
        card.addView(btnDelete);
        sheet.addView(card);

        // Cancel Card
        LinearLayout cancelCard = createActionGroup(ctx);
        TextView btnCancel = new TextView(ctx);
        btnCancel.setText("Hủy");
        btnCancel.setTextColor(ThemeTokens.COLOR_ACCENT);
        btnCancel.setTextSize(17);
        btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(ctx, 52)));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        cancelCard.addView(btnCancel);
        sheet.addView(cancelCard);

        show(dialog, sheet);
    }

    public static void showInputDialog(Context ctx, String title, String subtitle, String initialText, String confirmText,
                                       InputValidator validator, InputCallback callback) {
        Dialog dialog = createBaseDialog(ctx);
        LinearLayout sheet = createSheetRoot(ctx, dialog);

        TextView titleView = new TextView(ctx);
        titleView.setText(title);
        titleView.setTextSize(20);
        titleView.setTextColor(ThemeTokens.COLOR_LABEL);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(titleView);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView subView = new TextView(ctx);
            subView.setText(subtitle);
            subView.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
            subView.setTextSize(13);
            sheet.addView(subView);
        }

        EditText input = new EditText(ctx);
        input.setText(initialText);
        if (initialText != null) input.setSelection(initialText.length());
        input.setTextColor(ThemeTokens.COLOR_LABEL);
        input.setSingleLine(true);
        input.setPadding(ThemeTokens.dp(ctx, 14), ThemeTokens.dp(ctx, 12),
                ThemeTokens.dp(ctx, 14), ThemeTokens.dp(ctx, 12));

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(ThemeTokens.dp(ctx, ThemeTokens.RADIUS_INPUT_FIELD));
        inputBg.setColor(ThemeTokens.COLOR_ACTION_GROUP_BG);
        input.setBackground(inputBg);

        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.topMargin = ThemeTokens.dp(ctx, 16);
        sheet.addView(input, inLp);

        TextView errorText = new TextView(ctx);
        errorText.setTextColor(ThemeTokens.COLOR_DANGER);
        errorText.setTextSize(12);
        errorText.setVisibility(View.GONE);
        LinearLayout.LayoutParams errLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        errLp.topMargin = ThemeTokens.dp(ctx, 6);
        sheet.addView(errorText, errLp);

        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = ThemeTokens.dp(ctx, 16);
        btnRow.setLayoutParams(rlp);

        TextView btnCancel = new TextView(ctx);
        btnCancel.setText("Hủy");
        btnCancel.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        btnCancel.setTextSize(16);
        btnCancel.setPadding(ThemeTokens.dp(ctx, 16), ThemeTokens.dp(ctx, 10),
                ThemeTokens.dp(ctx, 16), ThemeTokens.dp(ctx, 10));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        TextView btnDone = new TextView(ctx);
        btnDone.setText(confirmText);
        btnDone.setTextColor(Color.WHITE);
        btnDone.setTextSize(16);
        btnDone.setTypeface(Typeface.DEFAULT_BOLD);
        btnDone.setPadding(ThemeTokens.dp(ctx, 20), ThemeTokens.dp(ctx, 10),
                ThemeTokens.dp(ctx, 20), ThemeTokens.dp(ctx, 10));

        GradientDrawable doneBg = new GradientDrawable();
        doneBg.setCornerRadius(ThemeTokens.dp(ctx, 999));
        doneBg.setColor(ThemeTokens.COLOR_ACCENT);
        btnDone.setBackground(doneBg);

        btnDone.setOnClickListener(v -> {
            String txt = input.getText().toString().trim();
            if (validator != null) {
                String err = validator.validate(txt);
                if (err != null) {
                    errorText.setText(err);
                    errorText.setVisibility(View.VISIBLE);
                    return;
                }
            }
            dialog.dismiss();
            if (callback != null) callback.onConfirm(txt);
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (errorText.getVisibility() == View.VISIBLE) errorText.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnRow.addView(btnDone);
        sheet.addView(btnRow);

        show(dialog, sheet, true);
    }
}
