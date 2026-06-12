package com.dev.ministudio;

import android.content.Context;
import android.content.SharedPreferences;

public class IconManager {
    // ใช้ไอคอนระบบแทน เพื่อป้องกัน Error "cannot find symbol"
    public static final int[] ICON_LIST = {
            android.R.drawable.btn_star,
            android.R.drawable.ic_menu_camera,
            android.R.drawable.ic_menu_gallery,
            android.R.drawable.ic_menu_compass
    };

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_SELECTED_ICON = "selected_icon";

    public static int getSelectedIcon(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_SELECTED_ICON, android.R.drawable.btn_star);
    }

    public static void saveSelectedIcon(Context context, int resId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_SELECTED_ICON, resId).apply();
    }
}
