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

        if (rawCode == null) {
            if (listener != null) listener.onError("โค้ดว่างเปล่า ไม่สามารถตรวจสอบได้");
            return;
        }

        // 🛠️ ตรวจสอบและกรองข้อมูลก่อนส่งหา AI (ป้องกันปัญหา Code 400 จาก Resource หลุด)
        String processedCode = rawCode;
        if (fileName != null && fileName.toLowerCase().endsWith(".xml")) {
            processedCode = cleanXmlResourceIdentifiers(rawCode);
        }

        // 🛠️ ป้องกัน JSON พัง: ทำการ Escape อักขระพิเศษที่มักจะทำให้เกิด Code 400
        processedCode = escapeForJson(processedCode);
        String safeFileName = escapeForJson(fileName != null ? fileName : "unknown_file");

        // จัดเตรียมชุดคำสั่ง Prompt ให้ AI ทำงานได้อย่างแม่นยำ ในรูปแบบข้อความที่ปลอดภัย
        String finalPrompt = "คุณคือผู้เชี่ยวชาญด้านการพัฒนา Android Application\\n"
                + "กรุณาตรวจสอบซอร์สโค้ดของไฟล์ชื่อ: " + safeFileName + "\\n"
                + "จงระบุจุดที่อาจจะทำให้คอมไพล์ไม่ผ่าน (Compile Errors) จุดบกพร่อง และแนะนำวิธีแก้ไขอย่างละเอียดเป็นภาษาไทย:\\n\\n"
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
        // เคลียร์ @id/ และ @+id/ เพิ่มเติมเพื่อลดความซับซ้อนของทรัพยากรที่ระบบคลาวด์ไม่รู้จัก
        cleaned = cleaned.replaceAll("@\\+?id/[a-zA-Z0-9_]+", "dummy_id");
        return cleaned;
    }

    /**
     * ฟังก์ชันสำหรับแปลงอักขระพิเศษให้ปลอดภัยต่อการจัดรูปแบบ JSON Payload
     */
    private String escapeForJson(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    // ควบคุมอักขระที่พิมพ์ไม่ได้ (Control Characters)
                    if (ch <= '\u001F' || (ch >= '\u007F' && ch <= '\u009F')) {
                        String ss = Integer.toHexString(ch);
                        sb.append("\\u");
                        for (int k = 0; k < 4 - ss.length(); k++) {
                            sb.append('0');
                        }
                        sb.append(ss.toUpperCase());
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }
}
