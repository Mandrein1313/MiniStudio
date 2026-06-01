package com.dev.ministudio;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildSummaryAnalyzer {

    public interface LogOutputListener {
        void onAppendLog(String text, int color);
    }

    // Java Compiler
    private static final Pattern JAVAC_ERROR =
            Pattern.compile("(.*?\\.java):(\\d+):\\s*error:\\s*(.*)",
                    Pattern.CASE_INSENSITIVE);
    // XML / AAPT2
    private static final Pattern XML_ERROR =
            Pattern.compile("(.*?\\.xml):(\\d+):.*?error:\\s*(.*)",
                    Pattern.CASE_INSENSITIVE);
    // Kotlin
    private static final Pattern KOTLIN_ERROR =
            Pattern.compile("(.*?\\.kt):(\\d+):\\s*error:\\s*(.*)",
                    Pattern.CASE_INSENSITIVE);
    private boolean hasError = false;
    private String errorType = "UNKNOWN";
    private String errorDetails = "";
    private ParsedError lastError;
    private final ArrayList<ParsedError> errorList = new ArrayList<>();
    private final int COLOR_ERROR =
            Color.parseColor("#FF8A80");
    private final int COLOR_WARNING =
            Color.parseColor("#FFB74D");
    private final int COLOR_SUCCESS =
            Color.parseColor("#81C784");
    public void clearErrors() {
        errorList.clear();
        lastError = null;
        hasError = false;
        errorType = "UNKNOWN";
        errorDetails = "";
    }
    public ParsedError getLastError() {
        return lastError;
    }
    public ArrayList<ParsedError> getErrorList() {
        return errorList;
    }
    public boolean analyzeLine(
            String line,
            int defaultColor,
            LogOutputListener listener
    ) {
        if (line == null) {
            return false;
        }
        // =========================
        // Java / XML / Kotlin
        // =========================
        if (checkRegexError(line, JAVAC_ERROR, "JAVA_ERROR")
                || checkRegexError(line, XML_ERROR, "XML_AAPT2_ERROR")
                || checkRegexError(line, KOTLIN_ERROR, "KOTLIN_ERROR")) {
            if (listener != null) {
                listener.onAppendLog(line + "\n", defaultColor);
            }

            return true;
        }
        // =========================
        // Column Marker (^)
        // =========================
        if (hasError && lastError != null && line.contains("^")) {

            int colIndex = line.indexOf("^");

            if (colIndex >= 0) {
                lastError.column = colIndex;
            }
        }
        // =========================
        // GitHub Repository Error
        // =========================
        String lowerLine = line.toLowerCase();
        if (lowerLine.contains("repository not found")) {
            hasError = true;
            errorType = "GIT_URL_MISSING";
            errorDetails = "ไม่พบ GitHub Repository";
            return true;
        }
        // =========================
        // Authentication Error
        // =========================
        if (line.contains("Authentication failed")
                || line.contains("401 Unauthorized")
                || line.contains("Bad credentials")
                || line.contains("403 Forbidden")) {
            hasError = true;
            errorType = "AUTH_ERROR";
            errorDetails =
                    "GitHub Token ไม่ถูกต้องหรือไม่มีสิทธิ์เข้าถึง";
            return true;
        }
        // =========================
        // Gradle Error
        // =========================
        if (line.contains("build.gradle' not found")
                || line.contains("Build file")
                || line.contains("settings.gradle")
                || line.contains("settings.gradle.kts")) {
            hasError = true;
            errorType = "GRADLE_STRUCTURE_ERROR";
            errorDetails =
                    "ไม่พบไฟล์ Gradle ที่จำเป็น";
            return true;
        }
        // =========================
        // AAPT2 Resource Error
        // =========================
        if (lowerLine.contains("aapt")
                && lowerLine.contains("error")) {
            hasError = true;
            errorType = "AAPT2_ERROR";
            errorDetails = line;

            if (listener != null) {
                listener.onAppendLog(line + "\n", COLOR_ERROR);
            }
            return true;
        }
        // =========================
        // Generic Error
        // =========================
        if (lowerLine.contains(" error ")
                || lowerLine.startsWith("error:")
                || lowerLine.contains("failed")) {
            hasError = true;
            if (errorType.equals("UNKNOWN")) {
                errorType = "GENERIC_ERROR";
                errorDetails = line;
            }
        }
        if (listener != null) {
            listener.onAppendLog(line + "\n", defaultColor);
        }
        return false;
    }
    private boolean checkRegexError(
            String line,
            Pattern pattern,
            String typeStr
    ) {
        Matcher m = pattern.matcher(line);
        if (!m.find()) {
            return false;
        }
        try {
            String file = m.group(1);

            int lineNumber =
                    Integer.parseInt(m.group(2));

            String message = m.group(3);

            lastError = new ParsedError(
                    file,
                    lineNumber,
                    0,
                    typeStr,
                    message
            );

            errorList.add(lastError);

            hasError = true;
            errorType = typeStr;
            errorDetails = message;

            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public void printSummary(
            LogOutputListener listener
    ) {

        if (!hasError || listener == null) {
            return;
        }

        listener.onAppendLog(
                "\n======================================\n",
                COLOR_ERROR
        );

        listener.onAppendLog(
                "🔍 วิเคราะห์สาเหตุการบิวด์ล้มเหลว\n",
                COLOR_ERROR
        );

        if (lastError != null) {

            listener.onAppendLog(
                    "📍 ไฟล์: "
                            + lastError.file
                            + "\n",
                    COLOR_ERROR
            );

            listener.onAppendLog(
                    "📍 บรรทัด: "
                            + lastError.line
                            + "\n",
                    COLOR_ERROR
            );
        }

        listener.onAppendLog(
                "📌 ประเภท: "
                        + errorType
                        + "\n",
                COLOR_WARNING
        );

        listener.onAppendLog(
                "💬 รายละเอียด: "
                        + errorDetails
                        + "\n",
                COLOR_WARNING
        );

        listener.onAppendLog(
                "======================================\n",
                COLOR_ERROR
        );
    }
}