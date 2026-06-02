package com.dev.ministudio.ai;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiAssistant {

    // 🔑 ไปที่ Google AI Studio เพื่อสร้าง API Key แล้วเอามาใส่ตรงนี้ครับ
    private static final String API_KEY = "YOUR_ACTUAL_GEMINI_API_KEY"; 
    // ใช้ Endpoint ล่าสุดที่เสถียรสำหรับข้อความ
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

    public interface AICallback {
        void onSuccess(String responseText);
        void onError(String errorMessage);
    }

    public void askAI(final String safePrompt, final AICallback callback) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                // ⚠️ สำคัญมาก: ต้องระบุว่าเป็น JSON เสมอ ป้องกันเซิร์ฟเวอร์ปฏิเสธข้อมูล
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                // 📦 สร้างโครงสร้าง JSON ที่ถูกต้องตามข้อกำหนดของ Gemini API 
                // ใช้ปีกกาและโครงสร้างแบบมีระเบียบ
                String jsonRequestBody = "{"
                        + "\"contents\": [{"
                        + "    \"parts\": [{"
                        + "        \"text\": \"" + safePrompt + "\""
                        + "    }]"
                        + "}]"
                        + "}";

                // ยิงข้อมูลออกแบบ UTF-8 ป้องกันภาษาไทยเพี้ยน
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonRequestBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // อ่านผลลัพธ์ที่ได้กลับมาเมื่อสำเร็จ
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.add(responseLine.trim());
                        }
                        
                        // 🪄 ตรงนี้ถ้าได้ผลลัพธ์เป็น JSON ดิบมา คุณอาจจะต้องเขียนลอจิกแกะเอาเฉพาะข้อความในคลาสสิกโหมดเพิ่ม
                        // เพื่อความรวดเร็วขอดึงข้อความดิบส่งกลับไปก่อน
                        callback.onSuccess(parseGeminiResponse(response.toString()));
                    }
                } else {
                    // หากไม่ได้ผลลัพธ์ 200 ให้ส่ง Error Code กลับไปแสดงที่ Console
                    callback.onError("Server Error: Code " + responseCode);
                }

            } catch (Exception e) {
                callback.onError("Connection Failed: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    /**
     * ฟังก์ชันแบบง่ายในการแกะเอาข้อความคำตอบออกจาก JSON ของ Gemini
     */
    private String parseGeminiResponse(String jsonResponse) {
        try {
            // เพื่อไม่ให้ต้องพึ่งพาไลบรารีภายนอกหนาๆ บนมือถือ ใช้ String manipulation แบบบ้านๆ แกะเอาข้อความ
            if (jsonResponse.contains("\"text\": \"")) {
                int start = jsonResponse.indexOf("\"text\": \"") + 9;
                int end = jsonResponse.indexOf("\"", start);
                if (start > 9 && end > start) {
                    String result = jsonResponse.substring(start, end);
                    // แปลงตัวแปลงกลับอักขระให้แสดงบนหน้าจอแอปสวยงาม
                    return result.replace("\\n", "\n").replace("\\\"", "\"");
                }
            }
            return jsonResponse; // คืนค่าดิบถ้าแกะไม่เจอ
        } catch (Exception e) {
            return jsonResponse;
        }
    }
}
