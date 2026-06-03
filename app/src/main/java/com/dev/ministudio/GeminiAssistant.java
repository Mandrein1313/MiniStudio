package com.dev.ministudio.ai;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.json.JSONArray;

public class GeminiAssistant {

    private static final String API_KEY = "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"; 
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public interface AICallback {
        void onSuccess(String responseText);
        void onError(String errorMessage);
    }

    public void askAI(final String prompt, final AICallback callback) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
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
