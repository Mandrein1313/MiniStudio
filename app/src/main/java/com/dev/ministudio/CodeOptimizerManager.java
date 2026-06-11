package com.dev.ministudio;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🪄 คลาสผู้จัดการ Optimize & Refactor ซอร์สโค้ด (เวอร์ชันปรับปรุง)
 */
public class CodeOptimizerManager {

    public static String createOptimizePrompt(String fileName, String rawCode) {
        if (rawCode == null || rawCode.trim().isEmpty()) return null;

        StringBuilder prompt = new StringBuilder();
        prompt.append("คุณคือ Senior Android Developer + Expert Code Reviewer ของ MiniStudio\n\n");
        prompt.append("**หน้าที่:** Refactor และ Optimize โค้ดนี้ให้ดีที่สุด (เร็วขึ้น, อ่านง่ายขึ้น, ใช้ทรัพยากรน้อยลง) โดยรักษาทุกฟังก์ชันการทำงานเดิมไว้\n\n");

        prompt.append("**ไฟล์:** ").append(fileName).append("\n\n");
        prompt.append("**โค้ดต้นฉบับ:**\n```java\n").append(rawCode).append("\n```\n\n");

        prompt.append("**กฎที่ต้องปฏิบัติตามอย่างเคร่งครัด:**\n");
        prompt.append("1. ห้ามตัดหรือลบฟังก์ชัน/ตรรกะสำคัญใดๆ\n");
        prompt.append("2. ใช้ Best Practice ของ Android/Java (Modern syntax, ลด duplication, ดีขึ้นเรื่อง performance/memory)\n");
        prompt.append("3. ตอบกลับในรูปแบบนี้ **เท่านั้น**:\n\n");
        prompt.append("✨ **โค้ดที่ปรับปรุงแล้ว**\n");
        prompt.append("```java\n// โค้ดทั้งไฟล์ที่นี่\n```\n\n");
        prompt.append("📝 **สรุปการปรับปรุง**\n- รายการเปลี่ยนแปลงเป็นข้อๆ (ภาษาไทย)\n");

        return prompt.toString();
    }

    /**
     * แยกโค้ดและคำอธิบายออกจากกันอย่างแม่นยำ
     */
    public static OptimizedResult parseAiResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return new OptimizedResult("", "⚠️ ไม่ได้รับการตอบกลับจาก AI");
        }

        String updatedCode = "";
        String explanation = "";

        try {
            // จับบล็อกโค้ด (รองรับ ```java และ ``` ธรรมดา)
            Pattern codePattern = Pattern.compile("```(?:java)?\\s*([\\s\\S]*?)\\s*```", Pattern.CASE_INSENSITIVE);
            Matcher matcher = codePattern.matcher(aiResponse);

            if (matcher.find()) {
                updatedCode = matcher.group(1).trim();
            }

            // หาคำอธิบาย
            int explIndex = aiResponse.indexOf("📝 **สรุปการปรับปรุง**");
            if (explIndex == -1) explIndex = aiResponse.indexOf("📝 [รายละเอียดการปรับปรุง]");

            if (explIndex != -1) {
                explanation = aiResponse.substring(explIndex).trim();
            } else {
                // fallback
                explanation = aiResponse.replaceAll("```[\\s\\S]*?```", "").trim();
            }

            if (updatedCode.isEmpty()) {
                explanation = "❌ ไม่พบบล็อกโค้ดในคำตอบของ AI\n\n" + aiResponse;
            }

        } catch (Exception e) {
            e.printStackTrace();
            explanation = "❌ Parser เกิดข้อผิดพลาด: " + e.getMessage();
        }

        return new OptimizedResult(updatedCode, explanation);
    }
}