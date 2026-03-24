package me.anisimov.desertbird;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {

    private static final String PREFS = "desert_bird_prefs";
    private static final String KEY_HIGH_SCORE = "high_score";
    private static final String KEY_MUTED = "muted";

    public static int loadHighScore(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_HIGH_SCORE, 0);
    }

    public static void saveHighScore(Context context, int highScore) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_HIGH_SCORE, highScore).apply();
    }

    public static boolean loadMuted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_MUTED, false);
    }

    public static void saveMuted(Context context, boolean muted) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_MUTED, muted).apply();
    }
}