package com.dev.ministudio;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🪄 คลาสผู้จัดการสกัดประมวลผลการ Refactor และ Optimize ซอร์สโค้ด (ระบบที่ 3)
 * ทำหน้าที่สร้างคำสั่ง Prompt และสกัดเอาเฉพาะท่อนรหัสโค้ดบริสุทธิ์ออกจากบล็อก Markdown
 */
public class CodeOptimizerManager {

    /**
     * 1. ฟังก์ชันสร้างข้อความคำสั่ง (Prompt) 
     */
    public static String createOptimizePrompt(String fileName, String rawCode) {
        if (rawCode == null || rawCode.trim().isEmpty()) return null;

        StringBuilder prompt = new StringBuilder();
        prompt.append("คุณคือ Senior Android Developer และ Expert Code Reviewer ประจำระบบ MiniStudio\n");
        prompt.append("หน้าที่ของคุณคือตรวจสอบและปรับปรุง (Refactor & Optimize) ซอร์สโค้ดนี้ให้มีประสิทธิภาพสูงสุด\n\n");
        prompt.append("[ข้อมูลไฟล์ที่ต้องปรับปรุง]\n");
        prompt.append("ชื่อไฟล์: ").append(fileName).append("\n\n");
        prompt.append("[ซอร์สโค้ดต้นฉบับ]:\n");
        prompt.append("```\n").append(rawCode).append("\n```\n\n");
        prompt.append("----------------------------------------\n");
        prompt.append("[เงื่อนไขและกฎเหล็กในการตอบกลับ]:\n");
        prompt.append("1. ปรับปรุงโค้ดให้สั้น กระชับ และมีประสิทธิภาพ\n");
        prompt.append("2. ห้ามตัดฟังก์ชันหรือตรรกะเดิมทิ้งเด็ดขาด\n");
        prompt.append("3. ตอบกลับตามรูปแบบนี้เท่านั้น:\n\n");
        prompt.append("✨ [โค้ดที่ปรับปรุงแล้ว]\n");
        prompt.append("```java\n");
        prompt.append("(ใส่ซอร์สโค้ดที่ปรับปรุงใหม่ทั้งหมดที่นี่)\n");
        prompt.append("```\n\n");
        prompt.append("📝 [รายละเอียดการปรับปรุง]\n");
        prompt.append("(อธิบายการปรับปรุงสั้นๆ เป็นภาษาไทย)\n");

        return prompt.toString();
    }

    /**
     * ✂️ 2. ฟังก์ชันสกัดลอกคัดแยกคำตอบจาก AI 
     */
    public static OptimizedResult parseAiResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return new OptimizedResult("", "⚠️ ไม่ได้รับการตอบกลับจาก AI ครับน้า");
        }

        String updatedCode = "";
        String explanation = "";

        try {
            // ใช้ Pattern ที่ครอบคลุมการดึงบล็อกโค้ด java
            Pattern codePattern = Pattern.compile("```java\\s*([\\s\\S]*?)\\s*```", Pattern.CASE_INSENSITIVE);
            Matcher matcher = codePattern.matcher(aiResponse);

            if (matcher.find()) {
                updatedCode = matcher.group(1).trim();
            }

            // แยกส่วนคำอธิบายโดยตัดที่หัวข้อ 📝 [รายละเอียดการปรับปรุง]
            String searchKey = "📝 [รายละเอียดการปรับปรุง]";
            int index = aiResponse.indexOf(searchKey);
            
            if (index != -1) {
                explanation = aiResponse.substring(index).trim();
            } else {
                // หากไม่พบหัวข้อคำอธิบาย ให้เอาข้อความทั้งหมดที่ไม่ใช่บล็อกโค้ดมาเป็นคำอธิบายแทน
                explanation = aiResponse.replaceAll("```[\\s\\S]*?```", "").trim();
            }

            // ถ้าหาโค้ดไม่เจอ ให้ส่งข้อความทั้งหมดกลับไปเป็นคำอธิบาย (เผื่อ AI ตอบพลาด)
            if (updatedCode.isEmpty()) {
                explanation = aiResponse;
            }

        } catch (Exception e) {
            e.printStackTrace();
            explanation = "❌ เกิดข้อผิดพลาดในการประมวลผล: " + e.getMessage();
        }

        return new OptimizedResult(updatedCode, explanation);
    }
}
