package com.velqor.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.velqor.app.archive.ZipManager;
import com.velqor.app.files.FileRepository;
import com.velqor.app.files.TextFileIO;
import com.velqor.app.security.HashUtils;
import com.velqor.app.security.VaultManager;
import com.velqor.app.tokens.ThemeTokens;
import com.velqor.app.ui.FileRowView;
import com.velqor.app.ui.FloatingTabBar;
import com.velqor.app.ui.HeaderView;
import com.velqor.app.ui.ImageViewerActivity;
import com.velqor.app.ui.IosSheet;
import com.velqor.app.ui.NewItemSheet;
import com.velqor.app.ui.TextEditorActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private File mCurrentDir;
    private HeaderView mHeaderView;
    private FloatingTabBar mFloatingTabBar;
    private LinearLayout mFileListGroup;
    private ScrollView mScrollView;
    private LinearLayout mSelectionBar;
    private TextView mSelectionCountText;

    private boolean mShowHidden = false;
    private int mSortField = 0; // 0: Name, 1: Date, 2: Size, 3: Type
    private boolean mSortAsc = true;

    private boolean mIsSelectionMode = false;
    private final Set<String> mSelectedPaths = new HashSet<>();

    private File mClipboardFile = null;
    private boolean mIsCutAction = false;
    private String mCurrentTab = "files";
    private boolean mIsSearchActive = false;
    private long mLastBackPressTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. True Edge-to-Edge Fullscreen
        setupEdgeToEdge();

        mCurrentDir = Environment.getExternalStorageDirectory();
        mShowHidden = getSharedPreferences("velqor_prefs", MODE_PRIVATE).getBoolean("show_hidden", false);
        checkAndRequestPermissions();

        // Predictive back gesture support for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                () -> {
                    if (!handleBackNavigation()) {
                        finish();
                    }
                }
            );
        }

        // 2. Build iOS-Grade Layered View Hierarchy
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(ThemeTokens.COLOR_BG);

        // Main Scrollable Area with Inset Grouped List
        mScrollView = new ScrollView(this);
        mScrollView.setFillViewport(true);
        mScrollView.setVerticalScrollBarEnabled(false);

        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);

        int topPadding = getStatusBarHeight() + ThemeTokens.dp(this, 100);
        int bottomPadding = getNavBarHeight() + ThemeTokens.dp(this, 112);
        scrollContent.setPadding(ThemeTokens.dp(this, 16), topPadding, ThemeTokens.dp(this, 16), bottomPadding);

        mFileListGroup = new LinearLayout(this);
        mFileListGroup.setOrientation(LinearLayout.VERTICAL);
        scrollContent.addView(mFileListGroup);

        mScrollView.addView(scrollContent);
        root.addView(mScrollView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Header Layer: Sticky iOS Frosted Header & Breadcrumbs
        mHeaderView = new HeaderView(this, getStatusBarHeight());
        mHeaderView.setOnHeaderActionListener(new HeaderView.OnHeaderActionListener() {
            @Override public void onSearchClick() { showSearchSheet(); }
            @Override public void onNewFolderClick() {
                NewItemSheet.showCreateMenu(MainActivity.this, mCurrentDir, new NewItemSheet.OnItemCreateListener() {
                    @Override
                    public void onCreateFolderRequested() {
                        showNewFolderSheet();
                    }

                    @Override
                    public void onFileCreated(File newFile) {
                        refreshDirectory();
                        vibrateLight();
                        openTextEditor(newFile);
                    }
                });
            }
            @Override public void onMoreClick() { showMoreOptionsSheet(); }
            @Override public void onBreadcrumbClick(File directory) {
                if (!directory.equals(mCurrentDir)) {
                    mCurrentDir = directory;
                    refreshDirectory();
                }
            }
        });
        FrameLayout.LayoutParams headerLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerLp.gravity = Gravity.TOP;
        root.addView(mHeaderView, headerLp);

        // Multi-Selection Floating Action Bar
        mSelectionBar = createSelectionBar();
        FrameLayout.LayoutParams selLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        selLp.gravity = Gravity.TOP;
        selLp.topMargin = getStatusBarHeight() + ThemeTokens.dp(this, 88);
        root.addView(mSelectionBar, selLp);

        // Floating Liquid Glass Tab Bar (Hardware Blur API 31+ / Thick Glass Fallback)
        mFloatingTabBar = new FloatingTabBar(this);
        mFloatingTabBar.setBackdropSource(mScrollView);
        mFloatingTabBar.setOnTabSelectedListener((tabKey, index) -> {
            mCurrentTab = tabKey;
            vibrateLight();
            switchTab(tabKey);
        });

        int barHeight = ThemeTokens.dp(this, ThemeTokens.HEIGHT_TAB_BAR);
        FrameLayout.LayoutParams tabLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, barHeight);
        tabLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        tabLp.leftMargin = ThemeTokens.dp(this, 20);
        tabLp.rightMargin = ThemeTokens.dp(this, 20);
        tabLp.bottomMargin = getNavBarHeight() + ThemeTokens.dp(this, 12);
        root.addView(mFloatingTabBar, tabLp);

        // Scroll Behavior: Header shrink & Realtime Backdrop Invalidation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                mHeaderView.setScrolledState(scrollY > ThemeTokens.dp(this, 30));
                mFloatingTabBar.invalidateBackdrop();
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

            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                      | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                      | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    private int getStatusBarHeight() {
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resId > 0 ? getResources().getDimensionPixelSize(resId) : ThemeTokens.dp(this, 24);
    }

    private int getNavBarHeight() {
        int resId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return resId > 0 ? getResources().getDimensionPixelSize(resId) : ThemeTokens.dp(this, 16);
    }

    private LinearLayout createSelectionBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 10),
                ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 10));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(ThemeTokens.dp(this, 14));
        bg.setColor(ThemeTokens.COLOR_SURFACE_ELEVATED);
        bar.setBackground(bg);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setVisibility(View.GONE);

        mSelectionCountText = new TextView(this);
        mSelectionCountText.setText("Đã chọn 0 mục");
        mSelectionCountText.setTextColor(ThemeTokens.COLOR_LABEL);
        mSelectionCountText.setTextSize(15);
        mSelectionCountText.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bar.addView(mSelectionCountText, countLp);

        // Action: Batch Options Sheet
        ImageView btnMore = createBarIcon(R.drawable.ic_more_dots, ThemeTokens.COLOR_ACCENT, v -> showBatchOptionsSheet());
        bar.addView(btnMore);

        // Action: Compress
        ImageView btnZip = createBarIcon(R.drawable.ic_file_archive, ThemeTokens.COLOR_ACCENT, v -> compressSelected());
        bar.addView(btnZip);

        // Action: Delete
        ImageView btnDel = createBarIcon(R.drawable.ic_delete, ThemeTokens.COLOR_DANGER, v -> showBatchDeleteConfirmSheet());
        bar.addView(btnDel);

        // Action: Done
        TextView btnDone = new TextView(this);
        btnDone.setText("Xong");
        btnDone.setTextColor(ThemeTokens.COLOR_ACCENT);
        btnDone.setTextSize(16);
        btnDone.setTypeface(Typeface.DEFAULT_BOLD);
        btnDone.setPadding(ThemeTokens.dp(this, 12), ThemeTokens.dp(this, 4), ThemeTokens.dp(this, 4), ThemeTokens.dp(this, 4));
        btnDone.setOnClickListener(v -> exitSelectionMode());
        bar.addView(btnDone);

        return bar;
    }

    private ImageView createBarIcon(int resId, int color, View.OnClickListener listener) {
        ImageView iv = new ImageView(this);
        iv.setImageResource(resId);
        iv.setColorFilter(color);
        iv.setPadding(ThemeTokens.dp(this, 8), ThemeTokens.dp(this, 4), ThemeTokens.dp(this, 8), ThemeTokens.dp(this, 4));
        iv.setLayoutParams(new LinearLayout.LayoutParams(ThemeTokens.dp(this, 40), ThemeTokens.dp(this, 36)));
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

    private void toggleFileSelection(String path) {
        if (mSelectedPaths.contains(path)) {
            mSelectedPaths.remove(path);
        } else {
            mSelectedPaths.add(path);
        }
        updateSelectionCount();
        refreshDirectory();
    }

    private void switchTab(String tabKey) {
        if (mHeaderView != null) {
            mHeaderView.setNewItemButtonVisibility("files".equals(tabKey));
        }
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

    private void refreshDirectory() {
        mIsSearchActive = false;
        mHeaderView.updateBreadcrumbs(mCurrentDir);
        mFileListGroup.removeAllViews();

        List<File> files = FileRepository.listFiles(mCurrentDir, mShowHidden, mSortField, mSortAsc);
        if (files.isEmpty()) {
            showEmptyState("Thư mục rỗng", "Không có tệp hoặc thư mục nào ở đây.");
            return;
        }

        GradientDrawable groupBg = new GradientDrawable();
        groupBg.setCornerRadius(ThemeTokens.dp(this, ThemeTokens.RADIUS_GROUP_LIST));
        groupBg.setColor(ThemeTokens.COLOR_SURFACE_ELEVATED);
        mFileListGroup.setBackground(groupBg);

        for (int i = 0; i < files.size(); i++) {
            File f = files.get(i);
            boolean isLast = (i == files.size() - 1);
            boolean isSelected = mSelectedPaths.contains(f.getAbsolutePath());

            View row = FileRowView.createRow(this, f, isLast, isSelected, mIsSelectionMode, new FileRowView.OnFileRowClickListener() {
                @Override
                public void onClick(File clickedFile) {
                    if (mIsSelectionMode) {
                        toggleFileSelection(clickedFile.getAbsolutePath());
                    } else {
                        if (clickedFile.isDirectory()) {
                            mCurrentDir = clickedFile;
                            refreshDirectory();
                        } else if (ImageViewerActivity.isImageFile(clickedFile)) {
                            openImageViewer(clickedFile);
                        } else if (TextFileIO.isTextFile(clickedFile)) {
                            openTextEditor(clickedFile);
                        } else if (isApkFile(clickedFile)) {
                            installApk(clickedFile);
                        } else {
                            showFileActionSheet(clickedFile);
                        }
                    }
                }

                @Override
                public void onLongClick(File clickedFile) {
                    if (mIsSelectionMode) {
                        toggleFileSelection(clickedFile.getAbsolutePath());
                    } else {
                        showFileActionSheet(clickedFile);
                    }
                }
            });
            mFileListGroup.addView(row);
        }
    }

    private void showEmptyState(String title, String subtitle) {
        mFileListGroup.setBackground(null);

        LinearLayout emptyLayout = new LinearLayout(this);
        emptyLayout.setOrientation(LinearLayout.VERTICAL);
        emptyLayout.setGravity(Gravity.CENTER);
        emptyLayout.setPadding(ThemeTokens.dp(this, 32), ThemeTokens.dp(this, 64),
                ThemeTokens.dp(this, 32), ThemeTokens.dp(this, 32));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_folder);
        icon.setColorFilter(ThemeTokens.COLOR_SEC_LABEL);
        icon.setAlpha(0.35f);
        icon.setLayoutParams(new LinearLayout.LayoutParams(ThemeTokens.dp(this, 64), ThemeTokens.dp(this, 64)));
        emptyLayout.addView(icon);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(ThemeTokens.COLOR_LABEL);
        titleView.setTextSize(17);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = ThemeTokens.dp(this, 16);
        titleView.setLayoutParams(tlp);
        emptyLayout.addView(titleView);

        TextView subView = new TextView(this);
        subView.setText(subtitle);
        subView.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        subView.setTextSize(14);
        subView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = ThemeTokens.dp(this, 6);
        subView.setLayoutParams(slp);
        emptyLayout.addView(subView);

        mFileListGroup.addView(emptyLayout);
    }

    // ==========================================
    // ACTION SHEETS (MODAL BOTTOM SHEETS)
    // ==========================================
    private void showFileActionSheet(File f) {
        vibrateLight();
        Dialog dialog = IosSheet.createBaseDialog(this);
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        // Header: Icon 40dp + Filename + Metadata footnote
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(ThemeTokens.dp(this, 8), ThemeTokens.dp(this, 4),
                ThemeTokens.dp(this, 8), ThemeTokens.dp(this, 12));

        LinearLayout iconBox = new LinearLayout(this);
        iconBox.setGravity(Gravity.CENTER);
        int boxSize = ThemeTokens.dp(this, ThemeTokens.SIZE_ICON_BOX);
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(boxSize, boxSize));
        GradientDrawable iconBoxBg = new GradientDrawable();
        iconBoxBg.setCornerRadius(ThemeTokens.dp(this, ThemeTokens.RADIUS_ICON_BOX));
        int tint = FileRowView.getSemanticColorForFile(f);
        iconBoxBg.setColor((tint & 0x00FFFFFF) | 0x2E000000);
        iconBox.setBackground(iconBoxBg);

        ImageView iv = new ImageView(this);
        iv.setImageResource(FileRowView.getIconResourceForFile(f));
        iv.setColorFilter(tint);
        int insideSize = ThemeTokens.dp(this, ThemeTokens.SIZE_ICON_INSIDE);
        iv.setLayoutParams(new LinearLayout.LayoutParams(insideSize, insideSize));
        iconBox.addView(iv);
        header.addView(iconBox);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.leftMargin = ThemeTokens.dp(this, 12);

        TextView nameText = new TextView(this);
        nameText.setText(FileRowView.middleEllipsis(f.getName(), 32));
        nameText.setTextColor(ThemeTokens.COLOR_LABEL);
        nameText.setTextSize(17);
        nameText.setTypeface(Typeface.DEFAULT_BOLD);
        nameText.setSingleLine(true);
        textCol.addView(nameText);

        TextView metaText = new TextView(this);
        String meta = f.isDirectory() ? ("Thư mục · " + FileRowView.formatItemDate(f.lastModified()))
                                     : (FileRowView.formatSize(f.length()) + " · " + FileRowView.formatItemDate(f.lastModified()));
        metaText.setText(meta);
        metaText.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        metaText.setTextSize(13);
        textCol.addView(metaText);

        header.addView(textCol, colLp);
        sheet.addView(header);

        // Separator line 0.5dp
        View sep = new View(this);
        sep.setBackgroundColor(ThemeTokens.COLOR_SEPARATOR_INSET);
        sep.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, ThemeTokens.dp(this, 1) / 2)));
        sheet.addView(sep);

        // Scrollable container for action groups
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);

        // Group 1: Cài đặt (nếu APK), Chỉnh sửa (nếu text), Mở bằng…, Chia sẻ, Chi tiết
        LinearLayout group1 = IosSheet.createActionGroup(this);
        if (isApkFile(f)) {
            group1.addView(IosSheet.createActionRow(this, R.drawable.ic_file_apk, "Cài đặt ứng dụng", false, v -> {
                dialog.dismiss();
                installApk(f);
            }, true));
        }
        if (TextFileIO.isTextFile(f)) {
            group1.addView(IosSheet.createActionRow(this, R.drawable.ic_rename, "Chỉnh sửa", false, v -> {
                dialog.dismiss();
                openTextEditor(f);
            }, true));
        }
        group1.addView(IosSheet.createActionRow(this, R.drawable.ic_open_with, "Mở bằng…", false, v -> {
            dialog.dismiss();
            openWith(f);
        }, true));
        group1.addView(IosSheet.createActionRow(this, R.drawable.ic_share, "Chia sẻ", false, v -> {
            dialog.dismiss();
            shareFile(f);
        }, true));
        group1.addView(IosSheet.createActionRow(this, R.drawable.ic_info, "Chi tiết", false, v -> {
            dialog.dismiss();
            showFileDetailsSheet(f);
        }, false));
        scrollContent.addView(group1);

        // Group 2: Sao chép, Di chuyển, Đổi tên
        LinearLayout group2 = IosSheet.createActionGroup(this);
        group2.addView(IosSheet.createActionRow(this, R.drawable.ic_copy, "Sao chép", false, v -> {
            dialog.dismiss();
            copyFileToClipboard(f);
        }, true));
        group2.addView(IosSheet.createActionRow(this, R.drawable.ic_move, "Di chuyển", false, v -> {
            dialog.dismiss();
            moveFileToClipboard(f);
        }, true));
        group2.addView(IosSheet.createActionRow(this, R.drawable.ic_rename, "Đổi tên", false, v -> {
            dialog.dismiss();
            showRenameSheet(f);
        }, false));
        scrollContent.addView(group2);

        // Group 3: Nén ZIP, Giải nén, Tính SHA-256, Mã hóa vào Vault
        LinearLayout group3 = IosSheet.createActionGroup(this);
        group3.addView(IosSheet.createActionRow(this, R.drawable.ic_file_archive, "Nén thành ZIP", false, v -> {
            dialog.dismiss();
            compressSingleToZip(f);
        }, true));

        if (ZipManager.isArchive(f)) {
            group3.addView(IosSheet.createActionRow(this, R.drawable.ic_unzip, "Giải nén tại đây", false, v -> {
                dialog.dismiss();
                extractZipAsync(f, mCurrentDir);
            }, true));
            group3.addView(IosSheet.createActionRow(this, R.drawable.ic_folder_plus, "Giải nén vào thư mục riêng", false, v -> {
                dialog.dismiss();
                extractZipToSeparateDir(f);
            }, true));
        }

        group3.addView(IosSheet.createActionRow(this, R.drawable.ic_tab_hash, "Tính SHA-256", false, v -> {
            dialog.dismiss();
            computeSha256Async(f);
        }, true));

        group3.addView(IosSheet.createActionRow(this, R.drawable.ic_lock, "Mã hóa vào Vault", false, v -> {
            dialog.dismiss();
            showEncryptToVaultSheet(f);
        }, false));
        scrollContent.addView(group3);

        // Group 4: Xóa
        LinearLayout group4 = IosSheet.createActionGroup(this);
        group4.addView(IosSheet.createActionRow(this, R.drawable.ic_delete, "Xóa", true, v -> {
            dialog.dismiss();
            IosSheet.showDeleteConfirm(this, f, () -> {
                FileRepository.deleteRecursive(f);
                refreshDirectory();
                Toast.makeText(this, "Đã xóa thành công", Toast.LENGTH_SHORT).show();
            });
        }, false));
        scrollContent.addView(group4);

        sv.addView(scrollContent);
        sheet.addView(sv);

        IosSheet.show(dialog, sheet);
    }

    private void showBatchDeleteConfirmSheet() {
        if (mSelectedPaths.isEmpty()) return;
        IosSheet.showDeleteConfirm(this, new File(mSelectedPaths.size() + " mục đã chọn"), () -> {
            for (String p : mSelectedPaths) {
                FileRepository.deleteRecursive(new File(p));
            }
            exitSelectionMode();
            Toast.makeText(this, "Đã xóa các mục đã chọn", Toast.LENGTH_SHORT).show();
        });
    }

    private void showRenameSheet(File f) {
        IosSheet.showInputDialog(this, "Đổi tên", null, f.getName(), "Xong",
                input -> {
                    if (input.isEmpty()) return "Tên tệp không được để trống";
                    if (!input.equals(f.getName()) && new File(f.getParentFile(), input).exists()) {
                        return "Tên này đã tồn tại trong thư mục";
                    }
                    return null;
                },
                newName -> {
                    if (FileRepository.rename(f, newName)) {
                        refreshDirectory();
                        Toast.makeText(this, "Đã đổi tên thành: " + newName, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showNewFolderSheet() {
        IosSheet.showInputDialog(this, "Thư mục mới", null, "", "Tạo",
                input -> {
                    if (input.isEmpty()) return "Tên thư mục không được để trống";
                    if (new File(mCurrentDir, input).exists()) {
                        return "Thư mục này đã tồn tại";
                    }
                    return null;
                },
                name -> {
                    if (FileRepository.createDirectory(mCurrentDir, name)) {
                        refreshDirectory();
                        Toast.makeText(this, "Đã tạo thư mục: " + name, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showFileDetailsSheet(File f) {
        Dialog dialog = IosSheet.createBaseDialog(this);
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Chi tiết tệp tin");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        LinearLayout card = IosSheet.createActionGroup(this);
        card.setPadding(ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 14),
                ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 14));

        addDetailItem(card, "Tên", f.getName());
        addDetailItem(card, "Đường dẫn", f.getAbsolutePath());
        addDetailItem(card, "Kích thước", FileRowView.formatSize(f.length()) + " (" + f.length() + " bytes)");
        addDetailItem(card, "Sửa đổi lần cuối", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date(f.lastModified())));
        addDetailItem(card, "Loại tệp", f.isDirectory() ? "Thư mục" : getMimeType(f));
        addDetailItem(card, "Quyền", (f.canRead() ? "Đọc " : "") + (f.canWrite() ? "Ghi " : ""));

        sheet.addView(card);

        TextView btnClose = new TextView(this);
        btnClose.setText("Đóng");
        btnClose.setTextColor(ThemeTokens.COLOR_ACCENT);
        btnClose.setTextSize(17);
        btnClose.setTypeface(Typeface.DEFAULT_BOLD);
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(this, 48)));
        btnClose.setOnClickListener(v -> dialog.dismiss());
        sheet.addView(btnClose);

        IosSheet.show(dialog, sheet);
    }

    private void addDetailItem(LinearLayout container, String key, String val) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, ThemeTokens.dp(this, 4), 0, ThemeTokens.dp(this, 4));

        TextView k = new TextView(this);
        k.setText(key);
        k.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        k.setTextSize(12);
        row.addView(k);

        TextView v = new TextView(this);
        v.setText(val);
        v.setTextColor(ThemeTokens.COLOR_LABEL);
        v.setTextSize(14);
        row.addView(v);

        container.addView(row);
    }

    private void showSha256Sheet(File f, String hex) {
        Dialog dialog = IosSheet.createBaseDialog(this);
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Mã băm SHA-256");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(f.getName());
        subtitle.setTextSize(13);
        subtitle.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        sheet.addView(subtitle);

        LinearLayout box = IosSheet.createActionGroup(this);
        box.setPadding(ThemeTokens.dp(this, 14), ThemeTokens.dp(this, 14),
                ThemeTokens.dp(this, 14), ThemeTokens.dp(this, 14));

        TextView hexTv = new TextView(this);
        hexTv.setText(hex);
        hexTv.setTextColor(ThemeTokens.COLOR_SUCCESS);
        hexTv.setTextSize(14);
        hexTv.setTypeface(Typeface.MONOSPACE);
        box.addView(hexTv);
        sheet.addView(box);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = ThemeTokens.dp(this, 16);
        btnRow.setLayoutParams(rlp);

        TextView btnClose = new TextView(this);
        btnClose.setText("Đóng");
        btnClose.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        btnClose.setTextSize(16);
        btnClose.setPadding(ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 10),
                ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 10));
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnClose);

        TextView btnCopy = new TextView(this);
        btnCopy.setText("Sao chép mã");
        btnCopy.setTextColor(Color.WHITE);
        btnCopy.setTextSize(16);
        btnCopy.setTypeface(Typeface.DEFAULT_BOLD);
        btnCopy.setPadding(ThemeTokens.dp(this, 20), ThemeTokens.dp(this, 10),
                ThemeTokens.dp(this, 20), ThemeTokens.dp(this, 10));
        GradientDrawable copyBg = new GradientDrawable();
        copyBg.setCornerRadius(ThemeTokens.dp(this, 999));
        copyBg.setColor(ThemeTokens.COLOR_ACCENT);
        btnCopy.setBackground(copyBg);
        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("SHA256", hex));
            Toast.makeText(this, "Đã sao chép mã SHA-256 vào bộ nhớ tạm", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        btnRow.addView(btnCopy);
        sheet.addView(btnRow);

        IosSheet.show(dialog, sheet);
    }

    private void showEncryptToVaultSheet(File src) {
        Dialog dialog = IosSheet.createBaseDialog(this);
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Mã hóa vào Vault");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Bảo vệ '" + src.getName() + "' bằng AES-256-GCM");
        sub.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        sub.setTextSize(13);
        sheet.addView(sub);

        EditText input = new EditText(this);
        input.setHint("Nhập mật khẩu mã hóa");
        input.setHintTextColor(ThemeTokens.COLOR_SEC_LABEL);
        input.setTextColor(ThemeTokens.COLOR_LABEL);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setPadding(ThemeTokens.dp(this, 14), ThemeTokens.dp(this, 12),
                ThemeTokens.dp(this, 14), ThemeTokens.dp(this, 12));

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(ThemeTokens.dp(this, ThemeTokens.RADIUS_INPUT_FIELD));
        inputBg.setColor(ThemeTokens.COLOR_ACTION_GROUP_BG);
        input.setBackground(inputBg);

        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.topMargin = ThemeTokens.dp(this, 16);
        sheet.addView(input, inLp);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = ThemeTokens.dp(this, 16);
        btnRow.setLayoutParams(rlp);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("Hủy");
        btnCancel.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        btnCancel.setTextSize(16);
        btnCancel.setPadding(ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 10),
                ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 10));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        TextView btnEnc = new TextView(this);
        btnEnc.setText("Mã hóa");
        btnEnc.setTextColor(Color.WHITE);
        btnEnc.setTextSize(16);
        btnEnc.setTypeface(Typeface.DEFAULT_BOLD);
        btnEnc.setPadding(ThemeTokens.dp(this, 20), ThemeTokens.dp(this, 10),
                ThemeTokens.dp(this, 20), ThemeTokens.dp(this, 10));
        GradientDrawable encBg = new GradientDrawable();
        encBg.setCornerRadius(ThemeTokens.dp(this, 999));
        encBg.setColor(ThemeTokens.COLOR_ACCENT);
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
                    VaultManager.encryptAesGcm(src, encFile, pwd);
                    runOnUiThread(() -> Toast.makeText(this, "Đã mã hóa an toàn vào Vault!", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, "Lỗi mã hóa: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        });
        btnRow.addView(btnEnc);
        sheet.addView(btnRow);

        IosSheet.show(dialog, sheet, true);
    }

    private void showSortActionSheet() {
        Dialog dialog = IosSheet.createBaseDialog(this);
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Sắp xếp theo");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        LinearLayout card = IosSheet.createActionGroup(this);
        String[] options = {"Tên (A-Z)", "Ngày sửa đổi", "Kích thước", "Loại tệp tin"};

        for (int i = 0; i < options.length; i++) {
            final int field = i;
            boolean isChosen = (mSortField == field);
            boolean isLast = (i == options.length - 1);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(ThemeTokens.dp(this, 16), 0, ThemeTokens.dp(this, 16), 0);
            row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(this, 50)));

            TextView tv = new TextView(this);
            tv.setText(options[i]);
            tv.setTextSize(17);
            tv.setTextColor(isChosen ? ThemeTokens.COLOR_ACCENT : ThemeTokens.COLOR_LABEL);
            tv.setTypeface(isChosen ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(tv, tlp);

            if (isChosen) {
                ImageView check = new ImageView(this);
                check.setImageResource(R.drawable.ic_check);
                check.setColorFilter(ThemeTokens.COLOR_ACCENT);
                check.setLayoutParams(new LinearLayout.LayoutParams(ThemeTokens.dp(this, 20), ThemeTokens.dp(this, 20)));
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
                div.setBackgroundColor(ThemeTokens.COLOR_SEPARATOR_INSET);
                LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, ThemeTokens.dp(this, 1) / 2));
                divLp.leftMargin = ThemeTokens.dp(this, 16);
                div.setLayoutParams(divLp);
                card.addView(div);
            }
        }
        sheet.addView(card);

        LinearLayout cancelCard = IosSheet.createActionGroup(this);
        TextView btnCancel = new TextView(this);
        btnCancel.setText("Hủy");
        btnCancel.setTextColor(ThemeTokens.COLOR_ACCENT);
        btnCancel.setTextSize(17);
        btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(this, 52)));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        cancelCard.addView(btnCancel);
        sheet.addView(cancelCard);

        IosSheet.show(dialog, sheet);
    }

    private void showSearchSheet() {
        Dialog dialog = IosSheet.createBaseDialog(this);
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Tìm kiếm trong thư mục");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        EditText input = new EditText(this);
        input.setHint("Nhập tên tệp hoặc thư mục");
        input.setHintTextColor(ThemeTokens.COLOR_SEC_LABEL);
        input.setTextColor(ThemeTokens.COLOR_LABEL);
        input.setTextSize(16);
        input.setPadding(ThemeTokens.dp(this, 14), ThemeTokens.dp(this, 12),
                ThemeTokens.dp(this, 14), ThemeTokens.dp(this, 12));

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(ThemeTokens.dp(this, ThemeTokens.RADIUS_INPUT_FIELD));
        inputBg.setColor(ThemeTokens.COLOR_ACTION_GROUP_BG);
        input.setBackground(inputBg);

        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.topMargin = ThemeTokens.dp(this, 16);
        sheet.addView(input, inLp);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = ThemeTokens.dp(this, 16);
        btnRow.setLayoutParams(rlp);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("Hủy");
        btnCancel.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        btnCancel.setTextSize(16);
        btnCancel.setPadding(ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 10),
                ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 10));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        TextView btnSearch = new TextView(this);
        btnSearch.setText("Tìm");
        btnSearch.setTextColor(Color.WHITE);
        btnSearch.setTextSize(16);
        btnSearch.setTypeface(Typeface.DEFAULT_BOLD);
        btnSearch.setPadding(ThemeTokens.dp(this, 24), ThemeTokens.dp(this, 10),
                ThemeTokens.dp(this, 24), ThemeTokens.dp(this, 10));
        GradientDrawable sBg = new GradientDrawable();
        sBg.setCornerRadius(ThemeTokens.dp(this, 999));
        sBg.setColor(ThemeTokens.COLOR_ACCENT);
        btnSearch.setBackground(sBg);
        btnSearch.setOnClickListener(v -> {
            String query = input.getText().toString().trim();
            dialog.dismiss();
            if (!query.isEmpty()) performSearch(query);
        });
        btnRow.addView(btnSearch);
        sheet.addView(btnRow);

        IosSheet.show(dialog, sheet, true);
        input.requestFocus();
    }

    private void performSearch(String query) {
        mIsSearchActive = true;
        mFileListGroup.removeAllViews();
        List<File> matched = FileRepository.search(mCurrentDir, query, 100);

        if (matched.isEmpty()) {
            showEmptyState("Không tìm thấy kết quả", "Không có tệp nào khớp với từ khóa \"" + query + "\".");
            return;
        }

        GradientDrawable groupBg = new GradientDrawable();
        groupBg.setCornerRadius(ThemeTokens.dp(this, ThemeTokens.RADIUS_GROUP_LIST));
        groupBg.setColor(ThemeTokens.COLOR_SURFACE_ELEVATED);
        mFileListGroup.setBackground(groupBg);

        for (int i = 0; i < matched.size(); i++) {
            File f = matched.get(i);
            boolean isLast = (i == matched.size() - 1);
            View row = FileRowView.createRow(this, f, isLast, false, false, new FileRowView.OnFileRowClickListener() {
                @Override
                public void onClick(File clickedFile) {
                    if (clickedFile.isDirectory()) {
                        mCurrentDir = clickedFile;
                        refreshDirectory();
                    } else if (ImageViewerActivity.isImageFile(clickedFile)) {
                        openImageViewer(clickedFile);
                    } else if (TextFileIO.isTextFile(clickedFile)) {
                        openTextEditor(clickedFile);
                    } else if (isApkFile(clickedFile)) {
                        installApk(clickedFile);
                    } else {
                        showFileActionSheet(clickedFile);
                    }
                }

                @Override
                public void onLongClick(File clickedFile) {
                    showFileActionSheet(clickedFile);
                }
            });
            mFileListGroup.addView(row);
        }
    }

    private void showMoreOptionsSheet() {
        Dialog dialog = IosSheet.createBaseDialog(this);
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Tùy chọn VELQOR");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        // Group 1
        LinearLayout group1 = IosSheet.createActionGroup(this);
        group1.addView(IosSheet.createActionRow(this, R.drawable.ic_folder_plus, "Tạo thư mục mới", false, v -> {
            dialog.dismiss();
            showNewFolderSheet();
        }, true));

        group1.addView(IosSheet.createActionRow(this, R.drawable.ic_file_text, "Tạo tệp mới", false, v -> {
            dialog.dismiss();
            NewItemSheet.showCreateFileDialog(this, mCurrentDir, "Tệp mới.txt", new NewItemSheet.OnItemCreateListener() {
                @Override
                public void onCreateFolderRequested() {
                    showNewFolderSheet();
                }

                @Override
                public void onFileCreated(File newFile) {
                    refreshDirectory();
                    vibrateLight();
                    openTextEditor(newFile);
                }
            });
        }, true));

        group1.addView(IosSheet.createActionRow(this, R.drawable.ic_sort, "Sắp xếp danh sách", false, v -> {
            dialog.dismiss();
            showSortActionSheet();
        }, true));

        int eyeIcon = mShowHidden ? R.drawable.ic_eye_off : R.drawable.ic_eye;
        String hiddenLabel = mShowHidden ? "Ẩn tệp tin ẩn" : "Hiện tệp tin ẩn";
        group1.addView(IosSheet.createActionRow(this, eyeIcon, hiddenLabel, false, v -> {
            dialog.dismiss();
            mShowHidden = !mShowHidden;
            getSharedPreferences("velqor_prefs", MODE_PRIVATE).edit().putBoolean("show_hidden", mShowHidden).apply();
            refreshDirectory();
        }, false));
        sheet.addView(group1);

        // Group 2: Paste clipboard or selection mode
        LinearLayout group2 = IosSheet.createActionGroup(this);
        if (mClipboardFile != null && mClipboardFile.exists()) {
            String pasteLabel = (mIsCutAction ? "Di chuyển " : "Dán ") + "'" + mClipboardFile.getName() + "' vào đây";
            group2.addView(IosSheet.createActionRow(this, R.drawable.ic_copy, pasteLabel, false, v -> {
                dialog.dismiss();
                pasteClipboardFile();
            }, true));
        }

        group2.addView(IosSheet.createActionRow(this, R.drawable.ic_check, "Chọn nhiều mục", false, v -> {
            dialog.dismiss();
            enterSelectionMode();
        }, false));
        sheet.addView(group2);

        IosSheet.show(dialog, sheet);
    }

    private void showBatchOptionsSheet() {
        if (mSelectedPaths.isEmpty()) return;
        Dialog dialog = IosSheet.createBaseDialog(this);
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Đã chọn " + mSelectedPaths.size() + " mục");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        LinearLayout group1 = IosSheet.createActionGroup(this);
        group1.addView(IosSheet.createActionRow(this, R.drawable.ic_file_archive, "Nén thành ZIP", false, v -> {
            dialog.dismiss();
            compressSelected();
        }, false));
        sheet.addView(group1);

        LinearLayout group2 = IosSheet.createActionGroup(this);
        group2.addView(IosSheet.createActionRow(this, R.drawable.ic_delete, "Xóa các mục đã chọn", true, v -> {
            dialog.dismiss();
            showBatchDeleteConfirmSheet();
        }, false));
        sheet.addView(group2);

        IosSheet.show(dialog, sheet);
    }

    // ==========================================
    // REAL FILE I/O OPERATIONS (OUTSIDE MAIN THREAD)
    // ==========================================
    private void openImageViewer(File f) {
        Intent intent = new Intent(this, ImageViewerActivity.class);
        intent.putExtra(ImageViewerActivity.EXTRA_PATH, f.getAbsolutePath());
        startActivity(intent);
    }

    private void openTextEditor(File f) {
        Intent intent = new Intent(this, TextEditorActivity.class);
        intent.putExtra(TextEditorActivity.EXTRA_PATH, f.getAbsolutePath());
        startActivity(intent);
    }

    private boolean isApkFile(File f) {
        return f != null && !f.isDirectory() && f.getName().toLowerCase().endsWith(".apk");
    }

    private void installApk(File f) {
        if (f == null || !f.exists()) {
            Toast.makeText(this, "Tệp APK không tồn tại", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                try {
                    Intent manageIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    manageIntent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(manageIntent);
                    Toast.makeText(this, "Vui lòng cho phép cài đặt ứng dụng không rõ nguồn cho VELQOR", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Intent settingsIntent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                    startActivity(settingsIntent);
                    Toast.makeText(this, "Vui lòng bật quyền cài đặt ứng dụng không rõ nguồn", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }

        try {
            Uri apkUri = VelqorFileProvider.getUriForFile(this, "com.velqor.app.fileprovider", f);
            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

            List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(installIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                grantUriPermission(packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            startActivity(installIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khởi chạy cài đặt: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openWith(File f) {
        try {
            Uri uri = VelqorFileProvider.getUriForFile(this, "com.velqor.app.fileprovider", f);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String mime = getMimeType(f);
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

            List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            Intent chooser = Intent.createChooser(intent, "Mở bằng…");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(chooser);
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

            List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

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
        File dest = FileRepository.getUniqueDestinationFile(mCurrentDir, src.getName());
        new Thread(() -> {
            try {
                if (mIsCutAction) {
                    FileRepository.move(src, dest);
                    mClipboardFile = null;
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Đã di chuyển thành công: " + dest.getName(), Toast.LENGTH_SHORT).show();
                        refreshDirectory();
                    });
                } else {
                    FileRepository.copy(src, dest);
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

    private String getMimeType(File f) {
        if (f == null || f.isDirectory()) return "*/*";
        String name = f.getName().toLowerCase();
        if (name.endsWith(".apk")) return "application/vnd.android.package-archive";
        if (name.endsWith(".zip")) return "application/zip";
        if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) return "application/x-gzip";
        if (name.endsWith(".tar")) return "application/x-tar";
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".log") || name.endsWith(".json")) return "text/plain";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".mp4")) return "video/mp4";
        if (name.endsWith(".mkv")) return "video/x-matroska";
        if (name.endsWith(".mp3")) return "audio/mpeg";
        if (name.endsWith(".m4a")) return "audio/mp4";

        String ext = FileRepository.getFileExtension(f).toLowerCase();
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
                ZipManager.compressFiles(files, zipFile);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Đã nén thành công: " + zipFile.getName(), Toast.LENGTH_SHORT).show();
                    refreshDirectory();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi nén: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void extractZipAsync(File zipFile, File destDir) {
        new Thread(() -> {
            try {
                ZipManager.extractZip(zipFile, destDir);
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
        extractZipAsync(archiveFile, dest);
    }

    private void computeSha256Async(File f) {
        new Thread(() -> {
            try {
                String hex = HashUtils.computeSha256(f);
                runOnUiThread(() -> showSha256Sheet(f, hex));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi tính hash: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ==========================================
    // OTHER TABS (ARCHIVE, VAULT, HASH, SETTINGS)
    // ==========================================
    private void showArchiveTab() {
        mFileListGroup.removeAllViews();
        mFileListGroup.setBackground(null);

        LinearLayout card = IosSheet.createActionGroup(this);
        card.setPadding(ThemeTokens.dp(this, 20), ThemeTokens.dp(this, 20),
                ThemeTokens.dp(this, 20), ThemeTokens.dp(this, 20));

        TextView title = new TextView(this);
        title.setText("Trình quản lý nén VELQOR PRO");
        title.setTextSize(18);
        title.setTextColor(ThemeTokens.COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(title);

        TextView info = new TextView(this);
        info.setText("\n• Định dạng hỗ trợ: ZIP, TAR, TAR.GZ, TGZ, 7Z, RAR\n• Công nghệ chống Zip Slip bảo mật đường dẫn tuyệt đối\n• Nén & giải nén trực tiếp với luồng I/O độc lập ngoài main thread\n\nĐể nén hoặc giải nén tệp, hãy chạm vào tệp bất kỳ trong tab 'Tệp'.");
        info.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        info.setTextSize(14);
        card.addView(info);

        mFileListGroup.addView(card);
    }

    private void showVaultTab() {
        mFileListGroup.removeAllViews();
        mFileListGroup.setBackground(null);

        LinearLayout card = IosSheet.createActionGroup(this);
        card.setPadding(ThemeTokens.dp(this, 20), ThemeTokens.dp(this, 20),
                ThemeTokens.dp(this, 20), ThemeTokens.dp(this, 20));

        TextView title = new TextView(this);
        title.setText("Kho bảo mật Vault (AES-256-GCM)");
        title.setTextSize(18);
        title.setTextColor(ThemeTokens.COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(title);

        File vaultDir = new File(getFilesDir(), "vault");
        File[] vFiles = vaultDir.listFiles();
        int count = (vFiles != null) ? vFiles.length : 0;

        TextView info = new TextView(this);
        info.setText("\n• Tiêu chuẩn mã hóa: AES/GCM/NoPadding 256-bit\n• Hàm suy biến khóa: PBKDF2WithHmacSHA256 (65,536 vòng lặp)\n• Hiện có: " + count + " tệp được bảo vệ an toàn\n\nChạm vào tệp trong tab 'Tệp' và chọn 'Mã hóa vào Vault' để đưa vào kho bảo mật.");
        info.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        info.setTextSize(14);
        card.addView(info);

        mFileListGroup.addView(card);
    }

    private void showHashTab() {
        mFileListGroup.removeAllViews();
        mFileListGroup.setBackground(null);

        LinearLayout card = IosSheet.createActionGroup(this);
        card.setPadding(ThemeTokens.dp(this, 20), ThemeTokens.dp(this, 20),
                ThemeTokens.dp(this, 20), ThemeTokens.dp(this, 20));

        TextView title = new TextView(this);
        title.setText("Kiểm tra toàn vẹn mã băm (Hash)");
        title.setTextSize(18);
        title.setTextColor(ThemeTokens.COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(title);

        TextView info = new TextView(this);
        info.setText("\n• Hỗ trợ tính toán mã băm SHA-256 nguyên bản\n• Buffer I/O 64KB tối ưu tốc độ đọc tệp lớn\n• Kết quả hiển thị tức thì với nút sao chép 1 chạm\n\nChạm vào tệp bất kỳ trong tab 'Tệp' và chọn 'Tính SHA-256' để kiểm tra checksum.");
        info.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        info.setTextSize(14);
        card.addView(info);

        mFileListGroup.addView(card);
    }

    private void showSettingsTab() {
        mFileListGroup.removeAllViews();
        mFileListGroup.setBackground(null);

        LinearLayout card = IosSheet.createActionGroup(this);
        card.setPadding(ThemeTokens.dp(this, 20), ThemeTokens.dp(this, 20),
                ThemeTokens.dp(this, 20), ThemeTokens.dp(this, 20));

        TextView title = new TextView(this);
        title.setText("Cài đặt VELQOR");
        title.setTextSize(18);
        title.setTextColor(ThemeTokens.COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(title);

        TextView info = new TextView(this);
        info.setText("\n• Phiên bản: VELQOR v2.0.7 (Build 9)\n• Thiết kế: Giao diện Liquid Glass Cupertino chuẩn Apple\n• Quyền bộ nhớ: MANAGE_EXTERNAL_STORAGE đã cấp\n• Động cơ xử lý: Pure Android SDK + Coroutines Flow\n• Nhà phát triển: Google DeepMind Pair Programming");
        info.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        info.setTextSize(14);
        card.addView(info);

        mFileListGroup.addView(card);

        // Thumbnail Settings Card
        android.content.SharedPreferences prefs = getSharedPreferences("velqor_editor_prefs", MODE_PRIVATE);
        boolean showThumbs = prefs.getBoolean("show_thumbnails", true);

        LinearLayout cardThumb = IosSheet.createActionGroup(this);
        cardThumb.setPadding(ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 12),
                ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 12));

        cardThumb.addView(IosSheet.createActionRow(this, R.drawable.ic_file_image,
                "Hiện thumbnail: " + (showThumbs ? "Bật" : "Tắt"), false, v -> {
            prefs.edit().putBoolean("show_thumbnails", !showThumbs).apply();
            showSettingsTab();
        }, true));

        long cacheBytes = com.velqor.app.files.ThumbnailCache.getInstance(this).getDiskCacheSize();
        cardThumb.addView(IosSheet.createActionRow(this, R.drawable.ic_delete,
                "Xóa bộ nhớ đệm thumbnail (" + FileRowView.formatSize(cacheBytes) + ")", false, v -> {
            com.velqor.app.files.ThumbnailCache.getInstance(this).clear();
            Toast.makeText(this, "Đã xóa sạch bộ nhớ đệm thumbnail", Toast.LENGTH_SHORT).show();
            showSettingsTab();
        }, false));

        mFileListGroup.addView(cardThumb);
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

    private void vibrateLight() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(12);
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onBackPressed() {
        if (handleBackNavigation()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (handleBackNavigation()) {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean handleBackNavigation() {
        // 1. Exit Selection Mode if active
        if (mIsSelectionMode) {
            exitSelectionMode();
            return true;
        }

        // 2. Exit Search Mode if active
        if (mIsSearchActive) {
            mIsSearchActive = false;
            refreshDirectory();
            return true;
        }

        // 3. Switch to "Tệp" tab if in another tab
        if (!"files".equals(mCurrentTab)) {
            mCurrentTab = "files";
            if (mFloatingTabBar != null) {
                mFloatingTabBar.selectTab("files");
            }
            switchTab("files");
            return true;
        }

        // 4. In "Tệp" tab: if inside a subfolder, navigate up to parent directory
        if (mCurrentDir != null && !isStorageRoot(mCurrentDir)) {
            File parent = mCurrentDir.getParentFile();
            if (parent != null && parent.canRead()) {
                mCurrentDir = parent;
                refreshDirectory();
                if (mScrollView != null) {
                    mScrollView.smoothScrollTo(0, 0);
                }
                return true;
            }
        }

        // 5. At root: double-back within 2 seconds to exit app
        long now = System.currentTimeMillis();
        if (now - mLastBackPressTime < 2000) {
            return false;
        } else {
            mLastBackPressTime = now;
            Toast.makeText(this, "Nhấn lần nữa để thoát VELQOR", Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    private boolean isStorageRoot(File dir) {
        if (dir == null) return true;
        File root = Environment.getExternalStorageDirectory();
        try {
            if (dir.getCanonicalPath().equals(root.getCanonicalPath())) return true;
        } catch (Exception ignored) {}

        String path = dir.getAbsolutePath();
        if (path.equals("/storage/emulated/0") || path.equals("/sdcard") ||
            path.equals("/storage/emulated") || path.equals("/storage") ||
            path.equals("/")) {
            return true;
        }

        File parent = dir.getParentFile();
        return parent == null || !parent.canRead();
    }
}
