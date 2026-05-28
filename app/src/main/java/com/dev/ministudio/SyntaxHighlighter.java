package com.dev.ministudio.editor;

import android.graphics.Color;
import android.text.Editable;
import android.text.style.ForegroundColorSpan;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {

    private boolean isHighlighting = false;

    // การประกาศ Pattern ไว้ข้างนอกแบบนี้ ช่วยให้คอมไพล์ครั้งเดียวตอนเปิดแอป ลดความหน่วงได้มหาศาล
    private static final Pattern PATTERN_JAVA_KEYWORDS = Pattern.compile("\\b(package|class|interface|enum|extends|implements|static|final|void|new|if|else|for|while|do|switch|case|default|try|catch|finally|return|throw|break|continue|this|super)\\b");
    private static final Pattern PATTERN_COMMENT = Pattern.compile("//.*|/\\*(?:.|[\\n\\r])*?\\*/");
    private static final Pattern PATTERN_STRING = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
    private static final Pattern PATTERN_NUMBER = Pattern.compile("\\b\\d+\\.?\\d*[fFL]?\\b");

    private final int COLOR_KEYWORD = Color.parseColor("#FF79C6");
    private final int COLOR_STRING  = Color.parseColor("#F1FA8C");
    private final int COLOR_NUMBER  = Color.parseColor("#BD93F9");
    private final int COLOR_COMMENT = Color.parseColor("#6272A4");

    public void highlight(Editable s, File currentOpenedFile) {
        if (isHighlighting) return;
        isHighlighting = true;

        try {
            // ลบ span เก่าออกทั้งหมด
            ForegroundColorSpan[] spans = s.getSpans(0, s.length(), ForegroundColorSpan.class);
            for (ForegroundColorSpan span : spans) {
                s.removeSpan(span);
            }

            // เพิ่ม logic การทำสีที่นี่ (ตัวอย่างสำหรับ Java)
            highlightPattern(s, PATTERN_JAVA_KEYWORDS, COLOR_KEYWORD);
            highlightPattern(s, PATTERN_COMMENT, COLOR_COMMENT);
            highlightPattern(s, PATTERN_STRING, COLOR_STRING);
            highlightPattern(s, PATTERN_NUMBER, COLOR_NUMBER);

        } finally {
            isHighlighting = false; // ปลดล็อกสถานะ
        }
    }

    private void highlightPattern(Editable s, Pattern pattern, int color) {
        Matcher matcher = pattern.matcher(s);
        while (matcher.find()) {
            s.setSpan(new ForegroundColorSpan(color), matcher.start(), matcher.end(), 0);
        }
    }
}
