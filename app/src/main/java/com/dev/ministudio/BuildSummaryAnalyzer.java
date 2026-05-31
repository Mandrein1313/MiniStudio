package com.dev.ministudio;

import android.graphics.Color;

public class BuildSummaryAnalyzer {

    // สร้าง Interface สำหรับส่ง Log กลับไปแสดงผลบนหน้าจอ Console
    public interface LogCallback {
        void onAppend(String text, int color);
    }

    private boolean hasError = false;
    private String errorType = "UNKNOWN";
    private String errorDetails = "";
    
    // สีกรอบข้อความเพื่อความสวยงาม
    private final int COLOR_ERROR = Color.parseColor("#FF8A80");
    private final int COLOR_WARNING = Color.parseColor("#FFB74D");
    private final int COLOR_SUCCESS = Color.parseColor("#81C784");

    /**
     * เมทอดสำหรับอ่านและวิเคราะห์ Log ทีละบรรทัด
     * @return true ถ้าเจอ Error วิกฤตและต้องการให้ระบบหยุดอ่าน Log ทันที เพื่อประหยัดเวลา
     */
    public boolean analyzeLine(String line, int defaultColor, LogCallback callback) {
        // 1. ตรวจสอบ Error: ลืมใส่พิกัด URL ของ Git Repository
        if (line.contains("ใส่_URL_Git_Repository_ของคุณตรงนี้") || line.contains("not found")) {
            hasError = true;
            errorType = "GIT_URL_MISSING";
            errorDetails = "คุณยังไม่ได้แก้ไขหรือระบุ URL ของ GitHub Repository ให้ถูกต้องในหน้าตั้งค่า";
            return true; // เจอจุดตายแล้ว สั่งหยุดดึง Log บรรทัดที่เหลือได้เลย
        }

        // 2. ตรวจสอบ Error: โค้ด Java พัง (Compilation Failed)
        if (line.contains("error: class, interface, enum, or record expected") || line.contains("Compilation failed")) {
            hasError = true;
            errorType = "JAVA_COMPILE_ERROR";
            // พยายามจับเอาชื่อไฟล์ที่พังมาแสดงผล
            if (line.contains(".java:")) {
                errorDetails = "พบข้อผิดพลาดทางไวยากรณ์ (Syntax Error) ในซอร์สโค้ดของคุณ: \n" + line.trim();
            } else {
                errorDetails = "โค้ด Java ไม่สามารถคอมไพล์ได้ กรุณาตรวจสอบวงเล็บปีกกา {} หรือเครื่องหมาย semicolon (;)";
            }
            return false; // ปล่อยให้อ่านต่อเผื่อเจอจุดที่พังเพิ่มเติม
        }

        // 3. ตรวจสอบ Error: ปัญหาเรื่องสิทธิ์เข้าถึง (Bad credentials / Token พัง)
        if (line.contains("Authentication failed") || line.contains("401 Unauthorized") || line.contains("Bad credentials")) {
            hasError = true;
            errorType = "AUTH_ERROR";
            errorDetails = "Personal Access Token (PAT) ไม่ถูกต้อง หรือไม่มีสิทธิ์เข้าถึง Repository นี้ (กรุณาเช็คสิทธิ์ repo และ workflow)";
            return true;
        }

        // 4. ตรวจสอบ Error: โครงสร้างโปรเจกต์พัง หรือหา build.gradle ไม่เจอ
        if (line.contains("Build file") || line.contains("build.gradle' not found")) {
            hasError = true;
            errorType = "GRADLE_STRUCTURE_ERROR";
            errorDetails = "ไม่พบไฟล์ build.gradle ในโฟลเดอร์หลัก กรุณาตรวจสอบโครงสร้างโฟลเดอร์โปรเจกต์ของคุณ";
            return true;
        }

        // พ่น Log ดิบแสดงผลบนหน้าจอตามปกติไปเรื่อยๆ
        callback.onAppend(line + "\n", defaultColor);
        return false;
    }

    /**
     * เมทอดสำหรับพิมพ์สรุปผลหลังจากอ่าน Log ทั้งหมดเสร็จสิ้นแล้ว
     */
    public void printSummary(LogCallback callback) {
        if (!hasError) return;

        callback.onAppend("\n======================================\n", COLOR_ERROR);
        callback.onAppend("🔍 [Error Parser] วิเคราะห์พบสาเหตุการบิวด์ล้มเหลว:\n", COLOR_ERROR);
        
        switch (errorType) {
            case "GIT_URL_MISSING":
                callback.onAppend("📌 สาเหตุ: " + errorDetails + "\n", COLOR_WARNING);
                callback.onAppend("💡 วิธีแก้ไข: ไปที่หน้าแอปหลัก -> กดไอคอนตั้งค่า (รูปเฟืองหรือประแจ) -> ตรวจสอบช่อง Git Repository URL และกรอกลิงก์ให้ถูกต้อง เช่น https://github.com/ユーザー名/リポジトリ名.git\n", COLOR_SUCCESS);
                break;
                
            case "JAVA_COMPILE_ERROR":
                callback.onAppend("📌 สาเหตุ: " + errorDetails + "\n", COLOR_WARNING);
                callback.onAppend("💡 วิธีแก้ไข: เปิดไฟล์ตามที่ระบบแจ้งเตือนด้านบน แล้วตรวจสอบโค้ดจุดล่าสุดที่คุณแก้ไขว่าพิมพ์อะไรตกหล่นไปหรือไม่\n", COLOR_SUCCESS);
                break;

            case "AUTH_ERROR":
                callback.onAppend("📌 สาเหตุ: " + errorDetails + "\n", COLOR_WARNING);
                callback.onAppend("💡 วิธีแก้ไข: เจนเนอเรต GitHub Token (Classic) ชุดใหม่ และตรวจสอบว่าได้ติ๊กเลือกสิทธิ์ 'repo', 'workflow' และ 'write:packages' เรียบร้อยแล้วก่อนนำมาบันทึกในแอป\n", COLOR_SUCCESS);
                break;

            case "GRADLE_STRUCTURE_ERROR":
                callback.onAppend("📌 สาเหตุ: " + errorDetails + "\n", COLOR_WARNING);
                callback.onAppend("💡 วิธีแก้ไข: ตรวจสอบให้แน่ใจว่าได้สร้างโปรเจกต์ในโฟลเดอร์ที่แอปกำหนด และมีไฟล์เซ็ตอัปพื้นฐานครบถ้วน\n", COLOR_SUCCESS);
                break;
                
            default:
                callback.onAppend("📌 สาเหตุ: เกิดข้อผิดพลาดที่ไม่รู้จักในขั้นตอนคอมไพล์\n", COLOR_WARNING);
                callback.onAppend("💡 วิธีแก้ไข: กรุณากดเข้าไปดู Log ฉบับเต็มบนเว็บ GitHub Actions เพื่อวิเคราะห์ปัญหาเพิ่มเติมครับ\n", COLOR_SUCCESS);
                break;
        }
        callback.onAppend("======================================\n", COLOR_ERROR);
    }
}
