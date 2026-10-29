package com.velqor.app.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.velqor.app.R;
import com.velqor.app.files.TextFileIO;
import com.velqor.app.tokens.ThemeTokens;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
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

    private EditText mEditor;
    private TextView mLineNumbers;
    private TextView mTitleText;
    private TextView mMetaText;

    // Search & Replace Bar
    private LinearLayout mSearchBar;
    private EditText mSearchInput;
    private EditText mReplaceInput;
    private TextView mSearchCountText;

    // Undo / Redo Stacks
    private final Deque<String> mUndoStack = new ArrayDeque<>();
    private final Deque<String> mRedoStack = new ArrayDeque<>();
    private boolean mIsUndoOrRedo = false;

    private SharedPreferences mPrefs;
    private ScaleGestureDetector mScaleDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupEdgeToEdge();

        mPrefs = getSharedPreferences("velqor_editor_prefs", MODE_PRIVATE);
        mFontSizeSp = mPrefs.getFloat("font_size", 14f);
        mIsWrapEnabled = mPrefs.getBoolean("wrap_enabled", true);

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

        buildLayout();
        loadFileContent();
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
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeTokens.COLOR_BG);

        // 1. Header Bar
        View header = createHeader();
        root.addView(header);

        // 2. Search & Replace Bar (Collapsible)
        mSearchBar = createSearchBar();
        root.addView(mSearchBar);

        // 3. Editor Container (Line Numbers + Editor inside ScrollViews)
        View editorContainer = createEditorContainer();
        LinearLayout.LayoutParams edLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(editorContainer, edLp);

        // 4. Accessory Key Toolbar (Above Keyboard)
        View accessoryBar = createAccessoryBar();
        root.addView(accessoryBar);

        setContentView(root);

        // Pinch to zoom font size
        mScaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                float newSize = mFontSizeSp * factor;
                if (newSize >= 10f && newSize <= 30f) {
                    mFontSizeSp = newSize;
                    mEditor.setTextSize(mFontSizeSp);
                    mLineNumbers.setTextSize(mFontSizeSp);
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

        // Back / Close
        ImageView btnClose = new ImageView(this);
        btnClose.setImageResource(R.drawable.ic_chevron_right);
        btnClose.setRotation(180);
        btnClose.setColorFilter(Color.WHITE);
        int sz = ThemeTokens.dp(this, 26);
        btnClose.setLayoutParams(new LinearLayout.LayoutParams(sz, sz));
        btnClose.setOnClickListener(v -> handleBackPress());
        bar.addView(btnClose);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.leftMargin = ThemeTokens.dp(this, 12);

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
        textCol.addView(mMetaText);

        bar.addView(textCol, colLp);

        // Search Action
        ImageView btnSearch = createHeaderIcon(R.drawable.ic_search, v -> toggleSearchBar());
        bar.addView(btnSearch);

        // Go to Line Action
        ImageView btnGoTo = createHeaderIcon(R.drawable.ic_tab_hash, v -> showGoToLineDialog());
        bar.addView(btnGoTo);

        // Save Action (Button Pill)
        TextView btnSave = new TextView(this);
        btnSave.setText("Lưu");
        btnSave.setTextColor(Color.WHITE);
        btnSave.setTextSize(14);
        btnSave.setTypeface(Typeface.DEFAULT_BOLD);
        btnSave.setPadding(ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 6),
                ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 6));

        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setCornerRadius(ThemeTokens.dp(this, 999));
        saveBg.setColor(ThemeTokens.COLOR_ACCENT);
        btnSave.setBackground(saveBg);
        btnSave.setOnClickListener(v -> saveDocument(false));
        bar.addView(btnSave);

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

    private LinearLayout createSearchBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setBackgroundColor(ThemeTokens.COLOR_ACTION_GROUP_BG);
        bar.setPadding(ThemeTokens.dp(this, 12), ThemeTokens.dp(this, 8),
                ThemeTokens.dp(this, 12), ThemeTokens.dp(this, 8));
        bar.setVisibility(View.GONE);

        // Search Row
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);

        mSearchInput = new EditText(this);
        mSearchInput.setHint("Tìm kiếm (hoặc regex)...");
        mSearchInput.setHintTextColor(ThemeTokens.COLOR_SEC_LABEL);
        mSearchInput.setTextColor(Color.WHITE);
        mSearchInput.setTextSize(13);
        mSearchInput.setSingleLine(true);
        mSearchInput.setBackgroundColor(Color.TRANSPARENT);
        searchRow.addView(mSearchInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        mSearchCountText = new TextView(this);
        mSearchCountText.setText("0/0");
        mSearchCountText.setTextColor(ThemeTokens.COLOR_SEC_LABEL);
        mSearchCountText.setTextSize(12);
        mSearchCountText.setPadding(ThemeTokens.dp(this, 8), 0, ThemeTokens.dp(this, 8), 0);
        searchRow.addView(mSearchCountText);

        ImageView btnPrev = createBarActionIcon(R.drawable.ic_chevron_right, v -> findNextMatch(false));
        btnPrev.setRotation(180);
        searchRow.addView(btnPrev);

        ImageView btnNext = createBarActionIcon(R.drawable.ic_chevron_right, v -> findNextMatch(true));
        searchRow.addView(btnNext);

        bar.addView(searchRow);

        // Replace Row
        LinearLayout replaceRow = new LinearLayout(this);
        replaceRow.setOrientation(LinearLayout.HORIZONTAL);
        replaceRow.setGravity(Gravity.CENTER_VERTICAL);
        replaceRow.setPadding(0, ThemeTokens.dp(this, 4), 0, 0);

        mReplaceInput = new EditText(this);
        mReplaceInput.setHint("Thay thế bằng...");
        mReplaceInput.setHintTextColor(ThemeTokens.COLOR_SEC_LABEL);
        mReplaceInput.setTextColor(Color.WHITE);
        mReplaceInput.setTextSize(13);
        mReplaceInput.setSingleLine(true);
        mReplaceInput.setBackgroundColor(Color.TRANSPARENT);
        replaceRow.addView(mReplaceInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView btnReplaceOne = createTextAction("Thay thế", v -> replaceCurrentMatch());
        replaceRow.addView(btnReplaceOne);

        TextView btnReplaceAll = createTextAction("Thay tất cả", v -> replaceAllMatches());
        replaceRow.addView(btnReplaceAll);

        bar.addView(replaceRow);

        return bar;
    }

    private View createEditorContainer() {
        ScrollView vScroll = new ScrollView(this);
        vScroll.setFillViewport(true);

        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        hScroll.setFillViewport(true);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        // Line Numbers Gutter
        mLineNumbers = new TextView(this);
        mLineNumbers.setTextColor(0x66FFFFFF); // 40% white
        mLineNumbers.setTextSize(mFontSizeSp);
        mLineNumbers.setTypeface(Typeface.MONOSPACE);
        mLineNumbers.setPadding(ThemeTokens.dp(this, 12), ThemeTokens.dp(this, 8),
                ThemeTokens.dp(this, 8), ThemeTokens.dp(this, 100));
        mLineNumbers.setGravity(Gravity.END);
        mLineNumbers.setText("1");
        row.addView(mLineNumbers);

        // Vertical Separator
        View sep = new View(this);
        sep.setBackgroundColor(ThemeTokens.COLOR_SEPARATOR_INSET);
        row.addView(sep, new LinearLayout.LayoutParams(ThemeTokens.dp(this, 1), ViewGroup.LayoutParams.MATCH_PARENT));

        // Editor
        mEditor = new EditText(this);
        mEditor.setTextColor(Color.WHITE);
        mEditor.setTextSize(mFontSizeSp);
        mEditor.setTypeface(Typeface.MONOSPACE);
        mEditor.setGravity(Gravity.TOP | Gravity.START);
        mEditor.setBackgroundColor(Color.TRANSPARENT);
        mEditor.setPadding(ThemeTokens.dp(this, 10), ThemeTokens.dp(this, 8),
                ThemeTokens.dp(this, 16), ThemeTokens.dp(this, 120));
        mEditor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        // Auto-indent on Enter
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
                mIsDirty = true;
                updateLineNumbers();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        row.addView(mEditor, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        hScroll.addView(row);
        vScroll.addView(hScroll);
        return vScroll;
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

        // Key shortcuts
        String[] keys = {"Tab", "{", "}", "(", ")", "[", "]", "<", ">", "\"", "'", ";", "=", "/", "\\"};
        for (String k : keys) {
            TextView btn = new TextView(this);
            btn.setText(k);
            btn.setTextColor(Color.WHITE);
            btn.setTextSize(15);
            btn.setTypeface(Typeface.MONOSPACE);
            btn.setPadding(ThemeTokens.dp(this, 12), ThemeTokens.dp(this, 6),
                    ThemeTokens.dp(this, 12), ThemeTokens.dp(this, 6));

            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setCornerRadius(ThemeTokens.dp(this, 6));
            btnBg.setColor(ThemeTokens.COLOR_ACTION_GROUP_BG);
            btn.setBackground(btnBg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = ThemeTokens.dp(this, 6);
            btn.setLayoutParams(lp);

            btn.setOnClickListener(v -> insertTextAtCursor("Tab".equals(k) ? "    " : k));
            row.addView(btn);
        }

        // Undo & Redo buttons
        TextView btnUndo = createAccessoryBtn("↶ Undo", v -> performUndo());
        row.addView(btnUndo);

        TextView btnRedo = createAccessoryBtn("↷ Redo", v -> performRedo());
        row.addView(btnRedo);

        hScroll.addView(row);
        return hScroll;
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
        int lineCount = mEditor.getLineCount();
        if (lineCount <= 0) lineCount = 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lineCount; i++) {
            sb.append(i).append("\n");
        }
        mLineNumbers.setText(sb.toString().trim());
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
                    mIsUndoOrRedo = false;
                    mIsDirty = false;
                    updateLineNumbers();

                    String meta = mDoc.charsetName + (mDoc.hasBom ? " (BOM)" : "") +
                            (" \u00B7 ") + ("\r\n".equals(mDoc.lineEnding) ? "CRLF" : "LF");
                    if (mDoc.isReadOnlyPaging) {
                        meta += " \u00B7 [Chỉ đọc theo trang: " + (mDoc.pageIndex + 1) + "/" + mDoc.totalPages + "]";
                        mEditor.setEnabled(false);
                    }
                    mMetaText.setText(meta);

                    if (mFile.length() > TextFileIO.WARN_FILE_THRESHOLD && !mDoc.isReadOnlyPaging) {
                        Toast.makeText(this, "Tệp lớn (" + (mFile.length() / 1024) + " KB) - Hãy cẩn thận khi chỉnh sửa", Toast.LENGTH_SHORT).show();
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
                    // Save into ZIP entry
                    TextFileIO.updateZipEntryText(new File(mZipPath), mEntryName, contentToSave, mDoc);
                } else {
                    TextFileIO.writeTextFile(mFile, contentToSave, mDoc);
                }
                runOnUiThread(() -> {
                    mIsDirty = false;
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

        // Show iOS Sheet confirm exit without saving
        IosSheet.showDeleteConfirm(this, new File("Thay đổi chưa được lưu!"), () -> {
            // Dismiss and exit without saving
            finish();
        });
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
        mSearchBar.setVisibility(mSearchBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void findNextMatch(boolean forward) {
        String query = mSearchInput.getText().toString();
        if (query.isEmpty()) return;

        try {
            Pattern pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(mEditor.getText());

            int currentPos = mEditor.getSelectionStart();
            int matchCount = 0;
            int nextStart = -1;
            int nextEnd = -1;

            while (matcher.find()) {
                matchCount++;
                if (matcher.start() > currentPos && nextStart == -1) {
                    nextStart = matcher.start();
                    nextEnd = matcher.end();
                }
            }

            mSearchCountText.setText(matchCount + " kết quả");
            if (nextStart != -1) {
                mEditor.setSelection(nextStart, nextEnd);
            } else if (matcher.reset().find()) {
                // Loop around to first
                mEditor.setSelection(matcher.start(), matcher.end());
            }
        } catch (PatternSyntaxException e) {
            Toast.makeText(this, "Regex không hợp lệ: " + e.getDescription(), Toast.LENGTH_SHORT).show();
        }
    }

    private void replaceCurrentMatch() {
        int start = mEditor.getSelectionStart();
        int end = mEditor.getSelectionEnd();
        if (start < end) {
            String rep = mReplaceInput.getText().toString();
            mEditor.getText().replace(start, end, rep);
            findNextMatch(true);
        }
    }

    private void replaceAllMatches() {
        String query = mSearchInput.getText().toString();
        String rep = mReplaceInput.getText().toString();
        if (query.isEmpty()) return;

        try {
            String updated = mEditor.getText().toString().replaceAll(query, rep);
            mEditor.setText(updated);
            Toast.makeText(this, "Đã thay thế tất cả", Toast.LENGTH_SHORT).show();
        } catch (PatternSyntaxException e) {
            Toast.makeText(this, "Regex sai: " + e.getDescription(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showGoToLineDialog() {
        IosSheet.showInputDialog(this, "Đi tới dòng", "Nhập số dòng", "", "Đi",
                input -> {
                    try {
                        int line = Integer.parseInt(input);
                        if (line < 1 || line > mEditor.getLineCount()) {
                            return "Dòng từ 1 đến " + mEditor.getLineCount();
                        }
                        return null;
                    } catch (Exception e) {
                        return "Số dòng không hợp lệ";
                    }
                },
                lineStr -> {
                    int line = Integer.parseInt(lineStr);
                    int charIndex = mEditor.getLayout().getLineStart(line - 1);
                    mEditor.setSelection(charIndex);
                });
    }

    private ImageView createBarActionIcon(int iconRes, View.OnClickListener listener) {
        ImageView iv = new ImageView(this);
        iv.setImageResource(iconRes);
        iv.setColorFilter(ThemeTokens.COLOR_ACCENT);
        int sz = ThemeTokens.dp(this, 20);
        iv.setLayoutParams(new LinearLayout.LayoutParams(sz, sz));
        iv.setOnClickListener(listener);
        return iv;
    }

    private TextView createTextAction(String text, View.OnClickListener listener) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(ThemeTokens.COLOR_ACCENT);
        tv.setTextSize(13);
        tv.setPadding(ThemeTokens.dp(this, 8), 0, ThemeTokens.dp(this, 8), 0);
        tv.setOnClickListener(listener);
        return tv;
    }

    private int getStatusBarHeight() {
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resId > 0 ? getResources().getDimensionPixelSize(resId) : ThemeTokens.dp(this, 24);
    }
}
