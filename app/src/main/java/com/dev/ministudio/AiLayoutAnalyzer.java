package com.dev.ministudio;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.dev.ministudio.ai.GeminiAssistant;

public class AiLayoutAnalyzer {

    private final GeminiAssistant aiAssistant;
    private final Handler mainHandler;

    // Interface สำหรับส่งผลลัพธ์กลับไปยัง UI
    public interface OnAnalysisListener {
        void onStart();
        void onSuccess(String result);
        void onError(String error);
    }

    public AiLayoutAnalyzer() {
        this.aiAssistant = new GeminiAssistant();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * ฟังก์ชันหลักในการวิเคราะห์ซอร์สโค้ด
     */
    public void analyzeCode(String fileName, String rawCode, final OnAnalysisListener listener) {
        if (listener != null) listener.onStart();

        if (rawCode == null || rawCode.trim().isEmpty()) {
            if (listener != null) listener.onError("โค้ดว่างเปล่า ไม่สามารถตรวจสอบได้");
            return;
        }

        // ทำความสะอาดโค้ดก่อนส่ง (เฉพาะไฟล์ XML)
        String processedCode = rawCode;
        if (fileName != null && fileName.toLowerCase().endsWith(".xml")) {
            processedCode = cleanXmlResourceIdentifiers(rawCode);
        }

        // สร้าง Prompt
        String finalPrompt = "คุณคือผู้เชี่ยวชาญด้านการพัฒนา Android Application\n" +
                "กรุณาตรวจสอบซอร์สโค้ดของไฟล์ชื่อ: " + (fileName != null ? fileName : "unknown_file") + "\n\n" +
                "จงระบุจุดที่อาจจะทำให้คอมไพล์ไม่ผ่าน (Compile Errors), จุดบกพร่อง, และแนะนำวิธีแก้ไขอย่างละเอียดเป็นภาษาไทย:\n\n" +
                processedCode;

        // เรียกใช้งาน Gemini
        aiAssistant.askAI(finalPrompt, new GeminiAssistant.AICallback() {
            @Override
            public void onSuccess(final String responseText) {
                mainHandler.post(() -> {
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

    /**
     * ทำความสะอาด Resource Identifiers ในไฟล์ XML
     */
    private String cleanXmlResourceIdentifiers(String xml) {
        if (xml == null) return "";

        String cleaned = xml
                .replaceAll("@string/[a-zA-Z0-9_]+", "Preview Text")
                .replaceAll("@drawable/[a-zA-Z0-9_]+", "ic_launcher_foreground")
                .replaceAll("@\\+?id/[a-zA-Z0-9_]+", "dummy_id")
                .replaceAll("@color/[a-zA-Z0-9_]+", "#FF0000")
                .replaceAll("@dimen/[a-zA-Z0-9_]+", "16dp");

        return cleaned;
    }
}

