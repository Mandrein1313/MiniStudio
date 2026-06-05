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
    
    // 🌟 เปลี่ยนมาเก็บวิวของแต่ละหน้าแยกกันอย่างชัดเจน ป้องกันปัญหาค่าหลุดหรือแชร์พิกัดผิดพลาด
    private TextView tvConsole;
    private TextView tvAiOutput;
    private EditText etAiInput;

    public PanelPagerAdapter(MainActivity activity) {
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            // สร้างหน้าต่างแสดงล็อกข้อความคอนโซล
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_console, parent, false);
        } else {
            // สร้างหน้าต่างของระบบแชท AI
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
            
            // 🛠️ แก้ไข: ผูก ID ให้ตรงกับไฟล์ layout_ai.xml (จาก btnSendToAi เปลี่ยนเป็น btnSendAi)
            android.view.View btnSend = holder.itemView.findViewById(R.id.btnSendAi);
            if (btnSend != null) {
                btnSend.setOnClickListener(v -> {
                    // เรียกฟังก์ชันแกนหลักใน MainActivity เพื่อประมวลผลคำถามถาม AI
                    activity.handleAiQuery();
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return 2; // แบ่งหน้าออกเป็น 2 แท็บคงที่ (Console และ AI)
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    // --- ส่วนส่งวิวกลับไปให้ MainActivity.java ดึงค่าไปใช้งานประมวลผล ---
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
