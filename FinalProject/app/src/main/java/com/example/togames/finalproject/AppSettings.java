package com.example.togames.finalproject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AppSettings {

    public static final String SAVED_THEME = "saved_theme";
    private static AppSettings instance = null;
    public boolean isDarkTheme;

    private AppSettings(boolean isDark){
        this.isDarkTheme = isDark;
    }

    public static synchronized AppSettings getInstance(Context context){
        if(null == instance){
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            boolean isDark = sharedPref.getBoolean(SAVED_THEME, false);
            instance = new AppSettings(isDark);
        }
        return instance;
    }

    public void setDark(boolean isDark){
        isDarkTheme = isDark;
    }
}
