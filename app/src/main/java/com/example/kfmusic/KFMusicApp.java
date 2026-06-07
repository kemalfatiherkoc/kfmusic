package com.example.kfmusic;

import android.app.Application;

import com.example.kfmusic.utils.AppSettings;

public class KFMusicApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppSettings.applySavedSettings(this);
    }
}
