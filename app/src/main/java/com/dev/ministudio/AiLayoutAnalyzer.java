package com.dev.ministudio;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.dev.ministudio.ai.GeminiAssistant;

public class AiLayoutAnalyzer {

    private final GeminiAssistant aiAssistant;
    private final Handler mainHandler;

    // Interface สำหรับส่งผลลัพธ์กลับไปยัง UI (Callback)
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

        // 🛠️ ตรวจสอบและกรองข้อมูลก่อนส่งหา AI (ป้องกันปัญหา Code 400 จาก Resource หลุด)
        String processedCode = rawCode;
        if (fileName != null && fileName.toLowerCase().endsWith(".xml")) {
            processedCode = cleanXmlResourceIdentifiers(rawCode);
        }

        // จัดเตรียมชุดคำสั่ง Prompt ให้ AI ทำงานได้อย่างแม่นยำ
        String finalPrompt = "คุณคือผู้เชี่ยวชาญด้านการพัฒนา Android Application\n"
                + "กรุณาตรวจสอบซอร์สโค้ดของไฟล์ชื่อ: " + fileName + "\n"
                + "จงระบุจุดที่อาจจะทำให้คอมไพล์ไม่ผ่าน (Compile Errors) จุดบกพร่อง และแนะนำวิธีแก้ไขอย่างละเอียดเป็นภาษาไทย:\n\n"
                + processedCode;

        // เรียกใช้งาน Gemini API
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
     * สคริปต์กรองตัวแปร XML เพื่อหลีกเลี่ยง Error 400 บนระบบคลาวด์ API
     */
    private String cleanXmlResourceIdentifiers(String xml) {
        if (xml == null) return "";
        // เปลี่ยนพวก @string/name ให้กลายเป็นข้อความ Plain text ดัมมี่
        String cleaned = xml.replaceAll("@string/[a-zA-Z0-9_]+", "Preview Text");
        // เปลี่ยนพวก @drawable/name ให้กลายเป็นทรัพยากรพื้นฐานระบบ
        cleaned = cleaned.replaceAll("@drawable/[a-zA-Z0-9_]+", "ic_launcher_foreground");
        return cleaned;
    }
}
