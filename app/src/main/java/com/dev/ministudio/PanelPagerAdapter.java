package com.dev.ministudio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PanelPagerAdapter extends RecyclerView.Adapter<PanelPagerAdapter.ViewHolder> {

    private final MainActivity activity;
    private TextView tvConsole;
    private TextView tvAiOutput;
    private EditText etAiInput;

    public PanelPagerAdapter(MainActivity activity) {
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // viewType 0 = หน้า Console, viewType 1 = หน้า AI ถามตอบ
        View view;
        if (viewType == 0) {
            // สร้างหน้าต่างดำๆ แสดงล็อกข้อความคอนโซล
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_console, parent, false);
        } else {
            // สร้างหน้าต่างของระบบแชท AI (ที่มีกล่องแชทประวัติประดับอยู่ข้างใน)
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_ai, parent, false);
        }
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position == 0) {
            this.tvConsole = holder.itemView.findViewById(R.id.tvConsole);
        } else {
            this.tvAiOutput = holder.itemView.findViewById(R.id.tvAiOutput);
            this.etAiInput = holder.itemView.findViewById(R.id.etAiInput);
            
            // 🌟 ดักเหตุการณ์ปุ่มส่งเงิน/ถาม AI ภายในแท็บย่อยให้วิ่งไปสั่งงานฟังก์ชันแกนหลักใน MainActivity
            android.view.View btnSend = holder.itemView.findViewById(R.id.btnSendToAi);
            if (btnSend != null) {
                btnSend.setOnClickListener(v -> activity.handleAiQuery());
            }
        }
    }

    @Override
    public int getItemCount() {
        return 2; // ยืนยันระบบแบ่งหน้าออกเป็น 2 แท็บคงที่
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    // --- ส่วนรับส่งพิกัดวิวัตถุกลับไปให้ MainActivity.java ดึงค่าไปประมวลผล ---
    public TextView getTvConsole() {
        return tvConsole;
    }

    public TextView getTvAiOutput() {
        return tvAiOutput;
    }

    public EditText getEtAiInput() {
        return etAiInput;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
        }
    }
}
