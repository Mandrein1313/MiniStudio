package com.dev.ministudio;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.dev.ministudio.TerminalColor;

public class BuildSummaryAnalyzer {

    private String detectedFileName = null;
    private String detectedLineNumber = null;
    private String detectedErrorText = null;
    private boolean isAborted = false;

    // Regex ระดับสากล แม่นยำที่สุดในการแกะโครงสร้าง Java Compiler Log
    private static final Pattern JAVA_ERROR_PATTERN = Pattern.compile("([^/\\\\]+\\.java):(\\d+):\\s*error:\\s*(.*)", Pattern.CASE_INSENSITIVE);

    public interface LogOutputListener {
        void onAppendLog(String text, int color);
    }

    public boolean analyzeLine(String text, int originalColor, LogOutputListener listener) {
        if (isAborted) return true;
        if (text == null) return false;

        String trimmedText = text.trim();
        String lowerText = trimmedText.toLowerCase();

        // 🔍 1. สแกนแกะข้อมูลไฟล์และเนื้อหา Error จริงด้วย Regex
        Matcher matcher = JAVA_ERROR_PATTERN.matcher(trimmedText);
        if (matcher.find()) {
            detectedFileName = matcher.group(1);     // เช่น "MainActivity.java"
            detectedLineNumber = matcher.group(2);   // เช่น "124"
            detectedErrorText = matcher.group(3);     // เช่น "incompatible types: String cannot be converted to int"
        }

        // 🛑 2. ดักจับบรรทัดสั่งหยุดระบบเมื่อเกิดกระบวนการล้มเหลว (Build Failed)
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

    private void generateFailureSummary(String fallbackText, LogOutputListener listener) {
        // แผงพ่นรายละเอียดสไตล์ GitHub Log ด้านบนกล่องสรุป
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
            listener.onAppendLog("##[error] DETAIL  -> " + fallbackText, TerminalColor.DETAIL_RED);
        }
        listener.onAppendLog("##[endgroup]", TerminalColor.ERROR_RED);

        // 📦 กล่องสรุปผลสไตล์ ANSI Terminal (แยกพ่นสีข้อความตามสั่งเด็ดขาด)
        listener.onAppendLog("\n┏━━━━━━━━━━━━━━━━━━━━━ Compilation Failure Summary ━━━━━━━━━━━━━━━━━━━━━┓", TerminalColor.BORDER_BLUE);
        
        // 1. บรรทัดล้มเหลว: หัวข้อขาวปกติ -> เนื้อหา Error สีแดงล้วน
        listener.onAppendLog("  ❌ ล้มเหลว : ", TerminalColor.TEXT_WHITE);
        listener.onAppendLog("การทำงานผิดพลาด (Process completed with exit code 1)\n", TerminalColor.ERROR_RED);
        
        if (detectedFileName != null && detectedErrorText != null) {
            // 2. บรรทัดชี้เป้า: หัวข้อขาวปกติ -> ชื่อไฟล์สีส้ม -> บรรทัดสีเหลือง
            listener.onAppendLog("  🎯 ชี้เป้า   : ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog("กรุณาแก้ไขโค้ดที่ไฟล์ ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog(detectedFileName, TerminalColor.TARGET_ORANGE);
            listener.onAppendLog(" ตรงบรรทัดที่ ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog(detectedLineNumber + "\n", TerminalColor.TARGET_YELLOW);
            
            // 🧠 ✨ ระบบวิเคราะห์คำแนะนำภาษาไทยแบบไดนามิก (เช็คจากเนื้อหา Regex โดยตรง)
            String dynamicSuggestion = "";
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
                // ตัวเลือกสุดท้ายถ้าไม่ตรงกับข้อไหนเลย
                dynamicSuggestion = "ตรวจพบปัญหา: " + detectedErrorText;
            }

            // 3. บรรทัดแนะนำ: หัวข้อขาวปกติ -> คำแนะนำภาษาไทยสีเขียวนำทางสว่าง
            listener.onAppendLog("  💡 แนะนำ   : ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog(dynamicSuggestion + "\n", TerminalColor.SUGGEST_GREEN);
        } else {
            listener.onAppendLog("  💡 แนะนำ   : ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog("ไม่พบตำแหน่งซอร์สโค้ดที่พัง คาดว่าเป็นปัญหาที่ไฟล์ build.gradle หรือโครงสร้างโปรเจกต์\n", TerminalColor.TARGET_ORANGE);
        }
        
        listener.onAppendLog("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛", TerminalColor.BORDER_BLUE);
    }
}
