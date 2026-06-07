package com.example.kfmusic.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import android.util.Log;

public class AudioEffectManager {
    private static final String TAG = "AudioEffectManager";
    private static final String PREF_NAME = "kfmusic_effects";
    
    private static AudioEffectManager instance;
    private final SharedPreferences prefs;

    private Equalizer equalizer;
    private BassBoost bassBoost;
    private Virtualizer virtualizer;

    private boolean isEnabled = false;
    private short activePreset = 0;
    private short bassStrength = 0;
    private short virtualizerStrength = 0;

    private AudioEffectManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadSettings();
    }

    public static synchronized AudioEffectManager getInstance(Context context) {
        if (instance == null) {
            instance = new AudioEffectManager(context);
        }
        return instance;
    }

    public void initEffects(int audioSessionId) {
        try {
            releaseEffects();

            // Equalizer
            equalizer = new Equalizer(0, audioSessionId);
            equalizer.setEnabled(isEnabled);

            // Bass Boost
            bassBoost = new BassBoost(0, audioSessionId);
            bassBoost.setEnabled(isEnabled);
            if (isEnabled) {
                bassBoost.setStrength(bassStrength);
            }

            // Virtualizer
            virtualizer = new Virtualizer(0, audioSessionId);
            virtualizer.setEnabled(isEnabled);
            if (isEnabled) {
                virtualizer.setStrength(virtualizerStrength);
            }

            applySavedPresets();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing audio effects", e);
        }
    }

    public void releaseEffects() {
        if (equalizer != null) {
            equalizer.release();
            equalizer = null;
        }
        if (bassBoost != null) {
            bassBoost.release();
            bassBoost = null;
        }
        if (virtualizer != null) {
            virtualizer.release();
            virtualizer = null;
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        prefs.edit().putBoolean("enabled", enabled).apply();

        if (equalizer != null) equalizer.setEnabled(enabled);
        if (bassBoost != null) bassBoost.setEnabled(enabled);
        if (virtualizer != null) virtualizer.setEnabled(enabled);
    }

    public short getBassStrength() {
        return bassStrength;
    }

    public void setBassStrength(short strength) {
        this.bassStrength = strength;
        prefs.edit().putInt("bass_strength", strength).apply();
        if (bassBoost != null && bassBoost.getStrengthSupported()) {
            try {
                bassBoost.setStrength(strength);
            } catch (Exception e) {
                Log.e(TAG, "Error setting bass strength", e);
            }
        }
    }

    public short getVirtualizerStrength() {
        return virtualizerStrength;
    }

    public void setVirtualizerStrength(short strength) {
        this.virtualizerStrength = strength;
        prefs.edit().putInt("virtualizer_strength", strength).apply();
        if (virtualizer != null && virtualizer.getStrengthSupported()) {
            try {
                virtualizer.setStrength(strength);
            } catch (Exception e) {
                Log.e(TAG, "Error setting virtualizer strength", e);
            }
        }
    }

    public short getActivePreset() {
        return activePreset;
    }

    public void applyPreset(short presetIndex) {
        this.activePreset = presetIndex;
        prefs.edit().putInt("active_preset", presetIndex).apply();
        if (equalizer != null && presetIndex >= 0 && presetIndex < equalizer.getNumberOfPresets()) {
            try {
                equalizer.usePreset(presetIndex);
            } catch (Exception e) {
                Log.e(TAG, "Error using equalizer preset", e);
            }
        }
    }

    public short getNumberOfPresets() {
        return equalizer != null ? equalizer.getNumberOfPresets() : 0;
    }

    public String getPresetName(short index) {
        return (equalizer != null && index >= 0 && index < equalizer.getNumberOfPresets()) 
                ? equalizer.getPresetName(index) 
                : "Default";
    }

    public void setBandLevel(short band, short level) {
        if (equalizer != null) {
            try {
                equalizer.setBandLevel(band, level);
                prefs.edit().putInt("band_" + band, level).apply();
            } catch (Exception e) {
                Log.e(TAG, "Error setting band level", e);
            }
        }
    }

    public short getBandLevel(short band) {
        if (equalizer != null) {
            try {
                return equalizer.getBandLevel(band);
            } catch (Exception e) {
                // ignore
            }
        }
        return (short) prefs.getInt("band_" + band, 0);
    }

    private void loadSettings() {
        isEnabled = prefs.getBoolean("enabled", false);
        activePreset = (short) prefs.getInt("active_preset", 0);
        bassStrength = (short) prefs.getInt("bass_strength", 0);
        virtualizerStrength = (short) prefs.getInt("virtualizer_strength", 0);
    }

    private void applySavedPresets() {
        if (equalizer != null) {
            applyPreset(activePreset);
        }
    }
}
