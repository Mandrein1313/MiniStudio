package com.dev.ministudio;

import android.os.Handler;
import android.os.Looper;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import com.dev.ministudio.editor.EditorController;
import com.dev.ministudio.editor.SyntaxHighlighter;

public class EditorManager {
    private final MainActivity activity;
    private final EditText codeEditor;
    private final SyntaxHighlighter syntaxHighlighter;
    private final Handler highlightHandler = new Handler(Looper.getMainLooper());
    private final Handler autoSaveHandler = new Handler(Looper.getMainLooper());
    private Runnable highlightRunnable;
    private Runnable saveRunnable;

    public EditorManager(MainActivity activity, EditText codeEditor, TextView tvSaveStatus, TextView lineNumbers) {
        this.activity = activity;
        this.codeEditor = codeEditor;
        this.syntaxHighlighter = new SyntaxHighlighter();
        new EditorController(codeEditor, lineNumbers);
        setupTextWatcher(tvSaveStatus);
    }

    private void setupTextWatcher(TextView tvSaveStatus) {
        codeEditor.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvSaveStatus.setText("Editing...");
                tvSaveStatus.setTextColor(android.graphics.Color.parseColor("#FFB74D"));

                // Highlighting (Debounce 300ms)
                highlightHandler.removeCallbacks(highlightRunnable);
                highlightRunnable = () -> {
                    new Thread(() -> {
                        if (activity.getCurrentProject() != null && activity.getCurrentProject().getCurrentOpenFile() != null) {
                            syntaxHighlighter.highlight(codeEditor.getText(), activity.getCurrentProject().getCurrentOpenFile());
                        }
                    }).start();
                };
                highlightHandler.postDelayed(highlightRunnable, 300);

                // Auto-Save (Debounce 1500ms)
                autoSaveHandler.removeCallbacks(saveRunnable);
                saveRunnable = () -> {
                    activity.saveFile();
                    tvSaveStatus.setText("Saved");
                    tvSaveStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
                };
                autoSaveHandler.postDelayed(saveRunnable, 1500);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }
}
