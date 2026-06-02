package com.dev.ministudio.ai;

import android.os.Handler;
import android.os.Looper;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class GeminiAssistant {

    // 🔑 ใส่ API Key ของ Google AI Studio ที่นี่ครับ
    private static final String API_KEY = "YOUR_GEMINI_API_KEY_HERE"; 
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AICallback {
        void onSuccess(String responseText);
        void onError(String errorMessage);
    }

    /**
     * 🚀 ส่งข้อความหรือโค้ดไปให้ AI ประมวลผลแบบเบื้องหลัง (Background Thread)
     */
    public void askAI(final String prompt, final AICallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // สร้าง JSON Payload ตามโครงสร้างมาตรฐานของ Gemini API
                JSONObject textObj = new JSONObject();
                textObj.put("text", prompt);

                JSONObject partsObj = new JSONObject();
                partsObj.put("parts", new JSONArray().put(textObj));

                JSONObject contentsObj = new JSONObject();
                contentsObj.put("contents", new JSONArray().put(partsObj));

                // พ่นข้อมูลส่งไปยังเซิร์ฟเวอร์ Google
                OutputStream os = conn.getOutputStream();
                os.write(contentsObj.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Scanner scanner = new Scanner(conn.getInputStream(), "UTF-8");
                    String responseBody = scanner.useDelimiter("\\A").next();
                    scanner.close();

                    // แกะเอาเฉพาะข้อความคำตอบของ AI ออกมา
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String aiText = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    // ส่งผลลัพธ์กลับไปที่หน้าจอหลัก (UI Thread)
                    mainHandler.post(() -> callback.onSuccess(aiText));
                } else {
                    mainHandler.post(() -> callback.onError("Server Error: Code " + responseCode));
                }
                conn.disconnect();

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}
