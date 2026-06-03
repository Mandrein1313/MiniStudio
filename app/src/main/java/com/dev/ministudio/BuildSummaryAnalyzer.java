package com.dev.ministudio;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildSummaryAnalyzer {

    public interface LogOutputListener {
        void onAppendLog(String text, int color);
    }

    // 🌟 Regex ปรับปรุงใหม่
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

    // 🎨 สีสันใหม่ที่สวยและชัดเจน
    private final int COLOR_HEADER = Color.parseColor("#FF5252");     // แดงสด (หัวข้อ)
    private final int COLOR_FILE = Color.parseColor("#FFAB40");       // ส้ม (ไฟล์)
    private final int COLOR_LINE = Color.parseColor("#64B5F6");       // น้ำเงิน (บรรทัด)
    private final int COLOR_TYPE = Color.parseColor("#FFAB40");       // ส้ม (ประเภท)
    private final int COLOR_MESSAGE = Color.parseColor("#FF8A80");    // แดงอ่อน (รายละเอียด)
    private final int COLOR_SUGGEST = Color.parseColor("#81C784");    // เขียว (คำแนะนำ)
    private final int COLOR_SUCCESS = Color.parseColor("#81C784");
    private final int COLOR_SEPARATOR = Color.parseColor("#BDBDBD");

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
        if (line == null) return false;

        // ตรวจสอบ Error ด้วย Regex
        if (checkRegexError(line, JAVAC_ERROR, "JAVA_ERROR") ||
            checkRegexError(line, XML_ERROR, "XML_AAPT2_ERROR") ||
            checkRegexError(line, KOTLIN_ERROR, "KOTLIN_ERROR")) {
            return false;
        }

        // ดักตำแหน่ง ^ (ลูกศรชี้ตำแหน่งผิด)
        if (hasError && lastError != null && line.contains("^")) {
            int colIndex = line.indexOf("^");
            if (colIndex >= 0) {
                lastError.column = colIndex;
            }
        }

        String lowerLine = line.toLowerCase();

        // ตรวจสอบข้อผิดพลาดอื่นๆ
        if (lowerLine.contains("repository not found")) {
            hasError = true;
            errorType = "GIT_URL_MISSING";
            errorDetails = "ไม่พบ GitHub Repository";
            return false;
        }

        if (line.contains("Authentication failed") || line.contains("401 Unauthorized") ||
            line.contains("Bad credentials") || line.contains("403 Forbidden")) {
            hasError = true;
            errorType = "AUTH_ERROR";
            errorDetails = "GitHub Token ไม่ถูกต้องหรือไม่มีสิทธิ์";
            return false;
        }

        if (lowerLine.contains("build failed") || lowerLine.contains("compilejava failed")) {
            hasError = true;
            if (errorType.equals("UNKNOWN")) {
                errorType = "BUILD_COMPILE_FAILED";
                errorDetails = "กระบวนการคอมไพล์ล้มเหลว";
            }
            return true;
        }

        return false;
    }

    private boolean checkRegexError(String line, Pattern pattern, String typeStr) {
        Matcher m = pattern.matcher(line);
        if (!m.find()) return false;

        try {
            String file = m.group(1).trim();
            int lineNumber = Integer.parseInt(m.group(2));
            String message = m.group(3).trim();

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

    // 🎨 ฟังก์ชันแสดงผลสรุปที่สวยงาม
    public void printSummary(LogOutputListener listener) {
        if (!hasError || listener == null) return;

        listener.onAppendLog("\n" + "═".repeat(50) + "\n", COLOR_HEADER);
        listener.onAppendLog("🔍 วิเคราะห์สาเหตุการบิวด์ล้มเหลว\n", COLOR_HEADER);

        if (lastError != null) {
            listener.onAppendLog("📍 ไฟล์: ", COLOR_HEADER);
            listener.onAppendLog(lastError.file + "\n", COLOR_FILE);
            
            listener.onAppendLog("📍 บรรทัดที่: ", COLOR_HEADER);
            listener.onAppendLog(lastError.line + "\n", COLOR_LINE);
        }

        switch (errorType) {
            case "JAVA_ERROR":
                listener.onAppendLog("📌 ประเภท: ", COLOR_HEADER);
                listener.onAppendLog("ข้อผิดพลาด Java (Compile Error)\n", COLOR_TYPE);
                break;
            case "XML_AAPT2_ERROR":
                listener.onAppendLog("📌 ประเภท: ", COLOR_HEADER);
                listener.onAppendLog("ข้อผิดพลาด XML (AAPT2)\n", COLOR_TYPE);
                break;
            case "KOTLIN_ERROR":
                listener.onAppendLog("📌 ประเภท: ", COLOR_HEADER);
                listener.onAppendLog("ข้อผิดพลาด Kotlin\n", COLOR_TYPE);
                break;
            default:
                listener.onAppendLog("📌 ประเภท: ", COLOR_HEADER);
                listener.onAppendLog(errorType + "\n", COLOR_TYPE);
                break;
        }

        listener.onAppendLog("💬 รายละเอียด: ", COLOR_HEADER);
        listener.onAppendLog(errorDetails + "\n", COLOR_MESSAGE);

        listener.onAppendLog("💡 คำแนะนำ: ", COLOR_HEADER);
        listener.onAppendLog(getSuggestion() + "\n", COLOR_SUGGEST);

        listener.onAppendLog("═".repeat(50) + "\n", COLOR_HEADER);
    }

    private String getSuggestion() {
        switch (errorType) {
            case "JAVA_ERROR":
                return "ตรวจสอบไวยากรณ์, เครื่องหมาย {}, (), ; ตรงบรรทัดที่ระบุ";
            case "XML_AAPT2_ERROR":
                return "ตรวจสอบแท็ก XML เปิด-ปิด ไม่ตรงกัน หรือแอตทริบิวต์ผิด";
            case "KOTLIN_ERROR":
                return "ตรวจสอบประเภทตัวแปร, Null Safety, หรือการสืบทอดคลาส";
            default:
                return "ตรวจสอบโค้ดและลอง Build ใหม่ครับ";
        }
    }
}
