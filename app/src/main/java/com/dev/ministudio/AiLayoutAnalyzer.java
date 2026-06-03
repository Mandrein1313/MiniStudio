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
                    // Fallback to English
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

        // Prompt ที่ดีขึ้น
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

    private String buildAnalysisPrompt(String fileName, String rawCode) {
        return """
                คุณคือผู้เชี่ยวชาญด้าน Android Development (Kotlin + Jetpack Compose / XML)
                วิเคราะห์โค้ดไฟล์นี้ให้ละเอียด:
                
                ไฟล์: %s
                
                โค้ด:
                %s
                
                โปรดวิเคราะห์ดังนี้:
                1. ปัญหาที่อาจเกิดขึ้น (Bug, Performance, Security, Memory Leak)
                2. Code Smell / Best Practice ที่ควรปรับปรุง
                3. คำแนะนำการแก้ไข (พร้อมตัวอย่างโค้ดถ้าจำเป็น)
                4. คะแนนคุณภาพโค้ด (เต็ม 10)
                
                ตอบด้วยภาษาไทยที่อ่านง่าย และใช้ **ข้อความสำคัญ** เพื่อให้เด่นชัด
                """.formatted(fileName, rawCode);
    }

    private void processResponse(String responseText, OnAnalysisListener listener) {
        // อ่านเสียง
        speakText(responseText);
        
        // จัดรูปแบบข้อความ
        SpannableString formatted = formatAiResponse(responseText);
        
        if (listener != null) {
            listener.onSuccess(formatted);
        }
    }

    private void speakText(String text) {
        if (tts != null && ttsInitialized) {
            String cleanText = text
                    .replaceAll("\\*\\*", "")
                    .replaceAll("#+", "")
                    .replaceAll("```[\\s\\S]*?```", "");
            
            tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "AI_ANALYSIS");
        }
    }

    private SpannableString formatAiResponse(String text) {
        SpannableString spannable = new SpannableString(text);

        // Highlighting สำหรับ Markdown และคำสำคัญ
        highlightPattern(spannable, "\\*\\*(.+?)\\*\\*", Color.parseColor("#FF9800")); // Bold
        highlightPattern(spannable, "(Error|ข้อผิดพลาด|ปัญหา|Bug)", Color.RED);
        highlightPattern(spannable, "(แนะนำ|ควร|ดีกว่า|ปรับปรุง|แก้ไข)", Color.parseColor("#4CAF50"));
        highlightPattern(spannable, "(คำแนะนำ|สรุป|คะแนน)", Color.parseColor("#2196F3"));

        return spannable;
    }

    private void highlightPattern(SpannableString spannable, String regex, int color) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(spannable.toString());

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            
            // กรณีเป็น **ข้อความ** ให้ตัด ** ออก
            if (regex.contains("\\*\\*")) {
                start += 2;
                end -= 2;
            }

            spannable.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    public void shutdown() {
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception e) {
                // Ignore
            }
            tts = null;
        }
    }
}