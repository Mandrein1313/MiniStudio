package com.dev.ministudio;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildSummaryAnalyzer {

    public interface LogOutputListener {
        void onAppendLog(String text, int color);
    }

    // 🌟 อัปเกรด Regex ใหม่: เพิ่ม (?:.*\\s)? ด้านหน้า เพื่อรองรับข้อความที่มี Prefix เวลา (Timestamp) แทรกเข้ามาแบบในรูปภาพล่าสุด
    private static final Pattern JAVAC_ERROR =
            Pattern.compile("(?:.*\\s)?(.*?\\.java):(\\d+):\\s*error:\\s*(.*)",
                    Pattern.CASE_INSENSITIVE);
                    
    private static final Pattern XML_ERROR =
            Pattern.compile("(?:.*\\s)?(.*?\\.xml):(\\d+):.*?error:\\s*(.*)",
                    Pattern.CASE_INSENSITIVE);
                    
    private static final Pattern KOTLIN_ERROR =
            Pattern.compile("(?:.*\\s)?(.*?\\.kt):(\\d+):\\s*error:\\s*(.*)",
                    Pattern.CASE_INSENSITIVE);

    private boolean hasError = false;
    private String errorType = "UNKNOWN";
    private String errorDetails = "";
    private ParsedError lastError;
    private final ArrayList<ParsedError> errorList = new ArrayList<>();

    private final int COLOR_ERROR = Color.parseColor("#FF8A80");
    private final int COLOR_WARNING = Color.parseColor("#FFB74D");
    private final int COLOR_SUCCESS = Color.parseColor("#81C784");

    public void clearErrors() {
        errorList.clear();
        lastError = null;
        hasError = false;
        errorType = "UNKNOWN";
        errorDetails = "";
    }

    public ParsedError getLastError() {
        return lastError;
    }

    public ArrayList<ParsedError> getErrorList() {
        return errorList;
    }

    public boolean analyzeLine(String line, int defaultColor, LogOutputListener listener) {
        if (line == null) {
            return false;
        }

        // ===================================
        // 🌟 1. ตรวจสอบกลุ่ม Java / XML / Kotlin ข้อผิดพลาดโค้ดพัง
        // ===================================
        if (checkRegexError(line, JAVAC_ERROR, "JAVA_ERROR")
                || checkRegexError(line, XML_ERROR, "XML_AAPT2_ERROR")
                || checkRegexError(line, KOTLIN_ERROR, "KOTLIN_ERROR")) {
            
            // หมายเหตุ: ปล่อยให้แสดงสถานะล็อกปกติไปก่อน ไม่เพิ่งด่วนตัดจบบัดเดี๋ยวนั้น เพื่อป้องกัน Log พ่นขาดตอน
            return false; 
        }

        // ===================================
        // 🌟 2. ดักพิกัดคอลัมน์จากสัญลักษณ์ลูกศรชี้ (Column Marker ^)
        // ===================================
        if (hasError && lastError != null && line.contains("^")) {
            int colIndex = line.indexOf("^");
            if (colIndex >= 0) {
                // บล็อกคำนวณตำแหน่งคอลัมน์: นำค่าความยาวนำหน้ามาลบออก (ถ้าเป็นไปได้) เพื่อให้เส้นใต้ชี้ตรงจุดคำผิดพอดี
                lastError.column = colIndex; 
            }
        }

        // แปลงเป็นพิมพ์เล็กเพื่อดักคำสั่งทั่วไป
        String lowerLine = line.toLowerCase();

        // ===================================
        // 🌟 3. ตรวจสอบหมวดหมู่ปัญหา Git / Token ขาดหาย
        // ===================================
        if (lowerLine.contains("repository not found")) {
            hasError = true;
            errorType = "GIT_URL_MISSING";
            errorDetails = "ไม่พบ GitHub Repository";
            return false;
        }

        if (line.contains("Authentication failed")
                || line.contains("401 Unauthorized")
                || line.contains("Bad credentials")
                || line.contains("403 Forbidden")) {
            hasError = true;
            errorType = "AUTH_ERROR";
            errorDetails = "GitHub Token ไม่ถูกต้องหรือไม่มีสิทธิ์เข้าถึง";
            return false;
        }

        // ===================================
        // 🌟 4. ตรวจสอบโครงสร้างไฟล์ Gradle หาย
        // ===================================
        if (line.contains("build.gradle' not found")
                || line.contains("Build file")
                || line.contains("settings.gradle")
                || line.contains("settings.gradle.kts")) {
            hasError = true;
            errorType = "GRADLE_STRUCTURE_ERROR";
            errorDetails = "ไม่พบไฟล์ Gradle ที่จำเป็น";
            return false;
        }

        // ===================================
        // 🌟 5. ตัวจับปัญหาระบบจัดหาทรัพยากรหน้าจอ (AAPT2)
        // ===================================
        if (lowerLine.contains("aapt") && lowerLine.contains("error")) {
            hasError = true;
            errorType = "AAPT2_ERROR";
            errorDetails = line;
            return false;
        }

        // ===================================
        // 🌟 6. ตัวแจ้งเตือนดักจบขั้นสุดท้ายเมื่อระบบหยุดรันลง (Exit Code)
        // ===================================
        if (lowerLine.contains("build failed") || lowerLine.contains("compilejava failed")) {
            hasError = true;
            if (errorType.equals("UNKNOWN")) {
                errorType = "BUILD_COMPILE_FAILED";
                errorDetails = "กระบวนการคอมไพล์ซอร์สโค้ดล้มเหลว";
            }
            return true; // 💥 ส่งค่า True เพื่อสั่งเบรกสเตตัสใน MainActivity ตอนจบกระบวนการจริง ๆ
        }

        return false;
    }

    private boolean checkRegexError(String line, Pattern pattern, String typeStr) {
        Matcher m = pattern.matcher(line);
        if (!m.find()) {
            return false;
        }
        try {
            String file = m.group(1).trim(); // 🌟 จุดเพิ่มเช็ค: ใช้ .trim() ป้องกันเศษเว้นวรรค
            int lineNumber = Integer.parseInt(m.group(2));
            String message = m.group(3).trim();

            // เคลียร์เศษพาร์ทระบบคลาวด์ส่วนเกินออก เพื่อให้ได้พาธไฟล์สัมพัทธ์ในโปรเจกต์เครื่องท่าน
            if (file.contains("app/src/")) {
                file = file.substring(file.indexOf("app/src/"));
            }

            lastError = new ParsedError(file, lineNumber, 0, typeStr, message);
            errorList.add(lastError);

            hasError = true;
            errorType = typeStr;
            errorDetails = message;

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void printSummary(LogOutputListener listener) {
        if (!hasError || listener == null) {
            return;
        }
        listener.onAppendLog("\n======================================\n", COLOR_ERROR);
        listener.onAppendLog("🔍 วิเคราะห์สาเหตุการบิวด์ล้มเหลว\n", COLOR_ERROR);

        if (lastError != null) {
            listener.onAppendLog("📍 ไฟล์: " + lastError.file + "\n", COLOR_ERROR);
            listener.onAppendLog("📍 บรรทัด: " + lastError.line + "\n", COLOR_ERROR);
        }

        // 🌟 จุดเพิ่มเช็คสวิตช์เคส: ปรับเงื่อนไขให้รองรับชื่อประเภทใหม่ตรงล็อก เพื่อแชร์สเตตัสให้หน้าจอหลักทำงานได้ถูกต้อง
        switch (errorType) {
            case "JAVA_ERROR":
                listener.onAppendLog("📌 ประเภท: ข้อผิดพลาดไฟล์ภาษา Java (Compile Error)\n", COLOR_WARNING);
                listener.onAppendLog("💬 รายละเอียด: " + errorDetails + "\n", COLOR_WARNING);
                listener.onAppendLog("💡 แนะนำ: ตรวจสอบไวยากรณ์โค้ด เครื่องหมายปีกกา คลาส หรือเซมิโคลอนในพิกัดบรรทัดดังกล่าวครับ\n", COLOR_SUCCESS);
                break;
            case "XML_AAPT2_ERROR":
                listener.onAppendLog("📌 ประเภท: ข้อผิดพลาดไฟล์เลย์เอาต์ XML (AAPT2)\n", COLOR_WARNING);
                listener.onAppendLog("💬 รายละเอียด: " + errorDetails + "\n", COLOR_WARNING);
                listener.onAppendLog("💡 แนะนำ: ตรวจสอบแท็กเปิด-ปิด หรือแอตทริบิวต์หน้าจอที่พิมพ์ผิดในไฟล์ XML ครับ\n", COLOR_SUCCESS);
                break;
            case "KOTLIN_ERROR":
                listener.onAppendLog("📌 ประเภท: ข้อผิดพลาดไฟล์ภาษา Kotlin (Kotlin Compiler)\n", COLOR_WARNING);
                listener.onAppendLog("💬 รายละเอียด: " + errorDetails + "\n", COLOR_WARNING);
                listener.onAppendLog("💡 แนะนำ: เช็คประเภทตัวแปร การสืบทอดคลาส หรือ Null Safety ตรงบรรทัดที่พังครับ\n", COLOR_SUCCESS);
                break;
            default:
                listener.onAppendLog("📌 ประเภท: " + errorType + "\n", COLOR_WARNING);
                listener.onAppendLog("💬 รายละเอียด: " + errorDetails + "\n", COLOR_WARNING);
                break;
        }
        listener.onAppendLog("======================================\n", COLOR_ERROR);
    }
}
