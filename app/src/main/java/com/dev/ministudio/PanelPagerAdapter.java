package com.dev.ministudio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.Map;

public class PanelPagerAdapter extends RecyclerView.Adapter<PanelPagerAdapter.ViewHolder> {

    private final Map<Integer, ViewHolder> boundHolders = new HashMap<>();

    public PanelPagerAdapter() {}
    public PanelPagerAdapter(MainActivity activity) {}

    public TextView getTvConsole() {
        ViewHolder holder = boundHolders.get(0);
        return holder != null ? holder.tvConsole : null;
    }

    public TextView getTvAiOutput() {
        ViewHolder holder = boundHolders.get(1);
        return holder != null ? holder.tvAiOutput : null;
    }

    // 🌟 เพิ่มฟังก์ชันดึงช่องพิมพ์ EditText ส่งไปให้ MainActivity
    public EditText getEtAiInput() {
        ViewHolder holder = boundHolders.get(1);
        return holder != null ? holder.etAiInput : null;
    }

    // 🌟 เพิ่มฟังก์ชันดึงปุ่มกด Button ส่งไปให้ MainActivity
    public Button getBtnSendToAi() {
        ViewHolder holder = boundHolders.get(1);
        return holder != null ? holder.btnSendToAi : null;
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
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_console, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_ai, parent, false);
        }
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        boundHolders.put(position, holder);
        
        // 🌟 ตั้งค่าให้ปุ่มกดทำงานเชื่อมกับ MainActivity ทันทีเมื่อหน้า AI ถูกสร้างขึ้นมา
        if (position == 1 && holder.btnSendToAi != null) {
            holder.btnSendToAi.setOnClickListener(v -> {
                // ตรวจสอบว่าใน MainActivity มีฟังก์ชันสำหรับประมวลผลคำถาม AI ไหม
                if (v.getContext() instanceof MainActivity) {
                    // เรียกเมธอดส่งคำถามของน้า (ตัวอย่าง: สมมุติว่าชื่อเมธอดดั้งเดิมคือ handleAiQuery)
                    ((MainActivity) v.getContext()).handleAiQuery(); 
                }
            });
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        boundHolders.values().remove(holder);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvConsole;
        TextView tvAiOutput;
        EditText etAiInput;  // 🌟 เพิ่มตัวแปรเก็บในคลาสย่อย
        Button btnSendToAi;  // 🌟 เพิ่มตัวแปรเก็บในคลาสย่อย

        ViewHolder(View itemView, int viewType) {
            super(itemView);
            if (viewType == 0) {
                tvConsole = itemView.findViewById(R.id.tvConsole);
            } else {
                tvAiOutput = itemView.findViewById(R.id.tvAiOutput);
                etAiInput = itemView.findViewById(R.id.etAiInput);   // 🌟 ผูกไอดีช่องพิมพ์
                btnSendToAi = itemView.findViewById(R.id.btnSendToAi); // 🌟 ผูกไอดีปุ่มส่ง
            }
        }
    }
}
