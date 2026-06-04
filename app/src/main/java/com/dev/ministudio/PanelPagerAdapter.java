package com.dev.ministudio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PanelPagerAdapter extends RecyclerView.Adapter<PanelPagerAdapter.ViewHolder> {

    // 🌟 สร้างตัวแปรเก็บ TextView แยกไว้ เพื่อส่งคืนกลับไปให้ MainActivity นำไปใช้งาน
    private TextView tvConsole;
    private TextView tvAiOutput;

    // 🌟 [แก้ไข 1] เพิ่ม Constructor แบบไม่มีอาร์กิวเมนต์ เพื่อแก้บั๊กบรรทัดที่ 299
    public PanelPagerAdapter() {
    }

    // มีเผื่อไว้ถ้าโค้ดส่วนอื่นต้องการเรียกใช้แบบส่งพารามิเตอร์
    public PanelPagerAdapter(MainActivity activity) {
    }

    // 🌟 [แก้ไข 2] เพิ่มฟังก์ชันคืนค่า TextView กลับไปให้ MainActivity ตามที่โค้ดเก่าน้าต้องการ
    public TextView getTvConsole() {
        return tvConsole;
    }

    public TextView getTvAiOutput() {
        return tvAiOutput;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.layout_console, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.layout_ai, parent, false);
        }
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // ในระบบของน้า เมื่อผูก ViewHolder เสร็จ ตัวแปรด้านบนจะถูกดึงไปใช้ผ่าน MainActivity เองครับ
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    // 🌟 [แก้ไข 3] ทำหน้าที่ผูกตัวแปรเข้ากับไอดี XML เมื่อหน้าจอแต่ละหน้าถูกสร้างขึ้นมา
    class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View itemView, int viewType) {
            super(itemView);
            if (viewType == 0) {
                tvConsole = itemView.findViewById(R.id.tvConsole);
            } else {
                tvAiOutput = itemView.findViewById(R.id.tvAiOutput);
            }
        }
    }
}
