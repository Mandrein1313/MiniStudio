package com.dev.ministudio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PanelPagerAdapter extends RecyclerView.Adapter<PanelPagerAdapter.ViewHolder> {

    private final Context context;
    private TextView tvConsole;
    private TextView tvAiOutput;
    private EditText etAiInput;
    private ImageButton btnSendAi;

    public PanelPagerAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            // โหลด layout_console.xml ดั้งเดิมที่น้ามีอยู่แล้ว
            view = LayoutInflater.from(context).inflate(R.layout.layout_console, parent, false);
        } else {
            // โหลด layout_ai.xml ดั้งเดิมที่น้ามีอยู่แล้ว
            view = LayoutInflater.from(context).inflate(R.layout.layout_ai, parent, false);
        }
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (getItemViewType(position) == 0) {
            tvConsole = (TextView) holder.itemView.findViewById(R.id.tvConsole);
        } else {
            tvAiOutput = (TextView) holder.itemView.findViewById(R.id.tvAiOutput);
            etAiInput = (EditText) holder.itemView.findViewById(R.id.etAiInput);
            btnSendAi = (ImageButton) holder.itemView.findViewById(R.id.btnSendAi);
            
            // ผูกฟังก์ชันเวลากดส่งจากในหน้า Dialog AI เต็มจอให้วาร์ปไปเรียก MainActivity
            if (btnSendAi != null) {
                btnSendAi.setOnClickListener(v -> {
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).handleAiQuery();
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return 2; // มี 2 แท็บ: 0 = Console, 1 = AI
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    // --- Getters สำหรับให้ MainActivity เรียกใช้ดึงอัปเดตแบบเรียลไทม์ ---
    public TextView getTvConsole() { return tvConsole; }
    public TextView getTvAiOutput() { return tvAiOutput; }
    public EditText getEtAiInput() { return etAiInput; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
        }
    }
}
