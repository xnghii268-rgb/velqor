package com.velqor.app.tokens;

import android.content.Context;
import android.util.DisplayMetrics;

public final class ThemeTokens {
    private ThemeTokens() {}

    // Core Colors (Dark Theme Apple Cupertino)
    public static final int COLOR_BG = 0xFF000000;
    public static final int COLOR_SURFACE_ELEVATED = 0xFF1C1C1E;
    public static final int COLOR_SURFACE_GLASS = 0xF41C1C1E; // 96% alpha glass fallback
    public static final int COLOR_ACTION_GROUP_BG = 0xFF2C2C2E;
    public static final int COLOR_LABEL = 0xFFFFFFFF;
    public static final int COLOR_SEC_LABEL = 0x99EBEBF5;      // 60% alpha
    public static final int COLOR_SEPARATOR = 0x99545458;      // 60% alpha
    public static final int COLOR_SEPARATOR_INSET = 0x4D545458;// 30% alpha (0.5dp line)
    public static final int COLOR_ACCENT = 0xFF0A84FF;         // Apple Blue
    public static final int COLOR_DANGER = 0xFFFF453A;         // Apple Red
    public static final int COLOR_SUCCESS = 0xFF30D158;        // Apple Green

    // Semantic Icon Colors (High contrast >= 3:1)
    public static final int COLOR_ICON_FOLDER = 0xFF0A84FF;
    public static final int COLOR_ICON_IMAGE = 0xFFFF9F0A;
    public static final int COLOR_ICON_VIDEO = 0xFFFF375F;
    public static final int COLOR_ICON_AUDIO = 0xFFBF5AF2;
    public static final int COLOR_ICON_DOCUMENT = 0xFF64D2FF;
    public static final int COLOR_ICON_ARCHIVE = 0xFF5E5CE6;
    public static final int COLOR_ICON_APK = 0xFF30D158;
    public static final int COLOR_ICON_OTHER = 0xFF8E8E93;

    // Specular Rim & Shading
    public static final int COLOR_SPECULAR_RIM = 0x33FFFFFF;
    public static final int COLOR_RIM_START = 0x59FFFFFF;       // 35% white top-left
    public static final int COLOR_RIM_END = 0x0DFFFFFF;         // 5% white bottom-right
    public static final int COLOR_DROP_BG = 0x240A84FF;         // 14% accent tint
    public static final int COLOR_DROP_BORDER = 0x40FFFFFF;     // 25% white border
    public static final int COLOR_SCRIM_BLACK = 0x73000000;     // 45% black scrim

    // Layout Dimensions in dp
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

    // DP to PX Helper
    public static int dp(Context context, int dp) {
        if (context == null) return dp * 2;
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * metrics.density);
    }
}
