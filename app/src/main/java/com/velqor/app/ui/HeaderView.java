package com.velqor.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.velqor.app.R;
import com.velqor.app.tokens.ThemeTokens;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HeaderView extends LinearLayout {

    public interface OnHeaderActionListener {
        void onSearchClick();
        void onNewFolderClick();
        void onMoreClick();
        void onBreadcrumbClick(File directory);
    }

    private TextView mLargeTitleText;
    private HorizontalScrollView mBreadcrumbScroll;
    private LinearLayout mBreadcrumbContainer;
    private OnHeaderActionListener mListener;

    public HeaderView(Context context, int statusBarHeight) {
        super(context);
        init(statusBarHeight);
    }

    private void init(int statusBarHeight) {
        setOrientation(LinearLayout.VERTICAL);
        setBackgroundColor(Color.TRANSPARENT);

        // Header Top Row
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(ThemeTokens.dp(getContext(), 16), statusBarHeight + ThemeTokens.dp(getContext(), 8),
                ThemeTokens.dp(getContext(), 16), ThemeTokens.dp(getContext(), 4));
        header.setGravity(Gravity.CENTER_VERTICAL);

        mLargeTitleText = new TextView(getContext());
        mLargeTitleText.setText("VELQOR");
        mLargeTitleText.setTextSize(34);
        mLargeTitleText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mLargeTitleText.setTextColor(ThemeTokens.COLOR_LABEL);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        header.addView(mLargeTitleText, titleLp);

        // Round Action Buttons (36dp)
        View btnSearch = createRoundButton(R.drawable.ic_search, v -> {
            if (mListener != null) mListener.onSearchClick();
        });
        header.addView(btnSearch);

        View btnNewFolder = createRoundButton(R.drawable.ic_folder_plus, v -> {
            if (mListener != null) mListener.onNewFolderClick();
        });
        LinearLayout.LayoutParams newFolderLp = new LinearLayout.LayoutParams(
                ThemeTokens.dp(getContext(), ThemeTokens.SIZE_HEADER_BTN),
                ThemeTokens.dp(getContext(), ThemeTokens.SIZE_HEADER_BTN));
        newFolderLp.leftMargin = ThemeTokens.dp(getContext(), 8);
        btnNewFolder.setLayoutParams(newFolderLp);
        header.addView(btnNewFolder);

        View btnMenu = createRoundButton(R.drawable.ic_more_dots, v -> {
            if (mListener != null) mListener.onMoreClick();
        });
        LinearLayout.LayoutParams menuLp = new LinearLayout.LayoutParams(
                ThemeTokens.dp(getContext(), ThemeTokens.SIZE_HEADER_BTN),
                ThemeTokens.dp(getContext(), ThemeTokens.SIZE_HEADER_BTN));
        menuLp.leftMargin = ThemeTokens.dp(getContext(), 8);
        btnMenu.setLayoutParams(menuLp);
        header.addView(btnMenu);

        addView(header);

        // Breadcrumbs Row
        mBreadcrumbScroll = new HorizontalScrollView(getContext());
        mBreadcrumbScroll.setHorizontalScrollBarEnabled(false);
        mBreadcrumbScroll.setPadding(ThemeTokens.dp(getContext(), 16), ThemeTokens.dp(getContext(), 2),
                ThemeTokens.dp(getContext(), 16), ThemeTokens.dp(getContext(), 6));

        mBreadcrumbContainer = new LinearLayout(getContext());
        mBreadcrumbContainer.setOrientation(LinearLayout.HORIZONTAL);
        mBreadcrumbContainer.setGravity(Gravity.CENTER_VERTICAL);
        mBreadcrumbScroll.addView(mBreadcrumbContainer);
        addView(mBreadcrumbScroll);

        // Bottom separator line (0.5dp)
        View divider = new View(getContext());
        divider.setBackgroundColor(ThemeTokens.COLOR_SEPARATOR_INSET);
        int divH = Math.max(1, ThemeTokens.dp(getContext(), 1) / 2);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, divH));
        addView(divider);
    }

    public void setOnHeaderActionListener(OnHeaderActionListener listener) {
        mListener = listener;
    }

    public void setScrolledState(boolean isScrolled) {
        if (isScrolled) {
            setBackgroundColor(ThemeTokens.COLOR_SURFACE_GLASS);
            mLargeTitleText.setTextSize(17);
            mLargeTitleText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            mLargeTitleText.setGravity(Gravity.CENTER_VERTICAL);
        } else {
            setBackgroundColor(Color.TRANSPARENT);
            mLargeTitleText.setTextSize(34);
            mLargeTitleText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            mLargeTitleText.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        }
    }

    public void updateBreadcrumbs(File currentDir) {
        if (currentDir == null) return;
        mBreadcrumbContainer.removeAllViews();
        String rootPrefix = Environment.getExternalStorageDirectory().getAbsolutePath();
        String currentPath = currentDir.getAbsolutePath();

        List<BreadcrumbPart> parts = new ArrayList<>();
        if (currentPath.equals(rootPrefix) || currentPath.equals("/sdcard") || currentPath.equals("/")) {
            parts.add(new BreadcrumbPart("Bộ nhớ trong", rootPrefix));
        } else if (currentPath.startsWith(rootPrefix)) {
            parts.add(new BreadcrumbPart("Bộ nhớ trong", rootPrefix));
            String sub = currentPath.substring(rootPrefix.length()).replaceAll("^/+", "");
            if (!sub.isEmpty()) {
                String[] segments = sub.split("/");
                String accum = rootPrefix;
                for (String seg : segments) {
                    accum = accum + "/" + seg;
                    parts.add(new BreadcrumbPart(seg, accum));
                }
            }
        } else {
            parts.add(new BreadcrumbPart(currentDir.getName(), currentPath));
        }

        for (int i = 0; i < parts.size(); i++) {
            BreadcrumbPart p = parts.get(i);
            boolean isLast = (i == parts.size() - 1);

            TextView chip = new TextView(getContext());
            chip.setText(p.name);
            chip.setTextSize(13);
            chip.setGravity(Gravity.CENTER);
            chip.setTextColor(isLast ? ThemeTokens.COLOR_LABEL : ThemeTokens.COLOR_SEC_LABEL);
            chip.setTypeface(isLast ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            chip.setPadding(ThemeTokens.dp(getContext(), 10), ThemeTokens.dp(getContext(), 5),
                    ThemeTokens.dp(getContext(), 10), ThemeTokens.dp(getContext(), 5));

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setCornerRadius(ThemeTokens.dp(getContext(), 999));
            chipBg.setColor(isLast ? ThemeTokens.COLOR_SURFACE_ELEVATED : 0x1A545458);
            chip.setBackground(chipBg);

            chip.setOnClickListener(v -> {
                File target = new File(p.fullPath);
                if (target.exists() && target.isDirectory() && mListener != null) {
                    mListener.onBreadcrumbClick(target);
                }
            });

            mBreadcrumbContainer.addView(chip);

            if (!isLast) {
                TextView arrow = new TextView(getContext());
                arrow.setText(" › ");
                arrow.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
                arrow.setTextSize(13);
                arrow.setPadding(ThemeTokens.dp(getContext(), 2), 0, ThemeTokens.dp(getContext(), 2), 0);
                mBreadcrumbContainer.addView(arrow);
            }
        }

        mBreadcrumbScroll.post(() -> mBreadcrumbScroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT));
    }

    private View createRoundButton(int iconResId, View.OnClickListener listener) {
        LinearLayout btn = new LinearLayout(getContext());
        int btnSize = ThemeTokens.dp(getContext(), ThemeTokens.SIZE_HEADER_BTN);
        btn.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
        btn.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(ThemeTokens.COLOR_SURFACE_ELEVATED);
        btn.setBackground(bg);

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(iconResId);
        icon.setColorFilter(ThemeTokens.COLOR_ACCENT);
        int iconSize = ThemeTokens.dp(getContext(), 18);
        icon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
        btn.addView(icon);

        btn.setOnClickListener(listener);
        return btn;
    }

    private static class BreadcrumbPart {
        final String name;
        final String fullPath;
        BreadcrumbPart(String name, String fullPath) {
            this.name = name;
            this.fullPath = fullPath;
        }
    }
}
