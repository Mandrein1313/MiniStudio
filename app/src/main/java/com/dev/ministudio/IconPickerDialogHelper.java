package com.dev.ministudio;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.GridView;
import android.widget.ImageView;
import java.util.List;

public class IconPickerDialogHelper {

    // อินเตอร์เฟซเพื่อส่งค่ากลับไปยังหน้า Activity หลัก
    public interface OnIconSelectedListener {
        void onIconSelected(int resId);
    }

    public static void show(Context context, OnIconSelectedListener listener) {
        List<Integer> myIcons = IconManager.getAllIconIds(context);
        GridView gridView = new GridView(context);
        gridView.setNumColumns(5);
        gridView.setAdapter(new IconAdapter(context, myIcons));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("เลือกไอคอนแอป")
                .setView(gridView)
                .create();

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            int selectedIcon = myIcons.get(position);
            listener.onIconSelected(selectedIcon); // ส่งค่ากลับไป
            dialog.dismiss();
        });

        dialog.show();
    }
}
