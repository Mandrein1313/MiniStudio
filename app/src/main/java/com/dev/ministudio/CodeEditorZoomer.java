package com.dev.ministudio;

import android.content.Context;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class CodeEditorZoomer {

    private final EditText codeEditor;
    private final TextView lineNumbers;
    private final ScaleGestureDetector scaleGestureDetector;
    
    private float currentTextSize = 14f; // ขนาดเริ่มต้น (SP)
    private final float MIN_TEXT_SIZE = 8f;   // ขนาดเล็กสุด
    private final float MAX_TEXT_SIZE = 32f;  // ขนาดใหญ่สุด

    public CodeEditorZoomer(Context context, EditText codeEditor, TextView lineNumbers) {
        this.codeEditor = codeEditor;
        this.lineNumbers = lineNumbers;

        // ดึงขนาดตัวอักษรปัจจุบันจาก EditText แปลงเป็นหน่วย SP
        if (context.getResources() != null && context.getResources().getDisplayMetrics() != null) {
            currentTextSize = codeEditor.getTextSize() / context.getResources().getDisplayMetrics().scaledDensity;
        }

        // สร้างระบบตรวจจับการกาง/หุบนิ้ว (Pinch-to-Zoom)
        this.scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                currentTextSize *= scaleFactor;

                // จำกัดขนาดไม่ให้เล็กหรือใหญ่เกินไป
                if (currentTextSize < MIN_TEXT_SIZE) currentTextSize = MIN_TEXT_SIZE;
                if (currentTextSize > MAX_TEXT_SIZE) currentTextSize = MAX_TEXT_SIZE;

                // ปรับขนาดตัวอักษรของเอดิเตอร์และเลขบรรทัดพร้อมกัน
                CodeEditorZoomer.this.codeEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentTextSize);
                if (CodeEditorZoomer.this.lineNumbers != null) {
                    CodeEditorZoomer.this.lineNumbers.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentTextSize);
                }
                return true;
            }
        });

        // เปิดใช้งานการรับค่าการสัมผัส
        setupTouchListener();
    }

    private void setupTouchListener() {
        codeEditor.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // ส่งเหตุการณ์การสัมผัสไปให้ ScaleGestureDetector คำนวณ
                scaleGestureDetector.onTouchEvent(event);

                // ถ้าใช้ 2 นิ้วขึ้นไป (กำลังซูม) ให้ล็อกหน้าจอไม่ให้เลื่อนสกรอลล์ เพื่อลดการกระตุก
                if (event.getPointerCount() >= 2) {
                    return true;
                }
                
                // ปล่อยให้ทำงานพิมพ์หรือเลื่อนเคอร์เซอร์ปกติเมื่อใช้ 1 นิ้ว
                return false;
            }
        });
    }
}
