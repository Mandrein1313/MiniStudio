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
    
    // 🌟 ย้ายวิวจริงไปเก็บไว้ในระดับผูกมัด ViewHolder ของแต่ละหน้าแทน เพื่อกันค่าหลุดตอนสลับหน้าจอหรือกดขยาย
    private ViewHolder consoleHolder;
    private ViewHolder aiHolder;

    public PanelPagerAdapter(MainActivity activity) {
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_console, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_ai, parent, false);
        }
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position == 0) {
            this.consoleHolder = holder;
        } else {
            this.aiHolder = holder;
            
            // 🛠️ ผูกเหตุการณ์กดปุ่มส่งหา AI จากวิวจริงใน Holder ปัจจุบัน
            if (holder.btnSendAi != null) {
                holder.btnSendAi.setOnClickListener(v -> {
                    activity.handleAiQuery();
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return 2; 
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    // --- ส่วนส่งวิวกลับไปให้ MainActivity.java ดึงค่าไปใช้งานอย่างปลอดภัย พิกัดไม่เพี้ยน ---
    public TextView getTvConsole() {
        return (consoleHolder != null) ? consoleHolder.tvConsole : null;
    }

    public TextView getTvAiOutput() {
        return (aiHolder != null) ? aiHolder.tvAiOutput : null;
    }

    public EditText getEtAiInput() {
        return (aiHolder != null) ? aiHolder.etAiInput : null;
    }

    // 🌟 ปรับปรุงกล่องเก็บวิวย่อย (ViewHolder) ให้ทำหน้าที่หาพิกัดและเฝ้าวิวไว้ให้มั่นคง
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvConsole;
        TextView tvAiOutput;
        EditText etAiInput;
        View btnSendAi;

        public ViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            if (viewType == 0) {
                tvConsole = itemView.findViewById(R.id.tvConsole);
            } else {
                tvAiOutput = itemView.findViewById(R.id.tvAiOutput);
                etAiInput = itemView.findViewById(R.id.etAiInput);
                btnSendAi = itemView.findViewById(R.id.btnSendAi);
            }
        }
    }
}
