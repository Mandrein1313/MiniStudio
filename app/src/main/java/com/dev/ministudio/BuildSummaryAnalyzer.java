package com.dev.ministudio;

import com.dev.ministudio.TerminalColor;

public class BuildSummaryAnalyzer {

    private String detectedFileName = null;
    private String detectedLineNumber = null;
    private String detectedErrorText = null;
    private boolean isAborted = false;
    
    // เปลี่ยนจาก flag ตัวเดียว เป็นการระบุสถานะของการรอข้อความ
    private int errorCaptureState = 0; // 0 = ปกติ, 1 = เจอพิกัดแล้ว รอหาข้อความ error จริง

    public interface LogOutputListener {
        void onAppendLog(String text, int color);
    }

    public boolean analyzeLine(String text, int originalColor, LogOutputListener listener) {
        if (isAborted) return true;
        if (text == null) return false;

        String trimmedText = text.trim();
        String lowerText = trimmedText.toLowerCase();

        // 🔍 บล็อกดักจับข้อความอธิบายรายละเอียด (หลังจากเจอไฟล์พังแล้ว)
        if (errorCaptureState == 1) {
            // ข้ามพวกบรรทัดที่เป็นซอร์สโค้ด หรือลูกศรชี้ตำแหน่ง (เช่น ^, symbol:, location:)
            if (!trimmedText.isEmpty() && !trimmedText.startsWith("^") && !lowerText.contains("symbol:") && !lowerText.contains("location:")) {
                detectedErrorText = trimmedText;
                errorCaptureState = 0; // ได้ข้อความแล้ว ปิดโหมดสแกนหาคำอธิบาย
            }
        }

        // 🔍 ดักจับรูปแบบไฟล์พังมาตรฐานของ Java Compiler: [เส้นทางไฟล์].java:[บรรทัด]: error: [ข้อความ]
        if (lowerText.contains(".java:") && lowerText.contains("error:")) {
            try {
                // ตัวอย่าง Log: /home/runner/work/MiniStudio/app/src/main/java/MainActivity.java:45: error: cannot find symbol
                int javaIdx = lowerText.indexOf(".java:");
                int errorIdx = lowerText.indexOf("error:");

                // แยกชื่อไฟล์ (ดึงเฉพาะชื่อไฟล์ท้ายเส้นทาง)
                String fullFilePath = trimmedText.substring(0, javaIdx + 5);
                if (fullFilePath.contains("/")) {
                    detectedFileName = fullFilePath.substring(fullFilePath.lastIndexOf("/") + 1);
                } else {
                    detectedFileName = fullFilePath;
                }

                // แยกหมายเลขบรรทัด
                String linePart = trimmedText.substring(javaIdx + 6, errorIdx).replace(":", "").trim();
                detectedLineNumber = linePart;

                // แยกรายละเอียด Error (กรณีอยู่ในบรรทัดเดียวกันเลย)
                String descriptionPart = trimmedText.substring(errorIdx + 6).trim();
                if (!descriptionPart.isEmpty()) {
                    detectedErrorText = descriptionPart;
                    errorCaptureState = 0; // เจอจบในบรรทัดเดียว ไม่ต้องรอสแกนบรรทัดถัดไป
                } else {
                    errorCaptureState = 1; // บรรทัดนี้ไม่มีรายละเอียด เปิดโหมดรอสแกนบรรทัดถัดไปแทน
                }
                
            } catch (Exception e) {
                // กันแอปแครชหากเจอนามสกุลซ้อนกัน
                errorCaptureState = 0;
            }
        }

        return false;
    }

    /**
     * เรียกใช้งานเมื่อสิ้นสุดขั้นตอนบิวด์ เพื่อสรุปผลวิเคราะห์ลงใน Terminal
     */
    public void printSummary(LogOutputListener listener) {
        if (detectedFileName != null && detectedLineNumber != null) {
            listener.onAppendLog("\n┏━━━━━━━━━━━━━━━━ คำแนะนำการแก้ไขโค้ด ━━━━━━━━━━━━━━━━┓\n", TerminalColor.TARGET_ORANGE);
            listener.onAppendLog("  📍 ตำแหน่ง  : ไฟล์ " + detectedFileName + " (บรรทัดที่ " + detectedLineNumber + ")\n", TerminalColor.TEXT_WHITE);
            
            String cleanError = detectedErrorText != null ? detectedErrorText.toLowerCase() : "";
            String dynamicSuggestion;

            if (cleanError.contains("cannot find symbol") || cleanError.contains("cannot be resolved")) {
                dynamicSuggestion = "หาตัวแปร, ฟังก์ชัน หรือคลาสนี้ไม่เจอ (ลองเช็คตัวสะกด พิมพ์ผิด หรือลืม import)";
            } else if (cleanError.contains("expected") || cleanError.contains(";")) {
                dynamicSuggestion = "ลืมใส่เครื่องหมายเซมิโคลอน (;) หรือลืมปิดวงเล็บ ) / ปีกกา } ในบรรทัดดังกล่าว";
            } else if (cleanError.contains("already defined") || cleanError.contains("duplicate")) {
                dynamicSuggestion = "มีการประกาศชื่อตัวแปร หรือเมทอด (Method) นี้ซ้ำซ้อนกันในขอบเขตเดียวกัน";
            } else if (cleanError.contains("is abstract") || cleanError.contains("does not override")) {
                dynamicSuggestion = "ลืมเขียนโค้ดเพื่อ Override เมทอดที่จำเป็นตามเงื่อนไขของ Interface / Abstract Class";
            } else if (detectedErrorText != null) {
                dynamicSuggestion = "ตรวจพบปัญหา: " + detectedErrorText;
            } else {
                dynamicSuggestion = "พบข้อผิดพลาดเกี่ยวกับไวยากรณ์ (Syntax Error) ภายในไฟล์นี้";
            }

            listener.onAppendLog("  💡 แนะนำ   : ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog(dynamicSuggestion + "\n", TerminalColor.SUGGEST_GREEN);
            listener.onAppendLog("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛\n", TerminalColor.TARGET_ORANGE);
        } else {
            listener.onAppendLog("\n❌ บิวด์ล้มเหลว: ไม่พบตำแหน่งซอร์สโค้ดที่พัง คาดว่าเป็นปัญหาที่ไฟล์ build.gradle หรือโครงสร้างระบบโปรเจกต์\n", TerminalColor.TARGET_ORANGE);
        }
    }
}
