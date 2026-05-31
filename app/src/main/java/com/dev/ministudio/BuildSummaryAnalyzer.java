package com.dev.ministudio;

import com.dev.ministudio.TerminalColor;

public class BuildSummaryAnalyzer {

    private String detectedFileName = null;
    private String detectedLineNumber = null;
    private String detectedErrorText = null;
    private boolean isAborted = false;
    private boolean lookForDescriptionInNextLine = false; // ตัวเปิดโหมดรอเก็บเนื้อหาบรรทัดถัดไป

    public interface LogOutputListener {
        void onAppendLog(String text, int color);
    }

    /**
     * สแกนตรวจสอบบรรทัด Log ทุกบรรทัดแบบเรียลไทม์ (เวอร์ชันทลายบั๊ก Log แยกบรรทัด)
     */
    public boolean analyzeLine(String text, int originalColor, LogOutputListener listener) {
        if (isAborted) return true;
        if (text == null) return false;

        String trimmedText = text.trim();
        String lowerText = trimmedText.toLowerCase();

        // 🧠 ส่วนเสริมพิเศษ: ถ้าบรรทัดก่อนหน้าบอกว่านี่คือพิกัดพัง บรรทัดนี้เก็บเนื้อหา Error ทันที!
        if (lookForDescriptionInNextLine && !trimmedText.isEmpty() && !lowerText.contains("exit code")) {
            detectedErrorText = trimmedText;
            lookForDescriptionInNextLine = false; // เก็บเสร็จแล้วปิดโหมด
        }

        // 🔍 1. ดักจับพิกัดไฟล์ .java (รองรับทั้งแบบมาเดี่ยว ๆ หรือมาพร้อมข้อความ)
        if (lowerText.contains(".java:") && (lowerText.contains("error:") || lowerText.contains("failed"))) {
            try {
                int javaIndex = lowerText.indexOf(".java");
                
                // แกะหาชื่อไฟล์
                int startPos = trimmedText.lastIndexOf("/", javaIndex);
                if (startPos == -1) startPos = trimmedText.lastIndexOf("\\", javaIndex);
                startPos = (startPos == -1) ? 0 : startPos + 1;
                
                // แกะหาเลขบรรทัด
                int endPos = trimmedText.indexOf(":", javaIndex + 5);
                if (endPos != -1) {
                    String rawFileAndLine = trimmedText.substring(startPos, endPos);
                    String[] parts = rawFileAndLine.split(":");
                    if (parts.length >= 2) {
                        detectedFileName = parts[0];   // เช่น "MainActivity.java"
                        detectedLineNumber = parts[1]; // เช่น "124"
                    }
                    
                    // เช็คต่อว่ามีเนื้อหา Error พ่วงท้ายมาในบรรทัดเดียวกันเลยไหม
                    int errorKeywordIndex = lowerText.indexOf("error:", endPos);
                    if (errorKeywordIndex != -1 && errorKeywordIndex + 6 < trimmedText.length()) {
                        detectedErrorText = trimmedText.substring(errorKeywordIndex + 6).trim();
                    } else {
                        // ถ้าไม่มีเนื้อหาต่อท้าย แสดงว่ามันจะขึ้นบรรทัดใหม่แน่ ๆ เปิดโหมดรอจับบรรทัดถัดไป
                        lookForDescriptionInNextLine = true;
                    }
                }
            } catch (Exception e) {
                // เซฟตี้ป้องกันแอปแครช
            }
        }
        
        // 🔍 2. ดักดักจับคำว่า "incompatible types" เผื่อกรณีที่มันลอยมาเดี่ยว ๆ กลางอากาศ
        if (lowerText.contains("incompatible types") || lowerText.contains("cannot find symbol") || lowerText.contains("expected")) {
            detectedErrorText = trimmedText;
        }

        // 🛑 3. ดักจับบรรทัดแจ้งหยุดทำงาน (Build Failed)
        if (lowerText.contains("compiledebugjavawithjavac failed") || 
            lowerText.contains("build failed") || 
            lowerText.contains("process completed with exit code 1") ||
            (originalColor == android.graphics.Color.RED && lowerText.contains("exit code 1"))) {
            
            isAborted = true; 
            generateFailureSummary(trimmedText, listener);
            return true; 
        }

        return false;
    }

    /**
     * พ่นกล่องข้อความสรุป ANSI Terminal
     */
    private void generateFailureSummary(String fallbackText, LogOutputListener listener) {
        // แผงพ่นรายละเอียดสไตล์ GitHub Log ด้านบนกล่องสรุป
        listener.onAppendLog("\n##[group]❌ รายละเอียดข้อผิดพลาดในการคอมไพล์ซอร์สโค้ด", TerminalColor.ERROR_RED);
        listener.onAppendLog("##[error] STATUS  -> กระบวนการหยุดทำงานด้วย Exit Code 1", TerminalColor.ERROR_TEXT);
        
        if (detectedFileName != null) {
            listener.onAppendLog("##[error] TARGET  -> 📄 ไฟล์: ", TerminalColor.TARGET_ORANGE);
            listener.onAppendLog(detectedFileName, TerminalColor.TARGET_YELLOW);
            listener.onAppendLog("  📍 บรรทัดที่: ", TerminalColor.TARGET_ORANGE);
            listener.onAppendLog(detectedLineNumber + "\n", TerminalColor.TARGET_YELLOW);
            listener.onAppendLog("##[error] DETAIL  -> " + (detectedErrorText != null ? detectedErrorText : fallbackText), TerminalColor.DETAIL_RED);
        } else {
            listener.onAppendLog("##[error] TARGET  -> ไม่สามารถระบุตำแหน่งไฟล์ในระบบคอมไพล์ได้ชัดเจน\n", TerminalColor.TARGET_ORANGE);
            listener.onAppendLog("##[error] DETAIL  -> " + fallbackText, TerminalColor.DETAIL_RED);
        }
        listener.onAppendLog("##[endgroup]", TerminalColor.ERROR_RED);

        // 📦 กล่องสรุปผลสไตล์ ANSI Terminal 
        listener.onAppendLog("\n┏━━━━━━━━━━━━━━━━━━━━━ Compilation Failure Summary ━━━━━━━━━━━━━━━━━━━━━┓", TerminalColor.BORDER_BLUE);
        
        listener.onAppendLog("  ❌ ล้มเหลว : ", TerminalColor.TEXT_WHITE);
        listener.onAppendLog("การทำงานผิดพลาด (Process completed with exit code 1)\n", TerminalColor.ERROR_RED);
        
        if (detectedFileName != null && detectedErrorText != null) {
            listener.onAppendLog("  🎯 ชี้เป้า   : ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog("กรุณาแก้ไขโค้ดที่ไฟล์ ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog(detectedFileName, TerminalColor.TARGET_ORANGE);
            listener.onAppendLog(" ตรงบรรทัดที่ ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog(detectedLineNumber + "\n", TerminalColor.TARGET_YELLOW);
            
            // 🧠 วิเคราะห์คำแนะนำจากเนื้อหาสะสม
            String dynamicSuggestion = "โปรดตรวจสอบโครงสร้างโค้ด หรือโครงสร้าง Syntax ในหน้าจอ Editor";
            String cleanError = detectedErrorText.toLowerCase();

            if (cleanError.contains("incompatible types")) {
                dynamicSuggestion = "ประเภทข้อมูลไม่ตรงกัน (Type Mismatch) เช่น เอาข้อมูลข้อความ (String) ไปใส่ในตัวแปรตัวเลข (int)";
            } else if (cleanError.contains("cannot find symbol") || cleanError.contains("cannot be resolved")) {
                dynamicSuggestion = "หาตัวแปร, ฟังก์ชัน หรือคลาสนี้ไม่เจอ (ลองเช็คตัวสะกด พิมพ์ผิด หรือลืม import)";
            } else if (cleanError.contains("expected") || cleanError.contains(";")) {
                dynamicSuggestion = "ลืมใส่เครื่องหมายเซมิโคลอน (;) หรือลืมปิดวงเล็บ ) / ปีกกา } ในบรรทัดดังกล่าว";
            } else if (cleanError.contains("already defined") || cleanError.contains("duplicate")) {
                dynamicSuggestion = "มีการประกาศชื่อตัวแปร หรือเมทอด (Method) นี้ซ้ำซ้อนกันในขอบเขตสโคปเดียวกัน";
            } else if (cleanError.contains("is abstract") || cleanError.contains("does not override")) {
                dynamicSuggestion = "ลืมเขียนโค้ดเพื่อ Override เมทอดที่จำเป็นตามเงื่อนไขของคลาส Interface / Abstract";
            } else {
                dynamicSuggestion = "ตรวจพบปัญหา: " + detectedErrorText;
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
