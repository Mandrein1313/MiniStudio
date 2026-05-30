package com.dev.ministudio;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.dev.ministudio.TerminalColor;

public class BuildSummaryAnalyzer {

    private String detectedFileName = null;
    private String detectedLineNumber = null;
    private String detectedErrorText = null;
    private boolean isAborted = false;

    // Regex สำหรับแกะโครงสร้างข้อความ Error ของ Java Compiler บน Terminal ทุกรูปแบบ
    // กลุ่ม 1: ชื่อไฟล์, กลุ่ม 2: เลขบรรทัด, กลุ่ม 3: เนื้อหา Error ทั้งหมด
    private static final Pattern JAVA_ERROR_PATTERN = Pattern.compile("([^/\\\\]+\\.java):(\\d+):\\s*error:\\s*(.*)", Pattern.CASE_INSENSITIVE);

    public interface LogOutputListener {
        void onAppendLog(String text, int color);
    }

    /**
     * สแกนตรวจสอบบรรทัด Log ทุกบรรทัดแบบเรียลไทม์
     */
    public boolean analyzeLine(String text, int originalColor, LogOutputListener listener) {
        if (isAborted) return true;

        if (text == null) return false;
        String trimmedText = text.trim();
        String lowerText = trimmedText.toLowerCase();

        // 🔍 1. ใช้ Regex สแกนหาพิกัดไฟล์และข้อความ Error (ตัดปัญหาเรื่องเครื่องหมายสแลชและข้อความบัง)
        Matcher matcher = JAVA_ERROR_PATTERN.matcher(trimmedText);
        if (matcher.find()) {
            detectedFileName = matcher.group(1);     // ได้ผลลัพธ์เช่น "MainActivity.java"
            detectedLineNumber = matcher.group(2);   // ได้ผลลัพธ์เช่น "124"
            detectedErrorText = matcher.group(3);     // ได้ผลลัพธ์เช่น "incompatible types: String cannot be converted to int"
        }

        // 🛑 2. ดักจับจุดตัดกระบวนการเมื่อระบบคอมไพล์สั่งหยุดทำงาน (Build Failed / Exit Code 1)
        if (lowerText.contains("compiledebugjavawithjavac failed") || 
            lowerText.contains("build failed") || 
            lowerText.contains("process completed with exit code 1") ||
            originalColor == android.graphics.Color.RED && lowerText.contains("exit code 1")) {
            
            isAborted = true; 
            generateFailureSummary(trimmedText, listener);
            return true; 
        }

        return false;
    }

    /**
     * พ่นกล่องข้อความสรุป ANSI ไฮไลท์สีเฉพาะจุดตัวหนังสือ Error
     */
    private void generateFailureSummary(String fallbackText, LogOutputListener listener) {
        // ส่วนพ่นรายละเอียดข้อมูลดิบด้านบนกล่องสรุป
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

        // 📦 ตีเส้นกล่องข้อความสรุปสไตล์ ANSI Terminal [แสดงสีแดงเฉพาะตัวข้อความพัง]
        listener.onAppendLog("\n┏━━━━━━━━━━━━━━━━━━━━━ Compilation Failure Summary ━━━━━━━━━━━━━━━━━━━━━┓", TerminalColor.BORDER_BLUE);
        
        // ❌ ล้มเหลว : แสดงสีขาวปกติ ตัวข้อความล้มเหลวแสดงสีแดงสะท้อนแสง
        listener.onAppendLog("  ❌ ล้มเหลว : ", TerminalColor.TEXT_WHITE);
        listener.onAppendLog("การทำงานผิดพลาด (Process completed with exit code 1)\n", TerminalColor.ERROR_RED);
        
        if (detectedFileName != null && detectedErrorText != null) {
            // 🎯 ชี้เป้า : ไอคอนสีขาวปกติ พิกัดชื่อไฟล์สีส้ม เลขบรรทัดสีเหลือง
            listener.onAppendLog("  🎯 ชี้เป้า   : ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog("กรุณาแก้ไขโค้ดที่ไฟล์ ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog(detectedFileName, TerminalColor.TARGET_ORANGE);
            listener.onAppendLog(" ตรงบรรทัดที่ ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog(detectedLineNumber + "\n", TerminalColor.TARGET_YELLOW);
            
            // 🧠 🧠 ระบบวิเคราะห์เปลี่ยนคำแนะนำแบบไดนามิกจากค่า Regex ล่าสุด
            String dynamicSuggestion = "โปรดตรวจสอบโครงสร้างโค้ด หรือโครงสร้าง Syntax ในหน้าจอ Editor";
            String cleanError = detectedErrorText.toLowerCase();

            if (cleanError.contains("incompatible types")) {
                dynamicSuggestion = "ประเภทข้อมูลไม่ตรงกัน (Type Mismatch) เช่น เอาข้อมูลข้อความ (String) ไปใส่ในตัวแปรตัวเลข (int)";
            } else if (cleanError.contains("cannot find symbol") || cleanError.contains("cannot be resolved")) {
                dynamicSuggestion = "หาตัวแปร, ฟังก์ชัน หรือคลาสนี้ไม่เจอ (ลองเช็คตัวสะกด พิมพ์ผิด หรือลืม import)";
            } else if (cleanError.contains("expected") || cleanError.contains(";")) {
                dynamicSuggestion = "ลืมใส่เครื่องหมายเซมิโคลอน (;) หรือลืมปิดวงเล็บ ) / ปีกกา } ในบรรทัดดังกล่าว";
            } else if (cleanError.contains("already defined") || cleanError.contains("duplicate")) {
                dynamicSuggestion = "มีการประกาศชื่อตัวแปร หรือเมทอด (Method) นี้ซ้ำซ้อนกันในสโคปเดียวกัน";
            } else if (cleanError.contains("is abstract") || cleanError.contains("does not override")) {
                dynamicSuggestion = "ลืมเขียนโค้ดเพื่อ Override เมทอดที่จำเป็นของ Interface / Abstract คลาส";
            } else {
                // หากไม่ตรงเงื่อนไขเจาะจง ให้ดึงเอาข้อความความพังที่ได้จากกลุ่มเนื้อหามาพ่นแสดงโดยตรง
                dynamicSuggestion = "ตรวจพบปัญหา: " + detectedErrorText;
            }

            // 💡 แนะนำ : ไอคอนสีขาวปกติ ตัวคำแนะนำไดนามิกแสดงเป็นสีเขียวนำทาง
            listener.onAppendLog("  💡 แนะนำ   : ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog(dynamicSuggestion + "\n", TerminalColor.SUGGEST_GREEN);
        } else {
            listener.onAppendLog("  💡 แนะนำ   : ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog("ไม่พบตำแหน่งซอร์สโค้ดที่พัง คาดว่าเป็นปัญหาที่ไฟล์ build.gradle หรือโครงสร้างโปรเจกต์\n", TerminalColor.TARGET_ORANGE);
        }
        
        listener.onAppendLog("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛", TerminalColor.BORDER_BLUE);
    }
}
