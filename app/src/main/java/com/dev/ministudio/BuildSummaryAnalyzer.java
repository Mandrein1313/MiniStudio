package com.dev.ministudio;

import android.graphics.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildSummaryAnalyzer {

    // Interface ตัวเดิมที่เข้าล็อกกับ MainActivity ของพี่
    public interface LogOutputListener {
        void onAppendLog(String text, int color);
    }

    // 🌟 1. เพิ่ม Regex Pattern สำหรับดักจับพิกัด javac error แบบสากล
    private static final Pattern JAVAC_ERROR = Pattern.compile("(.*?\\.java):(\\d+):\\s*error:\\s*(.*)");

    private boolean hasError = false;
    private String errorType = "UNKNOWN";
    private String errorDetails = "";
    
    // 🌟 2. ตัวแปรเก็บ Error ล่าสุดตามที่พี่ออกแบบ
    private ParsedError lastError;

    private final int COLOR_ERROR = Color.parseColor("#FF8A80");
    private final int COLOR_WARNING = Color.parseColor("#FFB74D");
    private final int COLOR_SUCCESS = Color.parseColor("#81C784");

    /**
     * 🌟 3. เพิ่ม Getter สำหรับให้ MainActivity เรียกดูค่าพิกัดบั๊ก
     */
    public ParsedError getLastError() {
        return lastError;
    }

    /**
     * เมทอดดักอ่าน Log ทีละบรรทัด
     */
    public boolean analyzeLine(String line, int defaultColor, LogOutputListener listener) {
        
        // 🌟 4. ใช้ Regex ดักจับพิกัดไฟล์และบรรทัดที่พังก่อนคำสั่ง contains ทั่วไป
        Matcher m = JAVAC_ERROR.matcher(line);
        if (m.find()) {
            String file = m.group(1);
            int lineNumber = Integer.parseInt(m.group(2));
            String message = m.group(3);

            // บันทึกข้อมูลลงวัตถุประเมินผล
            lastError = new ParsedError(
                file,
                lineNumber,
                0, // ค่าเริ่มต้นคอลัมน์
                "JAVA_ERROR",
                message
            );

            hasError = true;
            errorType = "JAVA_COMPILE_ERROR";
            errorDetails = message;
            
            // พ่น Log ดิบลงหน้าจอตามปกติ แต่ส่งสัญญาณ false เพื่อให้อ่านบรรทัดถัดไป (เผื่อเจอสัญลักษณ์ชี้คอลัมน์ ^)
            if (listener != null) {
                listener.onAppendLog(line + "\n", defaultColor);
            }
            return false;
        }

        // 🌟 5. ตัวเสริมความเทพ: ดักจับเครื่องหมาย ^ เพื่อระบุคอลัมน์ (ถ้ามีบรรทัดชี้เป้าวิ่งตามหลังมา)
        if (hasError && lastError != null && line.contains("^")) {
            int colIndex = line.indexOf("^");
            if (colIndex >= 0) {
                lastError.column = colIndex; // เก็บตำแหน่งคอลัมน์เพื่อความแม่นยำระดับเม็ดทราย
            }
        }

        // ดักจับ Error ภาพรวมระบบอื่นๆ (คงสภาพเดิมไว้ป้องกันระบบเอ๋อ)
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

        // พ่นข้อมูลลง Console ปกติ
        if (listener != null) {
            listener.onAppendLog(line + "\n", defaultColor);
        }
        return false;
    }

    /**
     * เมทอดพิมพ์สรุปแปลไทย
     */
    public void printSummary(LogOutputListener listener) {
        if (!hasError || listener == null) return;

        listener.onAppendLog("\n======================================\n", COLOR_ERROR);
        listener.onAppendLog("🔍 [Error Parser] วิเคราะห์พบสาเหตุการบิวด์ล้มเหลว:\n", COLOR_ERROR);
        
        // ถ้าแกะพิกัดสำเร็จ ให้พ่นที่อยู่ไฟล์ออกมาก่อนเลย
        if (lastError != null) {
            listener.onAppendLog("📍 พิกัดโค้ดพัง: " + lastError.file + " (บรรทัดที่ " + lastError.line + ")\n", COLOR_ERROR);
        }

        switch (errorType) {
            case "JAVA_COMPILE_ERROR":
                listener.onAppendLog("📌 สาเหตุ: " + errorDetails + "\n", COLOR_WARNING);
                listener.onAppendLog("💡 วิธีแก้ไข: กดที่พิกัดบั๊กด้านบนเพื่อกระโดดไปยังจุดแก้ไข จากนั้นตรวจสอบ syntax หรือตัวแปรระบุสัญลักษณ์ครับ\n", COLOR_SUCCESS);
                break;
            case "GIT_URL_MISSING":
                listener.onAppendLog("📌 สาเหตุ: " + errorDetails + "\n", COLOR_WARNING);
                listener.onAppendLog("💡 วิธีแก้ไข: ไปที่หน้าแรก -> แก้ไข URL ของที่เก็บโค้ดให้ถูกต้อง\n", COLOR_SUCCESS);
                break;
            default:
                listener.onAppendLog("📌 สาเหตุ: " + errorDetails + "\n", COLOR_WARNING);
                listener.onAppendLog("💡 วิธีแก้ไข: ตรวจเช็คจุดผิดพลาดตามรายงานข้อความด้านบนครับ\n", COLOR_SUCCESS);
                break;
        }
        listener.onAppendLog("======================================\n", COLOR_ERROR);
    }
}
