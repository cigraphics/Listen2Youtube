package com.listen2youtube;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by khang on 28/03/2016.
 * Email: khang.neon.1997@gmail.com
 */
public class Settings {
    private static final String TAG = "Settings";
    private static final String IS_ONLY_WIFI = "IS_ONLY_WIFI";
    private static final String IS_REPEAT = "IS_REPEAT";
    private static final String IS_SHUFFLE = "IS_SHUFFLE";
    public static Context context;

    public static void init(Context ctx) {
        context = ctx;
    }

    private static SharedPreferences getSharedPreferences() {
        return context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
    }

    public static boolean isOnlyWifi(){
        return getSharedPreferences().getBoolean(IS_ONLY_WIFI, true);
    }

    public static boolean isRepeat() {
        return getSharedPreferences().getBoolean(IS_REPEAT, true);
    }

    public static boolean isShuffle() {
        return getSharedPreferences().getBoolean(IS_SHUFFLE, false);
    }

    public static void setRepeat(boolean repeat) {
        getSharedPreferences().edit().putBoolean(IS_REPEAT, repeat).apply();
    }

    public static void setShuffle(boolean shuffle) {
        getSharedPreferences().edit().putBoolean(IS_SHUFFLE, shuffle).apply();
    }
}
