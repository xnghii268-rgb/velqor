package com.velqor.app.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.velqor.app.R;
import com.velqor.app.tokens.ThemeTokens;

import java.util.ArrayList;
import java.util.List;

public class FloatingTabBar extends FrameLayout {

    public interface OnTabSelectedListener {
        void onTabSelected(String tabKey, int index);
    }

    private BlurBackdropView mBackdropView;
    private View mWaterDropIndicator;
    private final List<View> mTabViews = new ArrayList<>();
    private final String[] mTabKeys = {"files", "archive", "vault", "hash", "settings"};
    private String mCurrentTab = "files";
    private OnTabSelectedListener mListener;

    public FloatingTabBar(Context context) {
        super(context);
        init();
    }

    private void init() {
        int h = ThemeTokens.dp(getContext(), ThemeTokens.HEIGHT_TAB_BAR);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));
        setClipChildren(false);
        setClipToPadding(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(ThemeTokens.dp(getContext(), ThemeTokens.ELEVATION_FLOATING_BAR));
        }

        // 1. Real Hardware Blur / Glass Fallback Backdrop View
        mBackdropView = new BlurBackdropView(getContext());
        mBackdropView.setCornerRadius(ThemeTokens.dp(getContext(), ThemeTokens.RADIUS_TAB_BAR));
        addView(mBackdropView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 2. Water-drop Indicator Pill (height = 56dp, radius = 28dp)
        mWaterDropIndicator = new View(getContext());
        GradientDrawable dropBg = new GradientDrawable();
        dropBg.setCornerRadius(ThemeTokens.dp(getContext(), ThemeTokens.RADIUS_WATER_DROP));
        dropBg.setColor(0x2E0A84FF); // 18% accent tint
        dropBg.setStroke(ThemeTokens.dp(getContext(), 1), 0x550A84FF);
        mWaterDropIndicator.setBackground(dropBg);

        int dropHeight = ThemeTokens.dp(getContext(), ThemeTokens.HEIGHT_WATER_DROP);
        FrameLayout.LayoutParams dropLp = new FrameLayout.LayoutParams(dropHeight, dropHeight);
        dropLp.gravity = Gravity.CENTER_VERTICAL;
        addView(mWaterDropIndicator, dropLp);

        // 3. 5 Tab Items in Horizontal Row
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mTabViews.clear();
        mTabViews.add(createTabItem(R.drawable.ic_folder, "Tệp", "files", 0));
        mTabViews.add(createTabItem(R.drawable.ic_file_archive, "Nén", "archive", 1));
        mTabViews.add(createTabItem(R.drawable.ic_tab_vault, "Vault", "vault", 2));
        mTabViews.add(createTabItem(R.drawable.ic_tab_hash, "Hash", "hash", 3));
        mTabViews.add(createTabItem(R.drawable.ic_tab_settings, "Cài đặt", "settings", 4));

        for (View tab : mTabViews) {
            row.addView(tab);
        }
        addView(row);

        post(() -> {
            adjustIndicatorWidth(0);
            animateIndicatorToTab(0, false);
        });
    }

    public void setBackdropSource(View target) {
        if (mBackdropView != null) {
            mBackdropView.setBackdropSource(target);
        }
    }

    public void invalidateBackdrop() {
        if (mBackdropView != null) {
            mBackdropView.invalidate();
        }
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        mListener = listener;
    }

    private View createTabItem(int iconResId, String label, String tabKey, int index) {
        LinearLayout item = new LinearLayout(getContext());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setClipChildren(false);
        item.setClipToPadding(false);
        item.setPadding(ThemeTokens.dp(getContext(), 2), ThemeTokens.dp(getContext(), 2),
                ThemeTokens.dp(getContext(), 2), ThemeTokens.dp(getContext(), 2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        item.setLayoutParams(lp);

        boolean isSelected = mCurrentTab.equals(tabKey);
        int tint = isSelected ? ThemeTokens.COLOR_ACCENT : ThemeTokens.COLOR_SEC_LABEL;

        ImageView iv = new ImageView(getContext());
        iv.setImageResource(iconResId);
        iv.setColorFilter(tint);
        int iconSize = ThemeTokens.dp(getContext(), 24);
        iv.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
        item.addView(iv);

        TextView tv = new TextView(getContext());
        tv.setText(label);
        tv.setTextSize(10);
        tv.setTextColor(tint);
        tv.setTypeface(isSelected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textLp.topMargin = ThemeTokens.dp(getContext(), 2);
        tv.setLayoutParams(textLp);
        item.addView(tv);

        item.setOnClickListener(v -> {
            if (!mCurrentTab.equals(tabKey)) {
                mCurrentTab = tabKey;
                animateIndicatorToTab(index, true);
                updateTabSelectionState();
                if (mListener != null) {
                    mListener.onTabSelected(tabKey, index);
                }
            }
        });

        return item;
    }

    public void updateTabSelectionState() {
        for (int i = 0; i < mTabViews.size(); i++) {
            LinearLayout item = (LinearLayout) mTabViews.get(i);
            String tabKey = mTabKeys[i];
            boolean isSelected = mCurrentTab.equals(tabKey);
            int tint = isSelected ? ThemeTokens.COLOR_ACCENT : ThemeTokens.COLOR_SEC_LABEL;

            ImageView iv = (ImageView) item.getChildAt(0);
            TextView tv = (TextView) item.getChildAt(1);

            iv.setColorFilter(tint);
            tv.setTextColor(tint);
            tv.setTypeface(isSelected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        }
    }

    private void adjustIndicatorWidth(int index) {
        if (index >= 0 && index < mTabViews.size()) {
            View tab = mTabViews.get(index);
            if (tab.getWidth() > 0) {
                int dropW = tab.getWidth() - ThemeTokens.dp(getContext(), 8);
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mWaterDropIndicator.getLayoutParams();
                int dropH = ThemeTokens.dp(getContext(), ThemeTokens.HEIGHT_WATER_DROP);
                if (lp != null && (lp.width != dropW || lp.height != dropH)) {
                    lp.width = dropW;
                    lp.height = dropH;
                    mWaterDropIndicator.setLayoutParams(lp);
                }
            }
        }
    }

    public void animateIndicatorToTab(int index, boolean animate) {
        if (index < 0 || index >= mTabViews.size()) return;
        View targetTab = mTabViews.get(index);
        adjustIndicatorWidth(index);
        int targetX = targetTab.getLeft() + ThemeTokens.dp(getContext(), 4);

        if (animate) {
            // Spring water-drop stretch (scaleX 1.22, scaleY 0.92) & snap
            mWaterDropIndicator.animate()
                .translationX(targetX)
                .scaleX(1.22f)
                .scaleY(0.92f)
                .setDuration(130)
                .withEndAction(() -> {
                    mWaterDropIndicator.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(180)
                        .setInterpolator(new OvershootInterpolator(1.2f))
                        .start();
                })
                .start();

            // Scale icon lightly
            ImageView icon = (ImageView) ((LinearLayout) targetTab).getChildAt(0);
            icon.animate().scaleX(1.18f).scaleY(1.18f).setDuration(100).withEndAction(() -> {
                icon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
            }).start();
        } else {
            mWaterDropIndicator.setTranslationX(targetX);
        }
    }
}
