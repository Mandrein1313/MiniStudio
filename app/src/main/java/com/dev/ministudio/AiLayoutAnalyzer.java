package com.dev.ministudio;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Color;
import android.graphics.Typeface;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dev.ministudio.ai.GeminiAssistant;

public class AiLayoutAnalyzer {

    private final GeminiAssistant aiAssistant;
    private final Handler mainHandler;
    private TextToSpeech tts;
    private boolean ttsInitialized = false;

    public interface OnAnalysisListener {
        void onStart();
        void onSuccess(SpannableString formattedResult);
        void onError(String error);
    }

    public AiLayoutAnalyzer(Context context) {
        this.aiAssistant = new GeminiAssistant();
        this.mainHandler = new Handler(Looper.getMainLooper());
        initTTS(context);
    }

    private void initTTS(Context context) {
        this.tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("th", "TH"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.US);
                }
                tts.setSpeechRate(0.95f);
                tts.setPitch(1.05f);
                ttsInitialized = true;
            }
        });
    }

    public void analyzeCode(String fileName, String rawCode, final OnAnalysisListener listener) {
        if (listener != null) listener.onStart();
        String prompt = buildAnalysisPrompt(fileName, rawCode);
        aiAssistant.askAI(prompt, new GeminiAssistant.AICallback() {
            @Override
            public void onSuccess(final String responseText) {
                mainHandler.post(() -> processResponse(responseText, listener));
            }
            @Override
            public void onError(final String errorMessage) {
                mainHandler.post(() -> {
                    if (listener != null) listener.onError(errorMessage);
                });
            }
        });
    }

    public void askAi(String userQuestion, final OnAnalysisListener listener) {
        if (listener != null) listener.onStart();
        String prompt = "คุณคือผู้เชี่ยวชาญด้าน Android Development ช่วยตอบคำถามหรือให้คำแนะนำเกี่ยวกับเรื่องนี้ให้หน่อยครับ: \n\n" + userQuestion;
        aiAssistant.askAI(prompt, new GeminiAssistant.AICallback() {
            @Override
            public void onSuccess(final String responseText) {
                mainHandler.post(() -> processResponse(responseText, listener));
            }
            @Override
            public void onError(final String errorMessage) {
                mainHandler.post(() -> {
                    if (listener != null) listener.onError(errorMessage);
                });
            }
        });
    }

    private void processResponse(String responseText, OnAnalysisListener listener) {
        // ทำความสะอาดข้อความสำหรับการพูด
        String cleanText = responseText
                .replaceAll("\\*\\*", "")
                .replaceAll("\\*", "")
                .replaceAll("#+", "")
                .replaceAll("`", "")
                .replaceAll("/", " ")
                .replaceAll("\\\\", " ")
                .replaceAll("-", " ");
        
        speakText(cleanText);
        
        // จัดรูปแบบข้อความสำหรับแสดงผล
        SpannableString formatted = formatAiResponse(responseText);
        
        if (listener != null) {
            listener.onSuccess(formatted);
        }
    }

    private void speakText(String text) {
        if (tts != null && ttsInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AI_ANALYSIS");
        }
    }

    private SpannableString formatAiResponse(String text) {
        // ลบเครื่องหมาย ** ออกจากข้อความที่จะแสดงผลเพื่อให้หน้าจอสะอาดตา
        String processedText = text.replaceAll("\\*\\*", "");
        SpannableString spannable = new SpannableString(processedText);

        highlightPattern(spannable, "(Error|ข้อผิดพลาด|ปัญหา|Bug)", Color.RED);
        highlightPattern(spannable, "(แนะนำ|ควร|ดีกว่า|ปรับปรุง|แก้ไข)", Color.parseColor("#4CAF50"));
        highlightPattern(spannable, "(คำแนะนำ|สรุป|คะแนน)", Color.parseColor("#2196F3"));

        return spannable;
    }

    private void highlightPattern(SpannableString spannable, String regex, int color) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(spannable.toString());
        while (matcher.find()) {
            spannable.setSpan(new ForegroundColorSpan(color), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private String buildAnalysisPrompt(String fileName, String rawCode) {
        return "ไฟล์: " + fileName + "\n\nโค้ด:\n" + rawCode + "\n\nช่วยวิเคราะห์ปัญหา, Code Smell, คำแนะนำ และให้คะแนน 1-10 เป็นภาษาไทย";
    }

    public void shutdown() {
        if (tts != null) {
            try { tts.stop(); tts.shutdown(); } catch (Exception e) {}
            tts = null;
        }
    }
}