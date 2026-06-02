package com.dev.ministudio.ai;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiAssistant {

    // 🔑 รอบนี้ถ้าได้คีย์ที่ขึ้นต้นด้วย "AIzaSy..." มา วางลงตรงนี้ได้เลยครับ!
    private static final String API_KEY = "AQ.Ab8RN6L6ygJRWh1TtmucqCINfFevtd74qbqU2B_PTcgLvha1Hg"; 
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

    public interface AICallback {
        void onSuccess(String responseText);
        void onError(String errorMessage);
    }

    public void askAI(final String prompt, final AICallback callback) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                // ตรวจสอบเบื้องต้นเผื่อลืมเปลี่ยนค่าคีย์
                if (API_KEY.startsWith("AQ.Ab8") || API_KEY.contains("your_real_key")) {
                    callback.onError("ระบบล็อก: คีย์ยังเป็นรูปแบบเดิม (AQ.) ซึ่งเซิร์ฟเวอร์ไม่รองรับ กรุณาใช้คีย์ที่ขึ้นต้นด้วย AIzaSy");
                    return;
                }

                URL url = new URL(API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(60000);

                // ประกอบ JSON วัตถุแบบปลอดภัยสากล
                org.json.JSONObject requestBody = new org.json.JSONObject();
                org.json.JSONArray contents = new org.json.JSONArray();
                org.json.JSONObject content = new org.json.JSONObject();
                org.json.JSONArray parts = new org.json.JSONArray();
                org.json.JSONObject part = new org.json.JSONObject();

                part.put("text", prompt);
                parts.put(part);
                content.put("parts", parts);
                contents.put(content);
                requestBody.put("contents", contents);

                String jsonRequestBody = requestBody.toString();

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonRequestBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }
                        callback.onSuccess(parseGeminiResponse(response.toString()));
                    }
                } else {
                    // อ่านรายละเอียดตัวเต็มส่งกลับไปโชว์ที่หน้าจอ Console
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        StringBuilder error = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            error.append(line);
                        }
                        callback.onError("Server Error: Code " + responseCode + " - " + error.toString());
                    }
                }

            } catch (Exception e) {
                callback.onError("Connection Failed: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private String parseGeminiResponse(String jsonResponse) {
        try {
            org.json.JSONObject json = new org.json.JSONObject(jsonResponse);
            return json.getJSONArray("candidates")
                       .getJSONObject(0)
                       .getJSONObject("content")
                       .getJSONArray("parts")
                       .getJSONObject(0)
                       .getString("text");
        } catch (Exception e) {
            return "แกะ JSON สำเร็จแต่โครงสร้างคำตอบผิดพลาด: " + jsonResponse;
        }
    }
}
