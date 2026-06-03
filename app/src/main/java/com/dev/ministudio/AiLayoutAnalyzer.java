package com.dev.ministudio;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import com.dev.ministudio.ai.GeminiAssistant;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiLayoutAnalyzer {

    private final GeminiAssistant aiAssistant;
    private final Handler mainHandler;

    private TextToSpeech tts;
    private boolean ttsInitialized = false;

    public interface OnAnalysisListener {
        void onStart();
        void onSuccess(SpannableString formattedResult, String rawResponse);
        void onError(String error);
    }

    public AiLayoutAnalyzer(Context context) {
        this.aiAssistant = new GeminiAssistant();
        this.mainHandler = new Handler(Looper.getMainLooper());
        initTTS(context);
    }

    private void initTTS(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("th", "TH"));
                if (result == TextToSpeech.LANG_MISSING_DATA || 
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.US);
                }

                tts.setSpeechRate(0.95f);
                tts.setPitch(1.05f);
                ttsInitialized = true;
            }
        });
    }

    public void analyzeCode(String fileName, String rawCode, OnAnalysisListener listener) {
        if (listener != null) listener.onStart();

        String prompt = buildAnalysisPrompt(fileName, rawCode);

        aiAssistant.askAI(prompt, new GeminiAssistant.AICallback() {
            @Override
            public void onSuccess(String responseText) {
                mainHandler.post(() -> processResponse(responseText, listener));
            }

            @Override
            public void onError(String errorMessage) {
                mainHandler.post(() -> {
                    if (listener != null) listener.onError(errorMessage);
                });
            }
        });
    }

    public void askAi(String userQuestion, OnAnalysisListener listener) {
        if (listener != null) listener.onStart();

        String prompt = buildGeneralAskPrompt(userQuestion);

        aiAssistant.askAI(prompt, new GeminiAssistant.AICallback() {
            @Override
            public void onSuccess(String responseText) {
                mainHandler.post(() -> processResponse(responseText, listener));
            }

            @Override
            public void onError(String errorMessage) {
                mainHandler.post(() -> {
                    if (listener != null) listener.onError(errorMessage);
                });
            }
        });
    }

    private String buildAnalysisPrompt(String fileName, String rawCode) {
        return """
                คุณคือ Senior Android Engineer ที่มีประสบการณ์กว่า 10 ปี
                
                ไฟล์: %s
                
                โค้ดปัจจุบัน:
                ```java
                %s
                ```
                
                วิเคราะห์ให้ละเอียดแบบ Senior Developer จริงๆ:
                1. สรุปภาพรวม
                2. ปัญหาที่พบ (เรียงตามความรุนแรง)
                3. คำแนะนำ + โค้ดที่ปรับปรุงแล้ว (ถ้ามี)
                
                ส่งโค้ดสมบูรณ์ใน Code Block เท่านั้น
                
                เริ่มเลยครับ
                """.formatted(fileName, rawCode);
    }

    private String buildGeneralAskPrompt(String userQuestion) {
        return """
                คุณคือ Senior Android Developer ชั้นนำ
                
                %s
                
                ถ้ามีการแก้ไขโค้ด ให้ตอบด้วยโค้ดฉบับสมบูรณ์ใน Code Block เท่านั้น
                
                ตอบให้ดีที่สุดเลยครับ
                """.formatted(userQuestion);
    }

    private void processResponse(String responseText, OnAnalysisListener listener) {
        // เก็บ raw response สำหรับคัดลอก
        String rawResponse = responseText;

        // เตรียมข้อความสำหรับอ่านออกเสียง (ไม่รวม code block)
        String cleanTextForSpeak = responseText
                .replaceAll("```[\\s\\S]*?```", "")
                .replaceAll("\\*\\*|__", "")
                .replaceAll("#+", "")
                .replaceAll("`", "")
                .trim();

        speakText(cleanTextForSpeak);

        SpannableString formatted = formatAiResponse(responseText);

        if (listener != null) {
            listener.onSuccess(formatted, rawResponse);
        }
    }

    private void speakText(String text) {
        if (tts != null && ttsInitialized && !text.isBlank()) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AI_ANALYSIS");
        }
    }

    private SpannableString formatAiResponse(String text) {
        SpannableString spannable = new SpannableString(text);

        highlightPattern(spannable, "(Error|ข้อผิดพลาด|Bug|ปัญหา|Critical)", Color.RED);
        highlightPattern(spannable, "(แนะนำ|ควร|ดีกว่า|ปรับปรุง|แก้ไข)", Color.parseColor("#4CAF50"));
        highlightPattern(spannable, "(สรุป|คะแนน|Recommendation)", Color.parseColor("#2196F3"));
        highlightPattern(spannable, "(```java|```xml|```kotlin)", Color.parseColor("#FF9800"));

        return spannable;
    }

    private void highlightPattern(SpannableString spannable, String regex, int color) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(spannable.toString());

        while (matcher.find()) {
            spannable.setSpan(new ForegroundColorSpan(color), 
                    matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), 
                    matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    public void shutdown() {
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {}
            tts = null;
        }
    }
}