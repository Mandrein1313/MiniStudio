package com.dev.ministudio;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import com.dev.ministudio.editor.EditorController;
import com.dev.ministudio.editor.SyntaxHighlighter;

public class EditorManager {

    private final MainActivity activity;
    private final EditText codeEditor;
    private final SyntaxHighlighter syntaxHighlighter;

    private final Handler highlightHandler =
            new Handler(Looper.getMainLooper());

    private final Handler autoSaveHandler =
            new Handler(Looper.getMainLooper());

    private Runnable highlightRunnable;
    private Runnable saveRunnable;

    // ✅ กัน loop ตอน setText()
    private boolean isProgrammaticChange = false;

    // ✅ กัน highlight ซ้อน
    private boolean isHighlighting = false;

    public EditorManager(
            MainActivity activity,
            EditText codeEditor,
            TextView tvSaveStatus,
            TextView lineNumbers
    ) {

        this.activity = activity;
        this.codeEditor = codeEditor;

        this.syntaxHighlighter =
                new SyntaxHighlighter();

        new EditorController(
                codeEditor,
                lineNumbers
        );

        setupTextWatcher(tvSaveStatus);
    }

    // ✅ MainActivity ใช้เรียก
    public void setProgrammaticChange(boolean val) {
        this.isProgrammaticChange = val;
    }

    // ✅ MainActivity ใช้ตรวจสอบ
    public boolean isProgrammaticChange() {
        return isProgrammaticChange;
    }

    private void setupTextWatcher(TextView tvSaveStatus) {

        codeEditor.addTextChangedListener(
                new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {

            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {

                // ✅ ถ้าโปรแกรมกำลัง setText()
                // ไม่ต้องทำ highlight
                if (isProgrammaticChange) {
                    return;
                }

                tvSaveStatus.setText("Editing...");
                tvSaveStatus.setTextColor(
                        android.graphics.Color.parseColor("#FFB74D")
                );

                // =========================
                // ✅ DELAYED HIGHLIGHT
                // =========================

                if (highlightRunnable != null) {
                    highlightHandler.removeCallbacks(
                            highlightRunnable
                    );
                }

                highlightRunnable = () -> {

                    // ✅ กัน highlight ซ้อน
                    if (isHighlighting) {
                        return;
                    }

                    isHighlighting = true;

                    new Thread(() -> {

                        try {

                            if (activity != null &&
                                    activity.getCurrentProject() != null &&
                                    activity.getCurrentProject().getCurrentOpenFile() != null) {

                                Editable editable =
                                        codeEditor.getText();

                                // ✅ กันไฟล์ใหญ่เกิน
                                if (editable.length() > 300000) {

                                    activity.runOnUiThread(() -> {
                                        tvSaveStatus.setText(
                                                "Large file mode"
                                        );
                                    });

                                    isHighlighting = false;
                                    return;
                                }

                                syntaxHighlighter.highlight(
                                        editable,
                                        activity.getCurrentProject()
                                                .getCurrentOpenFile()
                                );
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        activity.runOnUiThread(() -> {
                            isHighlighting = false;
                        });

                    }).start();
                };

                // ✅ delay 700ms
                highlightHandler.postDelayed(
                        highlightRunnable,
                        700
                );

                // =========================
                // ✅ AUTO SAVE
                // =========================

                if (saveRunnable != null) {
                    autoSaveHandler.removeCallbacks(
                            saveRunnable
                    );
                }

                saveRunnable = () -> {

                    if (activity != null) {

                        activity.saveFile();

                        tvSaveStatus.setText("Saved");

                        tvSaveStatus.setTextColor(
                                android.graphics.Color.parseColor("#4CAF50")
                        );
                    }
                };

                // ✅ save หลังหยุดพิมพ์ 1.5 วิ
                autoSaveHandler.postDelayed(
                        saveRunnable,
                        1500
                );
            }

            @Override
            public void afterTextChanged(
                    Editable s
            ) {

            }
        });
    }
}