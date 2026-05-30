package com.dev.ministudio;

import android.text.TextUtils;
import com.dev.ministudio.TerminalColor;

public class BuildSummaryAnalyzer {

    private String detectedFileName = null;
    private String detectedLineNumber = null;
    private String detectedErrorText = null;
    private boolean isAborted = false;

    public interface LogOutputListener {
        void onAppendLog(String text, int color);
    }

    public boolean analyzeLine(String text, int originalColor, LogOutputListener listener) {
        if (isAborted) return true;

        String lowerText = text.toLowerCase();

        // 🔍 1. ตรวจจับบรรทัดที่มีข้อผิดพลาดของไฟล์ .java
        if (text.contains(".java:") && (lowerText.contains("error:") || lowerText.contains("failed"))) {
            try {
                int javaIndex = text.indexOf(".java");
                int startPos = text.lastIndexOf("/", javaIndex);
                if (startPos == -1) startPos = text.lastIndexOf("\\", javaIndex);
                startPos = (startPos == -1) ? 0 : startPos + 1;
                
                int endPos = text.indexOf(":", javaIndex + 5);
                if (endPos != -1) {
                    String rawFileAndLine = text.substring(startPos, endPos);
                    String[] parts = rawFileAndLine.split(":");
                    if (parts.length >= 2) {
                        detectedFileName = parts[0];
                        detectedLineNumber = parts[1];
                        // เก็บข้อความ Log บรรทัดนั้นไว้ทั้งหมดเพื่อเอาไปวิเคราะห์ละเอียด
                        detectedErrorText = text.trim(); 
                    }
                }
            } catch (Exception e) {
                // ป้องกันแอปแครช
            }
        }

        // 🛑 2. ดักจับบรรทัดสั่งจบกระบวนการเมื่อเกิด Build Failed
        if (lowerText.contains("compiledebugjavawithjavac failed") || 
            lowerText.contains("build failed") || 
            (originalColor == android.graphics.Color.RED && text.contains("Process completed with exit code 1"))) {
            
            isAborted = true; 
            generateFailureSummary(text, listener);
            return true; 
        }

        return false;
    }

    private void generateFailureSummary(String fallbackText, LogOutputListener listener) {
        // ส่วนแสดงผล Log รายละเอียดด้านบนกล่องสรุป
        listener.onAppendLog("\n##[group]❌ รายละเอียดข้อผิดพลาดในการคอมไพล์ซอร์สโค้ด", TerminalColor.ERROR_RED);
        listener.onAppendLog("##[error] STATUS  -> กระบวนการหยุดทำงานด้วย Exit Code 1", TerminalColor.ERROR_TEXT);
        
        if (detectedFileName != null) {
            listener.onAppendLog("##[error] TARGET  -> 📄 ไฟล์: ", TerminalColor.TARGET_ORANGE);
            listener.onAppendLog(detectedFileName, TerminalColor.TARGET_YELLOW);
            listener.onAppendLog("  📍 บรรทัดที่: ", TerminalColor.TARGET_ORANGE);
            listener.onAppendLog(detectedLineNumber + "\n", TerminalColor.TARGET_YELLOW);
            listener.onAppendLog("##[error] DETAIL  -> " + detectedErrorText, TerminalColor.DETAIL_RED);
        } else {
            listener.onAppendLog("##[error] TARGET  -> ไม่สามารถระบุตำแหน่งไฟล์ในระบบคอมไพล์ได้ชัดเจน\n", TerminalColor.TARGET_ORANGE);
            listener.onAppendLog("##[error] DETAIL  -> " + fallbackText.trim(), TerminalColor.DETAIL_RED);
        }
        listener.onAppendLog("##[endgroup]", TerminalColor.ERROR_RED);

        // 📦 กล่องข้อความสรุปสไตล์ ANSI Terminal
        listener.onAppendLog("\n┏━━━━━━━━━━━━━━━━━━━━━ Compilation Failure Summary ━━━━━━━━━━━━━━━━━━━━━┓", TerminalColor.BORDER_BLUE);
        
        listener.onAppendLog("  ❌ ล้มเหลว : ", TerminalColor.TEXT_WHITE);
        listener.onAppendLog("การทำงานผิดพลาด (Process completed with exit code 1)\n", TerminalColor.ERROR_RED);
        
        if (detectedFileName != null && detectedErrorText != null) {
            listener.onAppendLog("  🎯 ชี้เป้า   : ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog("กรุณาแก้ไขโค้ดที่ไฟล์ ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog(detectedFileName, TerminalColor.TARGET_ORANGE);
            listener.onAppendLog(" ตรงบรรทัดที่ ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog(detectedLineNumber + "\n", TerminalColor.TARGET_YELLOW);
            
            // 🧠 ✨ ระบบวิเคราะห์และสลับข้อความแนะนำแบบไดนามิก (เรียงลำดับความสำคัญใหม่)
            String dynamicSuggestion = "โปรดตรวจสอบโครงสร้างโค้ด หรือโครงสร้าง Syntax ในหน้าจอ Editor";
            String cleanError = detectedErrorText.toLowerCase();

            if (cleanError.contains("cannot find symbol") || cleanError.contains("cannot be resolved")) {
                dynamicSuggestion = "หาตัวแปร, ฟังก์ชัน หรือคลาสนี้ไม่เจอ (ลองเช็คตัวสะกด พิมพ์ผิด หรือลืม import)";
            } else if (cleanError.contains("expected") || cleanError.contains(";")) {
                dynamicSuggestion = "ลืมใส่เครื่องหมายเซมิโคลอน (;) หรือลืมปิดวงเล็บ ) / ปีกกา } ในบรรทัดดังกล่าว";
            } else if (cleanError.contains("already defined") || cleanError.contains("duplicate")) {
                dynamicSuggestion = "มีการประกาศชื่อตัวแปร หรือเมทอด (Method) นี้ซ้ำซ้อนกันในสโคปเดียวกัน";
            } else if (cleanError.contains("incompatible types")) {
                dynamicSuggestion = "ประเภทข้อมูลไม่ตรงกัน (Type Mismatch) เช่น เอาข้อมูล String ไปใส่ในตัวแปร int";
            } else if (cleanError.contains("is abstract") || cleanError.contains("does not override")) {
                dynamicSuggestion = "ลืมเขียนโค้ดเพื่อ Override เมทอดที่จำเป็นของ Interface / Abstract คลาส";
            } else if (detectedErrorText.contains("error:")) {
                // เอาเงื่อนไขดักคำว่า error: ทั่วไปมาไว้ล่างสุด เพื่อไม่ให้มันไปแย่งตัดหน้าคำสั่งเฉพาะทางด้านบน
                dynamicSuggestion = "ตรวจพบปัญหา: " + detectedErrorText.substring(detectedErrorText.indexOf("error:") + 6).trim();
            }

            listener.onAppendLog("  💡 แนะนำ   : ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog(dynamicSuggestion + "\n", TerminalColor.SUGGEST_GREEN);
        } else {
            listener.onAppendLog("  💡 แนะนำ   : ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog("ไม่พบตำแหน่งซอร์สโค้ดที่พัง คาดว่าเป็นปัญหาที่ไฟล์ build.gradle หรือโครงสร้างโปรเจกต์\n", TerminalColor.TARGET_ORANGE);
        }
        
        listener.onAppendLog("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛", TerminalColor.BORDER_BLUE);
    }
}
