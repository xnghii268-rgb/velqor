package com.velqor.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Environment;
import android.view.Gravity;
import android.view.MotionEvent;
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
        void onViewModeToggleClick();
    }

    private TextView mLargeTitleText;
    private HorizontalScrollView mBreadcrumbScroll;
    private LinearLayout mBreadcrumbContainer;
    private HorizontalScrollView mShortcutsScroll;
    private LinearLayout mShortcutsContainer;
    private View mBtnNewItem;
    private ImageView mBtnViewModeIcon;
    private View mDivider;
    private boolean mIsGridMode = false;
    private OnHeaderActionListener mListener;

    public HeaderView(Context context, int statusBarHeight) {
        super(context);
        init(statusBarHeight);
    }

    private void init(int statusBarHeight) {
        setOrientation(LinearLayout.VERTICAL);
        setBackgroundColor(Color.TRANSPARENT);

        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(ThemeTokens.dp(getContext(), 16), statusBarHeight + ThemeTokens.dp(getContext(), 8),
                ThemeTokens.dp(getContext(), 16), ThemeTokens.dp(getContext(), 4));
        header.setGravity(Gravity.CENTER_VERTICAL);

        mLargeTitleText = new TextView(getContext());
        mLargeTitleText.setText("VELQOR");
        mLargeTitleText.setTextSize(34);
        mLargeTitleText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mLargeTitleText.setTextColor(ThemeTokens.getLabelColor(getContext()));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        header.addView(mLargeTitleText, titleLp);

        View btnSearch = createRoundButton(R.drawable.ic_search, v -> {
            if (mListener != null) mListener.onSearchClick();
        });
        header.addView(btnSearch);

        View btnViewMode = createRoundButton(R.drawable.ic_view_grid, v -> {
            if (mListener != null) mListener.onViewModeToggleClick();
        });
        mBtnViewModeIcon = (ImageView) ((LinearLayout) btnViewMode).getChildAt(0);
        LinearLayout.LayoutParams vmMargin = new LinearLayout.LayoutParams(
                ThemeTokens.dp(getContext(), ThemeTokens.SIZE_HEADER_BTN),
                ThemeTokens.dp(getContext(), ThemeTokens.SIZE_HEADER_BTN));
        vmMargin.leftMargin = ThemeTokens.dp(getContext(), 8);
        btnViewMode.setLayoutParams(vmMargin);
        header.addView(btnViewMode);

        View btnNewFolder = createRoundButton(R.drawable.ic_plus, v -> {
            if (mListener != null) mListener.onNewFolderClick();
        });
        mBtnNewItem = btnNewFolder;
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

        mBreadcrumbScroll = new HorizontalScrollView(getContext());
        mBreadcrumbScroll.setHorizontalScrollBarEnabled(false);
        mBreadcrumbScroll.setPadding(ThemeTokens.dp(getContext(), 16), ThemeTokens.dp(getContext(), 2),
                ThemeTokens.dp(getContext(), 16), ThemeTokens.dp(getContext(), 4));

        mBreadcrumbContainer = new LinearLayout(getContext());
        mBreadcrumbContainer.setOrientation(LinearLayout.HORIZONTAL);
        mBreadcrumbContainer.setGravity(Gravity.CENTER_VERTICAL);
        mBreadcrumbScroll.addView(mBreadcrumbContainer);
        addView(mBreadcrumbScroll);

        mShortcutsScroll = new HorizontalScrollView(getContext());
        mShortcutsScroll.setHorizontalScrollBarEnabled(false);
        mShortcutsScroll.setPadding(ThemeTokens.dp(getContext(), 16), 0,
                ThemeTokens.dp(getContext(), 16), ThemeTokens.dp(getContext(), 6));

        mShortcutsContainer = new LinearLayout(getContext());
        mShortcutsContainer.setOrientation(LinearLayout.HORIZONTAL);
        mShortcutsContainer.setGravity(Gravity.CENTER_VERTICAL);
        buildShortcuts();
        mShortcutsScroll.addView(mShortcutsContainer);
        addView(mShortcutsScroll);

        mDivider = new View(getContext());
        mDivider.setBackgroundColor(ThemeTokens.COLOR_SEPARATOR_INSET);
        int divH = Math.max(1, ThemeTokens.dp(getContext(), 1) / 2);
        mDivider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, divH));
        addView(mDivider);
    }

    private void buildShortcuts() {
        mShortcutsContainer.removeAllViews();
        File root = Environment.getExternalStorageDirectory();
        addShortcutChip("Tải về", new File(root, "Download"));
        addShortcutChip("Ảnh/DCIM", new File(root, "DCIM"));
        addShortcutChip("Tài liệu", new File(root, "Documents"));
        addShortcutChip("Hình ảnh", new File(root, "Pictures"));
        addShortcutChip("Âm nhạc", new File(root, "Music"));
        addShortcutChip("Phim", new File(root, "Movies"));
    }

    private void addShortcutChip(String label, File target) {
        TextView chip = new TextView(getContext());
        chip.setText(label);
        chip.setTextSize(12);
        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        chip.setTextColor(ThemeTokens.getSecondaryLabelColor(getContext()));
        chip.setPadding(ThemeTokens.dp(getContext(), 12), ThemeTokens.dp(getContext(), 6),
                ThemeTokens.dp(getContext(), 12), ThemeTokens.dp(getContext(), 6));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(ThemeTokens.dp(getContext(), 999));
        bg.setColor(ThemeTokens.getActionGroupBgColor(getContext()));
        bg.setStroke(Math.max(1, ThemeTokens.dp(getContext(), 1) / 2), ThemeTokens.getStrokeColor(getContext()));
        chip.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = ThemeTokens.dp(getContext(), 6);
        chip.setLayoutParams(lp);

        chip.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                    break;
            }
            return false;
        });

        chip.setOnClickListener(v -> {
            if (target != null && target.exists() && mListener != null) {
                mListener.onBreadcrumbClick(target);
            }
        });

        mShortcutsContainer.addView(chip);
    }

    public void setTitle(String title) {
        if (mLargeTitleText != null) {
            mLargeTitleText.setText(title);
        }
    }

    public void setPathVisibility(boolean visible) {
        int v = visible ? View.VISIBLE : View.GONE;
        if (mBreadcrumbScroll != null) mBreadcrumbScroll.setVisibility(v);
        if (mShortcutsScroll != null) mShortcutsScroll.setVisibility(v);
        if (mDivider != null) mDivider.setVisibility(v);
    }

    public void setViewMode(boolean isGrid) {
        mIsGridMode = isGrid;
        if (mBtnViewModeIcon != null) {
            mBtnViewModeIcon.setImageResource(isGrid ? R.drawable.ic_view_list : R.drawable.ic_view_grid);
        }
    }

    public void setOnHeaderActionListener(OnHeaderActionListener listener) {
        mListener = listener;
    }

    public void setNewItemButtonVisibility(boolean visible) {
        if (mBtnNewItem != null) {
            mBtnNewItem.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public void setScrolledState(boolean isScrolled) {
        if (isScrolled) {
            setBackgroundColor(ThemeTokens.getSurfaceGlassColor(getContext()));
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
            chip.setTextColor(isLast ? ThemeTokens.getLabelColor(getContext()) : ThemeTokens.getSecondaryLabelColor(getContext()));
            chip.setTypeface(isLast ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            chip.setPadding(ThemeTokens.dp(getContext(), 12), ThemeTokens.dp(getContext(), 5),
                    ThemeTokens.dp(getContext(), 12), ThemeTokens.dp(getContext(), 5));

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setCornerRadius(ThemeTokens.dp(getContext(), 999));
            if (isLast) {
                chipBg.setColor(ThemeTokens.getActionGroupBgColor(getContext()));
                chipBg.setStroke(Math.max(1, ThemeTokens.dp(getContext(), 1) / 2), ThemeTokens.getStrokeColor(getContext()));
            } else {
                chipBg.setColor(ThemeTokens.isDarkMode(getContext()) ? 0x18FFFFFF : 0x0F000000);
            }
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
                arrow.setTextColor(ThemeTokens.getSecondaryLabelColor(getContext()));
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
        bg.setColor(ThemeTokens.getActionGroupBgColor(getContext()));
        bg.setStroke(Math.max(1, ThemeTokens.dp(getContext(), 1) / 2), ThemeTokens.getStrokeColor(getContext()));
        btn.setBackground(bg);

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(iconResId);
        icon.setColorFilter(ThemeTokens.getAccentColor(getContext()));
        int iconSize = ThemeTokens.dp(getContext(), 18);
        icon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
        btn.addView(icon);

        btn.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                    break;
            }
            return false;
        });

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
