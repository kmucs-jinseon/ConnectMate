package com.example.connectmate.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * ThemeManager - Handles theme switching between light and dark mode
 */
public class ThemeManager {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    // Theme modes
    public static final int MODE_LIGHT = 0;
    public static final int MODE_DARK = 1;
    public static final int MODE_SYSTEM = 2;

    /**
     * Apply the saved theme preference
     */
    public static void applyTheme(Context context) {
        int themeMode = getThemeMode(context);
        applyTheme(themeMode);
    }

    /**
     * Apply a specific theme mode
     */
    public static void applyTheme(int mode) {
        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case MODE_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    /**
     * Save theme mode preference
     */
    public static void setThemeMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        applyTheme(mode);
    }

    /**
     * Get saved theme mode
     */
    public static int getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM); // Default to system preference
    }

    /**
     * Check if dark mode is currently active
     */
    public static boolean isDarkModeActive(Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode
            & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Get theme mode name for display
     */
    public static String getThemeModeName(int mode) {
        switch (mode) {
            case MODE_LIGHT:
                return "라이트 모드";
            case MODE_DARK:
                return "다크 모드";
            case MODE_SYSTEM:
            default:
                return "시스템 설정 따르기";
        }
    }
}
