package com.dev.ministudio;

import android.content.Context;
import java.util.List;

public class AiBuildDoctor {

    private GeminiAssistant ai;

    public AiBuildDoctor(Context context) {
        ai = new GeminiAssistant(context);
    }

    // วิเคราะห์ Build Error และให้คำแนะนำ
    public String analyzeBuildErrors(List<ParsedError> errors) {
        StringBuilder errorText = new StringBuilder();
        for (ParsedError e : errors) {
            errorText.append(e.getMessage()).append("\n");
        }

        // Prompt สำหรับ AI
        String prompt = "คุณเป็นผู้ช่วยวิเคราะห์ Android Build Error\n" +
                        "นี่คือ Error จากการ Build:\n" +
                        errorText.toString() +
                        "\nวิเคราะห์สาเหตุและให้คำแนะนำวิธีแก้ไขโดยสรุป";

        // เรียก AI
        String response = ai.getResponse(prompt);

        return response;
    }
}
