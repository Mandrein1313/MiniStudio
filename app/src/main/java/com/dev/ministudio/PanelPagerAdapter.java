package com.dev.ministudio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PanelPagerAdapter extends RecyclerView.Adapter<PanelPagerAdapter.ViewHolder> {

    private final Context context;

    private TextView tvConsole;
    private TextView tvAiOutput;
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
            view = LayoutInflater.from(context)
                    .inflate(R.layout.layout_console, parent, false);
        } else {
            view = LayoutInflater.from(context)
                    .inflate(R.layout.layout_ai, parent, false);
        }

        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        if (getItemViewType(position) == 0) {

            tvConsole = holder.itemView.findViewById(R.id.tvConsole);

        } else {

            tvAiOutput = holder.itemView.findViewById(R.id.tvAiOutput);
            etAiInput = holder.itemView.findViewById(R.id.etAiInput);

            // layout_ai.xml ใช้ ImageView
            btnSendAi = holder.itemView.findViewById(R.id.btnSendAi);

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

    public TextView getTvConsole() {
        return tvConsole;
    }

    public TextView getTvAiOutput() {
        return tvAiOutput;
    }

    public EditText getEtAiInput() {
        return etAiInput;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
        }
    }
}
