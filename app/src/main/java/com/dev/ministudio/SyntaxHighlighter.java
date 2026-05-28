package com.dev.ministudio.editor;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {

    // 🟢 ป้องกัน highlight ซ้อน
    private boolean isHighlighting = false;

    // 🟢 ใช้ Main Thread Handler
    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    // 🟢 จำกัดขนาดไฟล์
    private static final int MAX_HIGHLIGHT_LENGTH = 120000;

    // =========================
    // Patterns
    // =========================

    private static final Pattern PATTERN_JAVA_KEYWORDS =
            Pattern.compile(
                    "\\b(package|import|class|interface|enum|extends|implements|" +
                    "public|private|protected|static|final|void|new|if|else|for|" +
                    "while|do|switch|case|default|try|catch|finally|return|throw|" +
                    "break|continue|this|super|null|true|false)\\b"
            );

    private static final Pattern PATTERN_COMMENT =
            Pattern.compile("//.*|/\\*(.|\\R)*?\\*/");

    private static final Pattern PATTERN_STRING =
            Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");

    private static final Pattern PATTERN_NUMBER =
            Pattern.compile("\\b\\d+\\.?\\d*[fFdDlL]?\\b");

    // =========================
    // Colors
    // =========================

    private final int COLOR_KEYWORD =
            Color.parseColor("#FF79C6");

    private final int COLOR_STRING =
            Color.parseColor("#F1FA8C");

    private final int COLOR_NUMBER =
            Color.parseColor("#BD93F9");

    private final int COLOR_COMMENT =
            Color.parseColor("#6272A4");

    // =========================
    // Main Highlight Method
    // =========================

    public void highlight(Editable editable, File currentOpenedFile) {

        if (editable == null) return;

        // 🟢 กัน highlight ซ้อน
        if (isHighlighting) return;

        // 🟢 กันไฟล์ใหญ่เกินไป
        if (editable.length() > MAX_HIGHLIGHT_LENGTH) {
            return;
        }

        isHighlighting = true;

        try {

            // 🟢 ลบ span เฉพาะ ForegroundColorSpan
            ForegroundColorSpan[] spans =
                    editable.getSpans(
                            0,
                            editable.length(),
                            ForegroundColorSpan.class
                    );

            for (ForegroundColorSpan span : spans) {
                editable.removeSpan(span);
            }

            // 🟢 ทำ Highlight ทีละ Pattern
            applyHighlight(
                    editable,
                    PATTERN_COMMENT,
                    COLOR_COMMENT
            );

            applyHighlight(
                    editable,
                    PATTERN_STRING,
                    COLOR_STRING
            );

            applyHighlight(
                    editable,
                    PATTERN_NUMBER,
                    COLOR_NUMBER
            );

            applyHighlight(
                    editable,
                    PATTERN_JAVA_KEYWORDS,
                    COLOR_KEYWORD
            );

        } catch (Exception e) {
            e.printStackTrace();

        } finally {

            // 🟢 ปลดล็อกแบบ delay นิดนึง
            mainHandler.postDelayed(
                    () -> isHighlighting = false,
                    50
            );
        }
    }

    // =========================
    // Apply Pattern
    // =========================

    private void applyHighlight(
            Editable editable,
            Pattern pattern,
            int color
    ) {

        Matcher matcher =
                pattern.matcher(editable.toString());

        while (matcher.find()) {

            // 🟢 กัน index พัง
            if (matcher.start() < 0 ||
                matcher.end() > editable.length()) {
                continue;
            }

            editable.setSpan(
                    new ForegroundColorSpan(color),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }
}