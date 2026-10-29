package com.velqor.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.velqor.app.R;
import com.velqor.app.files.FileCreator;
import com.velqor.app.tokens.ThemeTokens;

import java.io.File;

public final class NewItemSheet {
    private NewItemSheet() {}

    public interface OnItemCreateListener {
        void onCreateFolderRequested();
        void onFileCreated(File newFile);
    }

    public static void showCreateMenu(Activity activity, File currentDir, OnItemCreateListener listener) {
        if (currentDir == null || !currentDir.canWrite()) {
            Toast.makeText(activity, "Không thể tạo tệp/thư mục: Không có quyền ghi trong thư mục này", Toast.LENGTH_LONG).show();
            return;
        }

        Dialog dialog = IosSheet.createBaseDialog(activity);
        LinearLayout sheet = IosSheet.createSheetRoot(activity, dialog);

        TextView title = new TextView(activity);
        title.setText("Tạo mới");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        
        LinearLayout group1 = IosSheet.createActionGroup(activity);
        group1.addView(IosSheet.createActionRow(activity, R.drawable.ic_folder_plus, "Thư mục mới", false, v -> {
            dialog.dismiss();
            if (listener != null) listener.onCreateFolderRequested();
        }, true));

        group1.addView(IosSheet.createActionRow(activity, R.drawable.ic_file_text, "Tệp văn bản mới (.txt)", false, v -> {
            dialog.dismiss();
            showCreateFileDialog(activity, currentDir, "Tệp mới.txt", listener);
        }, false));
        sheet.addView(group1);

        
        LinearLayout group2 = IosSheet.createActionGroup(activity);
        group2.addView(IosSheet.createActionRow(activity, R.drawable.ic_file_code, "Tài liệu Markdown (.md)", false, v -> {
            dialog.dismiss();
            showCreateFileDialog(activity, currentDir, "Ghi chú.md", listener);
        }, true));

        group2.addView(IosSheet.createActionRow(activity, R.drawable.ic_file_code, "Trang web HTML (.html)", false, v -> {
            dialog.dismiss();
            showCreateFileDialog(activity, currentDir, "index.html", listener);
        }, true));

        group2.addView(IosSheet.createActionRow(activity, R.drawable.ic_file_code, "Dữ liệu JSON (.json)", false, v -> {
            dialog.dismiss();
            showCreateFileDialog(activity, currentDir, "data.json", listener);
        }, true));

        group2.addView(IosSheet.createActionRow(activity, R.drawable.ic_file_code, "Tệp Shell Script (.sh)", false, v -> {
            dialog.dismiss();
            showCreateFileDialog(activity, currentDir, "script.sh", listener);
        }, true));

        group2.addView(IosSheet.createActionRow(activity, R.drawable.ic_file_code, "Mã nguồn Python (.py)", false, v -> {
            dialog.dismiss();
            showCreateFileDialog(activity, currentDir, "main.py", listener);
        }, true));

        group2.addView(IosSheet.createActionRow(activity, R.drawable.ic_file_doc, "Tệp trống (tùy đuôi)", false, v -> {
            dialog.dismiss();
            showCreateFileDialog(activity, currentDir, "tep_moi", listener);
        }, false));
        sheet.addView(group2);

        
        LinearLayout cancelCard = IosSheet.createActionGroup(activity);
        cancelCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((LinearLayout.LayoutParams) cancelCard.getLayoutParams()).topMargin = ThemeTokens.dp(activity, 12);

        TextView btnCancel = new TextView(activity);
        btnCancel.setText("Hủy");
        btnCancel.setTextColor(ThemeTokens.COLOR_ACCENT);
        btnCancel.setTextSize(17);
        btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(activity, 52)));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        cancelCard.addView(btnCancel);
        sheet.addView(cancelCard);

        IosSheet.show(dialog, sheet);
    }

    public static void showCreateFileDialog(Activity activity, File currentDir, String defaultName, OnItemCreateListener listener) {
        Dialog dialog = IosSheet.createBaseDialog(activity);
        LinearLayout sheet = IosSheet.createSheetRoot(activity, dialog);

        TextView title = new TextView(activity);
        title.setText("Tạo tệp mới");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        TextView subtitle = new TextView(activity);
        subtitle.setText("Nhập tên tệp và chọn phần mở rộng mong muốn");
        subtitle.setTextSize(13);
        subtitle.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = ThemeTokens.dp(activity, 4);
        subtitle.setLayoutParams(subLp);
        sheet.addView(subtitle);

        
        EditText input = new EditText(activity);
        input.setText(defaultName);
        input.setTextColor(ThemeTokens.COLOR_LABEL);
        input.setTextSize(16);
        input.setSingleLine(true);
        input.setPadding(ThemeTokens.dp(activity, 14), ThemeTokens.dp(activity, 12),
                ThemeTokens.dp(activity, 14), ThemeTokens.dp(activity, 12));

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(ThemeTokens.dp(activity, ThemeTokens.RADIUS_INPUT_FIELD));
        inputBg.setColor(ThemeTokens.COLOR_ACTION_GROUP_BG);
        input.setBackground(inputBg);

        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.topMargin = ThemeTokens.dp(activity, 16);
        sheet.addView(input, inLp);

        
        TextView errorText = new TextView(activity);
        errorText.setTextSize(13);
        errorText.setVisibility(View.GONE);
        LinearLayout.LayoutParams errLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        errLp.topMargin = ThemeTokens.dp(activity, 6);
        errLp.leftMargin = ThemeTokens.dp(activity, 4);
        sheet.addView(errorText, errLp);

        
        HorizontalScrollView chipScroll = new HorizontalScrollView(activity);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams chipScrollLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chipScrollLp.topMargin = ThemeTokens.dp(activity, 12);
        chipScroll.setLayoutParams(chipScrollLp);

        LinearLayout chipRow = new LinearLayout(activity);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setGravity(Gravity.CENTER_VERTICAL);

        String[] extensions = {".txt", ".md", ".html", ".json", ".sh", ".py", ".xml", ".csv"};
        for (String ext : extensions) {
            TextView chip = new TextView(activity);
            chip.setText(ext);
            chip.setTextColor(ThemeTokens.COLOR_LABEL);
            chip.setTextSize(13);
            chip.setPadding(ThemeTokens.dp(activity, 12), ThemeTokens.dp(activity, 6),
                    ThemeTokens.dp(activity, 12), ThemeTokens.dp(activity, 6));

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setCornerRadius(ThemeTokens.dp(activity, 999));
            chipBg.setColor(ThemeTokens.COLOR_ACTION_GROUP_BG);
            chip.setBackground(chipBg);

            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.rightMargin = ThemeTokens.dp(activity, 8);
            chip.setLayoutParams(clp);

            chip.setOnClickListener(v -> {
                String cur = input.getText().toString();
                int dot = cur.lastIndexOf('.');
                String base = (dot > 0) ? cur.substring(0, dot) : cur;
                if (base.isEmpty()) base = "Tệp mới";
                String newFullName = base + ext;
                input.setText(newFullName);
                input.setSelection(base.length());
            });

            chipRow.addView(chip);
        }
        chipScroll.addView(chipRow);
        sheet.addView(chipScroll);

        
        LinearLayout btnRow = new LinearLayout(activity);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = ThemeTokens.dp(activity, 20);
        btnRow.setLayoutParams(rlp);

        TextView btnCancel = new TextView(activity);
        btnCancel.setText("Hủy");
        btnCancel.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        btnCancel.setTextSize(16);
        btnCancel.setPadding(ThemeTokens.dp(activity, 16), ThemeTokens.dp(activity, 10),
                ThemeTokens.dp(activity, 16), ThemeTokens.dp(activity, 10));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        TextView btnCreate = new TextView(activity);
        btnCreate.setText("Tạo");
        btnCreate.setTextColor(Color.WHITE);
        btnCreate.setTextSize(16);
        btnCreate.setTypeface(Typeface.DEFAULT_BOLD);
        btnCreate.setPadding(ThemeTokens.dp(activity, 24), ThemeTokens.dp(activity, 10),
                ThemeTokens.dp(activity, 24), ThemeTokens.dp(activity, 10));

        GradientDrawable cBg = new GradientDrawable();
        cBg.setCornerRadius(ThemeTokens.dp(activity, 999));
        cBg.setColor(ThemeTokens.COLOR_ACCENT);
        btnCreate.setBackground(cBg);

        btnRow.addView(btnCreate);
        sheet.addView(btnRow);

        
        Runnable validate = () -> {
            String text = input.getText().toString();
            FileCreator.ValidationResult vr = FileCreator.validateFileName(currentDir, text);
            if (!vr.isValid) {
                errorText.setVisibility(View.VISIBLE);
                errorText.setTextColor(ThemeTokens.COLOR_DANGER);
                if (vr.suggestedName != null) {
                    errorText.setText(vr.message + " · Gợi ý: " + vr.suggestedName);
                    errorText.setOnClickListener(v -> {
                        input.setText(vr.suggestedName);
                        int dot = vr.suggestedName.lastIndexOf('.');
                        input.setSelection(dot > 0 ? dot : vr.suggestedName.length());
                    });
                } else {
                    errorText.setText(vr.message);
                    errorText.setOnClickListener(null);
                }
                btnCreate.setEnabled(false);
                btnCreate.setAlpha(0.4f);
            } else if (vr.isWarning) {
                errorText.setVisibility(View.VISIBLE);
                errorText.setTextColor(0xFFFFCC00); 
                errorText.setText(vr.message);
                errorText.setOnClickListener(null);
                btnCreate.setEnabled(true);
                btnCreate.setAlpha(1.0f);
            } else {
                errorText.setVisibility(View.GONE);
                errorText.setText("");
                errorText.setOnClickListener(null);
                btnCreate.setEnabled(true);
                btnCreate.setAlpha(1.0f);
            }
        };

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validate.run();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnCreate.setOnClickListener(v -> {
            String finalName = input.getText().toString().trim();
            btnCreate.setEnabled(false);
            new Thread(() -> {
                try {
                    File created = FileCreator.createNewFile(currentDir, finalName);
                    activity.runOnUiThread(() -> {
                        dialog.dismiss();
                        if (listener != null) listener.onFileCreated(created);
                    });
                } catch (Exception e) {
                    activity.runOnUiThread(() -> {
                        btnCreate.setEnabled(true);
                        errorText.setVisibility(View.VISIBLE);
                        errorText.setTextColor(ThemeTokens.COLOR_DANGER);
                        errorText.setText(e.getMessage());
                    });
                }
            }).start();
        });

        IosSheet.show(dialog, sheet, true);

        
        input.requestFocus();
        int dot = defaultName.lastIndexOf('.');
        if (dot > 0) {
            input.setSelection(0, dot);
        } else {
            input.selectAll();
        }
        validate.run();
    }
}
