package com.velqor.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
        row.setPadding(ThemeTokens.dp(ctx, 16), ThemeTokens.dp(ctx, 8),
                ThemeTokens.dp(ctx, 16), ThemeTokens.dp(ctx, 8));
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(ThemeTokens.dp(ctx, 56));

        if (isSelected) {
            row.setBackgroundColor(0x1F0A84FF);
        }

        // Selection Checkbox
        if (isSelectionMode) {
            ImageView check = new ImageView(ctx);
            check.setImageResource(R.drawable.ic_check);
            GradientDrawable checkBg = new GradientDrawable();
            checkBg.setShape(GradientDrawable.OVAL);
            if (isSelected) {
                checkBg.setColor(ThemeTokens.COLOR_ACCENT);
                check.setColorFilter(Color.WHITE);
            } else {
                checkBg.setColor(Color.TRANSPARENT);
                checkBg.setStroke(ThemeTokens.dp(ctx, 2), ThemeTokens.COLOR_SEPARATOR);
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

        // 40x40dp Icon in 10dp rounded box with semantic color tint (alpha 0.18, contrast >= 3:1)
        int iconRes = getIconResourceForFile(f);
        int tintColor = getSemanticColorForFile(f);
        String nameLower = f.getName().toLowerCase();
        boolean isApk = nameLower.endsWith(".apk");
        boolean isMedia = nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg") || nameLower.endsWith(".png") ||
                          nameLower.endsWith(".webp") || nameLower.endsWith(".mp4") || nameLower.endsWith(".mkv") ||
                          nameLower.endsWith(".mov") || nameLower.endsWith(".avi");

        android.widget.FrameLayout iconBox = new android.widget.FrameLayout(ctx);
        int boxSize = ThemeTokens.dp(ctx, ThemeTokens.SIZE_ICON_BOX);
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(boxSize, boxSize));

        GradientDrawable iconBoxBg = new GradientDrawable();
        iconBoxBg.setCornerRadius(ThemeTokens.dp(ctx, ThemeTokens.RADIUS_ICON_BOX));
        iconBoxBg.setColor(isApk ? 0x15FFFFFF : ((tintColor & 0x00FFFFFF) | 0x2E000000));
        iconBox.setBackground(iconBoxBg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            iconBox.setClipToOutline(true);
        }

        ImageView iv = new ImageView(ctx);
        iv.setImageResource(iconRes);
        iv.setColorFilter(tintColor);

        int insideSize = ThemeTokens.dp(ctx, isApk ? 32 : ThemeTokens.SIZE_ICON_INSIDE);
        android.widget.FrameLayout.LayoutParams ivLp = new android.widget.FrameLayout.LayoutParams(insideSize, insideSize);
        ivLp.gravity = Gravity.CENTER;
        iv.setLayoutParams(ivLp);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconBox.addView(iv);

        // Thumbnail Loader integration (if enabled in preferences)
        boolean showThumbs = ctx.getSharedPreferences("velqor_editor_prefs", Context.MODE_PRIVATE)
                .getBoolean("show_thumbnails", true);
        if (showThumbs && !f.isDirectory()) {
            ThumbnailLoader.getInstance(ctx).loadThumbnail(f, iv);
        }

        // Small play badge overlay for videos
        if (nameLower.endsWith(".mp4") || nameLower.endsWith(".mkv") || nameLower.endsWith(".avi") || nameLower.endsWith(".mov")) {
            ImageView playIcon = new ImageView(ctx);
            playIcon.setImageResource(R.drawable.ic_chevron_right); // Play-like vector
            playIcon.setColorFilter(Color.WHITE);
            int pSize = ThemeTokens.dp(ctx, 12);
            android.widget.FrameLayout.LayoutParams playLp = new android.widget.FrameLayout.LayoutParams(pSize, pSize);
            playLp.gravity = Gravity.BOTTOM | Gravity.END;
            playLp.rightMargin = ThemeTokens.dp(ctx, 3);
            playLp.bottomMargin = ThemeTokens.dp(ctx, 3);
            playIcon.setLayoutParams(playLp);
            iconBox.addView(playIcon);
        }

        row.addView(iconBox);

        // Title and Subtitle Info
        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.leftMargin = ThemeTokens.dp(ctx, 12);

        TextView nameText = new TextView(ctx);
        nameText.setText(middleEllipsis(f.getName(), 34));
        nameText.setTextColor(ThemeTokens.COLOR_LABEL);
        nameText.setTextSize(17);
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
        subText.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        subText.setTextSize(13);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = ThemeTokens.dp(ctx, 2);
        subText.setLayoutParams(subLp);
        textCol.addView(subText);

        row.addView(textCol, colLp);

        // Right chevron for folders
        if (f.isDirectory() && !isSelectionMode) {
            ImageView chevron = new ImageView(ctx);
            chevron.setImageResource(R.drawable.ic_chevron_right);
            chevron.setColorFilter(ThemeTokens.COLOR_SEC_LABEL);
            chevron.setAlpha(0.4f);
            int chevSize = ThemeTokens.dp(ctx, 16);
            chevron.setLayoutParams(new LinearLayout.LayoutParams(chevSize, chevSize));
            row.addView(chevron);
        }

        // Tap & Long-Press Interactions
        row.setOnClickListener(v -> {
            if (listener != null) listener.onClick(f);
        });

        row.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(f);
            return true;
        });

        rowContainer.addView(row);

        // Inset Separator: 0.5dp line, inset from text start (68dp), omitted on last item
        if (!isLast) {
            View sep = new View(ctx);
            sep.setBackgroundColor(ThemeTokens.COLOR_SEPARATOR_INSET);
            int divH = Math.max(1, ThemeTokens.dp(ctx, 1) / 2);
            LinearLayout.LayoutParams sepLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, divH);
            sepLp.leftMargin = ThemeTokens.dp(ctx, isSelectionMode ? 102 : 68);
            sep.setLayoutParams(sepLp);
            rowContainer.addView(sep);
        }

        return rowContainer;
    }

    public static int getIconResourceForFile(File f) {
        if (f.isDirectory()) return R.drawable.ic_folder;
        String name = f.getName().toLowerCase();
        if (ZipManager.isArchive(f)) return R.drawable.ic_file_archive;
        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif")) return R.drawable.ic_file_image;
        if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") || name.endsWith(".mov")) return R.drawable.ic_file_video;
        if (name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".m4a")) return R.drawable.ic_file_audio;
        if (name.endsWith(".apk")) return R.drawable.ic_file_apk;
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
        if (name.endsWith(".pdf") || name.endsWith(".txt") || name.endsWith(".doc") || name.endsWith(".docx")) return ThemeTokens.COLOR_ICON_DOCUMENT;
        return ThemeTokens.COLOR_ICON_OTHER;
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
