package com.velqor.app.tokens;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

public final class ThemeTokens {
    private ThemeTokens() {}

    public static final int COLOR_BG_DARK = 0xFF000000;
    public static final int COLOR_BG_LIGHT = 0xFFF2F2F7;
    public static final int COLOR_SURFACE_DARK = 0xFF1C1C1E;
    public static final int COLOR_SURFACE_LIGHT = 0xFFFFFFFF;
    public static final int COLOR_SURFACE_GLASS_DARK = 0xF41C1C1E;
    public static final int COLOR_SURFACE_GLASS_LIGHT = 0xF4FFFFFF;
    public static final int COLOR_ACTION_GROUP_BG_DARK = 0xFF2C2C2E;
    public static final int COLOR_ACTION_GROUP_BG_LIGHT = 0xFFE5E5EA;
    public static final int COLOR_LABEL_DARK = 0xFFFFFFFF;
    public static final int COLOR_LABEL_LIGHT = 0xFF000000;
    public static final int COLOR_SEC_LABEL_DARK = 0x99EBEBF5;
    public static final int COLOR_SEC_LABEL_LIGHT = 0x993C3C43;
    public static final int COLOR_SEPARATOR_DARK = 0x99545458;
    public static final int COLOR_SEPARATOR_LIGHT = 0x473C3C43;
    public static final int COLOR_SEPARATOR_INSET_DARK = 0x4D545458;
    public static final int COLOR_SEPARATOR_INSET_LIGHT = 0x2E3C3C43;

    public static final int COLOR_BG = 0xFF000000;
    public static final int COLOR_SURFACE_ELEVATED = 0xFF1C1C1E;
    public static final int COLOR_SURFACE_GLASS = 0xF41C1C1E;
    public static final int COLOR_ACTION_GROUP_BG = 0xFF2C2C2E;
    public static final int COLOR_LABEL = 0xFFFFFFFF;
    public static final int COLOR_SEC_LABEL = 0x99EBEBF5;
    public static final int COLOR_SEPARATOR = 0x99545458;
    public static final int COLOR_SEPARATOR_INSET = 0x4D545458;

    public static final int ACCENT_BLUE = 0xFF0A84FF;
    public static final int ACCENT_INDIGO = 0xFF5E5CE6;
    public static final int ACCENT_PURPLE = 0xFFBF5AF2;
    public static final int ACCENT_ORANGE = 0xFFFF9F0A;
    public static final int ACCENT_MINT = 0xFF64D2FF;
    public static final int ACCENT_RED = 0xFFFF453A;
    public static final int ACCENT_GREEN = 0xFF30D158;

    public static final int COLOR_ACCENT = 0xFF0A84FF;
    public static final int COLOR_DANGER = 0xFFFF453A;
    public static final int COLOR_SUCCESS = 0xFF30D158;

    public static final int COLOR_ICON_FOLDER = 0xFF0A84FF;
    public static final int COLOR_ICON_IMAGE = 0xFFFF9F0A;
    public static final int COLOR_ICON_VIDEO = 0xFFFF375F;
    public static final int COLOR_ICON_AUDIO = 0xFFBF5AF2;
    public static final int COLOR_ICON_DOCUMENT = 0xFF64D2FF;
    public static final int COLOR_ICON_ARCHIVE = 0xFF5E5CE6;
    public static final int COLOR_ICON_APK = 0xFF30D158;
    public static final int COLOR_ICON_OTHER = 0xFF8E8E93;

    public static final int COLOR_SPECULAR_RIM = 0x33FFFFFF;
    public static final int COLOR_RIM_START = 0x59FFFFFF;
    public static final int COLOR_RIM_END = 0x0DFFFFFF;
    public static final int COLOR_DROP_BG = 0x240A84FF;
    public static final int COLOR_DROP_BORDER = 0x40FFFFFF;
    public static final int COLOR_SCRIM_BLACK = 0x73000000;

    public static final int RADIUS_GROUP_LIST = 16;
    public static final int RADIUS_SHEET_TOP = 24;
    public static final int RADIUS_INPUT_FIELD = 12;
    public static final int RADIUS_ICON_BOX = 10;
    public static final int RADIUS_TAB_BAR = 32;
    public static final int RADIUS_WATER_DROP = 24;

    public static final int SIZE_ICON_BOX = 40;
    public static final int SIZE_ICON_INSIDE = 22;
    public static final int SIZE_HEADER_BTN = 36;
    public static final int HEIGHT_TAB_BAR = 64;
    public static final int HEIGHT_WATER_DROP = 48;
    public static final int HEIGHT_ACTION_ROW = 52;
    public static final int ELEVATION_FLOATING_BAR = 6;

    public static int dp(Context context, int dp) {
        if (context == null) return dp * 2;
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * metrics.density);
    }

    public static boolean isDarkMode(Context context) {
        if (context == null) return true;
        SharedPreferences sp = context.getSharedPreferences("velqor_prefs", Context.MODE_PRIVATE);
        String mode = sp.getString("theme_mode", "dark");
        if ("light".equals(mode)) return false;
        if ("dark".equals(mode)) return true;
        int nightMask = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMask == Configuration.UI_MODE_NIGHT_YES;
    }

    public static int getAccentColor(Context context) {
        if (context == null) return COLOR_ACCENT;
        SharedPreferences sp = context.getSharedPreferences("velqor_prefs", Context.MODE_PRIVATE);
        return sp.getInt("accent_color", COLOR_ACCENT);
    }

    public static int getBackgroundColor(Context context) {
        return isDarkMode(context) ? COLOR_BG_DARK : COLOR_BG_LIGHT;
    }

    public static int getSurfaceColor(Context context) {
        return isDarkMode(context) ? COLOR_SURFACE_DARK : COLOR_SURFACE_LIGHT;
    }

    public static int getLabelColor(Context context) {
        return isDarkMode(context) ? COLOR_LABEL_DARK : COLOR_LABEL_LIGHT;
    }

    public static int getSecondaryLabelColor(Context context) {
        return isDarkMode(context) ? COLOR_SEC_LABEL_DARK : COLOR_SEC_LABEL_LIGHT;
    }

    public static int getActionGroupBgColor(Context context) {
        return isDarkMode(context) ? COLOR_ACTION_GROUP_BG_DARK : COLOR_ACTION_GROUP_BG_LIGHT;
    }

    public static int getSeparatorColor(Context context) {
        return isDarkMode(context) ? COLOR_SEPARATOR_DARK : COLOR_SEPARATOR_LIGHT;
    }

    public static int getSeparatorInsetColor(Context context) {
        return isDarkMode(context) ? COLOR_SEPARATOR_INSET_DARK : COLOR_SEPARATOR_INSET_LIGHT;
    }
}
