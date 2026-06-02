package com.dev.ministudio.ai;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiAssistant {

    // 🔑 เปลี่ยนตรงนี้เป็น API Key ของคุณที่ได้มาจาก Google AI Studio นะครับ
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
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                // 📦 ปรับเปลี่ยนโครงสร้างการต่อข้อความ JSON Payload ให้แข็งแรงขึ้น ป้องกันการหลุดฟอร์แมต
                String jsonRequestBody = "{"
                        + "\"contents\": [{"
                        + "    \"parts\": [{"
                        + "        \"text\": \"" + safePrompt + "\""
                        + "    }]"
                        + "}]"
                        + "}";

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
                            // 🛠️ แก้ไขจาก .add() เป็น .append() เพื่ออุดรูรั่วคอมไพล์พังบรรทัดที่ 61 แล้วครับ
                            response.append(responseLine.trim());
                        }
                        
                        callback.onSuccess(parseGeminiResponse(response.toString()));
                    }
                } else {
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
     * ฟังก์ชันแกะเอาข้อความคำตอบออกจาก JSON ของ Gemini
     */
    private String parseGeminiResponse(String jsonResponse) {
        try {
            if (jsonResponse.contains("\"text\": \"")) {
                int start = jsonResponse.indexOf("\"text\": \"") + 9;
                int end = jsonResponse.indexOf("\"", start);
                if (start > 9 && end > start) {
                    String result = jsonResponse.substring(start, end);
                    // ปรับค่ากลับคืนเพื่อให้เว้นบรรทัดและแสดงผลภาษาไทยได้อย่างถูกต้อง
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
