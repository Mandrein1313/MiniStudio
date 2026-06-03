package com.dev.ministudio;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
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
        void onSuccess(CharSequence formattedResult);
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
                // ปรับความเร็วให้พอดี ไม่เร็วเกินไป
                tts.setSpeechRate(0.85f);
                tts.setPitch(1.0f);
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
        // ทำความสะอาดข้อความสำหรับการพูด และเพิ่มช่องว่างเพื่อให้ TTS เว้นจังหวะ
        String cleanText = responseText.replaceAll("[*#`]", "")
                                       .replaceAll("\\.", " . ")
                                       .replaceAll("!", " ! ");
        
        speakText(cleanText);
        
        // จัดรูปแบบข้อความสำหรับแสดงผล
        CharSequence formatted = formatAiResponse(responseText);
        
        if (listener != null) {
            listener.onSuccess(formatted);
        }
    }

    private void speakText(String text) {
        if (tts != null && ttsInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AI_ANALYSIS");
        }
    }

    private CharSequence formatAiResponse(String text) {
        String processedText = text.replaceAll("\\*\\*", "");
        SpannableStringBuilder ssb = new SpannableStringBuilder(processedText);

        // 1. หัวข้อสำคัญ (สีม่วงสดใส + ตัวใหญ่ขึ้น)
        applyStyle(ssb, "(วิเคราะห์|คำแนะนำ|สรุป|คะแนน|หัวข้อ)", Color.parseColor("#9C27B0"), true, 1.2f, -1);

        // 2. เน้น Error/Warning (สีแดงส้ม + พื้นหลังสีเหลืองอ่อน)
        applyStyle(ssb, "(Error|ข้อผิดพลาด|บัค|Bug|⚠️)", Color.parseColor("#D32F2F"), true, 1.0f, Color.parseColor("#FFF9C4"));

        // 3. เน้นทางออก/วิธีแก้ไข (สีเขียวเข้ม + พื้นหลังสีเขียวอ่อน)
        applyStyle(ssb, "(แก้ไข|ปรับปรุง|ดีกว่า|✅|Solution)", Color.parseColor("#2E7D32"), true, 1.0f, Color.parseColor("#E8F5E9"));

        // 4. เน้นตัวเลขคะแนน (สีน้ำเงินเข้ม + ตัวหนาพิเศษ)
        applyStyle(ssb, "\\d+/10", Color.parseColor("#1565C0"), true, 1.1f, -1);

        return ssb;
    }

    private void applyStyle(SpannableStringBuilder ssb, String regex, int color, boolean bold, float sizeScale, int bgColor) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(ssb.toString());
        while (matcher.find()) {
            ssb.setSpan(new ForegroundColorSpan(color), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (bold) ssb.setSpan(new StyleSpan(Typeface.BOLD), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (sizeScale != 1.0f) ssb.setSpan(new RelativeSizeSpan(sizeScale), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (bgColor != -1) ssb.setSpan(new BackgroundColorSpan(bgColor), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private String buildAnalysisPrompt(String fileName, String rawCode) {
        return "ไฟล์: " + fileName + "\n\nโค้ด:\n" + rawCode + "\n\nช่วยวิเคราะห์ปัญหา, Code Smell, คำแนะนำ และให้คะแนน 1-10 เป็นภาษาไทย พร้อมใส่ Emoji ให้ด้วย (เช่น ⚠️, ✅, 🌟)";
    }

    public void shutdown() {
        if (tts != null) {
            try { tts.stop(); tts.shutdown(); } catch (Exception e) {}
            tts = null;
        }
    }
}
