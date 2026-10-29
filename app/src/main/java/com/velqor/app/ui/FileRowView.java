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
import com.velqor.app.archive.ZipManager;
import com.velqor.app.tokens.ThemeTokens;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileRowView extends LinearLayout {

    public FileRowView(Context context) {
        super(context);
    }

    public interface OnFileRowClickListener {
        void onClick(File f);
        void onLongClick(File f);
    }

    public static View createRow(Context ctx, File f, boolean isLast, boolean isSelected, boolean isSelectionMode, OnFileRowClickListener listener) {
        LinearLayout rowContainer = new LinearLayout(ctx);
        rowContainer.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(ThemeTokens.dp(ctx, 16), ThemeTokens.dp(ctx, 9),
                ThemeTokens.dp(ctx, 16), ThemeTokens.dp(ctx, 9));
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(ThemeTokens.dp(ctx, 58));

        int accentColor = ThemeTokens.getAccentColor(ctx);
        if (isSelected) {
            row.setBackgroundColor((accentColor & 0x00FFFFFF) | 0x22000000);
        }

        if (isSelectionMode) {
            ImageView check = new ImageView(ctx);
            check.setImageResource(R.drawable.ic_check);
            GradientDrawable checkBg = new GradientDrawable();
            checkBg.setShape(GradientDrawable.OVAL);
            if (isSelected) {
                checkBg.setColor(accentColor);
                check.setColorFilter(Color.WHITE);
            } else {
                checkBg.setColor(Color.TRANSPARENT);
                checkBg.setStroke(ThemeTokens.dp(ctx, 2), ThemeTokens.getStrokeColor(ctx));
                check.setColorFilter(Color.TRANSPARENT);
            }
            check.setBackground(checkBg);
            check.setPadding(ThemeTokens.dp(ctx, 4), ThemeTokens.dp(ctx, 4),
                    ThemeTokens.dp(ctx, 4), ThemeTokens.dp(ctx, 4));

            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ThemeTokens.dp(ctx, 22), ThemeTokens.dp(ctx, 22));
            clp.rightMargin = ThemeTokens.dp(ctx, 12);
            check.setLayoutParams(clp);
            row.addView(check);
        }

        int iconRes = getIconResourceForFile(f);
        int tintColor = getSemanticColorForFile(f);
        String nameLower = f.getName().toLowerCase();
        boolean isApk = nameLower.endsWith(".apk");

        FrameLayout iconBox = new FrameLayout(ctx);
        int boxSize = ThemeTokens.dp(ctx, ThemeTokens.SIZE_ICON_BOX);
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(boxSize, boxSize));

        GradientDrawable iconBoxBg = new GradientDrawable();
        iconBoxBg.setCornerRadius(ThemeTokens.dp(ctx, ThemeTokens.RADIUS_ICON_BOX));
        iconBoxBg.setColor(isApk ? ThemeTokens.getStrokeColor(ctx) : ((tintColor & 0x00FFFFFF) | 0x2E000000));
        iconBox.setBackground(iconBoxBg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            iconBox.setClipToOutline(true);
        }

        ImageView iv = new ImageView(ctx);
        iv.setImageResource(iconRes);
        iv.setColorFilter(tintColor);

        int insideSize = ThemeTokens.dp(ctx, isApk ? 32 : ThemeTokens.SIZE_ICON_INSIDE);
        FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(insideSize, insideSize);
        ivLp.gravity = Gravity.CENTER;
        iv.setLayoutParams(ivLp);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconBox.addView(iv);

        boolean showThumbs = ctx.getSharedPreferences("velqor_editor_prefs", Context.MODE_PRIVATE)
                .getBoolean("show_thumbnails", true);
        if (showThumbs && !f.isDirectory()) {
            ThumbnailLoader.getInstance(ctx).loadThumbnail(f, iv);
        }

        if (nameLower.endsWith(".mp4") || nameLower.endsWith(".mkv") || nameLower.endsWith(".avi") || nameLower.endsWith(".mov")) {
            ImageView playIcon = new ImageView(ctx);
            playIcon.setImageResource(R.drawable.ic_chevron_right);
            playIcon.setColorFilter(Color.WHITE);
            int pSize = ThemeTokens.dp(ctx, 12);
            FrameLayout.LayoutParams playLp = new FrameLayout.LayoutParams(pSize, pSize);
            playLp.gravity = Gravity.BOTTOM | Gravity.END;
            playLp.rightMargin = ThemeTokens.dp(ctx, 3);
            playLp.bottomMargin = ThemeTokens.dp(ctx, 3);
            playIcon.setLayoutParams(playLp);
            iconBox.addView(playIcon);
        }

        row.addView(iconBox);

        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.leftMargin = ThemeTokens.dp(ctx, 12);

        TextView nameText = new TextView(ctx);
        nameText.setText(middleEllipsis(f.getName(), 34));
        nameText.setTextColor(ThemeTokens.getLabelColor(ctx));
        nameText.setTextSize(16);
        nameText.setTypeface(Typeface.DEFAULT);
        nameText.setSingleLine(true);
        textCol.addView(nameText);

        TextView subText = new TextView(ctx);
        String subtitle;
        if (f.isDirectory()) {
            int childCount = 0;
            try {
                String[] list = f.list();
                if (list != null) childCount = list.length;
            } catch (Exception ignored) {}
            subtitle = childCount + " mục · " + formatItemDate(f.lastModified());
        } else {
            subtitle = formatSize(f.length()) + " · " + formatItemDate(f.lastModified());
        }
        subText.setText(subtitle);
        subText.setTextColor(ThemeTokens.getSecondaryLabelColor(ctx));
        subText.setTextSize(13);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = ThemeTokens.dp(ctx, 2);
        subText.setLayoutParams(subLp);
        textCol.addView(subText);

        row.addView(textCol, colLp);

        if (f.isDirectory() && !isSelectionMode) {
            ImageView chevron = new ImageView(ctx);
            chevron.setImageResource(R.drawable.ic_chevron_right);
            chevron.setColorFilter(ThemeTokens.getSecondaryLabelColor(ctx));
            chevron.setAlpha(0.4f);
            int chevSize = ThemeTokens.dp(ctx, 16);
            chevron.setLayoutParams(new LinearLayout.LayoutParams(chevSize, chevSize));
            row.addView(chevron);
        }

        attachTapSpringEffect(row);

        row.setOnClickListener(v -> {
            if (listener != null) listener.onClick(f);
        });

        row.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(f);
            return true;
        });

        rowContainer.addView(row);

        if (!isLast) {
            View sep = new View(ctx);
            sep.setBackgroundColor(ThemeTokens.getSeparatorInsetColor(ctx));
            int divH = Math.max(1, ThemeTokens.dp(ctx, 1) / 2);
            LinearLayout.LayoutParams sepLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, divH);
            sepLp.leftMargin = ThemeTokens.dp(ctx, isSelectionMode ? 102 : 68);
            sep.setLayoutParams(sepLp);
            rowContainer.addView(sep);
        }

        return rowContainer;
    }

    public static View createGridCard(Context ctx, File f, boolean isSelected, boolean isSelectionMode, OnFileRowClickListener listener) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(ThemeTokens.dp(ctx, 12), ThemeTokens.dp(ctx, 14),
                ThemeTokens.dp(ctx, 12), ThemeTokens.dp(ctx, 12));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(ThemeTokens.dp(ctx, 18));
        int accentColor = ThemeTokens.getAccentColor(ctx);
        if (isSelected) {
            cardBg.setColor((accentColor & 0x00FFFFFF) | 0x28000000);
            cardBg.setStroke(ThemeTokens.dp(ctx, 2), accentColor);
        } else {
            cardBg.setColor(ThemeTokens.getActionGroupBgColor(ctx));
            cardBg.setStroke(ThemeTokens.dp(ctx, 1), ThemeTokens.getStrokeColor(ctx));
        }
        card.setBackground(cardBg);

        int iconRes = getIconResourceForFile(f);
        int tintColor = getSemanticColorForFile(f);
        String nameLower = f.getName().toLowerCase();
        boolean isApk = nameLower.endsWith(".apk");

        FrameLayout iconBox = new FrameLayout(ctx);
        int boxSize = ThemeTokens.dp(ctx, 56);
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(boxSize, boxSize));

        GradientDrawable iconBoxBg = new GradientDrawable();
        iconBoxBg.setCornerRadius(ThemeTokens.dp(ctx, 16));
        iconBoxBg.setColor(isApk ? ThemeTokens.getStrokeColor(ctx) : ((tintColor & 0x00FFFFFF) | 0x2E000000));
        iconBox.setBackground(iconBoxBg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            iconBox.setClipToOutline(true);
        }

        ImageView iv = new ImageView(ctx);
        iv.setImageResource(iconRes);
        iv.setColorFilter(tintColor);

        int insideSize = ThemeTokens.dp(ctx, isApk ? 38 : 30);
        FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(insideSize, insideSize);
        ivLp.gravity = Gravity.CENTER;
        iv.setLayoutParams(ivLp);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconBox.addView(iv);

        boolean showThumbs = ctx.getSharedPreferences("velqor_editor_prefs", Context.MODE_PRIVATE)
                .getBoolean("show_thumbnails", true);
        if (showThumbs && !f.isDirectory()) {
            ThumbnailLoader.getInstance(ctx).loadThumbnail(f, iv);
        }

        card.addView(iconBox);

        TextView nameText = new TextView(ctx);
        nameText.setText(middleEllipsis(f.getName(), 20));
        nameText.setTextColor(ThemeTokens.getLabelColor(ctx));
        nameText.setTextSize(14);
        nameText.setTypeface(Typeface.DEFAULT_BOLD);
        nameText.setGravity(Gravity.CENTER);
        nameText.setSingleLine(true);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = ThemeTokens.dp(ctx, 8);
        card.addView(nameText, nlp);

        TextView subText = new TextView(ctx);
        String subtitle;
        if (f.isDirectory()) {
            int childCount = 0;
            try {
                String[] list = f.list();
                if (list != null) childCount = list.length;
            } catch (Exception ignored) {}
            subtitle = childCount + " mục";
        } else {
            subtitle = formatSize(f.length());
        }
        subText.setText(subtitle);
        subText.setTextColor(ThemeTokens.getSecondaryLabelColor(ctx));
        subText.setTextSize(12);
        subText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = ThemeTokens.dp(ctx, 2);
        card.addView(subText, slp);

        attachTapSpringEffect(card);

        card.setOnClickListener(v -> {
            if (listener != null) listener.onClick(f);
        });

        card.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(f);
            return true;
        });

        return card;
    }

    public static View createSkeletonRow(Context ctx) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(ThemeTokens.dp(ctx, 16), ThemeTokens.dp(ctx, 10),
                ThemeTokens.dp(ctx, 16), ThemeTokens.dp(ctx, 10));
        row.setGravity(Gravity.CENTER_VERTICAL);

        View box = new View(ctx);
        int bSize = ThemeTokens.dp(ctx, ThemeTokens.SIZE_ICON_BOX);
        box.setLayoutParams(new LinearLayout.LayoutParams(bSize, bSize));
        GradientDrawable boxBg = new GradientDrawable();
        boxBg.setCornerRadius(ThemeTokens.dp(ctx, ThemeTokens.RADIUS_ICON_BOX));
        boxBg.setColor(ThemeTokens.getStrokeColor(ctx));
        box.setBackground(boxBg);
        row.addView(box);

        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.leftMargin = ThemeTokens.dp(ctx, 12);

        View line1 = new View(ctx);
        LinearLayout.LayoutParams l1Lp = new LinearLayout.LayoutParams(ThemeTokens.dp(ctx, 140), ThemeTokens.dp(ctx, 14));
        line1.setLayoutParams(l1Lp);
        GradientDrawable l1Bg = new GradientDrawable();
        l1Bg.setCornerRadius(ThemeTokens.dp(ctx, 4));
        l1Bg.setColor(ThemeTokens.getStrokeColor(ctx));
        line1.setBackground(l1Bg);
        textCol.addView(line1);

        View line2 = new View(ctx);
        LinearLayout.LayoutParams l2Lp = new LinearLayout.LayoutParams(ThemeTokens.dp(ctx, 80), ThemeTokens.dp(ctx, 10));
        l2Lp.topMargin = ThemeTokens.dp(ctx, 6);
        line2.setLayoutParams(l2Lp);
        GradientDrawable l2Bg = new GradientDrawable();
        l2Bg.setCornerRadius(ThemeTokens.dp(ctx, 3));
        l2Bg.setColor(ThemeTokens.getStrokeColor(ctx));
        line2.setBackground(l2Bg);
        textCol.addView(line2);

        row.addView(textCol, colLp);
        return row;
    }

    private static void attachTapSpringEffect(View v) {
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                    break;
            }
            return false;
        });
    }

    public static int getIconResourceForFile(File f) {
        if (f.isDirectory()) return R.drawable.ic_folder;
        String name = f.getName().toLowerCase();
        if (ZipManager.isArchive(f)) return R.drawable.ic_file_archive;
        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif")) return R.drawable.ic_file_image;
        if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") || name.endsWith(".mov")) return R.drawable.ic_file_video;
        if (name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".m4a")) return R.drawable.ic_file_audio;
        if (name.endsWith(".apk")) return R.drawable.ic_file_apk;
        if (isCodeFile(name)) return R.drawable.ic_file_code;
        if (isPlainTextFile(name)) return R.drawable.ic_file_text;
        return R.drawable.ic_file_doc;
    }

    public static int getSemanticColorForFile(File f) {
        if (f.isDirectory()) {
            String dirName = f.getName().toLowerCase();
            if (dirName.equals("download") || dirName.equals("downloads")) {
                return 0xFF0A84FF;
            } else if (dirName.equals("dcim") || dirName.equals("pictures") || dirName.equals("photos")) {
                return ThemeTokens.COLOR_ICON_IMAGE;
            } else if (dirName.equals("documents") || dirName.equals("doc")) {
                return ThemeTokens.COLOR_ICON_DOCUMENT;
            } else if (dirName.equals("music") || dirName.equals("audio") || dirName.equals("podcasts")) {
                return ThemeTokens.COLOR_ICON_AUDIO;
            } else if (dirName.equals("movies") || dirName.equals("videos")) {
                return ThemeTokens.COLOR_ICON_VIDEO;
            } else if (dirName.equals("android")) {
                return ThemeTokens.COLOR_ICON_APK;
            }
            return ThemeTokens.COLOR_ICON_FOLDER;
        }
        String name = f.getName().toLowerCase();
        if (ZipManager.isArchive(f)) return ThemeTokens.COLOR_ICON_ARCHIVE;
        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif")) return ThemeTokens.COLOR_ICON_IMAGE;
        if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") || name.endsWith(".mov")) return ThemeTokens.COLOR_ICON_VIDEO;
        if (name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".m4a")) return ThemeTokens.COLOR_ICON_AUDIO;
        if (name.endsWith(".apk")) return ThemeTokens.COLOR_ICON_APK;
        if (isCodeFile(name)) return 0xFF5AC8FA;
        if (isPlainTextFile(name)) return 0xFF8E8E93;
        if (name.endsWith(".pdf")) return 0xFFFF453A;
        if (name.endsWith(".doc") || name.endsWith(".docx")) return 0xFF0A84FF;
        return ThemeTokens.COLOR_ICON_DOCUMENT;
    }

    private static boolean isCodeFile(String name) {
        return name.endsWith(".html") || name.endsWith(".htm") || name.endsWith(".md") ||
               name.endsWith(".json") || name.endsWith(".xml") || name.endsWith(".js") ||
               name.endsWith(".css") || name.endsWith(".java") || name.endsWith(".kt") ||
               name.endsWith(".sh") || name.endsWith(".py") || name.endsWith(".c") ||
               name.endsWith(".cpp") || name.endsWith(".h") || name.endsWith(".sql") ||
               name.endsWith(".gradle") || name.endsWith(".yaml") || name.endsWith(".yml");
    }

    private static boolean isPlainTextFile(String name) {
        return name.endsWith(".txt") || name.endsWith(".log") || name.endsWith(".ini") ||
               name.endsWith(".conf") || name.endsWith(".properties") || name.endsWith(".env");
    }

    public static String middleEllipsis(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) return text;
        int half = (maxLen - 3) / 2;
        return text.substring(0, half) + "…" + text.substring(text.length() - half);
    }

    public static String formatItemDate(long time) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(time));
    }

    public static String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups == 0) return bytes + " B";
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
