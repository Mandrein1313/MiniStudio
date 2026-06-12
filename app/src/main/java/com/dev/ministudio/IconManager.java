package com.dev.ministudio;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class IconManager {

    // 1. รายการ ID ของไอคอน (น้าเพิ่มชื่อไอคอนใน drawable ของน้าต่อได้เลยครับ)
    private static final int[] ICON_IDS = {
            R.drawable.ic_launcher_foreground, // ใส่ชื่อไฟล์ไอคอนของน้าที่นี่
            android.R.drawable.ic_menu_add,
            android.R.drawable.ic_menu_save,
            android.R.drawable.ic_menu_search,
            android.R.drawable.ic_menu_camera
    };

    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_SELECTED_ICON = "selected_icon_res_id";

    // ฟังก์ชันเรียกหน้าต่างเลือกไอคอน
    public static void showIconPickerDialog(final Context context, final OnIconSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("เลือกไอคอนแอป");

        GridView gridView = new GridView(context);
        gridView.setAdapter(new IconAdapter(context));
        gridView.setNumColumns(4);
        gridView.setPadding(20, 20, 20, 20);

        builder.setView(gridView);
        final AlertDialog dialog = builder.create();

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            int selectedIcon = ICON_IDS[position];
            saveSelectedIcon(context, selectedIcon);
            if (listener != null) listener.onIconSelected(selectedIcon);
            dialog.dismiss();
        });

        dialog.show();
    }

    // บันทึกไอคอนที่เลือก
    public static void saveSelectedIcon(Context context, int resId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_SELECTED_ICON, resId).apply();
    }

    // ดึงไอคอนที่เคยเลือกไว้
    public static int getSelectedIcon(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_SELECTED_ICON, R.drawable.ic_launcher_foreground); // ค่าเริ่มต้น
    }

    // Adapter สำหรับแสดงไอคอน
    private static class IconAdapter extends BaseAdapter {
        private Context context;
        public IconAdapter(Context c) { context = c; }
        public int getCount() { return ICON_IDS.length; }
        public Object getItem(int position) { return null; }
        public long getItemId(int position) { return 0; }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView = new ImageView(context);
            imageView.setLayoutParams(new GridView.LayoutParams(120, 120));
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setImageResource(ICON_IDS[position]);
            return imageView;
        }
    }

    public interface OnIconSelectedListener {
        void onIconSelected(int resId);
    }
}
