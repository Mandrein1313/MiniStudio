package com.dev.ministudio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PanelPagerAdapter extends RecyclerView.Adapter<PanelPagerAdapter.ViewHolder> {

    private final Context context;
    private View tvConsoleView; 
    private WebView webAiOutput; // 🌟 เปลี่ยนเป็น WebView
    private EditText etAiInput;
    private ImageView btnSendAi;

    public PanelPagerAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            view = LayoutInflater.from(context).inflate(R.layout.layout_console, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.layout_ai, parent, false);
        }
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (getItemViewType(position) == 0) {
            tvConsoleView = holder.itemView.findViewById(R.id.tvConsole);
        } else {
            webAiOutput = holder.itemView.findViewById(R.id.webAiOutput); // 🌟 ผูกไอดีตัวใหม่
            etAiInput = holder.itemView.findViewById(R.id.etAiInput);
            btnSendAi = holder.itemView.findViewById(R.id.btnSendAi);
            
            // เปิดใช้งานการรันสคริปต์ JavaScript บน WebView เพื่อให้ปุ่มคัดลอกทำงานได้
            if (webAiOutput != null) {
                webAiOutput.getSettings().setJavaScriptEnabled(true);
                webAiOutput.setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"));
            }

            if (btnSendAi != null) {
                btnSendAi.setOnClickListener(v -> {
                    if (context instanceof MainActivity) {
                        try {
                            ((MainActivity) context).handleAiQuery();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
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

    public android.widget.TextView getTvConsole() { 
        return (android.widget.TextView) tvConsoleView; 
    }
    
    // 🌟 ส่งค่ากลับไปเป็น WebView ให้ MainActivity นำไปสั่งโหลดหน้าเว็บ
    public WebView getWebAiOutput() { 
        return webAiOutput; 
    }
    
    // 🌟 ฟังก์ชันสำรองดักไว้เพื่อป้องกันชิ้นส่วนอื่นใน MainActivity บิลด์พัง (ฟ้อง Cannot find symbol)
    public android.widget.TextView getTvAiOutput() {
        return null;
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
