package com.velqor.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.velqor.app.R;
import com.velqor.app.VelqorFileProvider;
import com.velqor.app.files.TextFileIO;
import com.velqor.app.tokens.ThemeTokens;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class TextEditorActivity extends Activity {

    public static final String EXTRA_PATH = "extra_path";
    public static final String EXTRA_ZIP_PATH = "extra_zip_path";
    public static final String EXTRA_ENTRY_NAME = "extra_entry_name";

    private File mFile;
    private String mZipPath;
    private String mEntryName;

    private TextFileIO.TextDocument mDoc;
    private boolean mIsDirty = false;
    private float mFontSizeSp = 14f;
    private boolean mIsWrapEnabled = true;

    
    private boolean mMatchCase = false;
    private boolean mUseRegex = false;
    private boolean mIsReplaceExpanded = false;
    private final List<int[]> mSearchResults = new ArrayList<>();
    private int mCurrentSearchIndex = -1;

    
    private LinearLayout mRootLayout;
    private TextView mTitleText;
    private TextView mMetaText;
    private TextView mBtnSave;
    private EditText mEditor;
    private TextView mLineNumbers;
    private ScrollView mVScroll;
    private HorizontalScrollView mHScroll;

    
    private LinearLayout mSearchBar;
    private EditText mSearchInput;
    private TextView mSearchCountText;
    private ImageView mBtnExpandReplace;
    private LinearLayout mReplaceRow;
    private EditText mReplaceInput;
    private TextView mChipCase;
    private TextView mChipRegex;

    
    private final Deque<String> mUndoStack = new ArrayDeque<>();
    private final Deque<String> mRedoStack = new ArrayDeque<>();
    private boolean mIsUndoOrRedo = false;
    private final Handler mHighlightHandler = new Handler(Looper.getMainLooper());
    private final Runnable mHighlightRunnable = () -> {
        if (mEditor != null && mEditor.getText() != null) {
            SyntaxHighlighter.highlight(mEditor.getText(), mFile != null ? mFile.getName() : "");
        }
    };

    private SharedPreferences mPrefs;
    private ScaleGestureDetector mScaleDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupEdgeToEdge();

        mPrefs = getSharedPreferences("velqor_editor_prefs", MODE_PRIVATE);
        mFontSizeSp = mPrefs.getFloat("font_size", 14f);

        String path = getIntent().getStringExtra(EXTRA_PATH);
        mZipPath = getIntent().getStringExtra(EXTRA_ZIP_PATH);
        mEntryName = getIntent().getStringExtra(EXTRA_ENTRY_NAME);

        if (path != null) {
            mFile = new File(path);
        } else if (mZipPath != null) {
            mFile = new File(mEntryName);
        } else {
            finish();
            return;
        }

        
        String ext = getFileExtension();
        boolean defaultWrap = getDefaultWordWrap(mFile != null ? mFile.getName() : "");
        mIsWrapEnabled = mPrefs.getBoolean("wrap_" + ext, defaultWrap);

        buildLayout();
        loadFileContent();
    }

    private String getFileExtension() {
        if (mFile == null) return "";
        String name = mFile.getName();
        int dot = name.lastIndexOf('.');
        return (dot >= 0) ? name.substring(dot).toLowerCase() : "";
    }

    private boolean getDefaultWordWrap(String fileName) {
        String nameLower = fileName.toLowerCase();
        if (nameLower.endsWith(".md") || nameLower.endsWith(".txt") || nameLower.endsWith(".log")) {
            return true;
        }
        return false; 
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

    private void buildLayout() {
        mRootLayout = new LinearLayout(this);
        mRootLayout.setOrientation(LinearLayout.VERTICAL);
        mRootLayout.setBackgroundColor(ThemeTokens.COLOR_BG);

        
        mRootLayout.setOnApplyWindowInsetsListener((v, insets) -> {
            int bottomInset = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets imeInsets = insets.getInsets(android.view.WindowInsets.Type.ime());
                bottomInset = imeInsets.bottom;
                if (bottomInset == 0) {
                    android.graphics.Insets navInsets = insets.getInsets(android.view.WindowInsets.Type.navigationBars());
                    bottomInset = navInsets.bottom;
                }
            } else {
                bottomInset = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(0, 0, 0, bottomInset);
            return insets;
        });

        
        View header = createHeader();
        mRootLayout.addView(header);

        
        View editorContainer = createEditorContainer();
        LinearLayout.LayoutParams edLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        mRootLayout.addView(editorContainer, edLp);

        
        mSearchBar = createCompactSearchBar();
        mRootLayout.addView(mSearchBar);

        
        View accessoryBar = createAccessoryBar();
        mRootLayout.addView(accessoryBar);

        setContentView(mRootLayout);

        
        mScaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                float newSize = mFontSizeSp * factor;
                if (newSize >= 10f && newSize <= 30f) {
                    mFontSizeSp = newSize;
                    applyFontSize(mFontSizeSp);
                    mPrefs.edit().putFloat("font_size", mFontSizeSp).apply();
                }
                return true;
            }
        });
    }

    private View createHeader() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        int padTop = getStatusBarHeight() + ThemeTokens.dp(this, 8);
        bar.setPadding(ThemeTokens.dp(this, 16), padTop, ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 8));
        bar.setBackgroundColor(ThemeTokens.COLOR_SURFACE_GLASS);

        
        ImageView btnBack = new ImageView(this);
        btnBack.setImageResource(R.drawable.ic_chevron_right);
        btnBack.setRotation(180);
        btnBack.setColorFilter(Color.WHITE);
        int sz = ThemeTokens.dp(this, 26);
        btnBack.setLayoutParams(new LinearLayout.LayoutParams(sz, sz));
        btnBack.setOnClickListener(v -> handleBackPress());
        bar.addView(btnBack);

        
        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.leftMargin = ThemeTokens.dp(this, 12);
        colLp.rightMargin = ThemeTokens.dp(this, 8);

        mTitleText = new TextView(this);
        mTitleText.setText(mFile.getName());
        mTitleText.setTextColor(Color.WHITE);
        mTitleText.setTextSize(16);
        mTitleText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mTitleText.setSingleLine(true);
        textCol.addView(mTitleText);

        mMetaText = new TextView(this);
        mMetaText.setText("UTF-8 · LF");
        mMetaText.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        mMetaText.setTextSize(11);
        mMetaText.setSingleLine(true);
        textCol.addView(mMetaText);

        bar.addView(textCol, colLp);

        
        ImageView btnSearch = createHeaderIcon(R.drawable.ic_search, v -> toggleSearchBar());
        bar.addView(btnSearch);

        
        ImageView btnMore = createHeaderIcon(R.drawable.ic_more_dots, v -> showEditorOptionsMenu());
        bar.addView(btnMore);

        
        mBtnSave = new TextView(this);
        mBtnSave.setText("Lưu");
        mBtnSave.setTextColor(Color.WHITE);
        mBtnSave.setTextSize(14);
        mBtnSave.setTypeface(Typeface.DEFAULT_BOLD);
        mBtnSave.setPadding(ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 6),
                ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 6));

        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setCornerRadius(ThemeTokens.dp(this, 999));
        saveBg.setColor(ThemeTokens.COLOR_ACCENT);
        mBtnSave.setBackground(saveBg);
        mBtnSave.setOnClickListener(v -> saveDocument(false));
        mBtnSave.setAlpha(0.35f);
        mBtnSave.setEnabled(false);
        bar.addView(mBtnSave);

        return bar;
    }

    private ImageView createHeaderIcon(int iconRes, View.OnClickListener listener) {
        ImageView iv = new ImageView(this);
        iv.setImageResource(iconRes);
        iv.setColorFilter(ThemeTokens.COLOR_ACCENT);
        int sz = ThemeTokens.dp(this, 24);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sz, sz);
        lp.rightMargin = ThemeTokens.dp(this, 12);
        iv.setLayoutParams(lp);
        iv.setOnClickListener(listener);
        return iv;
    }

    private LinearLayout createCompactSearchBar() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(ThemeTokens.COLOR_SURFACE_ELEVATED);
        container.setPadding(ThemeTokens.dp(this, 10), ThemeTokens.dp(this, 6),
                ThemeTokens.dp(this, 10), ThemeTokens.dp(this, 6));
        container.setVisibility(View.GONE);

        
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams r1Lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(this, 40));
        row1.setLayoutParams(r1Lp);

        
        LinearLayout searchBox = new LinearLayout(this);
        searchBox.setOrientation(LinearLayout.HORIZONTAL);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        searchBox.setPadding(ThemeTokens.dp(this, 8), 0, ThemeTokens.dp(this, 8), 0);

        GradientDrawable sbBg = new GradientDrawable();
        sbBg.setCornerRadius(ThemeTokens.dp(this, 10));
        sbBg.setColor(ThemeTokens.COLOR_ACTION_GROUP_BG);
        sbBg.setStroke(ThemeTokens.dp(this, 1), 0x26FFFFFF); 
        searchBox.setBackground(sbBg);

        ImageView searchIcon = new ImageView(this);
        searchIcon.setImageResource(R.drawable.ic_search);
        searchIcon.setColorFilter(ThemeTokens.COLOR_SEC_LABEL);
        int sIconSz = ThemeTokens.dp(this, 16);
        searchBox.addView(searchIcon, new LinearLayout.LayoutParams(sIconSz, sIconSz));

        mSearchInput = new EditText(this);
        mSearchInput.setHint("Tìm trong tệp...");
        mSearchInput.setHintTextColor(ThemeTokens.COLOR_SEC_LABEL);
        mSearchInput.setTextColor(Color.WHITE);
        mSearchInput.setTextSize(13);
        mSearchInput.setSingleLine(true);
        mSearchInput.setBackgroundColor(Color.TRANSPARENT);
        mSearchInput.setPadding(ThemeTokens.dp(this, 6), 0, ThemeTokens.dp(this, 6), 0);
        searchBox.addView(mSearchInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        mSearchCountText = new TextView(this);
        mSearchCountText.setText("0/0");
        mSearchCountText.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        mSearchCountText.setTextSize(11);
        searchBox.addView(mSearchCountText);

        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        sbLp.rightMargin = ThemeTokens.dp(this, 6);
        row1.addView(searchBox, sbLp);

        
        ImageView btnPrev = createBarActionIcon(R.drawable.ic_chevron_right, v -> navigateMatch(-1));
        btnPrev.setRotation(180);
        row1.addView(btnPrev);

        
        ImageView btnNext = createBarActionIcon(R.drawable.ic_chevron_right, v -> navigateMatch(1));
        row1.addView(btnNext);

        
        mBtnExpandReplace = createBarActionIcon(R.drawable.ic_chevron_right, v -> toggleReplaceRow());
        mBtnExpandReplace.setRotation(90);
        row1.addView(mBtnExpandReplace);

        
        ImageView btnClose = createBarActionIcon(R.drawable.ic_close, v -> toggleSearchBar());
        btnClose.setColorFilter(ThemeTokens.COLOR_SEC_LABEL);
        row1.addView(btnClose);

        container.addView(row1);

        
        mReplaceRow = new LinearLayout(this);
        mReplaceRow.setOrientation(LinearLayout.HORIZONTAL);
        mReplaceRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams r2Lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(this, 36));
        r2Lp.topMargin = ThemeTokens.dp(this, 6);
        mReplaceRow.setLayoutParams(r2Lp);
        mReplaceRow.setVisibility(View.GONE);

        
        LinearLayout replaceBox = new LinearLayout(this);
        replaceBox.setOrientation(LinearLayout.HORIZONTAL);
        replaceBox.setGravity(Gravity.CENTER_VERTICAL);
        replaceBox.setPadding(ThemeTokens.dp(this, 8), 0, ThemeTokens.dp(this, 8), 0);

        GradientDrawable rbBg = new GradientDrawable();
        rbBg.setCornerRadius(ThemeTokens.dp(this, 10));
        rbBg.setColor(ThemeTokens.COLOR_ACTION_GROUP_BG);
        rbBg.setStroke(ThemeTokens.dp(this, 1), 0x26FFFFFF);
        replaceBox.setBackground(rbBg);

        mReplaceInput = new EditText(this);
        mReplaceInput.setHint("Thay thế bằng...");
        mReplaceInput.setHintTextColor(ThemeTokens.COLOR_SEC_LABEL);
        mReplaceInput.setTextColor(Color.WHITE);
        mReplaceInput.setTextSize(13);
        mReplaceInput.setSingleLine(true);
        mReplaceInput.setBackgroundColor(Color.TRANSPARENT);
        mReplaceInput.setPadding(ThemeTokens.dp(this, 6), 0, ThemeTokens.dp(this, 6), 0);
        replaceBox.addView(mReplaceInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams rbLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        rbLp.rightMargin = ThemeTokens.dp(this, 6);
        mReplaceRow.addView(replaceBox, rbLp);

        
        TextView btnReplaceOne = createTextAction("Thay thế", v -> replaceCurrentMatch());
        mReplaceRow.addView(btnReplaceOne);

        
        TextView btnReplaceAll = createTextAction("Tất cả", v -> replaceAllMatches());
        mReplaceRow.addView(btnReplaceAll);

        
        mChipCase = createFilterChip("Aa", mMatchCase, v -> {
            mMatchCase = !mMatchCase;
            updateChipState(mChipCase, mMatchCase);
            performSearch();
        });
        mReplaceRow.addView(mChipCase);

        
        mChipRegex = createFilterChip(".*", mUseRegex, v -> {
            mUseRegex = !mUseRegex;
            updateChipState(mChipRegex, mUseRegex);
            performSearch();
        });
        mReplaceRow.addView(mChipRegex);

        container.addView(mReplaceRow);

        
        mSearchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return container;
    }

    private TextView createFilterChip(String label, boolean isActive, View.OnClickListener listener) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTextSize(12);
        chip.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        chip.setPadding(ThemeTokens.dp(this, 8), ThemeTokens.dp(this, 4),
                ThemeTokens.dp(this, 8), ThemeTokens.dp(this, 4));

        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.leftMargin = ThemeTokens.dp(this, 4);
        chip.setLayoutParams(clp);

        updateChipState(chip, isActive);
        chip.setOnClickListener(listener);
        return chip;
    }

    private void updateChipState(TextView chip, boolean isActive) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(ThemeTokens.dp(this, 999));
        if (isActive) {
            bg.setColor(ThemeTokens.COLOR_ACCENT);
            chip.setTextColor(Color.WHITE);
        } else {
            bg.setColor(ThemeTokens.COLOR_ACTION_GROUP_BG);
            bg.setStroke(ThemeTokens.dp(this, 1), 0x33FFFFFF);
            chip.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        }
        chip.setBackground(bg);
    }

    private void toggleReplaceRow() {
        mIsReplaceExpanded = !mIsReplaceExpanded;
        mReplaceRow.setVisibility(mIsReplaceExpanded ? View.VISIBLE : View.GONE);
        mBtnExpandReplace.setRotation(mIsReplaceExpanded ? 270 : 90);
    }

    private View createEditorContainer() {
        mVScroll = new ScrollView(this);
        mVScroll.setFillViewport(true);

        mHScroll = new HorizontalScrollView(this);
        mHScroll.setFillViewport(true);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        int padTop = ThemeTokens.dp(this, 12);
        int padBottom = ThemeTokens.dp(this, 100);

        
        mLineNumbers = new TextView(this);
        mLineNumbers.setTextColor(0x55FFFFFF); 
        mLineNumbers.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizeSp);
        mLineNumbers.setTypeface(Typeface.MONOSPACE);
        mLineNumbers.setPadding(ThemeTokens.dp(this, 10), padTop, ThemeTokens.dp(this, 8), padBottom);
        mLineNumbers.setGravity(Gravity.END);
        mLineNumbers.setText("1");
        adjustGutterWidth(1);
        row.addView(mLineNumbers);

        
        View sep = new View(this);
        sep.setBackgroundColor(ThemeTokens.COLOR_SEPARATOR_INSET);
        row.addView(sep, new LinearLayout.LayoutParams(ThemeTokens.dp(this, 1), ViewGroup.LayoutParams.MATCH_PARENT));

        
        mEditor = new EditText(this);
        mEditor.setTextColor(Color.WHITE);
        mEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizeSp);
        mEditor.setTypeface(Typeface.MONOSPACE);
        mEditor.setGravity(Gravity.TOP | Gravity.START);
        mEditor.setBackgroundColor(Color.TRANSPARENT);
        mEditor.setPadding(ThemeTokens.dp(this, 8), padTop, ThemeTokens.dp(this, 16), padBottom);
        mEditor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        
        applyWordWrap(mIsWrapEnabled);

        
        mEditor.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                int start = mEditor.getSelectionStart();
                Editable text = mEditor.getText();
                if (start > 0 && text != null) {
                    int lineStart = text.toString().lastIndexOf('\n', start - 1);
                    lineStart = (lineStart == -1) ? 0 : lineStart + 1;
                    String prevLine = text.subSequence(lineStart, start).toString();

                    StringBuilder indent = new StringBuilder();
                    for (int i = 0; i < prevLine.length(); i++) {
                        char c = prevLine.charAt(i);
                        if (c == ' ' || c == '\t') {
                            indent.append(c);
                        } else {
                            break;
                        }
                    }
                    if (indent.length() > 0) {
                        text.insert(start, "\n" + indent);
                        return true;
                    }
                }
            }
            return false;
        });

        mEditor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!mIsUndoOrRedo) {
                    pushUndoState(s.toString());
                }
                setDirty(true);
                updateLineNumbers();
                mHighlightHandler.removeCallbacks(mHighlightRunnable);
                mHighlightHandler.postDelayed(mHighlightRunnable, 350);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        row.addView(mEditor, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mHScroll.addView(row);
        mVScroll.addView(mHScroll);
        return mVScroll;
    }

    private void applyWordWrap(boolean wrap) {
        mIsWrapEnabled = wrap;
        mEditor.setHorizontallyScrolling(!wrap);
        mHScroll.setHorizontalScrollBarEnabled(!wrap);
        mEditor.post(this::updateLineNumbers);
    }

    private void applyFontSize(float sizeSp) {
        mEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        mLineNumbers.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        mEditor.post(this::updateLineNumbers);
    }

    private View createAccessoryBar() {
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        hScroll.setHorizontalScrollBarEnabled(false);
        hScroll.setBackgroundColor(ThemeTokens.COLOR_SURFACE_ELEVATED);
        hScroll.setPadding(ThemeTokens.dp(this, 6), ThemeTokens.dp(this, 4),
                ThemeTokens.dp(this, 6), ThemeTokens.dp(this, 4));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        
        String[] keys = {"Tab", "{", "}", "(", ")", "[", "]", "<", ">", "\"", "'", ";", "=", "/", "\\"};
        for (String k : keys) {
            TextView btn = new TextView(this);
            btn.setText(k);
            btn.setTextColor(Color.WHITE);
            btn.setTextSize(14);
            btn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            btn.setPadding(ThemeTokens.dp(this, 12), ThemeTokens.dp(this, 6),
                    ThemeTokens.dp(this, 12), ThemeTokens.dp(this, 6));

            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setCornerRadius(ThemeTokens.dp(this, 6));
            btnBg.setColor(ThemeTokens.COLOR_ACTION_GROUP_BG);
            btn.setBackground(btnBg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = ThemeTokens.dp(this, 6);
            btn.setLayoutParams(lp);

            btn.setOnClickListener(v -> handleAccessoryKey(k));
            row.addView(btn);
        }

        
        TextView btnUndo = createAccessoryBtn("↶ Hoàn tác", v -> performUndo());
        row.addView(btnUndo);

        TextView btnRedo = createAccessoryBtn("↷ Làm lại", v -> performRedo());
        row.addView(btnRedo);

        hScroll.addView(row);
        return hScroll;
    }

    private void handleAccessoryKey(String key) {
        if ("Tab".equals(key)) {
            insertTextAtCursor("    ");
            return;
        }

        int start = Math.max(0, mEditor.getSelectionStart());
        int end = Math.max(0, mEditor.getSelectionEnd());
        Editable text = mEditor.getText();
        if (text == null) return;

        
        if (key.length() == 1) {
            char ch = key.charAt(0);
            char closeChar = getPairedClosingChar(ch);

            if (closeChar != 0) {
                
                if (start < end) {
                    
                    String selected = text.subSequence(start, end).toString();
                    text.replace(start, end, ch + selected + closeChar);
                    mEditor.setSelection(start + 1, end + 1);
                } else {
                    
                    text.insert(start, "" + ch + closeChar);
                    mEditor.setSelection(start + 1);
                }
                return;
            } else if (isClosingChar(ch)) {
                
                if (start == end && start < text.length() && text.charAt(start) == ch) {
                    
                    mEditor.setSelection(start + 1);
                    return;
                }
            }
        }

        insertTextAtCursor(key);
    }

    private char getPairedClosingChar(char open) {
        switch (open) {
            case '{': return '}';
            case '(': return ')';
            case '[': return ']';
            case '<': return '>';
            case '"': return '"';
            case '\'': return '\'';
            default: return 0;
        }
    }

    private boolean isClosingChar(char ch) {
        return ch == '}' || ch == ')' || ch == ']' || ch == '>';
    }

    private TextView createAccessoryBtn(String label, View.OnClickListener listener) {
        TextView btn = new TextView(this);
        btn.setText(label);
        btn.setTextColor(ThemeTokens.COLOR_ACCENT);
        btn.setTextSize(13);
        btn.setPadding(ThemeTokens.dp(this, 10), ThemeTokens.dp(this, 6),
                ThemeTokens.dp(this, 10), ThemeTokens.dp(this, 6));

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setCornerRadius(ThemeTokens.dp(this, 6));
        btnBg.setColor(ThemeTokens.COLOR_ACTION_GROUP_BG);
        btn.setBackground(btnBg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = ThemeTokens.dp(this, 6);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(listener);
        return btn;
    }

    private void insertTextAtCursor(String str) {
        int start = Math.max(mEditor.getSelectionStart(), 0);
        int end = Math.max(mEditor.getSelectionEnd(), 0);
        mEditor.getText().replace(Math.min(start, end), Math.max(start, end), str, 0, str.length());
    }

    private void updateLineNumbers() {
        Layout layout = mEditor.getLayout();
        CharSequence text = mEditor.getText();
        if (layout == null || text == null || text.length() == 0) {
            mLineNumbers.setText("1");
            adjustGutterWidth(1);
            return;
        }

        int visualLineCount = layout.getLineCount();
        StringBuilder sb = new StringBuilder();
        int logicalLine = 1;

        for (int i = 0; i < visualLineCount; i++) {
            int lineStart = layout.getLineStart(i);
            if (i == 0) {
                sb.append(logicalLine++);
            } else if (lineStart > 0 && text.charAt(lineStart - 1) == '\n') {
                sb.append(logicalLine++);
            }
            if (i < visualLineCount - 1) {
                sb.append('\n');
            }
        }

        mLineNumbers.setText(sb.toString());
        adjustGutterWidth(logicalLine);
    }

    private void adjustGutterWidth(int totalLines) {
        int digits = (totalLines < 100) ? 3 : (totalLines < 1000 ? 4 : 5);
        int approxCharWidth = ThemeTokens.dp(this, (int) (mFontSizeSp * 0.65f));
        int minWidth = digits * approxCharWidth + ThemeTokens.dp(this, 18);
        mLineNumbers.setMinWidth(minWidth);
    }

    private void setDirty(boolean dirty) {
        mIsDirty = dirty;
        String fileName = (mFile != null) ? mFile.getName() : "Văn bản";
        mTitleText.setText((mIsDirty ? "● " : "") + fileName);
        mBtnSave.setAlpha(mIsDirty ? 1.0f : 0.35f);
        mBtnSave.setEnabled(mIsDirty);
    }

    private void loadFileContent() {
        new Thread(() -> {
            try {
                if (mFile != null && mFile.exists()) {
                    mDoc = TextFileIO.readTextFile(mFile);
                } else {
                    mDoc = new TextFileIO.TextDocument("", "UTF-8", false, "\n", 0, false);
                }

                runOnUiThread(() -> {
                    mIsUndoOrRedo = true;
                    mEditor.setText(mDoc.content);
                    SyntaxHighlighter.highlight(mEditor.getText(), mFile != null ? mFile.getName() : "");
                    mIsUndoOrRedo = false;
                    setDirty(false);
                    updateLineNumbers();

                    String meta = mDoc.charsetName + (mDoc.hasBom ? " (BOM)" : "") +
                            (" · ") + ("\r\n".equals(mDoc.lineEnding) ? "CRLF" : "LF");
                    if (mDoc.isReadOnlyPaging) {
                        meta += " · [Chỉ đọc: " + (mDoc.pageIndex + 1) + "/" + mDoc.totalPages + "]";
                        mEditor.setEnabled(false);
                    }
                    mMetaText.setText(meta);

                    if (mFile.length() > TextFileIO.WARN_FILE_THRESHOLD && !mDoc.isReadOnlyPaging) {
                        Toast.makeText(this, "Tệp lớn (" + (mFile.length() / 1024) + " KB) - Chú ý khi chỉnh sửa", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi đọc tệp: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void saveDocument(boolean saveAs) {
        if (mDoc != null && mDoc.isReadOnlyPaging) {
            Toast.makeText(this, "Tệp > 10MB đang mở ở chế độ chỉ đọc, không thể ghi đè", Toast.LENGTH_LONG).show();
            return;
        }

        String contentToSave = mEditor.getText().toString();

        new Thread(() -> {
            try {
                if (mZipPath != null && mEntryName != null) {
                    TextFileIO.updateZipEntryText(new File(mZipPath), mEntryName, contentToSave, mDoc);
                } else {
                    TextFileIO.writeTextFile(mFile, contentToSave, mDoc);
                }
                runOnUiThread(() -> {
                    setDirty(false);
                    Toast.makeText(this, "Đã lưu thành công!", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi lưu tệp: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void handleBackPress() {
        if (!mIsDirty) {
            finish();
            return;
        }

        
        Dialog dialog = IosSheet.createBaseDialog(this);
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Thay đổi chưa lưu");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Bạn có thay đổi chưa lưu trong '" + mFile.getName() + "'. Bạn có muốn lưu trước khi thoát?");
        subtitle.setTextSize(14);
        subtitle.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = ThemeTokens.dp(this, 6);
        subtitle.setLayoutParams(subLp);
        sheet.addView(subtitle);

        LinearLayout group = IosSheet.createActionGroup(this);
        group.addView(IosSheet.createActionRow(this, R.drawable.ic_check, "Lưu thay đổi", false, v -> {
            dialog.dismiss();
            saveDocument(false);
            finish();
        }, true));

        group.addView(IosSheet.createActionRow(this, R.drawable.ic_close, "Không lưu & Thoát", true, v -> {
            dialog.dismiss();
            finish();
        }, false));
        sheet.addView(group);

        LinearLayout cancelCard = IosSheet.createActionGroup(this);
        cancelCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((LinearLayout.LayoutParams) cancelCard.getLayoutParams()).topMargin = ThemeTokens.dp(this, 12);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("Hủy");
        btnCancel.setTextColor(ThemeTokens.COLOR_ACCENT);
        btnCancel.setTextSize(17);
        btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(this, 50)));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        cancelCard.addView(btnCancel);
        sheet.addView(cancelCard);

        IosSheet.show(dialog, sheet);
    }

    @Override
    public void onBackPressed() {
        handleBackPress();
    }

    private void pushUndoState(String text) {
        if (mUndoStack.size() >= 50) mUndoStack.pollFirst();
        mUndoStack.push(text);
        mRedoStack.clear();
    }

    private void performUndo() {
        if (mUndoStack.size() > 1) {
            mIsUndoOrRedo = true;
            mRedoStack.push(mUndoStack.pop());
            String prev = mUndoStack.peek();
            mEditor.setText(prev);
            mEditor.setSelection(mEditor.getText().length());
            mIsUndoOrRedo = false;
        }
    }

    private void performRedo() {
        if (!mRedoStack.isEmpty()) {
            mIsUndoOrRedo = true;
            String next = mRedoStack.pop();
            mUndoStack.push(next);
            mEditor.setText(next);
            mEditor.setSelection(mEditor.getText().length());
            mIsUndoOrRedo = false;
        }
    }

    private void toggleSearchBar() {
        boolean isVis = (mSearchBar.getVisibility() == View.VISIBLE);
        mSearchBar.setVisibility(isVis ? View.GONE : View.VISIBLE);
        if (!isVis) {
            mSearchInput.requestFocus();
            performSearch();
        } else {
            mSearchResults.clear();
            mCurrentSearchIndex = -1;
            mSearchCountText.setText("0/0");
        }
    }

    private void performSearch() {
        String query = mSearchInput.getText().toString();
        mSearchResults.clear();
        mCurrentSearchIndex = -1;

        if (query.isEmpty()) {
            mSearchCountText.setText("0/0");
            mSearchCountText.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
            return;
        }

        try {
            int flags = mMatchCase ? 0 : Pattern.CASE_INSENSITIVE;
            Pattern pattern = mUseRegex ? Pattern.compile(query, flags) : Pattern.compile(Pattern.quote(query), flags);
            Matcher matcher = pattern.matcher(mEditor.getText());

            while (matcher.find()) {
                mSearchResults.add(new int[]{matcher.start(), matcher.end()});
            }

            int count = mSearchResults.size();
            if (count > 0) {
                
                int cursorPos = mEditor.getSelectionStart();
                mCurrentSearchIndex = 0;
                for (int i = 0; i < count; i++) {
                    if (mSearchResults.get(i)[0] >= cursorPos) {
                        mCurrentSearchIndex = i;
                        break;
                    }
                }
                mSearchCountText.setText((mCurrentSearchIndex + 1) + "/" + count);
                mSearchCountText.setTextColor(ThemeTokens.COLOR_ACCENT);
                highlightCurrentMatch();
            } else {
                mSearchCountText.setText("0/0");
                mSearchCountText.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
            }
        } catch (PatternSyntaxException e) {
            mSearchCountText.setText("Lỗi cú pháp");
            mSearchCountText.setTextColor(ThemeTokens.COLOR_DANGER);
        }
    }

    private void navigateMatch(int direction) {
        if (mSearchResults.isEmpty()) return;
        mCurrentSearchIndex += direction;
        if (mCurrentSearchIndex < 0) {
            mCurrentSearchIndex = mSearchResults.size() - 1;
        } else if (mCurrentSearchIndex >= mSearchResults.size()) {
            mCurrentSearchIndex = 0;
        }
        mSearchCountText.setText((mCurrentSearchIndex + 1) + "/" + mSearchResults.size());
        highlightCurrentMatch();
    }

    private void highlightCurrentMatch() {
        if (mCurrentSearchIndex >= 0 && mCurrentSearchIndex < mSearchResults.size()) {
            int[] span = mSearchResults.get(mCurrentSearchIndex);
            mEditor.setSelection(span[0], span[1]);
        }
    }

    private void replaceCurrentMatch() {
        if (mCurrentSearchIndex < 0 || mCurrentSearchIndex >= mSearchResults.size()) return;
        int[] span = mSearchResults.get(mCurrentSearchIndex);
        String rep = mReplaceInput.getText().toString();
        mEditor.getText().replace(span[0], span[1], rep);
        performSearch();
    }

    private void replaceAllMatches() {
        String query = mSearchInput.getText().toString();
        if (query.isEmpty()) return;
        String rep = mReplaceInput.getText().toString();

        try {
            int flags = mMatchCase ? 0 : Pattern.CASE_INSENSITIVE;
            Pattern pattern = mUseRegex ? Pattern.compile(query, flags) : Pattern.compile(Pattern.quote(query), flags);
            String text = mEditor.getText().toString();
            String replaced = pattern.matcher(text).replaceAll(rep);
            mEditor.setText(replaced);
            performSearch();
            Toast.makeText(this, "Đã thay thế tất cả", Toast.LENGTH_SHORT).show();
        } catch (PatternSyntaxException e) {
            Toast.makeText(this, "Regex không hợp lệ: " + e.getDescription(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditorOptionsMenu() {
        Dialog dialog = IosSheet.createBaseDialog(this);
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Tùy chọn soạn thảo");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        
        LinearLayout group1 = IosSheet.createActionGroup(this);

        if (isHtmlOrMarkdown()) {
            group1.addView(IosSheet.createActionRow(this, R.drawable.ic_eye, "Xem trước trực tiếp", false, v -> {
                dialog.dismiss();
                showLivePreviewSheet();
            }, true));
        }

        group1.addView(IosSheet.createActionRow(this, R.drawable.ic_file_text, "Đi tới dòng...", false, v -> {
            dialog.dismiss();
            showGoToLineDialog();
        }, true));

        String wrapLabel = mIsWrapEnabled ? "Tắt tự xuống dòng (ngắt dòng)" : "Bật tự xuống dòng";
        group1.addView(IosSheet.createActionRow(this, R.drawable.ic_file_text, wrapLabel, false, v -> {
            dialog.dismiss();
            boolean newWrap = !mIsWrapEnabled;
            applyWordWrap(newWrap);
            String ext = getFileExtension();
            mPrefs.edit().putBoolean("wrap_" + ext, newWrap).apply();
            Toast.makeText(this, newWrap ? "Đã bật tự xuống dòng" : "Đã tắt tự xuống dòng", Toast.LENGTH_SHORT).show();
        }, true));

        group1.addView(IosSheet.createActionRow(this, R.drawable.ic_info, "Cỡ chữ (" + (int)mFontSizeSp + "sp)", false, v -> {
            dialog.dismiss();
            showFontSizeDialog();
        }, false));
        sheet.addView(group1);

        
        LinearLayout group2 = IosSheet.createActionGroup(this);

        group2.addView(IosSheet.createActionRow(this, R.drawable.ic_copy, "Lưu bản sao...", false, v -> {
            dialog.dismiss();
            showSaveCopyDialog();
        }, true));

        group2.addView(IosSheet.createActionRow(this, R.drawable.ic_share, "Chia sẻ tệp", false, v -> {
            dialog.dismiss();
            shareCurrentFile();
        }, false));
        sheet.addView(group2);

        
        LinearLayout group3 = IosSheet.createActionGroup(this);
        group3.setPadding(ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 12),
                ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 12));

        TextView tvMeta = new TextView(this);
        String info = "Mã hóa: " + (mDoc != null ? mDoc.charsetName : "UTF-8") +
                (mDoc != null && mDoc.hasBom ? " (có BOM)" : " (không BOM)") + "\n" +
                "Kiểu xuống dòng: " + (mDoc != null && "\r\n".equals(mDoc.lineEnding) ? "CRLF (Windows)" : "LF (Unix/Android)") + "\n" +
                "Kích thước: " + (mFile != null ? mFile.length() : 0) + " bytes" + "\n" +
                "Số dòng: " + (mEditor.getLayout() != null ? mEditor.getLayout().getLineCount() : 1);
        tvMeta.setText(info);
        tvMeta.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        tvMeta.setTextSize(13);
        tvMeta.setLineSpacing(ThemeTokens.dp(this, 3), 1f);
        group3.addView(tvMeta);
        sheet.addView(group3);

        IosSheet.show(dialog, sheet);
    }

    private void showGoToLineDialog() {
        int maxLine = (mEditor.getLayout() != null) ? mEditor.getLayout().getLineCount() : 1;
        IosSheet.showInputDialog(this, "Đi tới dòng", "Nhập số dòng (1 đến " + maxLine + ")", "", "Đi",
                input -> {
                    try {
                        int line = Integer.parseInt(input);
                        if (line < 1 || line > maxLine) {
                            return "Dòng hợp lệ từ 1 đến " + maxLine;
                        }
                        return null;
                    } catch (Exception e) {
                        return "Số dòng không hợp lệ";
                    }
                },
                lineStr -> {
                    int line = Integer.parseInt(lineStr);
                    if (mEditor.getLayout() != null) {
                        int charIndex = mEditor.getLayout().getLineStart(line - 1);
                        mEditor.setSelection(Math.min(charIndex, mEditor.getText().length()));
                    }
                });
    }

    private void showFontSizeDialog() {
        Dialog dialog = IosSheet.createBaseDialog(this);
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Chọn cỡ chữ");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        LinearLayout card = IosSheet.createActionGroup(this);
        int[] sizes = {12, 14, 16, 18, 20, 24};
        for (int i = 0; i < sizes.length; i++) {
            final int sz = sizes[i];
            boolean isChosen = ((int)mFontSizeSp == sz);
            boolean isLast = (i == sizes.length - 1);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(ThemeTokens.dp(this, 16), 0, ThemeTokens.dp(this, 16), 0);
            row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(this, 48)));

            TextView tv = new TextView(this);
            tv.setText(sz + " sp" + (sz == 14 ? " (Mặc định)" : ""));
            tv.setTextSize(16);
            tv.setTextColor(isChosen ? ThemeTokens.COLOR_ACCENT : ThemeTokens.COLOR_LABEL);
            tv.setTypeface(isChosen ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            if (isChosen) {
                ImageView check = new ImageView(this);
                check.setImageResource(R.drawable.ic_check);
                check.setColorFilter(ThemeTokens.COLOR_ACCENT);
                int checkSz = ThemeTokens.dp(this, 18);
                row.addView(check, new LinearLayout.LayoutParams(checkSz, checkSz));
            }

            row.setOnClickListener(v -> {
                dialog.dismiss();
                mFontSizeSp = sz;
                applyFontSize(mFontSizeSp);
                mPrefs.edit().putFloat("font_size", mFontSizeSp).apply();
            });

            card.addView(row);
            if (!isLast) {
                View div = new View(this);
                div.setBackgroundColor(ThemeTokens.COLOR_SEPARATOR_INSET);
                card.addView(div, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(this, 1)));
            }
        }
        sheet.addView(card);
        IosSheet.show(dialog, sheet);
    }

    private void showSaveCopyDialog() {
        String baseName = mFile != null ? mFile.getName() : "copy.txt";
        String defaultCopyName = "copy_" + baseName;

        IosSheet.showInputDialog(this, "Lưu bản sao", "Nhập tên tệp mới", defaultCopyName, "Lưu",
                input -> {
                    if (input == null || input.trim().isEmpty()) return "Tên tệp không được rỗng";
                    if (input.contains("/") || input.indexOf('\0') >= 0) return "Tên tệp chứa ký tự không hợp lệ";
                    File parent = (mFile != null) ? mFile.getParentFile() : null;
                    if (parent != null && new File(parent, input.trim()).exists()) {
                        return "Tệp đã tồn tại trong thư mục";
                    }
                    return null;
                },
                newName -> {
                    File parent = (mFile != null) ? mFile.getParentFile() : null;
                    if (parent != null) {
                        File dest = new File(parent, newName.trim());
                        new Thread(() -> {
                            try {
                                TextFileIO.writeTextFile(dest, mEditor.getText().toString(), mDoc);
                                runOnUiThread(() -> Toast.makeText(this, "Đã lưu bản sao: " + dest.getName(), Toast.LENGTH_SHORT).show());
                            } catch (Exception e) {
                                runOnUiThread(() -> Toast.makeText(this, "Lỗi lưu bản sao: " + e.getMessage(), Toast.LENGTH_LONG).show());
                            }
                        }).start();
                    }
                });
    }

    private void shareCurrentFile() {
        if (mFile == null || !mFile.exists()) {
            Toast.makeText(this, "Tệp không tồn tại để chia sẻ", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri uri = VelqorFileProvider.getUriForFile(this, "com.velqor.app.fileprovider", mFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ '" + mFile.getName() + "'"));
        } catch (Exception e) {
            Toast.makeText(this, "Không thể chia sẻ tệp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private ImageView createBarActionIcon(int iconRes, View.OnClickListener listener) {
        ImageView iv = new ImageView(this);
        iv.setImageResource(iconRes);
        iv.setColorFilter(ThemeTokens.COLOR_ACCENT);
        int sz = ThemeTokens.dp(this, 20);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sz, sz);
        lp.leftMargin = ThemeTokens.dp(this, 6);
        iv.setLayoutParams(lp);
        iv.setOnClickListener(listener);
        return iv;
    }

    private TextView createTextAction(String text, View.OnClickListener listener) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(ThemeTokens.COLOR_ACCENT);
        tv.setTextSize(12);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(ThemeTokens.dp(this, 6), ThemeTokens.dp(this, 4), ThemeTokens.dp(this, 6), ThemeTokens.dp(this, 4));
        tv.setOnClickListener(listener);
        return tv;
    }

    private int getStatusBarHeight() {
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resId > 0 ? getResources().getDimensionPixelSize(resId) : ThemeTokens.dp(this, 24);
    }

    private boolean isHtmlOrMarkdown() {
        if (mFile == null) return false;
        String name = mFile.getName().toLowerCase();
        return name.endsWith(".html") || name.endsWith(".htm") || name.endsWith(".md");
    }

    private void showLivePreviewSheet() {
        Dialog dialog = IosSheet.createBaseDialog(this);
        LinearLayout sheet = IosSheet.createSheetRoot(this, dialog);

        TextView title = new TextView(this);
        title.setText("Xem trước trực tiếp");
        title.setTextSize(20);
        title.setTextColor(ThemeTokens.COLOR_LABEL);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        WebView webView = new WebView(this);
        webView.setBackgroundColor(0xFF121214);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDefaultTextEncodingName("UTF-8");

        String content = mEditor.getText().toString();
        String html;
        if (mFile != null && mFile.getName().toLowerCase().endsWith(".md")) {
            html = convertMarkdownToHtml(content);
        } else {
            html = content;
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);

        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ThemeTokens.dp(this, 400));
        wlp.topMargin = ThemeTokens.dp(this, 12);
        sheet.addView(webView, wlp);

        IosSheet.show(dialog, sheet);
    }

    private String convertMarkdownToHtml(String md) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'><style>");
        sb.append("body { background: #121214; color: #f4f4f5; font-family: -apple-system, sans-serif; padding: 16px; line-height: 1.6; }");
        sb.append("h1, h2, h3 { color: #0A84FF; margin-top: 18px; margin-bottom: 8px; font-weight: 600; }");
        sb.append("h1 { font-size: 1.5em; border-bottom: 1px solid #2C2C2E; padding-bottom: 6px; }");
        sb.append("h2 { font-size: 1.3em; }");
        sb.append("h3 { font-size: 1.1em; }");
        sb.append("code { background: #2C2C2E; padding: 2px 6px; border-radius: 4px; font-family: monospace; color: #64D2FF; font-size: 0.9em; }");
        sb.append("pre { background: #1C1C1E; padding: 12px; border-radius: 8px; overflow-x: auto; border: 1px solid #2C2C2E; }");
        sb.append("pre code { background: transparent; padding: 0; color: #30D158; }");
        sb.append("blockquote { border-left: 3px solid #0A84FF; margin: 0; padding-left: 12px; color: #8E8E93; }");
        sb.append("p { margin: 8px 0; }");
        sb.append("</style></head><body>");

        String[] lines = md.split("\n");
        boolean inPre = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                if (inPre) {
                    sb.append("</code></pre>");
                    inPre = false;
                } else {
                    sb.append("<pre><code>");
                    inPre = true;
                }
                continue;
            }
            if (inPre) {
                sb.append(escapeHtml(line)).append("\n");
                continue;
            }
            if (trimmed.startsWith("### ")) {
                sb.append("<h3>").append(escapeHtml(trimmed.substring(4))).append("</h3>");
            } else if (trimmed.startsWith("## ")) {
                sb.append("<h2>").append(escapeHtml(trimmed.substring(3))).append("</h2>");
            } else if (trimmed.startsWith("# ")) {
                sb.append("<h1>").append(escapeHtml(trimmed.substring(2))).append("</h1>");
            } else if (trimmed.startsWith("> ")) {
                sb.append("<blockquote>").append(escapeHtml(trimmed.substring(2))).append("</blockquote>");
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                sb.append("<li>").append(escapeHtml(trimmed.substring(2))).append("</li>");
            } else if (!trimmed.isEmpty()) {
                String p = escapeHtml(line);
                p = p.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
                p = p.replaceAll("\\*(.+?)\\*", "<em>$1</em>");
                p = p.replaceAll("`(.+?)`", "<code>$1</code>");
                sb.append("<p>").append(p).append("</p>");
            }
        }
        if (inPre) {
            sb.append("</code></pre>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
