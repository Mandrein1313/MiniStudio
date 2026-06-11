package com.dev.ministudio;

public class OptimizedResult {
    public String updatedCode;   // โค้ดที่ AI ปรับปรุงแก้ไขให้สะอาดแล้ว
    public String explanation;   // คำอธิบายภาษาไทยว่าทำไมถึงแก้ และแก้จุดไหนไปบ้าง

    public OptimizedResult(String updatedCode, String explanation) {
        this.updatedCode = updatedCode;
        this.explanation = explanation;
    }
}
