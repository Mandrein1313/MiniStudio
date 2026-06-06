package com.dev.ministudio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PanelPagerAdapter extends RecyclerView.Adapter<PanelPagerAdapter.ViewHolder> {

    private final Context context;
    private View tvConsoleView; 
    private WebView webAiOutput; 
    private EditText etAiInput;
    private ImageView btnSendAi;
    private ImageView btnStopAiVoice; 
    private LinearLayout btnAiFixer; // ➕ ตัวแปรผูกปุ่มแก้บั๊กของหน้าคอนโซล

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
            btnAiFixer = holder.itemView.findViewById(R.id.btnAiFixer); // ➕ ผูกไอดีปุ่มแก้บั๊ก

            // 🎯 สั่งทำงานเมื่อผู้ใช้กดปุ่มแก้บั๊กในหน้า Console
            if (btnAiFixer != null) {
                btnAiFixer.setOnClickListener(v -> {
                    if (context instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) context;
                        mainActivity.triggerAiErrorFixerPipeline(); // สั่งยิงข้อมูลชุดพังหา AI ทันที
                    }
                });
            }
        } else {
            webAiOutput = holder.itemView.findViewById(R.id.webAiOutput); 
            etAiInput = holder.itemView.findViewById(R.id.etAiInput);
            btnSendAi = holder.itemView.findViewById(R.id.btnSendAi);
            btnStopAiVoice = holder.itemView.findViewById(R.id.btnStopAiVoice); 
            
            if (webAiOutput != null) {
                webAiOutput.getSettings().setJavaScriptEnabled(true);
                webAiOutput.getSettings().setDomStorageEnabled(true);
                webAiOutput.setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"));
                
                if (context instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) context;
                    webAiOutput.removeJavascriptInterface("AndroidBridge");
                    webAiOutput.addJavascriptInterface(mainActivity.new WebAppInterface(context), "AndroidBridge");
                }
            }

            if (btnStopAiVoice != null) {
                btnStopAiVoice.setOnClickListener(v -> {
                    if (context instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) context;
                        if (mainActivity.aiLayoutAnalyzer != null) {
                            mainActivity.aiLayoutAnalyzer.stopSpeaking();
                            Toast.makeText(context, "🤫 หยุดเล่นเสียงชั่วคราว", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
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
    public int getItemCount() { return 2; }

    @Override
    public int getItemViewType(int position) { return position; }

    public android.widget.TextView getTvConsole() { return (android.widget.TextView) tvConsoleView; }
    public WebView getWebAiOutput() { return webAiOutput; }
    public android.widget.TextView getTvAiOutput() { return null; }
    public EditText getEtAiInput() { return etAiInput; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView, int viewType) { super(itemView); }
    }
}