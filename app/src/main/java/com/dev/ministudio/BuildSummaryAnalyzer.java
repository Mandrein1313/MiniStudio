package com.dev.ministudio;

import android.graphics.Color;
import com.dev.ministudio.TerminalColor;

public class BuildSummaryAnalyzer {

    private String detectedFileName = null;
    private String detectedLineNumber = null;
    private String detectedErrorText = null;
    private boolean isAborted = false;
    
    // สถานะขั้นตอนการรอข้อความ: 0 = ปกติ, 1 = เจอพิกัดชี้เป้าแล้ว รอข้อความ Error จริงในบรรทัดถัดไป
    private int errorCaptureState = 0; 

    public interface LogOutputListener {
        void onAppendLog(String text, int color);
    }

    /**
     * สแกนตรวจสอบบรรทัด Log ทุกบรรทัดแบบเรียลไทม์
     * @return คืนค่า true หากต้องการส่งสัญญาณให้ระบบหลักหยุดดึง Log เพิ่มทันที
     */
    public boolean analyzeLine(String text, int originalColor, LogOutputListener listener) {
        // 🛑 สัญญาณหยุดทำงานทำงานไปแล้ว ปฏิเสธการสแกนบรรทัดถัดไปและบอกให้หยุดลูป
        if (isAborted) return true;
        if (text == null) return false;

        // 🛠️ แก้บั๊กคลาวด์: ตัดระบบ Timestamp ของ GitHub Actions ออกไปถ้าตรวจเจอ (เช่น 2026-05-31T12:34:56.1234567Z)
        // เพื่อป้องกันไม่ให้ฟังก์ชันสแกนตัวอักษรตัวแรกผิดพลาด
        text = text.replaceAll("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z\\s*", "");

        String trimmedText = text.trim();
        String lowerText = trimmedText.toLowerCase();

        // 🔍 บล็อกดักจับรายละเอียดข้อความคำอธิบาย (กรณีข้อมูลเลื่อนลงมาอยู่บรรทัดถัดไป)
        if (errorCaptureState == 1) {
            // คัดกรองและข้ามบรรทัดโค้ดตัวอย่าง สัญลักษณ์ลูกศรชี้พิกัด (^) หรือข้อความอธิบายบริบทของ compiler ออกไป
            if (!trimmedText.isEmpty() && !trimmedText.startsWith("^") && !lowerText.contains("symbol:") && !lowerText.contains("location:")) {
                detectedErrorText = trimmedText;
                errorCaptureState = 0; // เคลียร์สถานะล็อกเป้าหมาย
                
                // 🛑 ได้ข้อมูลครบเรียบร้อยแล้ว สั่ง Abort ระบบหลักทันที
                isAborted = true; 
                return true; 
            }
        }

        // 🔍 ตรวจสอบรูปแบบโครงสร้างการพังมาตรฐานของ Java Compiler: [เส้นทางไฟล์].java:[บรรทัด]: error: [คำอธิบาย]
        if (lowerText.contains(".java:") && lowerText.contains("error:")) {
            try {
                int javaIdx = lowerText.indexOf(".java:");
                int errorIdx = lowerText.indexOf("error:");

                // แยกและสกัดเอาเฉพาะชื่อไฟล์ซอร์สโค้ดจาก Path เต็ม
                String fullFilePath = trimmedText.substring(0, javaIdx + 5);
                if (fullFilePath.contains("/")) {
                    detectedFileName = fullFilePath.substring(fullFilePath.lastIndexOf("/") + 1);
                } else {
                    detectedFileName = fullFilePath;
                }

                // แยกหมายเลขบรรทัดที่เกิดปัญหา
                String linePart = trimmedText.substring(javaIdx + 6, errorIdx).replace(":", "").trim();
                detectedLineNumber = linePart;

                // ตรวจสอบรายละเอียดต่อท้ายคำว่า error: ในบรรทัดเดียวกัน
                String descriptionPart = trimmedText.substring(errorIdx + 6).trim();
                if (!descriptionPart.isEmpty()) {
                    detectedErrorText = descriptionPart;
                    errorCaptureState = 0;
                    
                    // 🛑 ตรวจพบข้อมูลสมบูรณ์ครบถ้วนในบรรทัดเดียว สั่งหยุดดึง Log เพิ่มเติมทันที
                    isAborted = true;
                    return true; 
                } else {
                    // หากยังไม่มีเนื้อหาต่อท้าย ให้เปิดสถานะระบบเพื่อเตรียมสแกนหาข้อความในบรรทัดถัดไป
                    errorCaptureState = 1; 
                }
                
            } catch (Exception e) {
                errorCaptureState = 0;
            }
        }

        return false;
    }

 public void printSummary(LogOutputListener listener) {
    if (detectedFileName != null && detectedLineNumber != null) {
        // 🛠️ ปรับเปลี่ยน: เปลี่ยนสีกรอบบนจาก TARGET_ORANGE เป็น DETAIL_RED เพื่อให้กล่องเป็นสีแดงทั้งหมด
        listener.onAppendLog("\n┏━━━━━━━━━━━━━━━━ คำแนะนำการแก้ไขโค้ด ━━━━━━━━━━━━━━━━┓\n", TerminalColor.DETAIL_RED);
        listener.onAppendLog("  📍 ตำแหน่ง  : ไฟล์ " + detectedFileName + " (บรรทัดที่ " + detectedLineNumber + ")\n", TerminalColor.TEXT_WHITE);
        
        String cleanError = detectedErrorText != null ? detectedErrorText.toLowerCase() : "";
        
        // ตรวจเช็คเงื่อนไขและแยกการแสดงผลตามประเภท
        if (cleanError.contains("cannot find symbol") || cleanError.contains("cannot be resolved")) {
            listener.onAppendLog("  💡 แนะนำ   : ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog("หาตัวแปร, ฟังก์ชัน หรือคลาสนี้ไม่เจอ (ลองเช็คตัวสะกด พิมพ์ผิด หรือลืม import)\n", TerminalColor.SUGGEST_GREEN);
            
        } else if (cleanError.contains("expected") || cleanError.contains(";")) {
            listener.onAppendLog("  💡 แนะนำ   : ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog("ลืมใส่เครื่องหมายเซมิโคลอน (;) หรือลืมปิดวงเล็บ ) / ปีกกา } ในบรรทัดดังกล่าว\n", TerminalColor.SUGGEST_GREEN);
            
        } else if (cleanError.contains("already defined") || cleanError.contains("duplicate")) {
            listener.onAppendLog("  💡 แนะนำ   : ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog("มีการประกาศชื่อตัวแปร หรือเมทอด (Method) นี้ซ้ำซ้อนกันในขอบเขตสโคปเดียวกัน\n", TerminalColor.SUGGEST_GREEN);
            
        } else if (cleanError.contains("is abstract") || cleanError.contains("does not override")) {
            listener.onAppendLog("  💡 แนะนำ   : ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog("ลืมเขียนโค้ดเพื่อ Override เมทอดที่จำเป็นตามเงื่อนไขของคลาส Interface / Abstract\n", TerminalColor.SUGGEST_GREEN);
            
        } else if (detectedErrorText != null) {
            listener.onAppendLog("  💥 ตรวจพบข้อผิดพลาด: ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog(detectedErrorText + "\n", TerminalColor.DETAIL_RED); 
            
        } else {
            listener.onAppendLog("  💡 แนะนำ   : ", TerminalColor.TEXT_WHITE);
            listener.onAppendLog("พบข้อผิดพลาดเกี่ยวกับไวยากรณ์ (Syntax Error) ภายในไฟล์นี้\n", TerminalColor.SUGGEST_GREEN);
        }

        // 🛠️ ปรับเปลี่ยน: เปลี่ยนสีกรอบล่างเป็น DETAIL_RED เพื่อปิดกล่องสีแดงให้สมบูรณ์
        listener.onAppendLog("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛\n", TerminalColor.DETAIL_RED);
    } else {
        listener.onAppendLog("\n❌ บิวด์ล้มเหลว: ไม่พบตำแหน่งซอร์สโค้ดที่พัง คาดว่าเป็นปัญหาที่ไฟล์ build.gradle หรือโครงสร้างระบบโปรเจกต์\n", TerminalColor.TARGET_ORANGE);
    }
}
 }
