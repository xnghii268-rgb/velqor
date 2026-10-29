package com.velqor.app.ui;

import android.text.Spannable;
import android.text.style.ForegroundColorSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SyntaxHighlighter {
    private SyntaxHighlighter() {}

    private static final int COLOR_KEYWORD = 0xFFFF375F;
    private static final int COLOR_STRING = 0xFF30D158;
    private static final int COLOR_NUMBER = 0xFF64D2FF;
    private static final int COLOR_COMMENT = 0xFF8E8E93;
    private static final int COLOR_TAG = 0xFFFF9F0A;
    private static final int COLOR_ATTR = 0xFFBF5AF2;

    private static final Pattern PATTERN_JAVA_KEYWORDS = Pattern.compile(
            "\\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|true|false|null)\\b");

    private static final Pattern PATTERN_JS_KEYWORDS = Pattern.compile(
            "\\b(async|await|break|case|catch|class|const|continue|debugger|default|delete|do|else|export|extends|finally|for|function|if|import|in|instanceof|let|new|return|super|switch|this|throw|try|typeof|var|void|while|with|yield|true|false|null|undefined)\\b");

    private static final Pattern PATTERN_PY_KEYWORDS = Pattern.compile(
            "\\b(and|as|assert|async|await|break|class|continue|def|del|elif|else|except|finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|pass|raise|return|try|while|with|yield|True|False|None)\\b");

    private static final Pattern PATTERN_SH_KEYWORDS = Pattern.compile(
            "\\b(if|then|else|elif|fi|case|esac|for|select|while|until|do|done|in|function|time|return|exit|export|local|echo|read|cd|mkdir|rm)\\b");

    private static final Pattern PATTERN_STRINGS = Pattern.compile("\"(\\\\.|[^\"])*\"|'(\\\\.|[^'])*'");
    private static final Pattern PATTERN_NUMBERS = Pattern.compile("\\b(0x[0-9a-fA-F]+|\\d+(\\.\\d+)?)\\b");
    private static final Pattern PATTERN_LINE_COMMENT = Pattern.compile("//.*");
    private static final Pattern PATTERN_HASH_COMMENT = Pattern.compile("#.*");
    private static final Pattern PATTERN_BLOCK_COMMENT = Pattern.compile("/\\*[\\s\\S]*?\\*/");
    private static final Pattern PATTERN_HTML_TAGS = Pattern.compile("</?[a-zA-Z0-9]+(\\s+[^>]*)?>");
    private static final Pattern PATTERN_MD_HEADERS = Pattern.compile("^(#{1,6}\\s+.*)$", Pattern.MULTILINE);

    public static void highlight(Spannable text, String fileName) {
        if (text == null || text.length() == 0 || text.length() > 500000) return;
        String name = (fileName != null) ? fileName.toLowerCase() : "";

        ForegroundColorSpan[] oldSpans = text.getSpans(0, text.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan s : oldSpans) {
            text.removeSpan(s);
        }

        if (name.endsWith(".java") || name.endsWith(".kt") || name.endsWith(".c") || name.endsWith(".cpp")) {
            applyPattern(text, PATTERN_JAVA_KEYWORDS, COLOR_KEYWORD);
            applyPattern(text, PATTERN_NUMBERS, COLOR_NUMBER);
            applyPattern(text, PATTERN_STRINGS, COLOR_STRING);
            applyPattern(text, PATTERN_LINE_COMMENT, COLOR_COMMENT);
            applyPattern(text, PATTERN_BLOCK_COMMENT, COLOR_COMMENT);
        } else if (name.endsWith(".js") || name.endsWith(".ts") || name.endsWith(".json")) {
            applyPattern(text, PATTERN_JS_KEYWORDS, COLOR_KEYWORD);
            applyPattern(text, PATTERN_NUMBERS, COLOR_NUMBER);
            applyPattern(text, PATTERN_STRINGS, COLOR_STRING);
            applyPattern(text, PATTERN_LINE_COMMENT, COLOR_COMMENT);
            applyPattern(text, PATTERN_BLOCK_COMMENT, COLOR_COMMENT);
        } else if (name.endsWith(".py")) {
            applyPattern(text, PATTERN_PY_KEYWORDS, COLOR_KEYWORD);
            applyPattern(text, PATTERN_NUMBERS, COLOR_NUMBER);
            applyPattern(text, PATTERN_STRINGS, COLOR_STRING);
            applyPattern(text, PATTERN_HASH_COMMENT, COLOR_COMMENT);
        } else if (name.endsWith(".sh") || name.endsWith(".bash")) {
            applyPattern(text, PATTERN_SH_KEYWORDS, COLOR_KEYWORD);
            applyPattern(text, PATTERN_NUMBERS, COLOR_NUMBER);
            applyPattern(text, PATTERN_STRINGS, COLOR_STRING);
            applyPattern(text, PATTERN_HASH_COMMENT, COLOR_COMMENT);
        } else if (name.endsWith(".html") || name.endsWith(".xml")) {
            applyPattern(text, PATTERN_HTML_TAGS, COLOR_TAG);
            applyPattern(text, PATTERN_STRINGS, COLOR_STRING);
        } else if (name.endsWith(".md")) {
            applyPattern(text, PATTERN_MD_HEADERS, COLOR_KEYWORD);
            applyPattern(text, PATTERN_STRINGS, COLOR_STRING);
        }
    }

    private static void applyPattern(Spannable text, Pattern pattern, int color) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            text.setSpan(new ForegroundColorSpan(color), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
