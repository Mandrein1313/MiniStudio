package com.dev.ministudio.ai;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiAssistant {

    // 🔑 อย่าลืมเช็ก API Key ตรงนี้อีกครั้งนะครับว่าถูกต้อง ไม่มีเว้นวรรคหลุดมา
    private static final String API_KEY = "YOUR_ACTUAL_GEMINI_API_KEY"; 
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
                // กำหนดประเภทข้อมูลและการเข้ารหัส UTF-8 ให้ชัดเจนเพื่อรองรับภาษาไทย
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                // 🛠️ ปรับลอจิกการทำความสะอาดข้อความขั้นสูงสุดก่อนประกอบร่างเป็น JSON
                String ultraCleanPrompt = cleanStringForJson(safePrompt);

                // ประกอบ JSON แบบเรียงบรรทัดเดียว คลีนที่สุด ไม่มีการขึ้นบรรทัดใหม่ในโครงสร้าง JSON
                String jsonRequestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + ultraCleanPrompt + "\"}]}]}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonRequestBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        
                        callback.onSuccess(parseGeminiResponse(response.toString()));
                    }
                } else {
                    // แอบพ่วงเอาข้อมูล Error Stream มาอ่านด้วยเวลาพัง เผื่อส่ง Log ไปตรวจสอบเพิ่มได้
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
     * ฟังก์ชันล้างสิ่งสกปรกและอักขระอันตรายขั้นรุนแรงที่ทำให้ JSON พัง
     */
    private String cleanStringForJson(String input) {
        if (input == null) return "";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n"); // แปลงตัวขึ้นบรรทัดใหม่ให้กลายเป็นอักขระสัญลักษณ์
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    // กรองอักขระควบคุม (Control characters) ที่พิมพ์ไม่ได้ออกทั้งหมด
                    if (ch >= 0 && ch <= 31) {
                        // ปล่อยผ่านเฉพาะค่าที่เราแปลงไปแล้วด้านบน นอกนั้นตัดทิ้งป้องกัน Code 400
                        continue;
                    } else {
                        sb.append(ch);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * ฟังก์ชันสำหรับแกะเอาข้อความคำตอบออกจาก JSON ของ Gemini
     */
    private String parseGeminiResponse(String jsonResponse) {
        try {
            if (jsonResponse.contains("\"text\": \"")) {
                int start = jsonResponse.indexOf("\"text\": \"") + 9;
                int end = jsonResponse.indexOf("\"", start);
                while (end > start && jsonResponse.charAt(end - 1) == '\\') {
                    // วนลูปหาเครื่องหมายคำพูดปิดที่แท้จริง (ไม่ใช่เครื่องหมายที่โดน Escape ไว้)
                    end = jsonResponse.indexOf("\"", end + 1);
                }
                if (start > 9 && end > start) {
                    String result = jsonResponse.substring(start, end);
                    // ปรับค่ากลับคืนเพื่อให้เว้นบรรทัดและแสดงผลบน Text ข้อความสวย ๆ
                    return result.replace("\\n", "\n")
                                 .replace("\\\"", "\"")
                                 .replace("\\\\", "\\");
                }
            }
            return jsonResponse; 
        } catch (Exception e) {
            return jsonResponse;
        }
    }
}
