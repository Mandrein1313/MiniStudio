package com.dev.ministudio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PanelPagerAdapter extends RecyclerView.Adapter<PanelPagerAdapter.ViewHolder> {

    private final MainActivity activity;
    // 🌟 สร้างตัวแปรเก็บข้อความแยกกันระหว่างหน้า Console และหน้า AI
    private String consoleText = "ยังไม่มี Build Log...";
    private String aiText = "พิมพ์คำถามด้านล่างเพื่อคุยกับ AI ของน้าได้เลยครับ";

    public PanelPagerAdapter(MainActivity activity) {
        this.activity = activity;
    }

    // ฟังก์ชันสำหรับสั่งอัปเดตข้อความจากฝั่ง MainActivity
    public void updateConsoleText(String text) {
        this.consoleText = text;
        notifyItemChanged(0); // แจ้งเตือนแท็บ Console (ตำแหน่ง 0) ให้อัปเดตจอ
    }

    public void updateAiText(String text) {
        this.aiText = text;
        notifyItemChanged(1); // แจ้งเตือนแท็บ AI (ตำแหน่ง 1) ให้อัปเดตจอ
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
        // 🌟 [แก้ไข] นำข้อความที่เก็บไว้ไปพ่นลง TextView ของแท็บนั้น ๆ
        if (position == 0) {
            if (holder.tvConsole != null) {
                holder.tvConsole.setText(consoleText);
            }
        } else {
            if (holder.tvAiOutput != null) {
                holder.tvAiOutput.setText(aiText);
            }
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        // 🌟 [แก้ไข] ประกาศตัวแปรเพื่ออ้างอิงถึง TextView ภายในไฟล์ XML ย่อย
        TextView tvConsole;
        TextView tvAiOutput;

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
