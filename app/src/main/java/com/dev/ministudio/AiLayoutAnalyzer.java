package com.dev.ministudio;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import com.dev.ministudio.ai.GeminiAssistant;

public class AiLayoutAnalyzer {

    private final GeminiAssistant aiAssistant;
    private final Handler mainHandler;
    private TextToSpeech tts;

    public interface OnAnalysisListener {
        void onStart();
        void onSuccess(String result);
        void onError(String error);
    }

    public AiLayoutAnalyzer(Context context) {
        this.aiAssistant = new GeminiAssistant();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("th", "TH"));
            }
        });
    }

    public void analyzeCode(String fileName, String rawCode, final OnAnalysisListener listener) {
        if (listener != null) listener.onStart();

        String finalPrompt = "ตรวจสอบโค้ด Android: " + fileName + "\n" + rawCode;

        aiAssistant.askAI(finalPrompt, new GeminiAssistant.AICallback() {
            @Override
            public void onSuccess(final String responseText) {
                mainHandler.post(() -> {
                    // พูดออกเสียง
                    if (tts != null) tts.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, null);
                    if (listener != null) listener.onSuccess(responseText);
                });
            }

            @Override
            public void onError(final String errorMessage) {
                mainHandler.post(() -> {
                    if (listener != null) listener.onError(errorMessage);
                });
            }
        });
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
