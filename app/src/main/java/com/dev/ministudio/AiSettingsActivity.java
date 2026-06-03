package com.dev.ministudio;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AiSettingsActivity extends AppCompatActivity {

    private EditText apiKeyInput;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_settings);

        apiKeyInput = findViewById(R.id.editTextApiKey);
        saveButton = findViewById(R.id.buttonSaveApiKey);

        // โหลดค่าเดิมจาก SharedPreferences
        SharedPreferences prefs = getSharedPreferences("ai_settings", MODE_PRIVATE);
        String apiKey = prefs.getString("groq_api_key", "");
        apiKeyInput.setText(apiKey);

        saveButton.setOnClickListener(v -> {
            String key = apiKeyInput.getText().toString().trim();
            if (key.isEmpty()) {
                Toast.makeText(this, "กรุณากรอก API Key", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit().putString("groq_api_key", key).apply();
            Toast.makeText(this, "บันทึก API Key เรียบร้อยแล้ว", Toast.LENGTH_SHORT).show();
            finish(); // ปิดหน้า Settings
        });
    }
}