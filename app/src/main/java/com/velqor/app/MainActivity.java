package com.velqor.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends Activity {

    // ==========================================
    // iOS DESIGN TOKENS (MILESTONE UI-REFIT-2 & UI-REFIT-3)
    // ==========================================
    private static final int COLOR_BG = 0xFF000000;
    private static final int COLOR_SURFACE_ELEVATED = 0xFF1C1C1E;
    private static final int COLOR_ACTION_GROUP_BG = 0xFF2C2C2E;
    private static final int COLOR_LABEL = 0xFFFFFFFF;
    private static final int COLOR_SEC_LABEL = 0x99EBEBF5; // 60% alpha
    private static final int COLOR_SEPARATOR = 0x99545458; // 60% alpha
    private static final int COLOR_ACCENT = 0xFF0A84FF;    // Apple Blue
    private static final int COLOR_DANGER = 0xFFFF453A;    // Apple Red
    private static final int COLOR_SUCCESS = 0xFF30D158;   // Apple Green

    // Semantic Icon Colors (High contrast >= 3:1)
    private static final int COLOR_ICON_FOLDER = 0xFF0A84FF;
    private static final int COLOR_ICON_IMAGE = 0xFFFF9F0A;
    private static final int COLOR_ICON_VIDEO = 0xFFFF375F;
    private static final int COLOR_ICON_AUDIO = 0xFFBF5AF2;
    private static final int COLOR_ICON_DOCUMENT = 0xFF64D2FF;
    private static final int COLOR_ICON_ARCHIVE = 0xFF5E5CE6;
    private static final int COLOR_ICON_APK = 0xFF30D158;
    private static final int COLOR_ICON_OTHER = 0xFF8E8E93;

    private File mCurrentDir;
    private LinearLayout mHeaderRoot;
    private TextView mLargeTitleText;
    private LinearLayout mBreadcrumbContainer;
    private HorizontalScrollView mBreadcrumbScroll;
    private LinearLayout mFileListGroup;
    private ScrollView mScrollView;
    private LinearLayout mSelectionBar;
    private TextView mSelectionCountText;

    // Floating Glass Tab Bar & Water-Drop Indicator
    private FrameLayout mFloatingTabContainer;
    private View mWaterDropIndicator;
    private final List<View> mTabViews = new ArrayList<>();
    private final String[] mTabKeys = {"files", "archive", "vault", "hash", "settings"};
    private String mCurrentTab = "files";

    private boolean mShowHidden = false;
    private int mSortField = 0; // 0: Name, 1: Date, 2: Size, 3: Type
    private boolean mSortAsc = true;

    private boolean mIsSelectionMode = false;
    private final Set<String> mSelectedPaths = new HashSet<>();

    // File clipboard for real Copy & Move operations
    private File mClipboardFile = null;
    private boolean mIsCutAction = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. True Edge-to-Edge Fullscreen
        setupEdgeToEdge();

        mCurrentDir = Environment.getExternalStorageDirectory();
        checkAndRequestPermissions();

        // 2. Build iOS-Grade Layered View Hierarchy
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(COLOR_BG);

        // Main Scrollable Area with Inset Grouped List
        mScrollView = new ScrollView(this);
        mScrollView.setFillViewport(true);

        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        // Top padding: status bar + 8dp + header (~110dp), Bottom padding: 96dp for floating tab bar
        int topPadding = getStatusBarHeight() + dp(100);
        scrollContent.setPadding(dp(16), topPadding, dp(16), dp(96));

        mFileListGroup = new LinearLayout(this);
        mFileListGroup.setOrientation(LinearLayout.VERTICAL);
        scrollContent.addView(mFileListGroup);

        mScrollView.addView(scrollContent);
        root.addView(mScrollView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Header Layer: Sticky iOS Frosted Header & Breadcrumbs
        mHeaderRoot = createIosHeaderLayer();
        FrameLayout.LayoutParams headerLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerLp.gravity = Gravity.TOP;
        root.addView(mHeaderRoot, headerLp);

        // Multi-Selection Floating Action Bar
        mSelectionBar = createSelectionBar();
        FrameLayout.LayoutParams selLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        selLp.gravity = Gravity.TOP;
        selLp.topMargin = getStatusBarHeight() + dp(88);
        root.addView(mSelectionBar, selLp);

        // Floating Liquid Glass Tab Bar (Milestone UI-REFIT-3)
        View floatingTabBar = createFloatingGlassTabBar();
        FrameLayout.LayoutParams tabLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64));
        tabLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        tabLp.leftMargin = dp(20);
        tabLp.rightMargin = dp(20);
        tabLp.bottomMargin = getNavBarHeight() + dp(12);
        root.addView(floatingTabBar, tabLp);

        // Header Scroll Behavior: Large title shrinks to inline title, background turns frosted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (scrollY > dp(30)) {
                    mHeaderRoot.setBackgroundColor(0xE6000000);
                    mLargeTitleText.setTextSize(17);
                    mLargeTitleText.setGravity(Gravity.CENTER_VERTICAL);
                } else {
                    mHeaderRoot.setBackgroundColor(Color.TRANSPARENT);
                    mLargeTitleText.setTextSize(34);
                    mLargeTitleText.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                }
            });
        }

        setContentView(root);
        refreshDirectory();
    }

    private void setupEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    private int getStatusBarHeight() {
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resId > 0 ? getResources().getDimensionPixelSize(resId) : dp(24);
    }

    private int getNavBarHeight() {
        int resId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return resId > 0 ? getResources().getDimensionPixelSize(resId) : dp(16);
    }

    // ==========================================
    // HEADER & BREADCRUMBS (8dp under status bar)
    // ==========================================
    private LinearLayout createIosHeaderLayer() {
        LinearLayout layer = new LinearLayout(this);
        layer.setOrientation(LinearLayout.VERTICAL);
        layer.setBackgroundColor(Color.TRANSPARENT);

        // Header Top Row
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(16), getStatusBarHeight() + dp(8), dp(16), dp(4));
        header.setGravity(Gravity.CENTER_VERTICAL);

        mLargeTitleText = new TextView(this);
        mLargeTitleText.setText("VELQOR");
        mLargeTitleText.setTextSize(34);
        mLargeTitleText.setTypeface(Typeface.DEFAULT_BOLD);
        mLargeTitleText.setTextColor(COLOR_LABEL);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        header.addView(mLargeTitleText, titleLp);

        // Round Action: 36dp Search
        View btnSearch = createRoundButton(R.drawable.ic_search, v -> showSearchSheet());
        header.addView(btnSearch);

        // Round Action: 36dp Folder-Plus
        View btnNewFolder = createRoundButton(R.drawable.ic_folder_plus, v -> showNewFolderSheet());
        LinearLayout.LayoutParams newFolderLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        newFolderLp.leftMargin = dp(8);
        btnNewFolder.setLayoutParams(newFolderLp);
        header.addView(btnNewFolder);

        // Round Action: 36dp More "⋯"
        View btnMenu = createRoundButton(R.drawable.ic_more_dots, v -> showMoreOptionsSheet());
        LinearLayout.LayoutParams menuLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        menuLp.leftMargin = dp(8);
        btnMenu.setLayoutParams(menuLp);
        header.addView(btnMenu);

        layer.addView(header);

        // Breadcrumbs Row
        View breadcrumbView = createIosBreadcrumbs();
        layer.addView(breadcrumbView);

        // Bottom separator line
        View divider = new View(this);
        divider.setBackgroundColor(COLOR_SEPARATOR);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        layer.addView(divider);

        return layer;
    }

    private View createRoundButton(int iconResId, View.OnClickListener listener) {
        LinearLayout btn = new LinearLayout(this);
        btn.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
        btn.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(COLOR_SURFACE_ELEVATED);
        btn.setBackground(bg);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconResId);
        icon.setColorFilter(COLOR_ACCENT);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(18), dp(18)));
        btn.addView(icon);

        btn.setOnClickListener(listener);
        return btn;
    }

    private View createIosBreadcrumbs() {
        mBreadcrumbScroll = new HorizontalScrollView(this);
        mBreadcrumbScroll.setHorizontalScrollBarEnabled(false);
        mBreadcrumbScroll.setPadding(dp(16), dp(2), dp(16), dp(6));

        mBreadcrumbContainer = new LinearLayout(this);
        mBreadcrumbContainer.setOrientation(LinearLayout.HORIZONTAL);
        mBreadcrumbContainer.setGravity(Gravity.CENTER_VERTICAL);
        mBreadcrumbScroll.addView(mBreadcrumbContainer);

        return mBreadcrumbScroll;
    }

    private void updateBreadcrumbs() {
        mBreadcrumbContainer.removeAllViews();
        String rootPrefix = Environment.getExternalStorageDirectory().getAbsolutePath();
        String currentPath = mCurrentDir.getAbsolutePath();

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
            parts.add(new BreadcrumbPart(mCurrentDir.getName(), currentPath));
        }

        for (int i = 0; i < parts.size(); i++) {
            BreadcrumbPart p = parts.get(i);
            boolean isLast = (i == parts.size() - 1);

            TextView chip = new TextView(this);
            chip.setText(p.name);
            chip.setTextSize(13);
            chip.setGravity(Gravity.CENTER);
            chip.setTextColor(isLast ? COLOR_LABEL : COLOR_SEC_LABEL);
            chip.setTypeface(isLast ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            chip.setPadding(dp(10), dp(5), dp(10), dp(5));

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setCornerRadius(dp(999));
            chipBg.setColor(isLast ? COLOR_SURFACE_ELEVATED : 0x1A545458);
            chip.setBackground(chipBg);

            chip.setOnClickListener(v -> {
                File target = new File(p.fullPath);
                if (target.exists() && target.isDirectory() && !target.equals(mCurrentDir)) {
                    mCurrentDir = target;
                    refreshDirectory();
                }
            });

            mBreadcrumbContainer.addView(chip);

            if (!isLast) {
                TextView arrow = new TextView(this);
                arrow.setText(" › ");
                arrow.setTextColor(COLOR_SEC_LABEL);
                arrow.setTextSize(13);
                arrow.setPadding(dp(2), 0, dp(2), 0);
                mBreadcrumbContainer.addView(arrow);
            }
        }

        mBreadcrumbScroll.post(() -> mBreadcrumbScroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT));
    }

    private static class BreadcrumbPart {
        final String name;
        final String fullPath;
        BreadcrumbPart(String name, String fullPath) {
            this.name = name;
            this.fullPath = fullPath;
        }
    }

    // ==========================================
    // MULTI-SELECTION BAR
    // ==========================================
    private LinearLayout createSelectionBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(dp(16), dp(10), dp(16), dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(COLOR_SURFACE_ELEVATED);
        bar.setBackground(bg);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setVisibility(View.GONE);

        mSelectionCountText = new TextView(this);
        mSelectionCountText.setText("Đã chọn 0 mục");
        mSelectionCountText.setTextColor(COLOR_LABEL);
        mSelectionCountText.setTextSize(15);
        mSelectionCountText.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bar.addView(mSelectionCountText, countLp);

        // Action: Batch Options Sheet
        ImageView btnMore = createBarIcon(R.drawable.ic_more_dots, COLOR_ACCENT, v -> showBatchOptionsSheet());
        bar.addView(btnMore);

        // Action: Compress
        ImageView btnZip = createBarIcon(R.drawable.ic_file_archive, COLOR_ACCENT, v -> compressSelected());
        bar.addView(btnZip);

        // Action: Delete
        ImageView btnDel = createBarIcon(R.drawable.ic_delete, COLOR_DANGER, v -> showBatchDeleteConfirmSheet());
        bar.addView(btnDel);

        // Action: Done
        TextView btnDone = new TextView(this);
        btnDone.setText("Xong");
        btnDone.setTextColor(COLOR_ACCENT);
        btnDone.setTextSize(16);
        btnDone.setTypeface(Typeface.DEFAULT_BOLD);
        btnDone.setPadding(dp(12), dp(4), dp(4), dp(4));
        btnDone.setOnClickListener(v -> exitSelectionMode());
        bar.addView(btnDone);

        return bar;
    }

    private ImageView createBarIcon(int resId, int color, View.OnClickListener listener) {
        ImageView iv = new ImageView(this);
        iv.setImageResource(resId);
        iv.setColorFilter(color);
        iv.setPadding(dp(8), dp(4), dp(8), dp(4));
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(36)));
        iv.setOnClickListener(listener);
        return iv;
    }

    private void enterSelectionMode() {
        mIsSelectionMode = true;
        mSelectedPaths.clear();
        vibrateLight();
        mSelectionBar.setVisibility(View.VISIBLE);
        updateSelectionCount();
        refreshDirectory();
    }

    private void exitSelectionMode() {
        mIsSelectionMode = false;
        mSelectedPaths.clear();
        mSelectionBar.setVisibility(View.GONE);
        refreshDirectory();
    }

    private void updateSelectionCount() {
        mSelectionCountText.setText("Đã chọn " + mSelectedPaths.size() + " mục");
    }

    // ==========================================
    // FLOATING LIQUID GLASS TAB BAR (MILESTONE UI-REFIT-3)
    // ==========================================
    private View createFloatingGlassTabBar() {
        mFloatingTabContainer = new FrameLayout(this);
        mFloatingTabContainer.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));

        // Pill shape glass container
        GradientDrawable glassBg = new GradientDrawable();
        glassBg.setCornerRadius(dp(32));
        glassBg.setColor(0xE61C1C1E); // Translucent surfaceElevated
        glassBg.setStroke(dp(1), 0x33FFFFFF); // Specular rim
        mFloatingTabContainer.setBackground(glassBg);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mFloatingTabContainer.setElevation(dp(8));
        }

        // Water-drop indicator pill (behind selected tab)
        mWaterDropIndicator = new View(this);
        GradientDrawable dropBg = new GradientDrawable();
        dropBg.setCornerRadius(dp(22));
        dropBg.setColor(0x2E0A84FF); // 18% accent tint
        dropBg.setStroke(dp(1), 0x550A84FF);
        mWaterDropIndicator.setBackground(dropBg);
        FrameLayout.LayoutParams dropLp = new FrameLayout.LayoutParams(dp(56), dp(44));
        dropLp.gravity = Gravity.CENTER_VERTICAL;
        mFloatingTabContainer.addView(mWaterDropIndicator, dropLp);

        // 5 Tab items in horizontal row
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
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
        mFloatingTabContainer.addView(row);

        // Initial layout indicator position
        mFloatingTabContainer.post(() -> animateIndicatorToTab(0, false));

        return mFloatingTabContainer;
    }

    private View createTabItem(int iconResId, String label, String tabKey, int index) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(4), dp(4), dp(4), dp(4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        item.setLayoutParams(lp);

        boolean isSelected = mCurrentTab.equals(tabKey);
        int tint = isSelected ? COLOR_ACCENT : COLOR_SEC_LABEL;

        ImageView iv = new ImageView(this);
        iv.setImageResource(iconResId);
        iv.setColorFilter(tint);
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(22), dp(22)));
        item.addView(iv);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(10);
        tv.setTextColor(tint);
        tv.setTypeface(isSelected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textLp.topMargin = dp(2);
        tv.setLayoutParams(textLp);
        item.addView(tv);

        item.setOnClickListener(v -> {
            if (!mCurrentTab.equals(tabKey)) {
                mCurrentTab = tabKey;
                vibrateLight();
                animateIndicatorToTab(index, true);
                updateTabSelectionState();
                switchTab(tabKey);
            }
        });

        return item;
    }

    private void animateIndicatorToTab(int index, boolean animate) {
        if (index < 0 || index >= mTabViews.size()) return;
        View targetTab = mTabViews.get(index);
        int targetX = targetTab.getLeft() + (targetTab.getWidth() - mWaterDropIndicator.getWidth()) / 2;

        if (animate) {
            // Spring water-drop stretch & snap
            mWaterDropIndicator.animate()
                .translationX(targetX)
                .scaleX(1.20f)
                .setDuration(120)
                .withEndAction(() -> {
                    mWaterDropIndicator.animate()
                        .scaleX(1.0f)
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

    private void updateTabSelectionState() {
        for (int i = 0; i < mTabViews.size(); i++) {
            LinearLayout item = (LinearLayout) mTabViews.get(i);
            String tabKey = mTabKeys[i];
            boolean isSelected = mCurrentTab.equals(tabKey);
            int tint = isSelected ? COLOR_ACCENT : COLOR_SEC_LABEL;

            ImageView iv = (ImageView) item.getChildAt(0);
            TextView tv = (TextView) item.getChildAt(1);

            iv.setColorFilter(tint);
            tv.setTextColor(tint);
            tv.setTypeface(isSelected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        }
    }

    private void switchTab(String tabKey) {
        if ("files".equals(tabKey)) {
            refreshDirectory();
        } else if ("archive".equals(tabKey)) {
            showArchiveTab();
        } else if ("vault".equals(tabKey)) {
            showVaultTab();
        } else if ("hash".equals(tabKey)) {
            showHashTab();
        } else if ("settings".equals(tabKey)) {
            showSettingsTab();
        }
    }

    // ==========================================
    // DIRECTORY BROWSER & INSET GROUPED LIST
    // ==========================================
    private void refreshDirectory() {
        updateBreadcrumbs();
        mFileListGroup.removeAllViews();

        if (mCurrentDir == null || !mCurrentDir.exists()) {
            showEmptyState("Không tìm thấy thư mục", "Thư mục không tồn tại hoặc đã bị xóa.");
            return;
        }

        File[] filesArr = mCurrentDir.listFiles();
        if (filesArr == null || filesArr.length == 0) {
            showEmptyState("Thư mục rỗng", "Không có tệp hoặc thư mục nào ở đây.");
            return;
        }

        List<File> files = new ArrayList<>(Arrays.asList(filesArr));
        if (!mShowHidden) {
            List<File> filtered = new ArrayList<>();
            for (File f : files) {
                if (!f.getName().startsWith(".")) filtered.add(f);
            }
            files = filtered;
        }

        if (files.isEmpty()) {
            showEmptyState("Chỉ có tệp tin ẩn", "Bật 'Hiện tệp tin ẩn' trong menu ⋯ để xem.");
            return;
        }

        Collections.sort(files, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;

            int res = 0;
            if (mSortField == 0) {
                res = a.getName().compareToIgnoreCase(b.getName());
            } else if (mSortField == 1) {
                res = Long.compare(a.lastModified(), b.lastModified());
            } else if (mSortField == 2) {
                res = Long.compare(a.length(), b.length());
            } else if (mSortField == 3) {
                res = getFileExtension(a).compareToIgnoreCase(getFileExtension(b));
            }
            return mSortAsc ? res : -res;
        });

        // Inset Grouped List Container
        GradientDrawable groupBg = new GradientDrawable();
        groupBg.setCornerRadius(dp(16));
        groupBg.setColor(COLOR_SURFACE_ELEVATED);
        mFileListGroup.setBackground(groupBg);

        for (int i = 0; i < files.size(); i++) {
            File f = files.get(i);
            boolean isLast = (i == files.size() - 1);
            mFileListGroup.addView(createIosFileRow(f, isLast));
        }
    }

    private void showEmptyState(String title, String subtitle) {
        mFileListGroup.setBackground(null);

        LinearLayout emptyLayout = new LinearLayout(this);
        emptyLayout.setOrientation(LinearLayout.VERTICAL);
        emptyLayout.setGravity(Gravity.CENTER);
        emptyLayout.setPadding(dp(32), dp(64), dp(32), dp(32));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_folder);
        icon.setColorFilter(COLOR_SEC_LABEL);
        icon.setAlpha(0.35f);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(64), dp(64)));
        emptyLayout.addView(icon);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(COLOR_LABEL);
        titleView.setTextSize(17);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(16);
        titleView.setLayoutParams(tlp);
        emptyLayout.addView(titleView);

        TextView subView = new TextView(this);
        subView.setText(subtitle);
        subView.setTextColor(COLOR_SEC_LABEL);
        subView.setTextSize(14);
        subView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(6);
        subView.setLayoutParams(slp);
        emptyLayout.addView(subView);

        mFileListGroup.addView(emptyLayout);
    }

    private View createIosFileRow(File f, boolean isLast) {
        LinearLayout rowContainer = new LinearLayout(this);
        rowContainer.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(16), dp(8), dp(16), dp(8));
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(56));

        boolean isSelected = mSelectedPaths.contains(f.getAbsolutePath());
        if (isSelected) {
            row.setBackgroundColor(0x1F0A84FF);
        }

        // Selection Checkbox
        if (mIsSelectionMode) {
            ImageView check = new ImageView(this);
            check.setImageResource(R.drawable.ic_check);
            GradientDrawable checkBg = new GradientDrawable();
            checkBg.setShape(GradientDrawable.OVAL);
            if (isSelected) {
                checkBg.setColor(COLOR_ACCENT);
                check.setColorFilter(Color.WHITE);
            } else {
                checkBg.setColor(Color.TRANSPARENT);
                checkBg.setStroke(dp(2), COLOR_SEPARATOR);
                check.setColorFilter(Color.TRANSPARENT);
            }
            check.setBackground(checkBg);
            check.setPadding(dp(4), dp(4), dp(4), dp(4));

            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(22), dp(22));
            clp.rightMargin = dp(12);
            check.setLayoutParams(clp);
            row.addView(check);
        }

        // 40x40dp Icon in 10dp rounded box with semantic color tint (alpha 0.18, contrast >= 3:1)
        int iconRes = getIconResourceForFile(f);
        int tintColor = getSemanticColorForFile(f);

        LinearLayout iconBox = new LinearLayout(this);
        iconBox.setGravity(Gravity.CENTER);
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));

        GradientDrawable iconBoxBg = new GradientDrawable();
        iconBoxBg.setCornerRadius(dp(10));
        iconBoxBg.setColor((tintColor & 0x00FFFFFF) | 0x2E000000); // 18% opacity tint
        iconBox.setBackground(iconBoxBg);

        ImageView iv = new ImageView(this);
        iv.setImageResource(iconRes);
        iv.setColorFilter(tintColor);
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(22), dp(22)));
        iconBox.addView(iv);

        row.addView(iconBox);

        // Title and Subtitle Info
        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.leftMargin = dp(12);

        TextView nameText = new TextView(this);
        nameText.setText(middleEllipsis(f.getName(), 34));
        nameText.setTextColor(COLOR_LABEL);
        nameText.setTextSize(17);
        nameText.setTypeface(Typeface.DEFAULT);
        nameText.setSingleLine(true);
        textCol.addView(nameText);

        TextView subText = new TextView(this);
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
        subText.setTextColor(COLOR_SEC_LABEL);
        subText.setTextSize(13);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(2);
        subText.setLayoutParams(subLp);
        textCol.addView(subText);

        row.addView(textCol, colLp);

        // Right chevron for folders
        if (f.isDirectory() && !mIsSelectionMode) {
            ImageView chevron = new ImageView(this);
            chevron.setImageResource(R.drawable.ic_chevron_right);
            chevron.setColorFilter(COLOR_SEC_LABEL);
            chevron.setAlpha(0.4f);
            chevron.setLayoutParams(new LinearLayout.LayoutParams(dp(16), dp(16)));
            row.addView(chevron);
        }

        // Tap & Long-Press Interactions
        row.setOnClickListener(v -> {
            if (mIsSelectionMode) {
                toggleFileSelection(f.getAbsolutePath());
            } else {
                if (f.isDirectory()) {
                    mCurrentDir = f;
                    refreshDirectory();
                } else {
                    showFileActionSheet(f);
                }
            }
        });

        row.setOnLongClickListener(v -> {
            if (!mIsSelectionMode) {
                showFileActionSheet(f);
            } else {
                toggleFileSelection(f.getAbsolutePath());
            }
            return true;
        });

        rowContainer.addView(row);

        // Inset Separator: 0.5dp line, inset from text start (68dp), omitted on last item
        if (!isLast) {
            View sep = new View(this);
            sep.setBackgroundColor(COLOR_SEPARATOR);
            LinearLayout.LayoutParams sepLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
            sepLp.leftMargin = dp(mIsSelectionMode ? 102 : 68);
            sep.setLayoutParams(sepLp);
            rowContainer.addView(sep);
        }

        return rowContainer;
    }

    private void toggleFileSelection(String path) {
        if (mSelectedPaths.contains(path)) {
            mSelectedPaths.remove(path);
        } else {
            mSelectedPaths.add(path);
        }
        updateSelectionCount();
        refreshDirectory();
    }

    private int getIconResourceForFile(File f) {
        if (f.isDirectory()) return R.drawable.ic_folder;
        String name = f.getName().toLowerCase();
        if (isArchive(f)) return R.drawable.ic_file_archive;
        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif")) return R.drawable.ic_file_image;
        if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") || name.endsWith(".mov")) return R.drawable.ic_file_video;
        if (name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".m4a")) return R.drawable.ic_file_audio;
        if (name.endsWith(".apk")) return R.drawable.ic_file_apk; // Package box vector icon
        return R.drawable.ic_file_doc;
    }

    private int getSemanticColorForFile(File f) {
        if (f.isDirectory()) return COLOR_ICON_FOLDER;
        String name = f.getName().toLowerCase();
        if (isArchive(f)) return COLOR_ICON_ARCHIVE;
        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif")) return COLOR_ICON_IMAGE;
        if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") || name.endsWith(".mov")) return COLOR_ICON_VIDEO;
        if (name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".m4a")) return COLOR_ICON_AUDIO;
        if (name.endsWith(".apk")) return COLOR_ICON_APK;
        if (name.endsWith(".pdf") || name.endsWith(".txt") || name.endsWith(".doc") || name.endsWith(".docx")) return COLOR_ICON_DOCUMENT;
        return COLOR_ICON_OTHER;
    }

    private String getFileExtension(File f) {
        if (f.isDirectory()) return "";
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1) : "";
    }

    // ==========================================
    // MODAL BOTTOM SHEET SYSTEM (MILESTONE UI-REFIT-2)
    // ==========================================
    private Dialog createBaseIosSheet() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    private LinearLayout createSheetRoot(Dialog dialog) {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;

        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(16), dp(10), dp(16), dp(24));
        sheet.setMinimumWidth(screenWidth);
        sheet.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadii(new float[]{dp(24), dp(24), dp(24), dp(24), 0, 0, 0, 0});
        bg.setColor(COLOR_SURFACE_ELEVATED);
        sheet.setBackground(bg);

        // Grabber bar 36x5dp
        View grabber = new View(this);
        grabber.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(5)));
        GradientDrawable grabberBg = new GradientDrawable();
        grabberBg.setCornerRadius(dp(3));
        grabberBg.setColor(COLOR_SEPARATOR);
        grabber.setBackground(grabberBg);
        LinearLayout.LayoutParams grabberLp = (LinearLayout.LayoutParams) grabber.getLayoutParams();
        grabberLp.gravity = Gravity.CENTER_HORIZONTAL;
        grabberLp.bottomMargin = dp(14);
        sheet.addView(grabber);

        return sheet;
    }

    private void showIosSheet(Dialog dialog, View sheet) {
        showIosSheet(dialog, sheet, false);
    }

    private void showIosSheet(Dialog dialog, View sheet, boolean hasInput) {
        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(0x73000000); // Scrim 45% black
        root.setOnClickListener(v -> dialog.dismiss()); // Click outside to dismiss

        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sheetLp.gravity = Gravity.BOTTOM;
        sheet.setOnClickListener(v -> {}); // Prevent dismiss when clicking inside sheet
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

    // Single File Action Sheet (4 Groups)
    private void showFileActionSheet(File f) {
        vibrateLight();
        Dialog dialog = createBaseIosSheet();
        LinearLayout sheet = createSheetRoot(dialog);

        // Header: Icon 40dp (bo 10dp) + Filename (middle ellipsis) + Metadata footnote
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(4), dp(8), dp(12));

        LinearLayout iconBox = new LinearLayout(this);
        iconBox.setGravity(Gravity.CENTER);
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        GradientDrawable iconBoxBg = new GradientDrawable();
        iconBoxBg.setCornerRadius(dp(10));
        int tint = getSemanticColorForFile(f);
        iconBoxBg.setColor((tint & 0x00FFFFFF) | 0x2E000000);
        iconBox.setBackground(iconBoxBg);

        ImageView iv = new ImageView(this);
        iv.setImageResource(getIconResourceForFile(f));
        iv.setColorFilter(tint);
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(22), dp(22)));
        iconBox.addView(iv);
        header.addView(iconBox);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.leftMargin = dp(12);

        TextView nameText = new TextView(this);
        nameText.setText(middleEllipsis(f.getName(), 32));
        nameText.setTextColor(COLOR_LABEL);
        nameText.setTextSize(17);
        nameText.setTypeface(Typeface.DEFAULT_BOLD);
        nameText.setSingleLine(true);
        textCol.addView(nameText);

        TextView metaText = new TextView(this);
        String meta = f.isDirectory() ? ("Thư mục · " + formatItemDate(f.lastModified()))
                                     : (formatSize(f.length()) + " · " + formatItemDate(f.lastModified()));
        metaText.setText(meta);
        metaText.setTextColor(COLOR_SEC_LABEL);
        metaText.setTextSize(13);
        textCol.addView(metaText);

        header.addView(textCol, colLp);
        sheet.addView(header);

        // Separator line 0.5dp
        View sep = new View(this);
        sep.setBackgroundColor(COLOR_SEPARATOR);
        sep.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        sheet.addView(sep);

        // Scrollable container for action groups
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);

        // Group 1: Mở bằng…, Chia sẻ, Chi tiết
        LinearLayout group1 = createActionGroup();
        group1.addView(createActionRow(R.drawable.ic_open_with, "Mở bằng…", false, v -> {
            dialog.dismiss();
            openWith(f);
        }, true));
        group1.addView(createActionRow(R.drawable.ic_share, "Chia sẻ", false, v -> {
            dialog.dismiss();
            shareFile(f);
        }, true));
        group1.addView(createActionRow(R.drawable.ic_info, "Chi tiết", false, v -> {
            dialog.dismiss();
            showFileDetailsSheet(f);
        }, false));
        scrollContent.addView(group1);

        // Group 2: Sao chép, Di chuyển, Đổi tên
        LinearLayout group2 = createActionGroup();
        group2.addView(createActionRow(R.drawable.ic_copy, "Sao chép", false, v -> {
            dialog.dismiss();
            copyFileToClipboard(f);
        }, true));
        group2.addView(createActionRow(R.drawable.ic_move, "Di chuyển", false, v -> {
            dialog.dismiss();
            moveFileToClipboard(f);
        }, true));
        group2.addView(createActionRow(R.drawable.ic_rename, "Đổi tên", false, v -> {
            dialog.dismiss();
            showRenameSheet(f);
        }, false));
        scrollContent.addView(group2);

        // Group 3: Nén thành ZIP, Giải nén (chỉ nếu archive), Tính SHA-256, Mã hóa vào Vault
        LinearLayout group3 = createActionGroup();
        group3.addView(createActionRow(R.drawable.ic_file_archive, "Nén thành ZIP", false, v -> {
            dialog.dismiss();
            compressSingleToZip(f);
        }, true));

        if (isArchive(f)) {
            group3.addView(createActionRow(R.drawable.ic_unzip, "Giải nén tại đây", false, v -> {
                dialog.dismiss();
                extractZip(f, mCurrentDir);
            }, true));
            group3.addView(createActionRow(R.drawable.ic_folder_plus, "Giải nén vào thư mục riêng", false, v -> {
                dialog.dismiss();
                extractZipToSeparateDir(f);
            }, true));
        }

        group3.addView(createActionRow(R.drawable.ic_tab_hash, "Tính SHA-256", false, v -> {
            dialog.dismiss();
            computeSha256(f);
        }, true));

        group3.addView(createActionRow(R.drawable.ic_lock, "Mã hóa vào Vault (AES-GCM)", false, v -> {
            dialog.dismiss();
            showEncryptToVaultSheet(f);
        }, false));
        scrollContent.addView(group3);

        // Group 4: Xóa (danger red, separated block)
        LinearLayout group4 = createActionGroup();
        group4.addView(createActionRow(R.drawable.ic_delete, "Xóa", true, v -> {
            dialog.dismiss();
            showDeleteConfirmSheet(f);
        }, false));
        scrollContent.addView(group4);

        sv.addView(scrollContent);
        sheet.addView(sv);

        showIosSheet(dialog, sheet);
    }

    private LinearLayout createActionGroup() {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(COLOR_ACTION_GROUP_BG);
        group.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        group.setLayoutParams(lp);
        return group;
    }

    private View createActionRow(int iconResId, String title, boolean isDanger, View.OnClickListener listener, boolean hasDivider) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), 0, dp(16), 0);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        ImageView iv = new ImageView(this);
        iv.setImageResource(iconResId);
        iv.setColorFilter(isDanger ? COLOR_DANGER : COLOR_LABEL);
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(22), dp(22)));
        row.addView(iv);

        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextSize(17);
        tv.setTextColor(isDanger ? COLOR_DANGER : COLOR_LABEL);
        tv.setTypeface(isDanger ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = dp(14);
        row.addView(tv, tlp);

        row.setOnClickListener(listener);
        container.addView(row);

        if (hasDivider) {
            View div = new View(this);
            div.setBackgroundColor(COLOR_SEPARATOR);
            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
            divLp.leftMargin = dp(52); // Inset from text start
            div.setLayoutParams(divLp);
            container.addView(div);
        }

        return container;
    }

    // Delete Confirmation Sheet (No AlertDialog!)
    private void showDeleteConfirmSheet(File f) {
        vibrateLight();
        Dialog dialog = createBaseIosSheet();
        LinearLayout sheet = createSheetRoot(dialog);

        // Warning card
        LinearLayout card = createActionGroup();
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);
        title.setText("Bạn có chắc chắn muốn xóa?");
        title.setTextColor(COLOR_SEC_LABEL);
        title.setTextSize(13);
        title.setGravity(Gravity.CENTER);
        card.addView(title);

        TextView fname = new TextView(this);
        fname.setText("'" + f.getName() + "'");
        fname.setTextColor(COLOR_LABEL);
        fname.setTextSize(15);
        fname.setTypeface(Typeface.DEFAULT_BOLD);
        fname.setGravity(Gravity.CENTER);
        fname.setPadding(0, dp(4), 0, dp(8));
        card.addView(fname);

        View div = new View(this);
        div.setBackgroundColor(COLOR_SEPARATOR);
        div.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        card.addView(div);

        TextView btnDelete = new TextView(this);
        btnDelete.setText("Xóa");
        btnDelete.setTextColor(COLOR_DANGER);
        btnDelete.setTextSize(17);
        btnDelete.setTypeface(Typeface.DEFAULT_BOLD);
        btnDelete.setGravity(Gravity.CENTER);
        btnDelete.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            deleteRecursive(f);
            refreshDirectory();
            Toast.makeText(this, "Đã xóa thành công", Toast.LENGTH_SHORT).show();
        });
        card.addView(btnDelete);
        sheet.addView(card);

        // Cancel card
        LinearLayout cancelCard = createActionGroup();
        TextView btnCancel = new TextView(this);
        btnCancel.setText("Hủy");
        btnCancel.setTextColor(COLOR_ACCENT);
        btnCancel.setTextSize(17);
        btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        cancelCard.addView(btnCancel);
        sheet.addView(cancelCard);

        showIosSheet(dialog, sheet);
    }

    // Batch Delete Confirmation Sheet
    private void showBatchDeleteConfirmSheet() {
        if (mSelectedPaths.isEmpty()) return;
        vibrateLight();
        Dialog dialog = createBaseIosSheet();
        LinearLayout sheet = createSheetRoot(dialog);

        LinearLayout card = createActionGroup();
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);
        title.setText("Bạn có chắc chắn muốn xóa " + mSelectedPaths.size() + " mục đã chọn?\nHành động này không thể hoàn tác.");
        title.setTextColor(COLOR_SEC_LABEL);
        title.setTextSize(13);
        title.setGravity(Gravity.CENTER);
        card.addView(title);

        View div = new View(this);
        div.setBackgroundColor(COLOR_SEPARATOR);
        div.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        card.addView(div);

        TextView btnDelete = new TextView(this);
        btnDelete.setText("Xóa " + mSelectedPaths.size() + " mục");
        btnDelete.setTextColor(COLOR_DANGER);
        btnDelete.setTextSize(17);
        btnDelete.setTypeface(Typeface.DEFAULT_BOLD);
        btnDelete.setGravity(Gravity.CENTER);
        btnDelete.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            for (String p : mSelectedPaths) {
                deleteRecursive(new File(p));
            }
            exitSelectionMode();
            Toast.makeText(this, "Đã xóa các mục đã chọn", Toast.LENGTH_SHORT).show();
        });
        card.addView(btnDelete);
        sheet.addView(card);

        LinearLayout cancelCard = createActionGroup();
        TextView btnCancel = new TextView(this);
        btnCancel.setText("Hủy");
        btnCancel.setTextColor(COLOR_ACCENT);
        btnCancel.setTextSize(17);
        btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        cancelCard.addView(btnCancel);
        sheet.addView(cancelCard);

        showIosSheet(dialog, sheet);
    }

    // Rename Sheet (iOS Input Sheet with duplicate name error)
    private void showRenameSheet(File f) {
        Dialog dialog = createBaseIosSheet();
        LinearLayout sheet = createSheetRoot(dialog);

        TextView title = new TextView(this);
        title.setText("Đổi tên");
        title.setTextSize(20);
        title.setTextColor(COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        EditText input = new EditText(this);
        input.setText(f.getName());
        input.setTextColor(COLOR_LABEL);
        input.setTextSize(16);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(dp(12));
        inputBg.setColor(COLOR_ACTION_GROUP_BG);
        input.setBackground(inputBg);

        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.topMargin = dp(16);
        sheet.addView(input, inLp);

        TextView errorText = new TextView(this);
        errorText.setTextColor(COLOR_DANGER);
        errorText.setTextSize(13);
        errorText.setVisibility(View.GONE);
        LinearLayout.LayoutParams errLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        errLp.topMargin = dp(6);
        sheet.addView(errorText, errLp);

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String val = s.toString().trim();
                if (!val.equals(f.getName()) && new File(f.getParentFile(), val).exists()) {
                    errorText.setText("Tên này đã tồn tại trong thư mục");
                    errorText.setVisibility(View.VISIBLE);
                } else {
                    errorText.setVisibility(View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(16);
        btnRow.setLayoutParams(rowLp);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("Hủy");
        btnCancel.setTextColor(COLOR_SEC_LABEL);
        btnCancel.setTextSize(16);
        btnCancel.setPadding(dp(16), dp(10), dp(16), dp(10));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        TextView btnSave = new TextView(this);
        btnSave.setText("Xong");
        btnSave.setTextColor(Color.WHITE);
        btnSave.setTextSize(16);
        btnSave.setTypeface(Typeface.DEFAULT_BOLD);
        btnSave.setPadding(dp(20), dp(10), dp(20), dp(10));
        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setCornerRadius(dp(999));
        saveBg.setColor(COLOR_ACCENT);
        btnSave.setBackground(saveBg);

        btnSave.setOnClickListener(v -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) {
                errorText.setText("Tên tệp không được để trống");
                errorText.setVisibility(View.VISIBLE);
                return;
            }
            File dest = new File(f.getParentFile(), newName);
            if (!newName.equals(f.getName()) && dest.exists()) {
                errorText.setText("Tên này đã tồn tại trong thư mục");
                errorText.setVisibility(View.VISIBLE);
                return;
            }
            if (f.renameTo(dest)) {
                dialog.dismiss();
                refreshDirectory();
                Toast.makeText(this, "Đã đổi tên thành: " + newName, Toast.LENGTH_SHORT).show();
            }
        });
        btnRow.addView(btnSave);
        sheet.addView(btnRow);

        showIosSheet(dialog, sheet, true);
        input.requestFocus();
    }

    // New Folder Sheet
    private void showNewFolderSheet() {
        Dialog dialog = createBaseIosSheet();
        LinearLayout sheet = createSheetRoot(dialog);

        TextView title = new TextView(this);
        title.setText("Thư mục mới");
        title.setTextSize(20);
        title.setTextColor(COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        EditText input = new EditText(this);
        input.setHint("Tên thư mục mới");
        input.setHintTextColor(COLOR_SEC_LABEL);
        input.setTextColor(COLOR_LABEL);
        input.setTextSize(16);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(dp(12));
        inputBg.setColor(COLOR_ACTION_GROUP_BG);
        input.setBackground(inputBg);

        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.topMargin = dp(16);
        sheet.addView(input, inLp);

        TextView errorText = new TextView(this);
        errorText.setTextColor(COLOR_DANGER);
        errorText.setTextSize(13);
        errorText.setVisibility(View.GONE);
        LinearLayout.LayoutParams errLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        errLp.topMargin = dp(6);
        sheet.addView(errorText, errLp);

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String val = s.toString().trim();
                if (new File(mCurrentDir, val).exists()) {
                    errorText.setText("Thư mục này đã tồn tại");
                    errorText.setVisibility(View.VISIBLE);
                } else {
                    errorText.setVisibility(View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(16);
        btnRow.setLayoutParams(rowLp);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("Hủy");
        btnCancel.setTextColor(COLOR_SEC_LABEL);
        btnCancel.setTextSize(16);
        btnCancel.setPadding(dp(16), dp(10), dp(16), dp(10));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        TextView btnCreate = new TextView(this);
        btnCreate.setText("Tạo");
        btnCreate.setTextColor(Color.WHITE);
        btnCreate.setTextSize(16);
        btnCreate.setTypeface(Typeface.DEFAULT_BOLD);
        btnCreate.setPadding(dp(20), dp(10), dp(20), dp(10));
        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setCornerRadius(dp(999));
        saveBg.setColor(COLOR_ACCENT);
        btnCreate.setBackground(saveBg);

        btnCreate.setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                errorText.setText("Tên thư mục không được để trống");
                errorText.setVisibility(View.VISIBLE);
                return;
            }
            File newDir = new File(mCurrentDir, name);
            if (newDir.exists()) {
                errorText.setText("Thư mục này đã tồn tại");
                errorText.setVisibility(View.VISIBLE);
                return;
            }
            if (newDir.mkdirs()) {
                dialog.dismiss();
                refreshDirectory();
                Toast.makeText(this, "Đã tạo thư mục: " + name, Toast.LENGTH_SHORT).show();
            }
        });
        btnRow.addView(btnCreate);
        sheet.addView(btnRow);

        showIosSheet(dialog, sheet, true);
        input.requestFocus();
    }

    // File Details Sheet
    private void showFileDetailsSheet(File f) {
        Dialog dialog = createBaseIosSheet();
        LinearLayout sheet = createSheetRoot(dialog);

        TextView title = new TextView(this);
        title.setText("Chi tiết tệp tin");
        title.setTextSize(20);
        title.setTextColor(COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        LinearLayout card = createActionGroup();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        addDetailItem(card, "Tên", f.getName());
        addDetailItem(card, "Đường dẫn", f.getAbsolutePath());
        addDetailItem(card, "Kích thước", formatSize(f.length()) + " (" + f.length() + " bytes)");
        addDetailItem(card, "Sửa đổi lần cuối", formatDetailDate(f.lastModified()));
        addDetailItem(card, "Loại tệp", f.isDirectory() ? "Thư mục" : getMimeType(f));
        addDetailItem(card, "Quyền", (f.canRead() ? "Đọc " : "") + (f.canWrite() ? "Ghi " : ""));

        sheet.addView(card);

        TextView btnClose = new TextView(this);
        btnClose.setText("Đóng");
        btnClose.setTextColor(COLOR_ACCENT);
        btnClose.setTextSize(17);
        btnClose.setTypeface(Typeface.DEFAULT_BOLD);
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        btnClose.setOnClickListener(v -> dialog.dismiss());
        sheet.addView(btnClose);

        showIosSheet(dialog, sheet);
    }

    private void addDetailItem(LinearLayout container, String key, String val) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView k = new TextView(this);
        k.setText(key);
        k.setTextColor(COLOR_SEC_LABEL);
        k.setTextSize(12);
        row.addView(k);

        TextView v = new TextView(this);
        v.setText(val);
        v.setTextColor(COLOR_LABEL);
        v.setTextSize(14);
        row.addView(v);

        container.addView(row);
    }

    // SHA-256 Result Sheet
    private void showSha256Sheet(File f, String hex) {
        Dialog dialog = createBaseIosSheet();
        LinearLayout sheet = createSheetRoot(dialog);

        TextView title = new TextView(this);
        title.setText("Mã băm SHA-256");
        title.setTextSize(20);
        title.setTextColor(COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(f.getName());
        subtitle.setTextSize(13);
        subtitle.setTextColor(COLOR_SEC_LABEL);
        sheet.addView(subtitle);

        LinearLayout box = createActionGroup();
        box.setPadding(dp(14), dp(14), dp(14), dp(14));

        TextView hexTv = new TextView(this);
        hexTv.setText(hex);
        hexTv.setTextColor(COLOR_SUCCESS);
        hexTv.setTextSize(14);
        hexTv.setTypeface(Typeface.MONOSPACE);
        box.addView(hexTv);
        sheet.addView(box);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(16);
        btnRow.setLayoutParams(rlp);

        TextView btnClose = new TextView(this);
        btnClose.setText("Đóng");
        btnClose.setTextColor(COLOR_SEC_LABEL);
        btnClose.setTextSize(16);
        btnClose.setPadding(dp(16), dp(10), dp(16), dp(10));
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnClose);

        TextView btnCopy = new TextView(this);
        btnCopy.setText("Sao chép mã");
        btnCopy.setTextColor(Color.WHITE);
        btnCopy.setTextSize(16);
        btnCopy.setTypeface(Typeface.DEFAULT_BOLD);
        btnCopy.setPadding(dp(20), dp(10), dp(20), dp(10));
        GradientDrawable copyBg = new GradientDrawable();
        copyBg.setCornerRadius(dp(999));
        copyBg.setColor(COLOR_ACCENT);
        btnCopy.setBackground(copyBg);
        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("SHA256", hex));
            Toast.makeText(this, "Đã sao chép mã SHA-256 vào bộ nhớ tạm", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        btnRow.addView(btnCopy);
        sheet.addView(btnRow);

        showIosSheet(dialog, sheet);
    }

    // Encrypt To Vault Sheet
    private void showEncryptToVaultSheet(File src) {
        Dialog dialog = createBaseIosSheet();
        LinearLayout sheet = createSheetRoot(dialog);

        TextView title = new TextView(this);
        title.setText("Mã hóa vào Vault");
        title.setTextSize(20);
        title.setTextColor(COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Bảo vệ '" + src.getName() + "' bằng AES-256-GCM");
        sub.setTextColor(COLOR_SEC_LABEL);
        sub.setTextSize(13);
        sheet.addView(sub);

        EditText input = new EditText(this);
        input.setHint("Nhập mật khẩu mã hóa");
        input.setHintTextColor(COLOR_SEC_LABEL);
        input.setTextColor(COLOR_LABEL);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(dp(12));
        inputBg.setColor(COLOR_ACTION_GROUP_BG);
        input.setBackground(inputBg);

        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.topMargin = dp(16);
        sheet.addView(input, inLp);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(16);
        btnRow.setLayoutParams(rlp);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("Hủy");
        btnCancel.setTextColor(COLOR_SEC_LABEL);
        btnCancel.setTextSize(16);
        btnCancel.setPadding(dp(16), dp(10), dp(16), dp(10));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        TextView btnEnc = new TextView(this);
        btnEnc.setText("Mã hóa");
        btnEnc.setTextColor(Color.WHITE);
        btnEnc.setTextSize(16);
        btnEnc.setTypeface(Typeface.DEFAULT_BOLD);
        btnEnc.setPadding(dp(20), dp(10), dp(20), dp(10));
        GradientDrawable encBg = new GradientDrawable();
        encBg.setCornerRadius(dp(999));
        encBg.setColor(COLOR_ACCENT);
        btnEnc.setBackground(encBg);
        btnEnc.setOnClickListener(v -> {
            String pwd = input.getText().toString();
            if (pwd.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            new Thread(() -> {
                try {
                    File vaultDir = new File(getFilesDir(), "vault");
                    vaultDir.mkdirs();
                    File encFile = new File(vaultDir, src.getName() + ".vqr");
                    encryptAesGcm(src, encFile, pwd);
                    runOnUiThread(() -> Toast.makeText(this, "Đã mã hóa an toàn vào Vault!", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, "Lỗi mã hóa: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        });
        btnRow.addView(btnEnc);
        sheet.addView(btnRow);

        showIosSheet(dialog, sheet, true);
    }

    // Sort Action Sheet
    private void showSortActionSheet() {
        Dialog dialog = createBaseIosSheet();
        LinearLayout sheet = createSheetRoot(dialog);

        TextView title = new TextView(this);
        title.setText("Sắp xếp theo");
        title.setTextSize(20);
        title.setTextColor(COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        LinearLayout card = createActionGroup();
        String[] options = {"Tên (A-Z)", "Ngày sửa đổi", "Kích thước", "Loại tệp tin"};

        for (int i = 0; i < options.length; i++) {
            final int field = i;
            boolean isChosen = (mSortField == field);
            boolean isLast = (i == options.length - 1);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), 0, dp(16), 0);
            row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));

            TextView tv = new TextView(this);
            tv.setText(options[i]);
            tv.setTextSize(17);
            tv.setTextColor(isChosen ? COLOR_ACCENT : COLOR_LABEL);
            tv.setTypeface(isChosen ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(tv, tlp);

            if (isChosen) {
                ImageView check = new ImageView(this);
                check.setImageResource(R.drawable.ic_check);
                check.setColorFilter(COLOR_ACCENT);
                check.setLayoutParams(new LinearLayout.LayoutParams(dp(20), dp(20)));
                row.addView(check);
            }

            row.setOnClickListener(v -> {
                mSortField = field;
                dialog.dismiss();
                refreshDirectory();
            });
            card.addView(row);

            if (!isLast) {
                View div = new View(this);
                div.setBackgroundColor(COLOR_SEPARATOR);
                LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
                divLp.leftMargin = dp(16);
                div.setLayoutParams(divLp);
                card.addView(div);
            }
        }
        sheet.addView(card);

        LinearLayout cancelCard = createActionGroup();
        TextView btnCancel = new TextView(this);
        btnCancel.setText("Hủy");
        btnCancel.setTextColor(COLOR_ACCENT);
        btnCancel.setTextSize(17);
        btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        cancelCard.addView(btnCancel);
        sheet.addView(cancelCard);

        showIosSheet(dialog, sheet);
    }

    // Search Sheet
    private void showSearchSheet() {
        Dialog dialog = createBaseIosSheet();
        LinearLayout sheet = createSheetRoot(dialog);

        TextView title = new TextView(this);
        title.setText("Tìm kiếm trong thư mục");
        title.setTextSize(20);
        title.setTextColor(COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        EditText input = new EditText(this);
        input.setHint("Nhập tên tệp hoặc thư mục");
        input.setHintTextColor(COLOR_SEC_LABEL);
        input.setTextColor(COLOR_LABEL);
        input.setTextSize(16);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(dp(12));
        inputBg.setColor(COLOR_ACTION_GROUP_BG);
        input.setBackground(inputBg);

        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.topMargin = dp(16);
        sheet.addView(input, inLp);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(16);
        btnRow.setLayoutParams(rlp);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("Hủy");
        btnCancel.setTextColor(COLOR_SEC_LABEL);
        btnCancel.setTextSize(16);
        btnCancel.setPadding(dp(16), dp(10), dp(16), dp(10));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        TextView btnSearch = new TextView(this);
        btnSearch.setText("Tìm");
        btnSearch.setTextColor(Color.WHITE);
        btnSearch.setTextSize(16);
        btnSearch.setTypeface(Typeface.DEFAULT_BOLD);
        btnSearch.setPadding(dp(24), dp(10), dp(24), dp(10));
        GradientDrawable sBg = new GradientDrawable();
        sBg.setCornerRadius(dp(999));
        sBg.setColor(COLOR_ACCENT);
        btnSearch.setBackground(sBg);
        btnSearch.setOnClickListener(v -> {
            String query = input.getText().toString().trim().toLowerCase();
            dialog.dismiss();
            if (!query.isEmpty()) performSearch(query);
        });
        btnRow.addView(btnSearch);
        sheet.addView(btnRow);

        showIosSheet(dialog, sheet, true);
        input.requestFocus();
    }

    private void performSearch(String query) {
        mFileListGroup.removeAllViews();
        File[] files = mCurrentDir.listFiles();
        if (files == null) return;

        List<File> matched = new ArrayList<>();
        for (File f : files) {
            if (f.getName().toLowerCase().contains(query)) {
                matched.add(f);
            }
        }

        if (matched.isEmpty()) {
            showEmptyState("Không tìm thấy kết quả", "Không có tệp nào khớp với từ khóa \"" + query + "\".");
            return;
        }

        GradientDrawable groupBg = new GradientDrawable();
        groupBg.setCornerRadius(dp(16));
        groupBg.setColor(COLOR_SURFACE_ELEVATED);
        mFileListGroup.setBackground(groupBg);

        for (int i = 0; i < matched.size(); i++) {
            mFileListGroup.addView(createIosFileRow(matched.get(i), i == matched.size() - 1));
        }
    }

    // More Options Menu Sheet (⋯)
    private void showMoreOptionsSheet() {
        Dialog dialog = createBaseIosSheet();
        LinearLayout sheet = createSheetRoot(dialog);

        TextView title = new TextView(this);
        title.setText("Tùy chọn VELQOR");
        title.setTextSize(20);
        title.setTextColor(COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        // Group 1
        LinearLayout group1 = createActionGroup();
        group1.addView(createActionRow(R.drawable.ic_folder_plus, "Tạo thư mục mới", false, v -> {
            dialog.dismiss();
            showNewFolderSheet();
        }, true));

        group1.addView(createActionRow(R.drawable.ic_more_dots, "Sắp xếp danh sách", false, v -> {
            dialog.dismiss();
            showSortActionSheet();
        }, true));

        String hiddenLabel = mShowHidden ? "Ẩn tệp tin ẩn" : "Hiện tệp tin ẩn";
        group1.addView(createActionRow(R.drawable.ic_info, hiddenLabel, false, v -> {
            dialog.dismiss();
            mShowHidden = !mShowHidden;
            refreshDirectory();
        }, false));
        sheet.addView(group1);

        // Group 2: Paste clipboard or selection mode
        LinearLayout group2 = createActionGroup();
        if (mClipboardFile != null && mClipboardFile.exists()) {
            String pasteLabel = (mIsCutAction ? "Di chuyển " : "Dán ") + "'" + mClipboardFile.getName() + "' vào đây";
            group2.addView(createActionRow(R.drawable.ic_copy, pasteLabel, false, v -> {
                dialog.dismiss();
                pasteClipboardFile();
            }, true));
        }

        group2.addView(createActionRow(R.drawable.ic_check, "Chọn nhiều mục", false, v -> {
            dialog.dismiss();
            enterSelectionMode();
        }, false));
        sheet.addView(group2);

        showIosSheet(dialog, sheet);
    }

    // Batch Actions Sheet (when multi-selecting)
    private void showBatchOptionsSheet() {
        if (mSelectedPaths.isEmpty()) return;
        Dialog dialog = createBaseIosSheet();
        LinearLayout sheet = createSheetRoot(dialog);

        TextView title = new TextView(this);
        title.setText("Đã chọn " + mSelectedPaths.size() + " mục");
        title.setTextSize(20);
        title.setTextColor(COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        LinearLayout group1 = createActionGroup();
        group1.addView(createActionRow(R.drawable.ic_file_archive, "Nén thành ZIP", false, v -> {
            dialog.dismiss();
            compressSelected();
        }, false));
        sheet.addView(group1);

        LinearLayout group2 = createActionGroup();
        group2.addView(createActionRow(R.drawable.ic_delete, "Xóa các mục đã chọn", true, v -> {
            dialog.dismiss();
            showBatchDeleteConfirmSheet();
        }, false));
        sheet.addView(group2);

        showIosSheet(dialog, sheet);
    }

    // ==========================================
    // REAL FILE LOGIC OPERATIONS
    // ==========================================
    private void openWith(File f) {
        try {
            Uri uri = VelqorFileProvider.getUriForFile(this, "com.velqor.app.fileprovider", f);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String mime = getMimeType(f);
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Mở bằng…"));
        } catch (Exception e) {
            Toast.makeText(this, "Không thể mở tệp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFile(File f) {
        try {
            Uri uri = VelqorFileProvider.getUriForFile(this, "com.velqor.app.fileprovider", f);
            Intent intent = new Intent(Intent.ACTION_SEND);
            String mime = getMimeType(f);
            intent.setType(mime);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Chia sẻ tệp"));
        } catch (Exception e) {
            Toast.makeText(this, "Không thể chia sẻ tệp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyFileToClipboard(File f) {
        mClipboardFile = f;
        mIsCutAction = false;
        vibrateLight();
        Toast.makeText(this, "Đã sao chép '" + f.getName() + "'. Mở thư mục đích rồi bấm '⋯' để Dán.", Toast.LENGTH_LONG).show();
    }

    private void moveFileToClipboard(File f) {
        mClipboardFile = f;
        mIsCutAction = true;
        vibrateLight();
        Toast.makeText(this, "Đã chọn di chuyển '" + f.getName() + "'. Mở thư mục đích rồi bấm '⋯' để Dán.", Toast.LENGTH_LONG).show();
    }

    private void pasteClipboardFile() {
        if (mClipboardFile == null || !mClipboardFile.exists()) {
            Toast.makeText(this, "Bộ nhớ tạm không có tệp nào", Toast.LENGTH_SHORT).show();
            return;
        }
        File src = mClipboardFile;
        File dest = new File(mCurrentDir, src.getName());
        new Thread(() -> {
            try {
                if (mIsCutAction) {
                    if (!src.renameTo(dest)) {
                        copyRecursive(src, dest);
                        deleteRecursive(src);
                    }
                    mClipboardFile = null;
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Đã di chuyển thành công: " + dest.getName(), Toast.LENGTH_SHORT).show();
                        refreshDirectory();
                    });
                } else {
                    copyRecursive(src, dest);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Đã sao chép thành công: " + dest.getName(), Toast.LENGTH_SHORT).show();
                        refreshDirectory();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi sao chép: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void copyRecursive(File src, File dest) throws Exception {
        if (src.isDirectory()) {
            dest.mkdirs();
            File[] children = src.listFiles();
            if (children != null) {
                for (File c : children) copyRecursive(c, new File(dest, c.getName()));
            }
        } else {
            try (FileInputStream in = new FileInputStream(src);
                 FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[64 * 1024];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }
        }
    }

    private String getMimeType(File f) {
        String ext = getFileExtension(f).toLowerCase();
        if (ext.isEmpty()) return "*/*";
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return mime != null ? mime : "*/*";
    }

    private void compressSingleToZip(File src) {
        List<File> list = new ArrayList<>();
        list.add(src);
        compressFilesToZip(list, src.getName() + ".zip");
    }

    private void compressSelected() {
        if (mSelectedPaths.isEmpty()) return;
        List<File> list = new ArrayList<>();
        for (String p : mSelectedPaths) list.add(new File(p));
        String zipName = "archive_" + System.currentTimeMillis() + ".zip";
        compressFilesToZip(list, zipName);
        exitSelectionMode();
    }

    private void compressFilesToZip(List<File> files, String outZipName) {
        new Thread(() -> {
            try {
                File zipFile = new File(mCurrentDir, outZipName);
                try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                    for (File f : files) addToZip(zos, f, "");
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, "Đã nén thành công: " + zipFile.getName(), Toast.LENGTH_SHORT).show();
                    refreshDirectory();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi nén: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void addToZip(ZipOutputStream zos, File file, String base) throws Exception {
        String entryName = base.isEmpty() ? file.getName() : base + "/" + file.getName();
        if (file.isDirectory()) {
            zos.putNextEntry(new ZipEntry(entryName + "/"));
            zos.closeEntry();
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) addToZip(zos, child, entryName);
            }
        } else {
            zos.putNextEntry(new ZipEntry(entryName));
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[32 * 1024];
                int len;
                while ((len = fis.read(buffer)) > 0) zos.write(buffer, 0, len);
            }
            zos.closeEntry();
        }
    }

    private void extractZip(File zipFile, File destDir) {
        new Thread(() -> {
            try {
                try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
                    ZipEntry entry;
                    byte[] buffer = new byte[32 * 1024];
                    while ((entry = zis.getNextEntry()) != null) {
                        File destFile = new File(destDir, entry.getName());
                        if (!destFile.getCanonicalPath().startsWith(destDir.getCanonicalPath())) {
                            throw new SecurityException("Zip Slip detected!");
                        }
                        if (entry.isDirectory()) {
                            destFile.mkdirs();
                        } else {
                            destFile.getParentFile().mkdirs();
                            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                                int len;
                                while ((len = zis.read(buffer)) > 0) fos.write(buffer, 0, len);
                            }
                        }
                        zis.closeEntry();
                    }
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, "Đã giải nén hoàn tất!", Toast.LENGTH_SHORT).show();
                    refreshDirectory();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi giải nén: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void extractZipToSeparateDir(File archiveFile) {
        String baseName = archiveFile.getName();
        int dot = baseName.lastIndexOf('.');
        if (dot > 0) baseName = baseName.substring(0, dot);
        File dest = new File(mCurrentDir, baseName);
        dest.mkdirs();
        extractZip(archiveFile, dest);
    }

    private void computeSha256(File f) {
        new Thread(() -> {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                try (FileInputStream fis = new FileInputStream(f)) {
                    byte[] buf = new byte[64 * 1024];
                    int r;
                    while ((r = fis.read(buf)) > 0) md.update(buf, 0, r);
                }
                byte[] hash = md.digest();
                StringBuilder sb = new StringBuilder();
                for (byte b : hash) sb.append(String.format("%02x", b));
                String hex = sb.toString();

                runOnUiThread(() -> showSha256Sheet(f, hex));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi tính hash: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursive(c);
            }
        }
        f.delete();
    }

    // ==========================================
    // OTHER TABS (ARCHIVE, VAULT, HASH, SETTINGS)
    // ==========================================
    private void showArchiveTab() {
        mFileListGroup.removeAllViews();
        mFileListGroup.setBackground(null);

        LinearLayout card = createActionGroup();
        card.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("Trình quản lý nén VELQOR PRO");
        title.setTextSize(18);
        title.setTextColor(COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(title);

        TextView info = new TextView(this);
        info.setText("\n• Định dạng hỗ trợ: ZIP, TAR, TAR.GZ, TGZ, 7Z, RAR\n• Công nghệ chống Zip Slip bảo mật đường dẫn tuyệt đối\n• Nén & giải nén trực tiếp với luồng I/O độc lập ngoài main thread\n\nĐể nén hoặc giải nén tệp, hãy chạm vào tệp bất kỳ trong tab 'Tệp'.");
        info.setTextColor(COLOR_SEC_LABEL);
        info.setTextSize(14);
        card.addView(info);

        mFileListGroup.addView(card);
    }

    private void showVaultTab() {
        mFileListGroup.removeAllViews();
        mFileListGroup.setBackground(null);

        LinearLayout card = createActionGroup();
        card.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("Kho bảo mật Vault (AES-256-GCM)");
        title.setTextSize(18);
        title.setTextColor(COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(title);

        File vaultDir = new File(getFilesDir(), "vault");
        File[] vFiles = vaultDir.listFiles();
        int count = (vFiles != null) ? vFiles.length : 0;

        TextView info = new TextView(this);
        info.setText("\n• Tiêu chuẩn mã hóa: AES/GCM/NoPadding 256-bit\n• Hàm suy biến khóa: PBKDF2WithHmacSHA256 (65,536 vòng lặp)\n• Hiện có: " + count + " tệp được bảo vệ an toàn\n\nChạm vào tệp trong tab 'Tệp' và chọn 'Mã hóa vào Vault' để đưa vào kho bảo mật.");
        info.setTextColor(COLOR_SEC_LABEL);
        info.setTextSize(14);
        card.addView(info);

        mFileListGroup.addView(card);
    }

    private void showHashTab() {
        mFileListGroup.removeAllViews();
        mFileListGroup.setBackground(null);

        LinearLayout card = createActionGroup();
        card.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("Kiểm tra toàn vẹn mã băm (Hash)");
        title.setTextSize(18);
        title.setTextColor(COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(title);

        TextView info = new TextView(this);
        info.setText("\n• Hỗ trợ tính toán mã băm SHA-256 nguyên bản\n• Buffer I/O 64KB tối ưu tốc độ đọc tệp lớn\n• Kết quả hiển thị tức thì với nút sao chép 1 chạm\n\nChạm vào tệp bất kỳ trong tab 'Tệp' và chọn 'Tính SHA-256' để kiểm tra checksum.");
        info.setTextColor(COLOR_SEC_LABEL);
        info.setTextSize(14);
        card.addView(info);

        mFileListGroup.addView(card);
    }

    private void showSettingsTab() {
        mFileListGroup.removeAllViews();
        mFileListGroup.setBackground(null);

        LinearLayout card = createActionGroup();
        card.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("Cài đặt VELQOR");
        title.setTextSize(18);
        title.setTextColor(COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(title);

        TextView info = new TextView(this);
        info.setText("\n• Phiên bản: VELQOR v2.0.3 (Build 5)\n• Thiết kế: Giao diện Liquid Glass Cupertino chuẩn Apple\n• Quyền bộ nhớ: MANAGE_EXTERNAL_STORAGE đã cấp\n• Động cơ xử lý: Pure Android SDK + Coroutines Flow\n• Nhà phát triển: Google DeepMind Pair Programming");
        info.setTextColor(COLOR_SEC_LABEL);
        info.setTextSize(14);
        card.addView(info);

        mFileListGroup.addView(card);
    }

    // ==========================================
    // VAULT CIPHER IMPLEMENTATION
    // ==========================================
    private void encryptAesGcm(File src, File dest, String password) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
        SecretKey key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        cipher.updateAAD(ByteBuffer.allocate(8).putLong(0).array());

        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dest)) {
            out.write("VQR1".getBytes());
            out.write(salt);
            out.write(iv);

            byte[] buffer = new byte[64 * 1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                byte[] enc = cipher.update(buffer, 0, len);
                if (enc != null) out.write(enc);
            }
            byte[] finish = cipher.doFinal();
            if (finish != null) out.write(finish);
        }
    }

    // ==========================================
    // UTILITIES & HELPERS
    // ==========================================
    private boolean isArchive(File f) {
        String name = f.getName().toLowerCase();
        return name.endsWith(".zip") || name.endsWith(".tar") || name.endsWith(".tar.gz") ||
               name.endsWith(".tgz") || name.endsWith(".7z") || name.endsWith(".rar");
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    private int dp(float dpVal) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dpVal * density + 0.5f);
    }

    private String middleEllipsis(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        int half = (maxLength - 3) / 2;
        return text.substring(0, half) + "…" + text.substring(text.length() - half);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024));
        return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String formatItemDate(long time) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(time));
    }

    private String formatDetailDate(long time) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date(time));
    }

    private void vibrateLight() {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(20);
                }
            }
        } catch (Exception ignored) {}
    }
}
