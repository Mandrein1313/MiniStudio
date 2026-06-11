package com.dev.ministudio;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeOptimizerManager {

    /**
     * 🪄 1. ฟังก์ชันสร้าง Prompt ส่งโค้ดในหน้าจอไปให้ AI วิเคราะห์ปรับปรุง
     */
    public static String createOptimizePrompt(String fileName, String rawCode) {
        if (rawCode == null || rawCode.trim().isEmpty()) return null;

        StringBuilder prompt = new StringBuilder();
        prompt.append("คุณคือ Senior Android Developer และ Expert Code Reviewer\n");
        prompt.append("หน้าที่ของคุณคือตรวจสอบและปรับปรุง (Refactor & Optimize) ซอร์สโค้ดนี้ให้สะอาดที่สุด\n\n");
        prompt.append("[ข้อมูลไฟล์]\n");
        prompt.append("ชื่อไฟล์: ").append(fileName).append("\n\n");
        prompt.append("[ซอร์สโค้ดต้นฉบับ]\n");
        prompt.append("```\n").append(rawCode).append("\n```\n\n");
        prompt.append("----------------------------------------\n");
        prompt.append("[เงื่อนไขและกฎเหล็กในการตอบกลับ]:\n");
        prompt.append("1. ค้นหาจุดที่เยิ่นเย้อ, จุดที่เสี่ยงกินแรม (Memory Leak), หรือโค้ดที่ทำให้อืด\n");
        prompt.append("2. ปรับปรุงโครงสร้างโค้ดให้ สั้น กระชับ มีประสิทธิภาพสูงขึ้น และอ่านง่ายตามสากล\n");
        prompt.append("3. ห้ามตัดฟังก์ชันสำคัญหรือตรรกะเดิมที่จำเป็นทำงานอยู่ทิ้ง\n");
        prompt.append("4. บังคับใช้รูปแบบการตอบกลับตามโครงสร้างด้านล่างนี้เท่านั้น ห้ามใช้คำนำอื่น:\n\n");
        prompt.append("✨ [โค้ดที่ปรับปรุงแล้ว]\n");
        prompt.append("```\n");
        prompt.append("(ใส่ซอร์สโค้ดที่ปรับปรุงใหม่ทั้งหมดแบบสมบูรณ์ตั้งแต่บรรทัดแรกถึงบรรทัดสุดท้ายที่นี่)\n");
        prompt.append("```\n\n");
        prompt.append("📝 [รายละเอียดการปรับปรุง]\n");
        prompt.append("(อธิบายเป็นข้อๆ สั้นกระชับในภาษาไทยว่า มีจุดไหนเยิ่นเย้อ และปรับปรุงให้ดีขึ้นอย่างไรบ้าง)\n");

        return prompt.toString();
    }

    /**
     * ✂️ 2. ฟังก์ชันตัดแยกคำตอบจาก AI เพื่อดึงโค้ดสะอาดออกมาใช้งานแยกจากคำอธิบาย
     */
    public static OptimizedResult parseAiResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) return null;

        String updatedCode = "";
        String explanation = "";

        // ดึงข้อความใน Markdown Code Block (```...```) อันแรกซึ่งจะเป็นส่วนของโค้ดใหม่
        Pattern codePattern = Pattern.compile("
http://googleusercontent.com/immersive_entry_chip/0

---

### ✨ ความเจ๋งหลังจากน้าประกอบระบบที่ 3 นี้เสร็จ:
* **แอปเราจะ Pro ขึ้นมากครับ:** น้าสามารถกดรีวิวโค้ดตัวเองได้ตลอดเวลา โดยไม่ต้องรอให้บิวด์พัง
* **เปลี่ยนโค้ดอัตโนมัติ:** ระบบฉลาดพอที่จะแกะเอาเฉพาะตัวซอร์สโค้ดใหม่เอี่ยมไปเขียนทับในหน้าจอของน้าได้เองทันทีหลังกดตกลง โดยไม่เอาตัวหนังสือภาษาไทยพ่วงติดไปในไฟล์โค้ดครับ

น้าลองสวมคลาสทั้ง 2 ตัวนี้เข้าไปในโปรเจกต์ MiniStudio แล้วลองผูกเข้ากับปุ่มบนหน้าจอแก้ไขโค้ดดูนะครับ ได้ผลการทำงานเป็นอย่างไร หรือติดปัญหาการเชื่อม API ตรงไหน ส่งสัญญาณบอกหลานได้ทันทีเลยครับน้า! 🪄😎
