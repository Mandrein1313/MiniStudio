package com.dev.ministudio;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildSummaryAnalyzer {

    public interface LogOutputListener {
        void onAppendLog(String text, int color);
    }

    // 🌟 ข้อ 2: เพิ่ม Regex ครอบคลุมทั้ง Java, XML (AAPT2) และ Kotlin
    private static final Pattern JAVAC_ERROR = Pattern.compile("(.*?\\.java):(\\d+):\\s*error:\\s*(.*)");
    private static final Pattern XML_ERROR = Pattern.compile("(.*?\\.xml):(\\d+):\\s*error:\\s*(.*)");
    private static final Pattern KOTLIN_ERROR = Pattern.compile("(.*?\\.kt):(\\d+):\\s*error:\\s*(.*)");

    private boolean hasError = false;
    private String errorType = "UNKNOWN";
    private String errorDetails = "";
    
    private ParsedError lastError;
    // อาเรย์เก็บรายการ Error ทั้งหมดสำหรับส่งให้ RecyclerView Panel
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

    /**
     * เมทอดวิเคราะห์ Log ทีละบรรทัด
     */
    public boolean analyzeLine(String line, int defaultColor, LogOutputListener listener) {
        
        // ตรวจสอบ Regex ทั้ง 3 รูปแบบ
        if (checkRegexError(line, JAVAC_ERROR, "JAVA_ERROR") ||
            checkRegexError(line, XML_ERROR, "XML_AAPT2_ERROR") ||
            checkRegexError(line, KOTLIN_ERROR, "KOTLIN_ERROR")) {
            
            if (listener != null) {
                listener.onAppendLog(line + "\n", defaultColor);
            }
            // 🌟 ข้อ 1: ปรับเปลี่ยนเป็น return true ทันทีเมื่อตรวจจับเจอพิกัดพัง เพื่อหยุด Pipeline (Fast-Fail)
            return true; 
        }

        // ดักจับตำแหน่งคอลัมน์จากสัญลักษณ์ ^ สำหรับ Error ล่าสุดที่พบ
        if (hasError && lastError != null && line.contains("^")) {
            int colIndex = line.indexOf("^");
            if (colIndex >= 0) {
                lastError.column = colIndex;
            }
        }

        // เช็คกรณีข้อผิดพลาดร้ายแรงอื่น ๆ ในระดับโครงสร้างหรือระบบความปลอดภัย
        if (line.contains("ใส่_URL_Git_Repository_ของคุณตรงนี้") || line.contains("not found")) {
            hasError = true;
            errorType = "GIT_URL_MISSING";
            errorDetails = "คุณยังไม่ได้ระบุ URL ของ GitHub Repository ให้ถูกต้อง";
            return true; 
        }

        if (line.contains("Authentication failed") || line.contains("401 Unauthorized") || line.contains("Bad credentials")) {
            hasError = true;
            errorType = "AUTH_ERROR";
            errorDetails = "Token (PAT) ไม่ถูกต้องหรือไม่มีสิทธิ์เข้าถึงพิกัดนี้";
            return true;
        }

        if (line.contains("Build file") || line.contains("build.gradle' not found")) {
            hasError = true;
            errorType = "GRADLE_STRUCTURE_ERROR";
            errorDetails = "ไม่พบไฟล์ควบคุมสคริปต์หลัก (build.gradle)";
            return true;
        }

        if (listener != null) {
            listener.onAppendLog(line + "\n", defaultColor);
        }
        return false;
    }

    /**
     * เมทอดช่วยสกัดกลุ่มคำและตรวจสอบโครงสร้าง Regex 
     */
    private boolean checkRegexError(String line, Pattern pattern, String typeStr) {
        Matcher m = pattern.matcher(line);
        if (m.find()) {
            String file = m.group(1);
            int lineNumber = Integer.parseInt(m.group(2));
            String message = m.group(3);

            lastError = new ParsedError(file, lineNumber, 0, typeStr, message);
            errorList.add(lastError);

            hasError = true;
            errorType = "JAVA_COMPILE_ERROR";
            errorDetails = message;
            return true;
        }
        return false;
    }

    public void printSummary(LogOutputListener listener) {
        if (!hasError || listener == null) return;

        listener.onAppendLog("\n======================================\n", COLOR_ERROR);
        listener.onAppendLog("🔍 [Error Parser] วิเคราะห์พบสาเหตุการบิวด์ล้มเหลว:\n", COLOR_ERROR);
        
        if (lastError != null) {
            listener.onAppendLog("📍 พิกัดโค้ดพัง: " + lastError.file + " (บรรทัดที่ " + lastError.line + ")\n", COLOR_ERROR);
        }

        switch (errorType) {
            case "JAVA_COMPILE_ERROR":
                listener.onAppendLog("📌 สาเหตุ: " + errorDetails + "\n", COLOR_WARNING);
                listener.onAppendLog("💡 วิธีแก้ไข: ตรวจสอบไวยากรณ์โค้ด หรือสัญลักษณ์ที่ระบุไว้ในพิกัดแถบสีแดงด้านบนครับ\n", COLOR_SUCCESS);
                break;
            default:
                listener.onAppendLog("📌 สาเหตุ: " + errorDetails + "\n", COLOR_WARNING);
                break;
        }
        listener.onAppendLog("======================================\n", COLOR_ERROR);
    }
}
