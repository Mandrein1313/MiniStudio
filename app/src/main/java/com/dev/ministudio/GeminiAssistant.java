package com.dev.ministudio.ai;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.json.JSONArray;

public class GeminiAssistant {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    
    // 🌟 เพิ่มตัวแปรสำหรับเก็บ Context
    private final Context context;

    // 🌟 ปรับปรุง Constructor ให้รับ Context เข้ามาใช้งาน
    public GeminiAssistant(Context context) {
        this.context = context;
    }

    // 🌟 เมธอดสำหรับดึง API Key จาก SharedPreferences บันทึกในชื่อไฟล์ ai_settings
    private String getApiKey() {
        SharedPreferences prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE);
        return prefs.getString("groq_api_key", "");
    }

    public interface AICallback {
        void onSuccess(String responseText);
        void onError(String errorMessage);
    }

    public void askAI(final String prompt, final AICallback callback) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                // ตรวจสอบเบื้องต้นว่ามีคีย์หรือไม่ ถ้าไม่มีให้แจ้งเตือนทันทีโดยไม่ต้องยิง API ให้เสียเวลา
                String apiKey = getApiKey();
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    callback.onError("ไม่พบ Groq API Key ในการตั้งค่า กรุณากรอกคีย์ก่อนใช้งาน");
                    return;
                }

                URL url = new URL(API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                
                // 🌟 แก้ไขมาใช้คีย์ที่ดึงมาจาก SharedPreferences
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(60000);

                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "llama-3.3-70b-versatile");
                JSONArray messages = new JSONArray();
                JSONObject message = new JSONObject();
                message.put("role", "user");
                message.put("content", prompt);
                messages.put(message);
                requestBody.put("messages", messages);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) response.append(line);
                        callback.onSuccess(parseGroqResponse(response.toString()));
                    }
                } else {
                    callback.onError("Error: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                callback.onError("Exception: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private String parseGroqResponse(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            return json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        } catch (Exception e) {
            return "ไม่สามารถอ่านข้อมูลได้";
        }
    }
}
