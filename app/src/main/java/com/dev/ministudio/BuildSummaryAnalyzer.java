package com.dev.ministudio;

import android.graphics.Color;

public class BuildSummaryAnalyzer {

    // 🌟 แก้ทางแก้เผ็ด Java: เปลี่ยนจาก interface เป็นคลาสธรรมดาเพื่อปลดล็อกกฎการบังคับ @Override ทุกกรณี
    public static class LogOutputListener {
        public void onLogAppend(String text, int color) {}
        public void onAppend(String text, int color) {}
    }

    private boolean hasError = false;
    private String errorType = "UNKNOWN";
    private String errorDetails = "";
    
    private final int COLOR_ERROR = Color.parseColor("#FF8A80");
    private final int COLOR_WARNING = Color.parseColor("#FFB74D");
    private final int COLOR_SUCCESS = Color.parseColor("#81C784");

    /**
     * เมทอดดักอ่าน Log ทีละบรรทัด
     */
    public boolean analyzeLine(String line, int defaultColor, LogOutputListener listener) {
        // 1. ตรวจสอบ: ลืมระบุลิงก์ Git Repository
        if (line.contains("ใส่_URL_Git_Repository_ของคุณตรงนี้") || line.contains("not found")) {
            hasError = true;
            errorType = "GIT_URL_MISSING";
            errorDetails = "คุณยังไม่ได้ระบุ URL ของ GitHub Repository ให้ถูกต้องในหน้าตั้งค่า";
            return true; 
        }

        // 2. ตรวจสอบ: ซอร์สโค้ด Java พังคอมไพล์ไม่ผ่าน
        if (line.contains("error: class, interface, enum, or record expected") || 
            line.contains("Compilation failed") || 
            line.contains("ข้อผิดพลาด:") || 
            line.contains("error:")) {
            
            hasError = true;
            errorType = "JAVA_COMPILE_ERROR";
            
            if (line.contains(".java:")) {
                errorDetails = "พบข้อผิดพลาดทางไวยากรณ์ (Syntax Error) ในไฟล์โค้ดของคุณ: \n" + line.trim();
            } else if (errorDetails.isEmpty()) {
                errorDetails = "โค้ด Java บางจุดไม่สามารถคอมไพล์ได้ กรุณาตรวจสอบคำสะกด วงเล็บปีกกา {} หรือเครื่องหมายเซมิโคลอน (;)";
            }
            return false; 
        }

        // 3. ตรวจสอบ: สิทธิ์เข้าถึงล้มเหลว (Token ผิดพลาด)
        if (line.contains("Authentication failed") || line.contains("401 Unauthorized") || line.contains("Bad credentials")) {
            hasError = true;
            errorType = "AUTH_ERROR";
            errorDetails = "Personal Access Token (PAT) ไม่ถูกต้อง หรือไม่มีสิทธิ์เข้าถึงพิกัดโปรเจกต์นี้";
            return true;
        }

        // 4. ตรวจสอบ: โครงสร้างโปรเจกต์พังหาไฟล์หลักไม่เจอ
        if (line.contains("Build file") || line.contains("build.gradle' not found")) {
            hasError = true;
            errorType = "GRADLE_STRUCTURE_ERROR";
            errorDetails = "ไม่พบไฟล์ควบคุมสคริปต์หลัก (build.gradle) กรุณาตรวจสอบโฟลเดอร์โปรเจกต์ของคุณ";
            return true;
        }

        // ส่งข้อมูล Log ดิบกลับไปพ่นบนหน้าจอ โดยพ่นดักไว้ทั้งสองชื่อเมทอดเพื่อความปลอดภัย
        if (listener != null) {
            listener.onLogAppend(line + "\n", defaultColor);
            listener.onAppend(line + "\n", defaultColor);
        }
        return false;
    }

    /**
     * เมทอดพิมพ์สรุปผลลัพธ์ฉบับแปลภาษาไทย
     */
    public void printSummary(LogOutputListener listener) {
        if (!hasError || listener == null) return;

        sendText("\n======================================\n", COLOR_ERROR, listener);
        sendText("🔍 [Error Parser] วิเคราะห์พบสาเหตุการบิวด์ล้มเหลว:\n", COLOR_ERROR, listener);
        
        switch (errorType) {
            case "GIT_URL_MISSING":
                sendText("📌 สาเหตุ: " + errorDetails + "\n", COLOR_WARNING, listener);
                sendText("💡 วิธีแก้ไข: เข้าสู่หน้าแอปหลัก -> เปิดเมนูตั้งค่าบัญชี GitHub -> ตรวจสอบและกรอกที่อยู่ Git Repository URL ให้เรียบร้อย\n", COLOR_SUCCESS, listener);
                break;
                
            case "JAVA_COMPILE_ERROR":
                sendText("📌 สาเหตุ: " + errorDetails + "\n", COLOR_WARNING, listener);
                sendText("💡 วิธีแก้ไข: ตรวจสอบบันทึก Log บรรทัดสีแดงด้านบน ค้นหาชื่อไฟล์ .java ที่ระบบรายงานว่าพัง แล้วเข้าไปเช็คโค้ดจุดล่าสุดที่คุณแก้ไขครับ\n", COLOR_SUCCESS, listener);
                break;

            case "AUTH_ERROR":
                sendText("📌 สาเหตุ: " + errorDetails + "\n", COLOR_WARNING, listener);
                sendText("💡 วิธีแก้ไข: ทำการสร้าง Token (Classic) ชุดใหม่บน GitHub โดยติ๊กเลือกเปิดสิทธิ์ 'repo' และ 'workflow' ให้ครบถ้วน\n", COLOR_SUCCESS, listener);
                break;

            case "GRADLE_STRUCTURE_ERROR":
                sendText("📌 สาเหตุ: " + errorDetails + "\n", COLOR_WARNING, listener);
                sendText("💡 วิธีแก้ไข: ตรวจสอบดูว่าไฟล์ build.gradle มีการเผลอกดลบ หรืออยู่ในตำแหน่งที่ถูกต้องหรือไม่\n", COLOR_SUCCESS, listener);
                break;
                
            default:
                sendText("📌 สาเหตุ: พบจุดพังในขั้นตอนการคอมไพล์โค้ด\n", COLOR_WARNING, listener);
                sendText("💡 วิธีแก้ไข: ตรวจดู Log บรรทัดก่อนหน้าเพื่อดูไฟล์และบรรทัดที่ระบบชี้เป้าว่าเขียนโค้ดผิดพลาดครับ\n", COLOR_SUCCESS, listener);
                break;
        }
        sendText("======================================\n", COLOR_ERROR, listener);
    }

    /**
     * เมทอดช่วยกระจายข้อความสรุปไปยังทุกเมทอดปลายทางที่มีการเรียกใช้
     */
    private void sendText(String text, int color, LogOutputListener listener) {
        listener.onLogAppend(text, color);
        listener.onAppend(text, color);
    }
}
