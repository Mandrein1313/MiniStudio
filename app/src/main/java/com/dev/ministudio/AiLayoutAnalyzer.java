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
import java.util.ArrayList;
import java.util.List;

import com.dev.ministudio.ai.GeminiAssistant;

public class AiLayoutAnalyzer {

    private final GeminiAssistant aiAssistant;
    private final Handler mainHandler;
    private TextToSpeech tts;
    private boolean ttsInitialized = false;

    public interface OnAnalysisListener {
        void onStart();
        void onSuccess(SpannableString formattedResult);
        void onCodeExtracted(List<String> codes); // เพิ่ม Callback สำหรับดึงโค้ด
        void onError(String error);
    }

    public AiLayoutAnalyzer(Context context) {
        this.aiAssistant = new GeminiAssistant();
        this.mainHandler = new Handler(Looper.getMainLooper());
        initTTS(context);
    }

    // ฟังก์ชันสำหรับดึงโค้ดจากข้อความตอบกลับของ AI
    public List<String> extractCodes(String responseText) {
        List<String> codes = new ArrayList<>();
        // Regex ค้นหาข้อความใน ``` ... ```
        Pattern pattern = Pattern.compile("```[\\s\\S]*?
```");
        Matcher matcher = pattern.matcher(responseText);
        while (matcher.find()) {
            // ลบเครื่องหมาย ``` ออก
            String code = matcher.group().replaceAll("
```\\w*", "").replaceAll("```", "").trim();
            codes.add(code);
        }
        return codes;
    }

    private void initTTS(Context context) {
        this.tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("th", "TH"));
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
                mainHandler.post(() -> {
                    processResponse(responseText, listener);
                    // สกัดโค้ดและส่งกลับไปที่ UI ถ้ามีโค้ดในคำตอบ
                    List<String> extracted = extractCodes(responseText);
                    if (!extracted.isEmpty() && listener != null) {
                        listener.onCodeExtracted(extracted);
                    }
                });
            }
            @Override
            public void onError(final String errorMessage) {
                mainHandler.post(() -> { if (listener != null) listener.onError(errorMessage); });
            }
        });
    }

    public void askAi(String userQuestion, final OnAnalysisListener listener) {
        if (listener != null) listener.onStart();
        String prompt = "คุณคือผู้เชี่ยวชาญด้าน Android Development ช่วยตอบคำถามและถ้ามีโค้ดตัวอย่าง ให้ใส่ใน Code Block (```) เสมอ:\n\n" + userQuestion;
        aiAssistant.askAI(prompt, new GeminiAssistant.AICallback() {
            @Override
            public void onSuccess(final String responseText) {
                mainHandler.post(() -> {
                    processResponse(responseText, listener);
                    List<String> extracted = extractCodes(responseText);
                    if (!extracted.isEmpty() && listener != null) {
                        listener.onCodeExtracted(extracted);
                    }
                });
            }
            @Override
            public void onError(final String errorMessage) {
                mainHandler.post(() -> { if (listener != null) listener.onError(errorMessage); });
            }
        });
    }

    private void processResponse(String responseText, OnAnalysisListener listener) {
        String cleanText = responseText.replaceAll("```[\\s\\S]*?```", "").replaceAll("[*#`]", "");

        speakText(cleanText);
        if (listener != null) listener.onSuccess(formatAiResponse(responseText));
    }

    private void speakText(String text) {
        if (tts != null && ttsInitialized) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AI_ANALYSIS");
    }

    private SpannableString formatAiResponse(String text) {
        SpannableString spannable = new SpannableString(text);
        highlightPattern(spannable, "(Error|ข้อผิดพลาด|ปัญหา|Bug)", Color.RED);
        highlightPattern(spannable, "(แนะนำ|ควร|ดีกว่า|ปรับปรุง|แก้ไข)", Color.parseColor("#4CAF50"));
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
        return "ไฟล์: " + fileName + "\n\nโค้ด:\n" + rawCode + "\n\nช่วยวิเคราะห์ปัญหา และให้โค้ดที่แก้ไขแล้วใน Code Block เท่านั้น";
    }

    public void shutdown() {
        if (tts != null) { tts.stop(); tts.shutdown(); tts = null; }
    }
}
