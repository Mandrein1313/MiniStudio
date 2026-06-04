package com.dev.ministudio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PanelPagerAdapter extends RecyclerView.Adapter<PanelPagerAdapter.ViewHolder> {

    private TextView tvConsole;
    private TextView tvAiOutput;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_console, parent, false);
            tvConsole = view.findViewById(R.id.tvConsole);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_ai, parent, false);
            tvAiOutput = view.findViewById(R.id.tvAiOutput);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // ปล่อยว่างไว้ได้เนื่องจากเราผูกไอดีตรงใน onCreateViewHolder ตอนวิวถูกสร้างแล้วครับ
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    // เมธอดดึงวัตถุ TextView ออกไปให้ MainActivity ใช้ส่งค่าเข้า Console Log
    public TextView getTvConsole() {
        return tvConsole;
    }

    // เมธอดดึงวัตถุ TextView ออกไปให้ MainActivity ใช้พิมพ์คำตอบ AI
    public TextView getTvAiOutput() {
        return tvAiOutput;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
