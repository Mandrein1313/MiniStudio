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
        void onSuccess(SpannableString formattedResult);
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
                    if (listener != null) {
                        listener.onError(errorMessage);
                    }
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
                    if (listener != null) {
                        listener.onError(errorMessage);
                    }
                });
            }
        });
    }

    private String buildAnalysisPrompt(String fileName, String rawCode) {
        return """
                คุณคือ Senior Android Engineer ที่มีประสบการณ์กว่า 10 ปี 
                เชี่ยวชาญด้าน Clean Architecture, Performance Optimization และ Modern Android Development
                
                ไฟล์ที่กำลังวิเคราะห์: %s
                
                โค้ดปัจจุบัน:
                ```java
                %s
                ```
                
                กรุณาวิเคราะห์โค้ดนี้อย่างละเอียดในฐานะ Senior Developer:
                
                ### 1. สรุปภาพรวม
                - จุดแข็งของโค้ด
                - ปัญหาหลักที่พบ (เรียงตามความรุนแรง)
                
                ### 2. ปัญหาที่พบ
                สำหรับแต่ละข้อ ระบุ:
                - ปัญหา / Code Smell / Bug / Performance Issue
                - ผลกระทบ
                - คำแนะนำการแก้ไข
                
                ### 3. โค้ดที่ปรับปรุงแล้ว (ถ้าจำเป็น)
                ให้ส่งโค้ดเวอร์ชันสมบูรณ์เท่านั้นใน Code Block
                
                ```java
                // Complete improved code here
                ```
                
                **กฎสำคัญ:**
                - ใช้ Best Practice ปี 2025-2026
                - โค้ดต้อง Compile ได้จริง
                - ใช้ภาษาที่เป็นธรรมชาติเหมือนรีวิวโค้ดให้เพื่อนร่วมทีม
                - ถ้าไม่จำเป็นต้องแก้ทั้งไฟล์ ให้บอกชัดเจน
                
                เริ่มวิเคราะห์ได้เลยครับ
                """.formatted(fileName, rawCode);
    }

    private String buildGeneralAskPrompt(String userQuestion) {
        return """
                คุณคือ Senior Android Developer ที่เชี่ยวชาญสูง
                
                %s
                
                ถ้าต้องแก้ไขหรือเขียนโค้ดใหม่ ให้ส่งโค้ดฉบับสมบูรณ์ภายใน Code Block เท่านั้น
                
                ```java
                // โค้ดสมบูรณ์ที่นี่
                ```
                
                หรือ
                
                ```xml
                <!-- XML Code -->
                ```
                
                ห้ามส่งโค้ดลอยๆ นอก Code Block เพื่อให้ระบบสามารถนำไปใช้งานได้ทันที
                
                ตอบอย่างละเอียดและเป็นมิตรครับ
                """.formatted(userQuestion);
    }

    private void processResponse(String responseText, OnAnalysisListener listener) {
        // อ่านออกเสียงเฉพาะข้อความหลัก (ไม่รวม markdown)
        String cleanTextForSpeak = responseText
                .replaceAll("```[\\s\\S]*?```", "") // ลบ code block
                .replaceAll("\\*\\*|__", "")
                .replaceAll("#+", "")
                .replaceAll("`", "")
                .trim();

        speakText(cleanTextForSpeak);

        SpannableString formatted = formatAiResponse(responseText);

        if (listener != null) {
            listener.onSuccess(formatted);
        }
    }

    private void speakText(String text) {
        if (tts != null && ttsInitialized && !text.isBlank()) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AI_ANALYSIS");
        }
    }

    private SpannableString formatAiResponse(String text) {
        SpannableString spannable = new SpannableString(text);

        // Highlight important keywords
        highlightPattern(spannable, "(Error|ข้อผิดพลาด|Bug|ปัญหา|Critical)", Color.RED);
        highlightPattern(spannable, "(แนะนำ|ควร|ดีกว่า|ปรับปรุง|แก้ไข|แนะนำให้)", Color.parseColor("#4CAF50"));
        highlightPattern(spannable, "(สรุป|คะแนน|Overall|Recommendation)", Color.parseColor("#2196F3"));
        highlightPattern(spannable, "(```java|```xml|```kotlin)", Color.parseColor("#FF9800"));

        return spannable;
    }

    private void highlightPattern(SpannableString spannable, String regex, int color) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(spannable.toString());

        while (matcher.find()) {
            // Color
            spannable.setSpan(
                    new ForegroundColorSpan(color),
                    matcher.start(),
                    matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            // Bold
            spannable.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    matcher.start(),
                    matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    public void shutdown() {
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {
            }
            tts = null;
        }
    }
}