package com.example.kfmusic.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public final class AppSettings {
    private static final String PREFS = "kfmusic_settings";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_THEME = "theme";

    public static final String LANGUAGE_SYSTEM = "system";
    public static final String LANGUAGE_EN = "en";
    public static final String LANGUAGE_TR = "tr";

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    private AppSettings() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getLanguage(Context context) {
        return prefs(context).getString(KEY_LANGUAGE, LANGUAGE_SYSTEM);
    }

    public static void setLanguage(Context context, String language) {
        prefs(context).edit().putString(KEY_LANGUAGE, language).apply();
        applyLanguage(context);
    }

    public static String getTheme(Context context) {
        return prefs(context).getString(KEY_THEME, THEME_DARK);
    }

    public static void setTheme(Context context, String theme) {
        prefs(context).edit().putString(KEY_THEME, theme).apply();
        applyTheme(context);
    }

    public static void applySavedSettings(Context context) {
        applyLanguage(context);
        applyTheme(context);
    }

    public static void applyLanguage(Context context) {
        String language = getLanguage(context);
        String tags = "";
        if (LANGUAGE_EN.equals(language)) {
            tags = "en";
        } else if (LANGUAGE_TR.equals(language)) {
            tags = "tr";
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags));
    }

    public static void applyTheme(Context context) {
        String theme = getTheme(context);
        int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if (THEME_LIGHT.equals(theme)) {
            mode = AppCompatDelegate.MODE_NIGHT_NO;
        } else if (THEME_DARK.equals(theme)) {
            mode = AppCompatDelegate.MODE_NIGHT_YES;
        }
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
