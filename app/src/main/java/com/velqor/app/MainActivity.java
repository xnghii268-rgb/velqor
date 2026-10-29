package com.velqor.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
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

import com.velqor.app.archive.ZipManager;
import com.velqor.app.files.DirectoryWatcher;
import com.velqor.app.files.FileRepository;
import com.velqor.app.files.StorageScanner;
import com.velqor.app.files.TextFileIO;
import com.velqor.app.files.TrashManager;
import com.velqor.app.tokens.ThemeTokens;
import com.velqor.app.ui.DonutChartView;
import com.velqor.app.ui.FileRowView;
import com.velqor.app.ui.FloatingTabBar;
import com.velqor.app.ui.HeaderView;
import com.velqor.app.ui.ImageViewerActivity;
import com.velqor.app.ui.IosPullScrollView;
import com.velqor.app.ui.IosSheet;
import com.velqor.app.ui.IosSwitchView;
import com.velqor.app.ui.NewItemSheet;
import com.velqor.app.ui.SettingsViewHelper;
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
    private View mBottomGradientScrim;
    private boolean mIsBarVisible = true;
    private LinearLayout mFileListGroup;
    private IosPullScrollView mScrollView;
    private LinearLayout mScrollContent;
    private DirectoryWatcher mDirectoryWatcher;
    private LinearLayout mSelectionBar;
    private TextView mSelectionCountText;

    private boolean mShowHidden = false;
    private int mSortField = 0; 
    private boolean mSortAsc = true;

    private boolean mIsSelectionMode = false;
    private final Set<String> mSelectedPaths = new HashSet<>();

    private File mClipboardFile = null;
    private boolean mIsCutAction = false;
    private String mCurrentTab = "files";
    private boolean mIsSearchActive = false;
    private boolean mIsGridMode = false;
    private long mLastBackPressTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
        setupEdgeToEdge();

        mCurrentDir = Environment.getExternalStorageDirectory();
        mShowHidden = getSharedPreferences("velqor_prefs", MODE_PRIVATE).getBoolean("show_hidden", false);
        mIsGridMode = getSharedPreferences("velqor_prefs", MODE_PRIVATE).getBoolean("grid_mode", false);
        checkAndRequestPermissions();

        
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

        
        mDirectoryWatcher = new DirectoryWatcher(this::onDirectoryContentsChanged);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(ThemeTokens.getBackgroundColor(this));

        mScrollView = new IosPullScrollView(this);
        mScrollView.setFillViewport(true);
        mScrollView.setVerticalScrollBarEnabled(false);
        mScrollView.setOnRefreshListener(() -> {
            if ("files".equals(mCurrentTab)) {
                refreshDirectory();
            } else if ("archive".equals(mCurrentTab)) {
                showArchiveTab();
                mScrollView.finishRefresh();
            } else if ("settings".equals(mCurrentTab)) {
                showSettingsTab();
                mScrollView.finishRefresh();
            } else {
                mScrollView.finishRefresh();
            }
        });

        mScrollContent = new LinearLayout(this);
        mScrollContent.setOrientation(LinearLayout.VERTICAL);

        updateScrollPadding(true);

        mFileListGroup = new LinearLayout(this);
        mFileListGroup.setOrientation(LinearLayout.VERTICAL);
        mScrollContent.addView(mFileListGroup);

        mScrollView.addView(mScrollContent);
        root.addView(mScrollView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        
        mHeaderView = new HeaderView(this, getStatusBarHeight());
        mHeaderView.setViewMode(mIsGridMode);
        mHeaderView.setOnHeaderActionListener(new HeaderView.OnHeaderActionListener() {
            @Override public void onSearchClick() { showSearchSheet(); }
            @Override public void onViewModeToggleClick() {
                mIsGridMode = !mIsGridMode;
                mHeaderView.setViewMode(mIsGridMode);
                getSharedPreferences("velqor_prefs", MODE_PRIVATE).edit().putBoolean("grid_mode", mIsGridMode).apply();
                refreshDirectory();
            }
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

        
        mSelectionBar = createSelectionBar();
        FrameLayout.LayoutParams selLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        selLp.gravity = Gravity.TOP;
        selLp.topMargin = getStatusBarHeight() + ThemeTokens.dp(this, 88);
        root.addView(mSelectionBar, selLp);

        int barHeight = ThemeTokens.dp(this, ThemeTokens.HEIGHT_TAB_BAR);

        
        int scrimHeight = barHeight + getNavBarHeight() + ThemeTokens.dp(this, 16);
        mBottomGradientScrim = new View(this) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                return false; 
            }
        };
        int scrimColor = ThemeTokens.getScrimColor(this);
        int midScrim = (scrimColor & 0x00FFFFFF) | 0x4D000000;
        int endScrim = (scrimColor & 0x00FFFFFF) | 0xE6000000;
        GradientDrawable scrimBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00000000, midScrim, endScrim}
        );
        mBottomGradientScrim.setBackground(scrimBg);
        mBottomGradientScrim.setClickable(false);
        mBottomGradientScrim.setFocusable(false);
        FrameLayout.LayoutParams scrimLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, scrimHeight);
        scrimLp.gravity = Gravity.BOTTOM;
        root.addView(mBottomGradientScrim, scrimLp);

        
        mFloatingTabBar = new FloatingTabBar(this);
        mFloatingTabBar.setBackdropSource(mScrollView);
        mFloatingTabBar.setOnTabSelectedListener((tabKey, index) -> {
            mCurrentTab = tabKey;
            vibrateLight();
            switchTab(tabKey);
        });

        FrameLayout.LayoutParams tabLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, barHeight);
        tabLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        tabLp.leftMargin = ThemeTokens.dp(this, 20);
        tabLp.rightMargin = ThemeTokens.dp(this, 20);
        tabLp.bottomMargin = getNavBarHeight() + ThemeTokens.dp(this, 12);
        root.addView(mFloatingTabBar, tabLp);

        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                mHeaderView.setScrolledState(scrollY > ThemeTokens.dp(this, 30));
                mFloatingTabBar.invalidateBackdrop();

                int dy = scrollY - oldScrollY;
                int childH = (mScrollView.getChildAt(0) != null) ? mScrollView.getChildAt(0).getHeight() : 0;
                int maxScroll = childH - mScrollView.getHeight();

                if (scrollY <= ThemeTokens.dp(this, 30)) {
                    setFloatingBarVisible(true);
                } else if (maxScroll > 0 && scrollY >= maxScroll - ThemeTokens.dp(this, 40)) {
                    setFloatingBarVisible(true);
                } else if (dy > ThemeTokens.dp(this, 8)) {
                    setFloatingBarVisible(false);
                } else if (dy < -ThemeTokens.dp(this, 4)) {
                    setFloatingBarVisible(true);
                }
            });
        }

        setContentView(root);
        refreshDirectory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDirectoryWatcher != null && "files".equals(mCurrentTab) && !mIsSelectionMode) {
            mDirectoryWatcher.setPaused(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mDirectoryWatcher != null) {
            mDirectoryWatcher.setPaused(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDirectoryWatcher != null) {
            mDirectoryWatcher.destroy();
        }
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
        bg.setColor(ThemeTokens.getActionGroupBgColor(this));
        bg.setStroke(ThemeTokens.dp(this, 1), ThemeTokens.getStrokeColor(this));
        bar.setBackground(bg);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setVisibility(View.GONE);

        mSelectionCountText = new TextView(this);
        mSelectionCountText.setText("Đã chọn 0 mục");
        mSelectionCountText.setTextColor(ThemeTokens.getLabelColor(this));
        mSelectionCountText.setTextSize(15);
        mSelectionCountText.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bar.addView(mSelectionCountText, countLp);

        
        ImageView btnMore = createBarIcon(R.drawable.ic_more_dots, ThemeTokens.getAccentColor(this), v -> showBatchOptionsSheet());
        bar.addView(btnMore);

        
        ImageView btnZip = createBarIcon(R.drawable.ic_file_archive, ThemeTokens.getAccentColor(this), v -> compressSelected());
        bar.addView(btnZip);

        
        ImageView btnDel = createBarIcon(R.drawable.ic_delete, ThemeTokens.COLOR_DANGER, v -> showBatchDeleteConfirmSheet());
        bar.addView(btnDel);

        
        TextView btnDone = new TextView(this);
        btnDone.setText("Xong");
        btnDone.setTextColor(ThemeTokens.getAccentColor(this));
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
        if (mDirectoryWatcher != null) {
            mDirectoryWatcher.setPaused(true);
        }
        mSelectedPaths.clear();
        vibrateLight();
        mSelectionBar.setVisibility(View.VISIBLE);
        updateSelectionCount();
        refreshDirectory();
    }

    private void exitSelectionMode() {
        mIsSelectionMode = false;
        if (mDirectoryWatcher != null) {
            mDirectoryWatcher.setPaused(false);
        }
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

    private void setFloatingBarVisible(boolean visible) {
        if (mIsBarVisible == visible || mFloatingTabBar == null) return;
        mIsBarVisible = visible;

        boolean reduceMotion = false;
        try {
            float scale = Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
            if (scale == 0f) reduceMotion = true;
        } catch (Throwable ignored) {}

        int barHeight = ThemeTokens.dp(this, ThemeTokens.HEIGHT_TAB_BAR) + getNavBarHeight() + ThemeTokens.dp(this, 24);
        float targetY = visible ? 0f : barHeight;
        float targetAlpha = visible ? 1.0f : 0.0f;

        if (reduceMotion) {
            mFloatingTabBar.setTranslationY(targetY);
            mFloatingTabBar.setAlpha(targetAlpha);
            if (mBottomGradientScrim != null) mBottomGradientScrim.setAlpha(targetAlpha);
        } else {
            mFloatingTabBar.animate()
                    .translationY(targetY)
                    .alpha(targetAlpha)
                    .setDuration(250)
                    .setInterpolator(visible ? new OvershootInterpolator(1.05f) : new AccelerateInterpolator())
                    .start();
            if (mBottomGradientScrim != null) {
                mBottomGradientScrim.animate().alpha(targetAlpha).setDuration(250).start();
            }
        }
    }

    private void updateScrollPadding(boolean pathVisible) {
        if (mScrollContent == null) return;
        int topPadding = getStatusBarHeight() + ThemeTokens.dp(this, pathVisible ? 144 : 68);
        int bottomPadding = getNavBarHeight() + ThemeTokens.dp(this, 112);
        mScrollContent.setPadding(ThemeTokens.dp(this, 16), topPadding, ThemeTokens.dp(this, 16), bottomPadding);
        if (mScrollView != null) {
            mScrollView.setIndicatorTopBase(topPadding);
        }
    }

    private void switchTab(String tabKey) {
        mCurrentTab = tabKey;
        setFloatingBarVisible(true);
        boolean isFiles = "files".equals(tabKey);
        if (mHeaderView != null) {
            mHeaderView.setNewItemButtonVisibility(isFiles);
            mHeaderView.setPathVisibility(isFiles);
            if (isFiles) {
                mHeaderView.setTitle("VELQOR");
            } else if ("archive".equals(tabKey)) {
                mHeaderView.setTitle("Nén");
            } else if ("settings".equals(tabKey)) {
                mHeaderView.setTitle("Cài đặt");
            }
        }
        updateScrollPadding(isFiles);
        if (isFiles) {
            refreshDirectory();
            if (mDirectoryWatcher != null) {
                mDirectoryWatcher.watchDirectory(mCurrentDir, mShowHidden, mSortField, mSortAsc);
            }
        } else {
            if (mDirectoryWatcher != null) {
                mDirectoryWatcher.stop();
            }
            if ("archive".equals(tabKey)) {
                showArchiveTab();
            } else if ("settings".equals(tabKey)) {
                showSettingsTab();
            }
        }
    }

    private void onDirectoryContentsChanged(List<File> files) {
        if (!"files".equals(mCurrentTab) || mIsSelectionMode || mIsSearchActive) return;
        final int scrollY = mScrollView.getScrollY();
        populateFileList(files);
        mScrollView.post(() -> mScrollView.scrollTo(0, scrollY));
    }

    private void refreshDirectory() {
        mIsSearchActive = false;
        mHeaderView.updateBreadcrumbs(mCurrentDir);
        List<File> files = FileRepository.listFiles(mCurrentDir, mShowHidden, mSortField, mSortAsc);
        populateFileList(files);
        if (mScrollView != null) {
            mScrollView.finishRefresh();
        }
        if (mDirectoryWatcher != null) {
            mDirectoryWatcher.watchDirectory(mCurrentDir, mShowHidden, mSortField, mSortAsc);
        }
    }

    private void populateFileList(List<File> files) {
        mFileListGroup.removeAllViews();
        if (files == null || files.isEmpty()) {
            showEmptyState("Thư mục rỗng", "Không có tệp hoặc thư mục nào ở đây.");
            return;
        }

        FileRowView.OnFileRowClickListener rowListener = new FileRowView.OnFileRowClickListener() {
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
        };

        if (mIsGridMode) {
            mFileListGroup.setBackground(null);
            for (int i = 0; i < files.size(); i += 2) {
                LinearLayout gridRow = new LinearLayout(this);
                gridRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rlp.bottomMargin = ThemeTokens.dp(this, 8);
                gridRow.setLayoutParams(rlp);

                File f1 = files.get(i);
                boolean isSel1 = mSelectedPaths.contains(f1.getAbsolutePath());
                View card1 = FileRowView.createGridCard(this, f1, isSel1, mIsSelectionMode, rowListener);
                LinearLayout.LayoutParams c1Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                c1Lp.rightMargin = ThemeTokens.dp(this, 4);
                gridRow.addView(card1, c1Lp);

                if (i + 1 < files.size()) {
                    File f2 = files.get(i + 1);
                    boolean isSel2 = mSelectedPaths.contains(f2.getAbsolutePath());
                    View card2 = FileRowView.createGridCard(this, f2, isSel2, mIsSelectionMode, rowListener);
                    LinearLayout.LayoutParams c2Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    c2Lp.leftMargin = ThemeTokens.dp(this, 4);
                    gridRow.addView(card2, c2Lp);
                } else {
                    View dummy = new View(this);
                    LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(0, 1, 1f);
                    dLp.leftMargin = ThemeTokens.dp(this, 4);
                    gridRow.addView(dummy, dLp);
                }
                mFileListGroup.addView(gridRow);
            }
        } else {
            GradientDrawable groupBg = new GradientDrawable();
            groupBg.setCornerRadius(ThemeTokens.dp(this, ThemeTokens.RADIUS_GROUP_LIST));
            groupBg.setColor(ThemeTokens.getActionGroupBgColor(this));
            groupBg.setStroke(ThemeTokens.dp(this, 1), ThemeTokens.getStrokeColor(this));
            mFileListGroup.setBackground(groupBg);

            for (int i = 0; i < files.size(); i++) {
                File f = files.get(i);
                boolean isLast = (i == files.size() - 1);
                boolean isSelected = mSelectedPaths.contains(f.getAbsolutePath());
                View row = FileRowView.createRow(this, f, isLast, isSelected, mIsSelectionMode, rowListener);
                mFileListGroup.addView(row);
            }
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
        icon.setColorFilter(ThemeTokens.getSecondaryLabelColor(this));
        icon.setAlpha(0.35f);
        icon.setLayoutParams(new LinearLayout.LayoutParams(ThemeTokens.dp(this, 64), ThemeTokens.dp(this, 64)));
        emptyLayout.addView(icon);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(ThemeTokens.getLabelColor(this));
        titleView.setTextSize(17);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = ThemeTokens.dp(this, 16);
        titleView.setLayoutParams(tlp);
        emptyLayout.addView(titleView);

        TextView subView = new TextView(this);
        subView.setText(subtitle);
        subView.setTextColor(ThemeTokens.getSecondaryLabelColor(this));
        subView.setTextSize(14);
        subView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = ThemeTokens.dp(this, 6);
        subView.setLayoutParams(slp);
        emptyLayout.addView(subView);

        mFileListGroup.addView(emptyLayout);
    }

    
    
    
    private Dialog createManagedDialog() {
        Dialog dialog = IosSheet.createBaseDialog(this);
        if (mDirectoryWatcher != null) {
            mDirectoryWatcher.setPaused(true);
            dialog.setOnDismissListener(d -> {
                if (mDirectoryWatcher != null && "files".equals(mCurrentTab) && !mIsSelectionMode) {
                    mDirectoryWatcher.setPaused(false);
                }
            });
        }
        return dialog;
    }

    private void showFileActionSheet(File f) {
        vibrateLight();
        Dialog dialog = createManagedDialog();
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        
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
        nameText.setTextColor(ThemeTokens.getLabelColor(this));
        nameText.setTextSize(17);
        nameText.setTypeface(Typeface.DEFAULT_BOLD);
        nameText.setSingleLine(true);
        textCol.addView(nameText);

        TextView metaText = new TextView(this);
        String meta = f.isDirectory() ? ("Thư mục · " + FileRowView.formatItemDate(f.lastModified()))
                                     : (FileRowView.formatSize(f.length()) + " · " + FileRowView.formatItemDate(f.lastModified()));
        metaText.setText(meta);
        metaText.setTextColor(ThemeTokens.getSecondaryLabelColor(this));
        metaText.setTextSize(13);
        textCol.addView(metaText);

        header.addView(textCol, colLp);
        sheet.addView(header);

        
        View sep = new View(this);
        sep.setBackgroundColor(ThemeTokens.getSeparatorInsetColor(this));
        sep.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, ThemeTokens.dp(this, 1) / 2)));
        sheet.addView(sep);

        
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);

        
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

        LinearLayout group3 = IosSheet.createActionGroup(this);
        boolean isArch = ZipManager.isArchive(f);
        group3.addView(IosSheet.createActionRow(this, R.drawable.ic_file_archive, "Nén thành ZIP", false, v -> {
            dialog.dismiss();
            compressSingleToZip(f);
        }, isArch));

        if (isArch) {
            group3.addView(IosSheet.createActionRow(this, R.drawable.ic_unzip, "Giải nén tại đây", false, v -> {
                dialog.dismiss();
                extractZipAsync(f, mCurrentDir);
            }, true));
            group3.addView(IosSheet.createActionRow(this, R.drawable.ic_folder_plus, "Giải nén vào thư mục riêng", false, v -> {
                dialog.dismiss();
                extractZipToSeparateDir(f);
            }, false));
        }
        scrollContent.addView(group3);

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
        Dialog dialog = createManagedDialog();
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText(f.isDirectory() ? "Chi tiết thư mục" : "Chi tiết tệp tin");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.getLabelColor(this));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        LinearLayout card = IosSheet.createActionGroup(this);
        card.setPadding(ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 14),
                ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 14));

        addDetailItem(card, "Tên", f.getName());
        addDetailItem(card, "Đường dẫn", f.getAbsolutePath());

        if (f.isDirectory()) {
            TextView tvSize = addDetailItem(card, "Kích thước", "Đang tính toán...");
            int directCount = 0;
            try {
                String[] list = f.list();
                if (list != null) directCount = list.length;
            } catch (Exception ignored) {}
            TextView tvContent = addDetailItem(card, "Nội dung", directCount + " mục trực tiếp");
            addDetailItem(card, "Sửa đổi lần cuối", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date(f.lastModified())));
            addDetailItem(card, "Loại tệp", "Thư mục");
            addDetailItem(card, "Quyền", (f.canRead() ? "Đọc " : "") + (f.canWrite() ? "Ghi " : ""));

            
            new Thread(() -> {
                FileRepository.FolderInfo info = FileRepository.calculateFolderInfo(f);
                runOnUiThread(() -> {
                    if (dialog.isShowing()) {
                        tvSize.setText(FileRowView.formatSize(info.totalBytes) + " (" + String.format(Locale.getDefault(), "%,d", info.totalBytes) + " bytes)");
                        tvContent.setText(String.format(Locale.getDefault(), "%d tệp, %d thư mục con", info.fileCount, info.dirCount));
                    }
                });
            }).start();
        } else {
            addDetailItem(card, "Kích thước", FileRowView.formatSize(f.length()) + " (" + String.format(Locale.getDefault(), "%,d", f.length()) + " bytes)");
            addDetailItem(card, "Sửa đổi lần cuối", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date(f.lastModified())));
            addDetailItem(card, "Loại tệp", getMimeType(f));
            addDetailItem(card, "Quyền", (f.canRead() ? "Đọc " : "") + (f.canWrite() ? "Ghi " : ""));
        }

        sheet.addView(card);

        TextView btnClose = new TextView(this);
        btnClose.setText("Đóng");
        btnClose.setTextColor(ThemeTokens.getAccentColor(this));
        btnClose.setTextSize(17);
        btnClose.setTypeface(Typeface.DEFAULT_BOLD);
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(this, 48)));
        btnClose.setOnClickListener(v -> dialog.dismiss());
        sheet.addView(btnClose);

        IosSheet.show(dialog, sheet);
    }

    private TextView addDetailItem(LinearLayout container, String key, String val) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, ThemeTokens.dp(this, 4), 0, ThemeTokens.dp(this, 4));

        TextView k = new TextView(this);
        k.setText(key);
        k.setTextColor(ThemeTokens.getSecondaryLabelColor(this));
        k.setTextSize(12);
        row.addView(k);

        TextView v = new TextView(this);
        v.setText(val);
        v.setTextColor(ThemeTokens.getLabelColor(this));
        v.setTextSize(14);
        row.addView(v);

        container.addView(row);
        return v;
    }

    private void showSortActionSheet() {
        Dialog dialog = createManagedDialog();
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Sắp xếp theo");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.getLabelColor(this));
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
            tv.setTextColor(isChosen ? ThemeTokens.getAccentColor(this) : ThemeTokens.getLabelColor(this));
            tv.setTypeface(isChosen ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(tv, tlp);

            if (isChosen) {
                ImageView check = new ImageView(this);
                check.setImageResource(R.drawable.ic_check);
                check.setColorFilter(ThemeTokens.getAccentColor(this));
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
                div.setBackgroundColor(ThemeTokens.getSeparatorInsetColor(this));
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
        btnCancel.setTextColor(ThemeTokens.getAccentColor(this));
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
        Dialog dialog = createManagedDialog();
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Tìm kiếm trong thư mục");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.getLabelColor(this));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        EditText input = new EditText(this);
        input.setHint("Nhập tên tệp hoặc thư mục");
        input.setHintTextColor(ThemeTokens.getSecondaryLabelColor(this));
        input.setTextColor(ThemeTokens.getLabelColor(this));
        input.setTextSize(16);
        input.setPadding(ThemeTokens.dp(this, 14), ThemeTokens.dp(this, 12),
                ThemeTokens.dp(this, 14), ThemeTokens.dp(this, 12));

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(ThemeTokens.dp(this, ThemeTokens.RADIUS_INPUT_FIELD));
        inputBg.setColor(ThemeTokens.getActionGroupBgColor(this));
        inputBg.setStroke(ThemeTokens.dp(this, 1), ThemeTokens.getStrokeColor(this));
        input.setBackground(inputBg);

        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.topMargin = ThemeTokens.dp(this, 16);
        sheet.addView(input, inLp);

        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        hScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams hslp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hslp.topMargin = ThemeTokens.dp(this, 12);
        hScroll.setLayoutParams(hslp);

        LinearLayout chipsContainer = new LinearLayout(this);
        chipsContainer.setOrientation(LinearLayout.HORIZONTAL);
        hScroll.addView(chipsContainer);
        sheet.addView(hScroll);

        String[] filters = {"Tất cả", "Ảnh", "Video", "Tài liệu", "Kho nén", "Lớn >10MB"};
        final String[] selectedFilter = {"Tất cả"};
        final List<TextView> chipViews = new ArrayList<>();

        for (String filter : filters) {
            TextView chip = new TextView(this);
            chip.setText(filter);
            chip.setTextSize(13);
            chip.setPadding(ThemeTokens.dp(this, 14), ThemeTokens.dp(this, 6),
                    ThemeTokens.dp(this, 14), ThemeTokens.dp(this, 6));

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setCornerRadius(ThemeTokens.dp(this, 999));
            boolean isSel = filter.equals(selectedFilter[0]);
            chipBg.setColor(isSel ? ThemeTokens.getAccentColor(this) : ThemeTokens.getActionGroupBgColor(this));
            chipBg.setStroke(ThemeTokens.dp(this, 1), isSel ? ThemeTokens.getAccentColor(this) : ThemeTokens.getStrokeColor(this));
            chip.setBackground(chipBg);
            chip.setTextColor(isSel ? Color.WHITE : ThemeTokens.getSecondaryLabelColor(this));

            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.rightMargin = ThemeTokens.dp(this, 8);
            chip.setLayoutParams(clp);

            chip.setOnClickListener(v -> {
                selectedFilter[0] = filter;
                for (int i = 0; i < filters.length; i++) {
                    TextView cv = chipViews.get(i);
                    boolean s = filters[i].equals(selectedFilter[0]);
                    GradientDrawable bg = (GradientDrawable) cv.getBackground();
                    bg.setColor(s ? ThemeTokens.getAccentColor(this) : ThemeTokens.getActionGroupBgColor(this));
                    bg.setStroke(ThemeTokens.dp(this, 1), s ? ThemeTokens.getAccentColor(this) : ThemeTokens.getStrokeColor(this));
                    cv.setTextColor(s ? Color.WHITE : ThemeTokens.getSecondaryLabelColor(this));
                }
            });

            chipViews.add(chip);
            chipsContainer.addView(chip);
        }

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = ThemeTokens.dp(this, 16);
        btnRow.setLayoutParams(rlp);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("Hủy");
        btnCancel.setTextColor(ThemeTokens.getSecondaryLabelColor(this));
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
        sBg.setColor(ThemeTokens.getAccentColor(this));
        btnSearch.setBackground(sBg);
        btnSearch.setOnClickListener(v -> {
            String query = input.getText().toString().trim();
            dialog.dismiss();
            performSearch(query, selectedFilter[0]);
        });
        btnRow.addView(btnSearch);
        sheet.addView(btnRow);

        IosSheet.show(dialog, sheet, true);
        input.requestFocus();
    }

    private void performSearch(String query, String filterType) {
        mIsSearchActive = true;
        mFileListGroup.removeAllViews();
        List<File> matched = FileRepository.search(mCurrentDir, query, 150);

        List<File> filtered = new ArrayList<>();
        for (File f : matched) {
            String name = f.getName().toLowerCase();
            if ("Tất cả".equals(filterType)) {
                filtered.add(f);
            } else if ("Ảnh".equals(filterType)) {
                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                    name.endsWith(".webp") || name.endsWith(".gif") || name.endsWith(".bmp")) {
                    filtered.add(f);
                }
            } else if ("Video".equals(filterType)) {
                if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") || name.endsWith(".mov")) {
                    filtered.add(f);
                }
            } else if ("Tài liệu".equals(filterType)) {
                if (name.endsWith(".pdf") || name.endsWith(".txt") || name.endsWith(".doc") ||
                    name.endsWith(".docx") || name.endsWith(".md") || name.endsWith(".json")) {
                    filtered.add(f);
                }
            } else if ("Kho nén".equals(filterType)) {
                if (ZipManager.isArchive(f)) {
                    filtered.add(f);
                }
            } else if ("Lớn >10MB".equals(filterType)) {
                if (f.length() >= 10L * 1024 * 1024) {
                    filtered.add(f);
                }
            }
        }

        if (filtered.isEmpty()) {
            showEmptyState("Không tìm thấy kết quả", "Không có tệp nào khớp với từ khóa \"" + query + "\" [" + filterType + "].");
            return;
        }

        GradientDrawable groupBg = new GradientDrawable();
        groupBg.setCornerRadius(ThemeTokens.dp(this, ThemeTokens.RADIUS_GROUP_LIST));
        groupBg.setColor(ThemeTokens.getActionGroupBgColor(this));
        groupBg.setStroke(ThemeTokens.dp(this, 1), ThemeTokens.getStrokeColor(this));
        mFileListGroup.setBackground(groupBg);

        for (int i = 0; i < filtered.size(); i++) {
            File f = filtered.get(i);
            boolean isLast = (i == filtered.size() - 1);
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
        Dialog dialog = createManagedDialog();
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Tùy chọn VELQOR");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.getLabelColor(this));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        
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
        Dialog dialog = createManagedDialog();
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Đã chọn " + mSelectedPaths.size() + " mục");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.getLabelColor(this));
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

    private void showArchiveTab() {
        mFileListGroup.removeAllViews();
        mFileListGroup.setBackground(null);

        LinearLayout heroCard = IosSheet.createActionGroup(this);
        heroCard.setPadding(ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 16),
                ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 16));
        heroCard.setOrientation(LinearLayout.VERTICAL);

        LinearLayout heroRow = new LinearLayout(this);
        heroRow.setOrientation(LinearLayout.HORIZONTAL);
        heroRow.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout iconBox = new FrameLayout(this);
        int boxSize = ThemeTokens.dp(this, 44);
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(boxSize, boxSize));
        GradientDrawable iconBoxBg = new GradientDrawable();
        iconBoxBg.setCornerRadius(ThemeTokens.dp(this, 12));
        int accent = ThemeTokens.getAccentColor(this);
        iconBoxBg.setColor((accent & 0x00FFFFFF) | 0x2E000000);
        iconBox.setBackground(iconBoxBg);

        ImageView heroIcon = new ImageView(this);
        heroIcon.setImageResource(R.drawable.ic_file_archive);
        heroIcon.setColorFilter(accent);
        int insideSize = ThemeTokens.dp(this, 24);
        FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(insideSize, insideSize);
        ivLp.gravity = Gravity.CENTER;
        heroIcon.setLayoutParams(ivLp);
        iconBox.addView(heroIcon);
        heroRow.addView(iconBox);

        LinearLayout heroTextCol = new LinearLayout(this);
        heroTextCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tclp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tclp.leftMargin = ThemeTokens.dp(this, 12);

        TextView title = new TextView(this);
        title.setText("Kho lưu trữ & Tệp nén");
        title.setTextSize(17);
        title.setTextColor(ThemeTokens.getLabelColor(this));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        heroTextCol.addView(title);

        TextView info = new TextView(this);
        info.setText("Duyệt nội dung tệp nén ZIP, TAR, GZ trực tiếp không cần giải nén.");
        info.setTextColor(ThemeTokens.getSecondaryLabelColor(this));
        info.setTextSize(12);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = ThemeTokens.dp(this, 2);
        info.setLayoutParams(subLp);
        heroTextCol.addView(info);

        heroRow.addView(heroTextCol, tclp);
        heroCard.addView(heroRow);
        mFileListGroup.addView(heroCard);

        LinearLayout listCard = IosSheet.createActionGroup(this);
        LinearLayout.LayoutParams lclp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lclp.topMargin = ThemeTokens.dp(this, 14);
        listCard.setLayoutParams(lclp);

        TextView loading = new TextView(this);
        loading.setText("Đang tìm tệp nén trên thiết bị...");
        loading.setTextColor(ThemeTokens.getSecondaryLabelColor(this));
        loading.setTextSize(14);
        loading.setPadding(ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 16),
                ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 16));
        listCard.addView(loading);
        mFileListGroup.addView(listCard);

        new Thread(() -> {
            List<File> archives = new ArrayList<>();
            File root = Environment.getExternalStorageDirectory();
            scanArchives(root, archives, 30);
            scanArchives(new File(root, "Download"), archives, 30);
            scanArchives(new File(root, "Documents"), archives, 30);

            Set<String> uniquePaths = new HashSet<>();
            List<File> distinctArchives = new ArrayList<>();
            for (File a : archives) {
                if (uniquePaths.add(a.getAbsolutePath())) {
                    distinctArchives.add(a);
                }
            }

            runOnUiThread(() -> {
                listCard.removeAllViews();
                if (distinctArchives.isEmpty()) {
                    TextView emptyTv = new TextView(this);
                    emptyTv.setText("Chưa tìm thấy tệp nén nào trong máy.");
                    emptyTv.setTextColor(ThemeTokens.getSecondaryLabelColor(this));
                    emptyTv.setTextSize(14);
                    emptyTv.setPadding(ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 16),
                            ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 16));
                    listCard.addView(emptyTv);
                } else {
                    for (int i = 0; i < distinctArchives.size(); i++) {
                        File a = distinctArchives.get(i);
                        boolean isLast = (i == distinctArchives.size() - 1);
                        listCard.addView(IosSheet.createActionRow(this, R.drawable.ic_file_archive,
                                a.getName() + " (" + FileRowView.formatSize(a.length()) + ")", false, v -> {
                            showArchiveInspectorSheet(a);
                        }, !isLast));
                    }
                }
            });
        }).start();
    }

    private void scanArchives(File dir, List<File> result, int max) {
        if (dir == null || !dir.exists() || result.size() >= max) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (result.size() >= max) break;
            if (f.isFile() && ZipManager.isArchive(f)) {
                result.add(f);
            }
        }
    }

    private void showArchiveInspectorSheet(File archiveFile) {
        Dialog dialog = createManagedDialog();
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText(archiveFile.getName());
        title.setTextSize(18);
        title.setTextColor(ThemeTokens.getLabelColor(this));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Kích thước: " + FileRowView.formatSize(archiveFile.length()) + " · Đang nạp danh mục...");
        sub.setTextColor(ThemeTokens.getSecondaryLabelColor(this));
        sub.setTextSize(12);
        sheet.addView(sub);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams brlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        brlp.topMargin = ThemeTokens.dp(this, 12);
        btnRow.setLayoutParams(brlp);

        TextView btnExtractAll = new TextView(this);
        btnExtractAll.setText("Giải nén toàn bộ");
        btnExtractAll.setTextColor(Color.WHITE);
        btnExtractAll.setTextSize(14);
        btnExtractAll.setTypeface(Typeface.DEFAULT_BOLD);
        btnExtractAll.setGravity(Gravity.CENTER);
        btnExtractAll.setPadding(ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 8),
                ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 8));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(ThemeTokens.dp(this, 999));
        bg.setColor(ThemeTokens.getAccentColor(this));
        btnExtractAll.setBackground(bg);
        btnExtractAll.setOnClickListener(v -> {
            dialog.dismiss();
            extractZipAsync(archiveFile, mCurrentDir);
        });
        btnRow.addView(btnExtractAll);
        sheet.addView(btnRow);

        ScrollView sv = new ScrollView(this);
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(this, 320));
        svLp.topMargin = ThemeTokens.dp(this, 12);
        sv.setLayoutParams(svLp);

        LinearLayout entriesCard = IosSheet.createActionGroup(this);
        sv.addView(entriesCard);
        sheet.addView(sv);

        new Thread(() -> {
            try {
                List<ZipManager.ArchiveEntryInfo> list = ZipManager.listZipEntries(archiveFile);
                runOnUiThread(() -> {
                    sub.setText("Tổng cộng " + list.size() + " mục bên trong tệp nén");
                    entriesCard.removeAllViews();
                    for (int i = 0; i < list.size(); i++) {
                        ZipManager.ArchiveEntryInfo entry = list.get(i);
                        boolean isLast = (i == list.size() - 1);
                        int icon = entry.isDirectory ? R.drawable.ic_folder : R.drawable.ic_file_doc;
                        String label = entry.name + (entry.isDirectory ? "" : " (" + FileRowView.formatSize(entry.size) + ")");
                        entriesCard.addView(IosSheet.createActionRow(this, icon, label, false, v -> {
                            if (!entry.isDirectory) {
                                IosSheet.showConfirmDialog(this, "Giải nén tệp", "Bạn có muốn trích xuất '" + entry.name + "' ra thư mục hiện tại không?", "Giải nén", () -> {
                                    File destFile = new File(mCurrentDir, new File(entry.name).getName());
                                    new Thread(() -> {
                                        try {
                                            boolean ok = ZipManager.extractSingleZipEntry(archiveFile, entry.name, destFile);
                                            runOnUiThread(() -> {
                                                if (ok) {
                                                    Toast.makeText(this, "Đã trích xuất: " + destFile.getName(), Toast.LENGTH_SHORT).show();
                                                    refreshDirectory();
                                                } else {
                                                    Toast.makeText(this, "Không thể trích xuất tệp", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } catch (Exception ex) {
                                            runOnUiThread(() -> Toast.makeText(this, "Lỗi trích xuất: " + ex.getMessage(), Toast.LENGTH_SHORT).show());
                                        }
                                    }).start();
                                });
                            }
                        }, !isLast));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> sub.setText("Không thể đọc danh mục: " + e.getMessage()));
            }
        }).start();

        IosSheet.show(dialog, sheet);
    }

    private void showSettingsTab() {
        mFileListGroup.removeAllViews();
        mFileListGroup.setBackground(null);

        SharedPreferences prefs = getSharedPreferences("velqor_prefs", MODE_PRIVATE);
        String themeMode = prefs.getString("theme_mode", "dark");
        int accentColor = ThemeTokens.getAccentColor(this);

        View profileCard = SettingsViewHelper.createProfileCard(this, "VELQOR PRO", "v2.1.1 (Build 16)", v -> showAboutSheet());
        mFileListGroup.addView(profileCard);

        mFileListGroup.addView(SettingsViewHelper.createGroupHeader(this, "Giao diện"));
        LinearLayout gTheme = SettingsViewHelper.createGroupCard(this);

        String modeText = "dark".equals(themeMode) ? "Tối" : ("light".equals(themeMode) ? "Sáng" : "Tự động");
        gTheme.addView(SettingsViewHelper.createRow(this, R.drawable.ic_eye, 0xFF5856D6,
                "Chế độ giao diện", modeText, true, v -> showThemePickerSheet(), true));

        String accentName = getAccentName(accentColor);
        gTheme.addView(SettingsViewHelper.createRow(this, R.drawable.ic_star, accentColor,
                "Màu nhấn Apple", accentName, true, v -> showAccentPickerSheet(), true));

        String viewModeText = mIsGridMode ? "Lưới (Grid)" : "Danh sách (List)";
        gTheme.addView(SettingsViewHelper.createRow(this, mIsGridMode ? R.drawable.ic_view_list : R.drawable.ic_view_grid,
                0xFF007AFF, "Kiểu hiển thị", viewModeText, true, v -> {
                    mIsGridMode = !mIsGridMode;
                    prefs.edit().putBoolean("grid_mode", mIsGridMode).apply();
                    mHeaderView.setViewMode(mIsGridMode);
                    showSettingsTab();
                }, true));

        gTheme.addView(SettingsViewHelper.createSwitchRow(this, R.drawable.ic_eye, 0xFF8E8E93,
                "Hiện tệp ẩn", mShowHidden, (switchView, isChecked) -> {
                    mShowHidden = isChecked;
                    prefs.edit().putBoolean("show_hidden", mShowHidden).apply();
                    if (mDirectoryWatcher != null) {
                        mDirectoryWatcher.updateFilterSort(mShowHidden, mSortField, mSortAsc);
                    }
                }, false));
        mFileListGroup.addView(gTheme);

        mFileListGroup.addView(SettingsViewHelper.createGroupHeader(this, "Lưu trữ"));
        LinearLayout gStorage = SettingsViewHelper.createGroupCard(this);

        long trashBytes = TrashManager.getTrashSize(this);
        int trashCount = TrashManager.listTrashItems(this).size();
        gStorage.addView(SettingsViewHelper.createRow(this, R.drawable.ic_delete, 0xFFFF3B30,
                "Thùng rác", trashCount + " mục · " + FileRowView.formatSize(trashBytes), true, v -> showTrashSheet(), true));

        gStorage.addView(SettingsViewHelper.createRow(this, R.drawable.ic_info, 0xFFFF9500,
                "Phân tích dung lượng", "Xem chi tiết", true, v -> showStorageDashboardSheet(), false));
        mFileListGroup.addView(gStorage);

        mFileListGroup.addView(SettingsViewHelper.createGroupHeader(this, "Hiệu năng"));
        LinearLayout gPerf = SettingsViewHelper.createGroupCard(this);

        SharedPreferences editorPrefs = getSharedPreferences("velqor_editor_prefs", MODE_PRIVATE);
        boolean showThumbs = editorPrefs.getBoolean("show_thumbnails", true);

        gPerf.addView(SettingsViewHelper.createSwitchRow(this, R.drawable.ic_file_image, 0xFF34C759,
                "Hiển thị ảnh thu nhỏ (Thumbnail)", showThumbs, (switchView, isChecked) -> {
                    editorPrefs.edit().putBoolean("show_thumbnails", isChecked).apply();
                }, true));

        long cacheBytes = com.velqor.app.files.ThumbnailCache.getInstance(this).getDiskCacheSize();
        gPerf.addView(SettingsViewHelper.createRow(this, R.drawable.ic_delete, 0xFFFF2D55,
                "Bộ nhớ đệm thumbnail", FileRowView.formatSize(cacheBytes), true, v -> {
                    IosSheet.showConfirmDialog(this, "Xóa bộ nhớ đệm",
                            "Dung lượng hiện tại: " + FileRowView.formatSize(cacheBytes) + ".\nBạn có chắc muốn xóa sạch toàn bộ thumbnail đã lưu tạm?",
                            "Xóa bộ nhớ đệm", () -> {
                                com.velqor.app.files.ThumbnailCache.getInstance(this).clear();
                                Toast.makeText(this, "Đã xóa sạch bộ nhớ đệm thumbnail", Toast.LENGTH_SHORT).show();
                                showSettingsTab();
                            });
                }, false));
        mFileListGroup.addView(gPerf);

        mFileListGroup.addView(SettingsViewHelper.createGroupHeader(this, "Thông tin"));
        LinearLayout gInfo = SettingsViewHelper.createGroupCard(this);

        gInfo.addView(SettingsViewHelper.createRow(this, R.drawable.ic_info, 0xFF8E8E93,
                "Phiên bản", "v2.1.1 (Build 16)", true, v -> showAboutSheet(), true));

        gInfo.addView(SettingsViewHelper.createRow(this, R.drawable.ic_star, 0xFF007AFF,
                "Giới thiệu VELQOR", "", true, v -> showAboutSheet(), false));
        mFileListGroup.addView(gInfo);
    }

    private String getAccentName(int color) {
        if (color == ThemeTokens.ACCENT_BLUE) return "Apple Blue";
        if (color == ThemeTokens.ACCENT_INDIGO) return "Cupertino Indigo";
        if (color == ThemeTokens.ACCENT_PURPLE) return "Deep Purple";
        if (color == ThemeTokens.ACCENT_ORANGE) return "Vibrant Orange";
        if (color == ThemeTokens.ACCENT_MINT) return "Mint Cyan";
        if (color == ThemeTokens.ACCENT_RED) return "Imperial Red";
        if (color == ThemeTokens.ACCENT_GREEN) return "Emerald Green";
        return "Apple Blue";
    }

    private void showThemePickerSheet() {
        Dialog dialog = createManagedDialog();
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Chế độ giao diện");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.getLabelColor(this));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        LinearLayout card = IosSheet.createActionGroup(this);
        String[] modes = {"dark", "light", "auto"};
        String[] labels = {"Tối (Dark)", "Sáng (Light)", "Tự động (Theo hệ thống)"};
        SharedPreferences prefs = getSharedPreferences("velqor_prefs", MODE_PRIVATE);
        String currentMode = prefs.getString("theme_mode", "dark");

        for (int i = 0; i < modes.length; i++) {
            final String m = modes[i];
            boolean isChosen = currentMode.equals(m);
            boolean isLast = (i == modes.length - 1);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(ThemeTokens.dp(this, 16), 0, ThemeTokens.dp(this, 16), 0);
            row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(this, 48)));

            TextView tv = new TextView(this);
            tv.setText(labels[i]);
            tv.setTextSize(16);
            tv.setTextColor(isChosen ? ThemeTokens.getAccentColor(this) : ThemeTokens.getLabelColor(this));
            tv.setTypeface(isChosen ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            if (isChosen) {
                ImageView check = new ImageView(this);
                check.setImageResource(R.drawable.ic_check);
                check.setColorFilter(ThemeTokens.getAccentColor(this));
                int checkSz = ThemeTokens.dp(this, 18);
                row.addView(check, new LinearLayout.LayoutParams(checkSz, checkSz));
            }

            row.setOnClickListener(v -> {
                prefs.edit().putString("theme_mode", m).apply();
                dialog.dismiss();
                showSettingsTab();
            });

            card.addView(row);
            if (!isLast) {
                View div = new View(this);
                div.setBackgroundColor(ThemeTokens.getSeparatorInsetColor(this));
                int divH = Math.max(1, ThemeTokens.dp(this, 1) / 2);
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, divH);
                dlp.leftMargin = ThemeTokens.dp(this, 16);
                card.addView(div, dlp);
            }
        }
        sheet.addView(card);
        dialog.show();
    }

    private void showAboutSheet() {
        Dialog dialog = createManagedDialog();
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        LinearLayout card = IosSheet.createActionGroup(this);
        card.setPadding(ThemeTokens.dp(this, 20), ThemeTokens.dp(this, 20),
                ThemeTokens.dp(this, 20), ThemeTokens.dp(this, 20));
        card.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout logoBox = new FrameLayout(this);
        int bSize = ThemeTokens.dp(this, 56);
        logoBox.setLayoutParams(new LinearLayout.LayoutParams(bSize, bSize));
        GradientDrawable logoBg = new GradientDrawable();
        logoBg.setCornerRadius(ThemeTokens.dp(this, 14));
        int accent = ThemeTokens.getAccentColor(this);
        logoBg.setColors(new int[]{accent, 0xFF5856D6});
        logoBg.setOrientation(GradientDrawable.Orientation.TL_BR);
        logoBox.setBackground(logoBg);

        TextView letter = new TextView(this);
        letter.setText("V");
        letter.setTextColor(Color.WHITE);
        letter.setTextSize(28);
        letter.setTypeface(Typeface.DEFAULT_BOLD);
        letter.setGravity(Gravity.CENTER);
        logoBox.addView(letter, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        card.addView(logoBox);

        TextView title = new TextView(this);
        title.setText("VELQOR PRO");
        title.setTextColor(ThemeTokens.getLabelColor(this));
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = ThemeTokens.dp(this, 12);
        title.setLayoutParams(tlp);
        card.addView(title);

        TextView ver = new TextView(this);
        ver.setText("Phiên bản v2.1.1 (Build 16) · Apple Liquid Glass");
        ver.setTextColor(ThemeTokens.getSecondaryLabelColor(this));
        ver.setTextSize(13);
        ver.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vlp.topMargin = ThemeTokens.dp(this, 4);
        ver.setLayoutParams(vlp);
        card.addView(ver);

        View div = new View(this);
        div.setBackgroundColor(ThemeTokens.getSeparatorInsetColor(this));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, ThemeTokens.dp(this, 1) / 2));
        divLp.topMargin = ThemeTokens.dp(this, 16);
        divLp.bottomMargin = ThemeTokens.dp(this, 16);
        div.setLayoutParams(divLp);
        card.addView(div);

        TextView info = new TextView(this);
        info.setText("• Thiết kế: Apple iOS Cupertino Native Liquid Glass\n• Hiệu năng: Tự động cập nhật ngầm 3 giây & Kéo làm mới mượt mà\n• Trình duyệt tệp nén: Đọc trực tiếp ZIP không cần giải nén\n• Công nghệ: Java 17 + Android View AOSP Native (0 thư viện ngoài)\n• Bản quyền: Google DeepMind Pair Programming");
        info.setTextColor(ThemeTokens.getSecondaryLabelColor(this));
        info.setTextSize(13);
        info.setLineSpacing(ThemeTokens.dp(this, 4), 1f);
        card.addView(info);
        sheet.addView(card);

        LinearLayout cancelCard = IosSheet.createActionGroup(this);
        TextView btnClose = new TextView(this);
        btnClose.setText("Đóng");
        btnClose.setTextColor(ThemeTokens.getAccentColor(this));
        btnClose.setTextSize(17);
        btnClose.setTypeface(Typeface.DEFAULT_BOLD);
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(this, 52)));
        btnClose.setOnClickListener(v -> dialog.dismiss());
        cancelCard.addView(btnClose);
        sheet.addView(cancelCard);

        dialog.show();
    }

    private void showAccentPickerSheet() {
        Dialog dialog = createManagedDialog();
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Chọn màu nhấn Apple");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.getLabelColor(this));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        LinearLayout card = IosSheet.createActionGroup(this);
        int[] accents = {
                ThemeTokens.ACCENT_BLUE,
                ThemeTokens.ACCENT_INDIGO,
                ThemeTokens.ACCENT_PURPLE,
                ThemeTokens.ACCENT_ORANGE,
                ThemeTokens.ACCENT_MINT,
                ThemeTokens.ACCENT_RED,
                ThemeTokens.ACCENT_GREEN
        };
        String[] names = {
                "Apple Blue",
                "Cupertino Indigo",
                "Deep Purple",
                "Vibrant Orange",
                "Mint Cyan",
                "Imperial Red",
                "Emerald Green"
        };

        int cur = ThemeTokens.getAccentColor(this);
        for (int i = 0; i < accents.length; i++) {
            final int c = accents[i];
            boolean isChosen = (cur == c);
            boolean isLast = (i == accents.length - 1);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(ThemeTokens.dp(this, 16), 0, ThemeTokens.dp(this, 16), 0);
            row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(this, 48)));

            View dot = new View(this);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(c);
            dot.setBackground(d);
            int dotSize = ThemeTokens.dp(this, 16);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dotSize, dotSize);
            dlp.rightMargin = ThemeTokens.dp(this, 12);
            row.addView(dot, dlp);

            TextView tv = new TextView(this);
            tv.setText(names[i]);
            tv.setTextSize(16);
            tv.setTextColor(isChosen ? c : ThemeTokens.getLabelColor(this));
            tv.setTypeface(isChosen ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            if (isChosen) {
                ImageView check = new ImageView(this);
                check.setImageResource(R.drawable.ic_check);
                check.setColorFilter(c);
                int checkSz = ThemeTokens.dp(this, 18);
                row.addView(check, new LinearLayout.LayoutParams(checkSz, checkSz));
            }

            row.setOnClickListener(v -> {
                dialog.dismiss();
                getSharedPreferences("velqor_prefs", MODE_PRIVATE).edit().putInt("accent_color", c).apply();
                showSettingsTab();
            });

            card.addView(row);
            if (!isLast) {
                View div = new View(this);
                div.setBackgroundColor(ThemeTokens.getSeparatorInsetColor(this));
                card.addView(div, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(this, 1)));
            }
        }
        sheet.addView(card);
        IosSheet.show(dialog, sheet);
    }

    private void showTrashSheet() {
        Dialog dialog = createManagedDialog();
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Thùng rác VELQOR");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.getLabelColor(this));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        List<TrashManager.TrashItem> items = TrashManager.listTrashItems(this);
        TextView sub = new TextView(this);
        sub.setText("Tổng cộng " + items.size() + " mục · " + FileRowView.formatSize(TrashManager.getTrashSize(this)));
        sub.setTextColor(ThemeTokens.getSecondaryLabelColor(this));
        sub.setTextSize(13);
        sheet.addView(sub);

        if (!items.isEmpty()) {
            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams brlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            brlp.topMargin = ThemeTokens.dp(this, 12);
            btnRow.setLayoutParams(brlp);

            TextView btnEmpty = new TextView(this);
            btnEmpty.setText("Dọn sạch thùng rác");
            btnEmpty.setTextColor(Color.WHITE);
            btnEmpty.setTextSize(14);
            btnEmpty.setTypeface(Typeface.DEFAULT_BOLD);
            btnEmpty.setGravity(Gravity.CENTER);
            btnEmpty.setPadding(ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 8),
                    ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 8));
            GradientDrawable ebg = new GradientDrawable();
            ebg.setCornerRadius(ThemeTokens.dp(this, 999));
            ebg.setColor(ThemeTokens.COLOR_DANGER);
            btnEmpty.setBackground(ebg);
            btnEmpty.setOnClickListener(v -> {
                IosSheet.showConfirmDialog(this, "Xác nhận xóa sạch", "Tất cả các tệp trong thùng rác sẽ bị xóa vĩnh viễn?", "Xóa sạch", () -> {
                    dialog.dismiss();
                    TrashManager.emptyTrash(this);
                    Toast.makeText(this, "Đã dọn sạch thùng rác", Toast.LENGTH_SHORT).show();
                    showSettingsTab();
                });
            });
            btnRow.addView(btnEmpty);
            sheet.addView(btnRow);
        }

        ScrollView sv = new ScrollView(this);
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(this, 300));
        svLp.topMargin = ThemeTokens.dp(this, 12);
        sv.setLayoutParams(svLp);

        LinearLayout card = IosSheet.createActionGroup(this);
        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Thùng rác đang rỗng.");
            empty.setTextColor(ThemeTokens.getSecondaryLabelColor(this));
            empty.setTextSize(14);
            empty.setPadding(ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 16),
                    ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 16));
            card.addView(empty);
        } else {
            for (int i = 0; i < items.size(); i++) {
                TrashManager.TrashItem item = items.get(i);
                boolean isLast = (i == items.size() - 1);
                card.addView(IosSheet.createActionRow(this, R.drawable.ic_file_doc,
                        item.originalName + " (" + FileRowView.formatSize(item.size) + ")", false, v -> {
                    IosSheet.showConfirmDialog(this, "Khôi phục tệp", "Khôi phục '" + item.originalName + "' về thư mục hiện tại?", "Khôi phục", () -> {
                        dialog.dismiss();
                        TrashManager.restoreItem(item, mCurrentDir);
                        Toast.makeText(this, "Đã khôi phục: " + item.originalName, Toast.LENGTH_SHORT).show();
                        showSettingsTab();
                    });
                }, !isLast));
            }
        }
        sv.addView(card);
        sheet.addView(sv);

        IosSheet.show(dialog, sheet);
    }

    private void showStorageDashboardSheet() {
        Dialog dialog = createManagedDialog();
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Dashboard dung lượng");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.getLabelColor(this));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Đang phân tích cấu trúc bộ nhớ...");
        sub.setTextColor(ThemeTokens.getSecondaryLabelColor(this));
        sub.setTextSize(13);
        sheet.addView(sub);

        DonutChartView donut = new DonutChartView(this);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ThemeTokens.dp(this, 180), ThemeTokens.dp(this, 180));
        dlp.gravity = Gravity.CENTER_HORIZONTAL;
        dlp.topMargin = ThemeTokens.dp(this, 14);
        donut.setLayoutParams(dlp);
        sheet.addView(donut);

        ScrollView sv = new ScrollView(this);
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(this, 220));
        svLp.topMargin = ThemeTokens.dp(this, 12);
        sv.setLayoutParams(svLp);

        LinearLayout cardDetails = IosSheet.createActionGroup(this);
        sv.addView(cardDetails);
        sheet.addView(sv);

        new Thread(() -> {
            StorageScanner.StorageBreakdown b = StorageScanner.scanStorage(mCurrentDir, 500);
            runOnUiThread(() -> {
                long total = b.totalBytes;
                long used = b.usedBytes;
                int percent = (total > 0) ? (int) ((used * 100) / total) : 0;
                sub.setText(FileRowView.formatSize(used) + " đã dùng / " + FileRowView.formatSize(total));

                List<DonutChartView.Segment> segs = new ArrayList<>();
                segs.add(new DonutChartView.Segment("Ảnh", b.imagesBytes, ThemeTokens.COLOR_ICON_IMAGE));
                segs.add(new DonutChartView.Segment("Video", b.videosBytes, ThemeTokens.COLOR_ICON_VIDEO));
                segs.add(new DonutChartView.Segment("Nhạc", b.audioBytes, ThemeTokens.COLOR_ICON_AUDIO));
                segs.add(new DonutChartView.Segment("Tài liệu", b.docsBytes, ThemeTokens.COLOR_ICON_DOCUMENT));
                segs.add(new DonutChartView.Segment("APK", b.apksBytes, ThemeTokens.COLOR_ICON_APK));
                segs.add(new DonutChartView.Segment("Kho nén", b.archivesBytes, ThemeTokens.COLOR_ICON_ARCHIVE));
                segs.add(new DonutChartView.Segment("Khác", b.otherBytes, ThemeTokens.COLOR_ICON_OTHER));
                donut.setData(segs, total, percent + "%", "Đã dùng");

                cardDetails.removeAllViews();
                cardDetails.addView(IosSheet.createActionRow(this, R.drawable.ic_file_image,
                        "Ảnh: " + FileRowView.formatSize(b.imagesBytes), false, null, true));
                cardDetails.addView(IosSheet.createActionRow(this, R.drawable.ic_file_video,
                        "Video: " + FileRowView.formatSize(b.videosBytes), false, null, true));
                cardDetails.addView(IosSheet.createActionRow(this, R.drawable.ic_file_audio,
                        "Âm nhạc: " + FileRowView.formatSize(b.audioBytes), false, null, true));
                cardDetails.addView(IosSheet.createActionRow(this, R.drawable.ic_file_doc,
                        "Tài liệu: " + FileRowView.formatSize(b.docsBytes), false, null, true));
                cardDetails.addView(IosSheet.createActionRow(this, R.drawable.ic_file_apk,
                        "File cài APK: " + FileRowView.formatSize(b.apksBytes), false, null, true));
                cardDetails.addView(IosSheet.createActionRow(this, R.drawable.ic_file_archive,
                        "Tệp nén: " + FileRowView.formatSize(b.archivesBytes), false, null, true));
                cardDetails.addView(IosSheet.createActionRow(this, R.drawable.ic_info,
                        "Tệp lớn >50MB (" + b.largeFiles.size() + " tệp)", false, v -> {
                    Toast.makeText(this, "Có " + b.largeFiles.size() + " tệp lớn hơn 50MB", Toast.LENGTH_SHORT).show();
                }, true));
                cardDetails.addView(IosSheet.createActionRow(this, R.drawable.ic_copy,
                        "Tệp trùng lặp (" + b.duplicateGroups.size() + " nhóm)", false, v -> {
                    Toast.makeText(this, "Có " + b.duplicateGroups.size() + " nhóm tệp trùng lặp", Toast.LENGTH_SHORT).show();
                }, false));
            });
        }).start();

        IosSheet.show(dialog, sheet);
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
        
        if (mIsSelectionMode) {
            exitSelectionMode();
            return true;
        }

        
        if (mIsSearchActive) {
            mIsSearchActive = false;
            refreshDirectory();
            return true;
        }

        
        if (!"files".equals(mCurrentTab)) {
            mCurrentTab = "files";
            if (mFloatingTabBar != null) {
                mFloatingTabBar.selectTab("files");
            }
            switchTab("files");
            return true;
        }

        
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
