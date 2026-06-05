package com.dev.ministudio;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue; 
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView; 
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.FrameLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;

import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.langs.java.JavaLanguage;
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula;
import io.github.rosemoe.sora.event.ContentChangeEvent;

import com.dev.ministudio.fs.FileSystemManager;
import com.dev.ministudio.model.ProjectModel;
import com.dev.ministudio.model.FileNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import android.text.SpannableString;
import android.content.Intent;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;


public class MainActivity extends AppCompatActivity {

    // Views
    private TextView tvSaveStatus, tvFilePath;
    private CodeEditor codeEditor; 
    private DrawerLayout drawerLayout;
    private ListView treeView; 
    private LinearLayout searchBar;
    private android.widget.EditText etFind, etReplace; 
    
    // Tab System Views
    private RecyclerView tabRecyclerView;
    private TabAdapter tabAdapter;

    // 🌟 ระบบ Dialog เต็มหน้าจอชุดใหม่ (Full-screen Panel)
    private android.app.Dialog fullPanelDialog;
    private TabLayout dialogTabLayout;
    private ViewPager2 dialogViewPager;
    private PanelPagerAdapter dialogPanelAdapter;
    
    private TextView tvConsole;
        
    // Controllers & Models
    private ProjectModel currentProject;

    // Utils
    private final Handler autoSaveHandler = new Handler(); 
    private Runnable saveRunnable;
    private int lastSearchIndex = 0;
    
    private float currentCodeFontSize = 14.0f; 

    // 🛠️ แยกออกไปจัดการที่ระบบภายนอกคลาสหลัก
    private ProjectTreeManager projectTreeManager;

    private BuildEnvironmentManager buildEnvManager;
    private static final int PICK_FILE_REQUEST_CODE = 2026; 
    
    private ProjectDialogManager dialogManager;
    
    // 🤖 ตัวจัดการวิเคราะห์เลย์เอาต์ระดับสูงเพื่อความเสถียร
    private com.dev.ministudio.AiLayoutAnalyzer aiLayoutAnalyzer; 
    
    private RecyclerView rvErrorPanel;
    
    // 🌟 ระบบ XML Preview
    private FrameLayout previewContainer;
    private boolean isPreviewMode = false; 
    private String chatHistory = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#1E1E1E"));
        setContentView(R.layout.activity_main);
        
        buildEnvManager = new BuildEnvironmentManager(this);
        
        initViews();
        setupLogic();
    }

    private void initViews() {
        etFind = findViewById(R.id.etFind);
        etReplace = findViewById(R.id.etReplace);
        searchBar = findViewById(R.id.searchBar);
        codeEditor = findViewById(R.id.codeEditor); 
        tvFilePath = findViewById(R.id.tvFilePath); 
        tvSaveStatus = findViewById(R.id.tvSaveStatus);
        
        treeView = findViewById(R.id.treeView); 
        tabRecyclerView = findViewById(R.id.tabRecyclerView);
        tabRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, android.R.string.ok, android.R.string.cancel);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        findViewById(R.id.btnNext).setOnClickListener(v -> findAndHighlight());
        findViewById(R.id.btnReplace).setOnClickListener(v -> replaceText());
        
        setupShortcutBar();

        rvErrorPanel = findViewById(R.id.rvErrorPanel);
        if (rvErrorPanel != null) {
            rvErrorPanel.setLayoutManager(new LinearLayoutManager(this));
        }

        previewContainer = findViewById(R.id.previewContainer);
    }

    private void setupLogic() {
        aiLayoutAnalyzer = new com.dev.ministudio.AiLayoutAnalyzer(this);
        dialogManager = new ProjectDialogManager(this, parentNode -> {
            triggerTreeRefresh(parentNode);
        });

        codeEditor.setEditorLanguage(new JavaLanguage()); 
        codeEditor.setColorScheme(new SchemeDarcula()); 
        codeEditor.setTextSize(currentCodeFontSize); 
        codeEditor.setTypefaceText(android.graphics.Typeface.MONOSPACE); 
        codeEditor.setLineSpacing(2f, 1.2f); 
        codeEditor.setWordwrap(false); 
        codeEditor.setUndoEnabled(true); 
        codeEditor.setHighlightCurrentBlock(true); 

        codeEditor.subscribeEvent(ContentChangeEvent.class, (event, unsubscribe) -> {
            tvSaveStatus.setText("Editing...");
            tvSaveStatus.setTextColor(android.graphics.Color.parseColor("#FFB74D"));

            autoSaveHandler.removeCallbacks(saveRunnable);
            saveRunnable = () -> {
                saveFile();
                tvSaveStatus.setText("Saved");
                tvSaveStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
            };
            autoSaveHandler.postDelayed(saveRunnable, 1500);
        });

        String projectName = getIntent().getStringExtra("projectName");
        if (projectName != null) {
            String rootPath = "/sdcard/MiniStudio/" + projectName;
            currentProject = new ProjectModel(projectName, rootPath);
            getSupportActionBar().setTitle(currentProject.getProjectName());
            
            setupTabLogic();
            
            // 🛠️ เรียกทำงานผ่านโครงสร้างผู้จัดการต้นไม้ตัวใหม่ที่แยกออกไป
            projectTreeManager = new ProjectTreeManager(this, treeView);
            projectTreeManager.initializeFileTree();
        }
    }

    // 🌟 ฟังก์ชันเปิดหน้าต่าง Dialog คอนโซลแบบเต็มหน้าจอ (เวอร์ชันแก้ไขให้เห็น Status Bar)
    private void showFullPanelDialog(int initialTabPosition) {
        if (fullPanelDialog != null && fullPanelDialog.isShowing()) {
            dialogViewPager.setCurrentItem(initialTabPosition, true);
            return;
        }

        fullPanelDialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar);
        fullPanelDialog.setContentView(R.layout.dialog_full_console_panel);
        fullPanelDialog.setCancelable(true);

        if (fullPanelDialog.getWindow() != null) {
            fullPanelDialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            );
            fullPanelDialog.getWindow().setStatusBarColor(android.graphics.Color.parseColor("#1E1E1E"));
        }

        dialogTabLayout = fullPanelDialog.findViewById(R.id.tabLayout);
        dialogViewPager = fullPanelDialog.findViewById(R.id.viewPager);
        
        fullPanelDialog.findViewById(R.id.btnCloseConsole).setOnClickListener(v -> fullPanelDialog.dismiss());
        
        View btnToggleExpand = fullPanelDialog.findViewById(R.id.btnToggleExpand);
        if (btnToggleExpand != null) btnToggleExpand.setVisibility(View.GONE);

        fullPanelDialog.findViewById(R.id.btnClearConsole).setOnClickListener(v -> {
            if (dialogPanelAdapter != null) {
                TextView consoleView = dialogPanelAdapter.getTvConsole();
                android.webkit.WebView webView = dialogPanelAdapter.getWebAiOutput();
                
                if (consoleView != null) consoleView.setText("");
                if (webView != null) {
                    chatHistory = ""; 
                    webView.loadDataWithBaseURL(null, "<html><body style='background-color:#1E1E1E;'></body></html>", "text/html", "utf-8", null);
                }
            }
            if (tvConsole != null) tvConsole.setText("");
        });

        dialogPanelAdapter = new PanelPagerAdapter(this);
        dialogViewPager.setAdapter(dialogPanelAdapter);
        dialogViewPager.setUserInputEnabled(false); 

        new TabLayoutMediator(dialogTabLayout, dialogViewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Console" : "AI");
        }).attach();

        dialogViewPager.post(() -> {
            if (dialogPanelAdapter != null) {
                tvConsole = dialogPanelAdapter.getTvConsole();
                dialogViewPager.setCurrentItem(initialTabPosition, false);
            }
        });

        fullPanelDialog.show();
    }

    public void handleAiQuery() {
        if (fullPanelDialog == null || !fullPanelDialog.isShowing()) {
            showFullPanelDialog(1);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (dialogPanelAdapter == null) return;

            android.widget.EditText etAiInput = dialogPanelAdapter.getEtAiInput();
            android.webkit.WebView webAiOutput = dialogPanelAdapter.getWebAiOutput();

            if (etAiInput == null || webAiOutput == null) return;

            String userQuestion = etAiInput.getText().toString().trim();
            if (userQuestion.isEmpty()) {
                chatHistory += "\n\n⚠️ *กรุณาพิมพ์คำถามก่อนครับ*";
                String html = AiHtmlFormatter.convertMarkdownToHtml(chatHistory);
                webAiOutput.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
                return;
            }

            dialogViewPager.setCurrentItem(1, true);

            chatHistory += "\n\n👤 **คุณ:** " + userQuestion;
            String htmlUser = AiHtmlFormatter.convertMarkdownToHtml(chatHistory);
            webAiOutput.loadDataWithBaseURL(null, htmlUser, "text/html", "utf-8", null);

            String fullPrompt = chatHistory + "\nผู้ใช้ถาม: " + userQuestion;

            aiLayoutAnalyzer.askAi(fullPrompt, new AiLayoutAnalyzer.OnAnalysisListener() {
                @Override
                public void onStart() {
                    runOnUiThread(() -> {
                        try {
                            android.webkit.WebView currentWeb = dialogPanelAdapter.getWebAiOutput();
                            if (currentWeb != null) {
                                String tempHtml = AiHtmlFormatter.convertMarkdownToHtml(chatHistory + "\n\n🤖 *AI กำลังคิด...*");
                                currentWeb.loadDataWithBaseURL(null, tempHtml, "text/html", "utf-8", null);
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                }

                @Override
                public void onSuccess(android.text.SpannableString formattedResult) {
                    runOnUiThread(() -> {
                        try {
                            android.webkit.WebView currentWeb = dialogPanelAdapter.getWebAiOutput();
                            chatHistory += "\n\n🤖 **AI:** " + formattedResult.toString();
                            
                            if (currentWeb != null) {
                                String htmlResult = AiHtmlFormatter.convertMarkdownToHtml(chatHistory);
                                currentWeb.loadDataWithBaseURL(null, htmlResult, "text/html", "utf-8", null);
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        try {
                            android.webkit.WebView currentWeb = dialogPanelAdapter.getWebAiOutput();
                            chatHistory += "\n\n❌ **AI เกิดข้อผิดพลาด:** " + errorMessage;
                            
                            if (currentWeb != null) {
                                String htmlError = AiHtmlFormatter.convertMarkdownToHtml(chatHistory);
                                currentWeb.loadDataWithBaseURL(null, htmlError, "text/html", "utf-8", null);
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                }
            });

            etAiInput.setText("");
        }, 300);
    }

    private void toggleXmlPreview() {
        if (codeEditor == null || previewContainer == null) {
            showToast("⚠️ ไม่พบแผงควบคุมระบบพรีวิวในหน้าจอนี้");
            return;
        }

        if (!isPreviewMode) {
            try {
                String currentXmlCode = codeEditor.getText().toString();
                XmlPreviewManager previewManager = new XmlPreviewManager(MainActivity.this);
                View generatedView = previewManager.inflateXml(currentXmlCode);

                if (generatedView != null) {
                    previewContainer.removeAllViews();
                    previewContainer.addView(generatedView);

                    codeEditor.setVisibility(View.GONE);
                    previewContainer.setVisibility(View.VISIBLE);
                    
                    isPreviewMode = true;
                    showToast("✨ แสดงผลพรีวิวเลย์เอาต์สำเร็จ!");
                    invalidateOptionsMenu(); 
                }
            } catch (Exception e) {
                showToast("❌ ไวยากรณ์ XML ขัดข้อง: " + e.getMessage());
            }
        } else {
            previewContainer.setVisibility(View.GONE);
            codeEditor.setVisibility(View.VISIBLE);
            isPreviewMode = false;
            invalidateOptionsMenu();
        }
    }

    private void startCloudBuildPipeline() {
        if (currentProject == null) {
            showToast("กรุณาเปิดโปรเจกต์ก่อนทำการรัน");
            return;
        }

        SharedPreferences prefs = getSharedPreferences("GitHubPrefs", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "");
        String savedToken = prefs.getString("token", "");

        if (username.isEmpty() || savedToken.isEmpty()) {
            showToast("❌ ยังไม่ได้ตั้งค่าบัญชี GitHub กรุณาตั้งค่าที่ปุ่มฟันเพืองหน้าแรกก่อนครับ");
            return;
        }

        saveFile(); 
        showFullPanelDialog(0);

        final BuildSummaryAnalyzer analyzer = new BuildSummaryAnalyzer();
        analyzer.clearErrors(); 
        
        final boolean[] isPipelineStopped = {false};

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (dialogPanelAdapter != null) {
                tvConsole = dialogPanelAdapter.getTvConsole();
            }
            if (tvConsole != null) tvConsole.setText("");

            appendLog("##[group]เริ่มขั้นตอนการตั้งค่า & ตรวจสอบโปรเจกต์เบื้องต้น", TerminalColor.LOG_GRAY); 
            appendLog("🔔 [กำลังจัดเตรียมสภาพแวดล้อม...] เริ่มทำงานระบบ Workflow สำเร็จ", TerminalColor.LOG_WHITE);
            appendLog("📂 ที่อยู่โปรเจกต์ (Root Path): " + currentProject.getRootPath(), TerminalColor.BORDER_BLUE); 
            appendLog("##[endgroup]", TerminalColor.LOG_GRAY);

            BuildTaskManager buildTask = new BuildTaskManager(
                MainActivity.this, 
                currentProject.getRootPath(),
                new BuildTaskManager.BuildListener() {
                    
                    @Override 
                    public void onLogAppend(final String text, final int color) { 
                        if (isPipelineStopped[0]) return;

                        String lowerText = text != null ? text.toLowerCase() : "";
                        boolean isErrorLine = lowerText.contains("error:") || lowerText.contains("failed:") || color == Color.RED;

                        boolean hasFailed = analyzer.analyzeLine(text, color, new BuildSummaryAnalyzer.LogOutputListener() {
                            @Override
                            public void onAppendLog(String logText, int logColor) {
                                appendLog(logText, logColor); 
                            }
                        });

                        if (hasFailed) {
                            isPipelineStopped[0] = true;
                            showToast("💥 บิวด์ล้มเหลว! (Exit Code 1)");
                            return;
                        }

                        if (text != null && (text.startsWith("📍") || text.startsWith("💬"))) {
                            return;
                        }

                        if (color == Color.GREEN || lowerText.contains("success")) {
                            appendLog(text, TerminalColor.SUGGEST_GREEN); 
                        } else if (color == Color.YELLOW) {
                            appendLog(text, TerminalColor.TARGET_YELLOW); 
                        } else if (color == Color.CYAN) {
                            appendLog(text, TerminalColor.LOG_CYAN); 
                        } else if (isErrorLine) {
                            appendLog(text, TerminalColor.DETAIL_RED); 
                        } else {
                            appendLog(text, TerminalColor.TEXT_WHITE); 
                        }
                    }

                    @Override 
                    public void onBuildStarted() { 
                        showToast("กำลังเริ่มระบบ Cloud Workflow... 🐙"); 
                        appendLog("\n##[group]🚀 เรียกทำงานคำสั่ง: compileJava", TerminalColor.LOG_GRAY);
                        appendLog("🔄 กำลังเชื่อมต่อไปยังเซิร์ฟเวอร์คอมไพล์บนคลาวด์...", TerminalColor.LOG_WHITE);
                    }

                    @Override
                    public void onBuildFinished(boolean success, String apkPath) {
                        if (isPipelineStopped[0]) return;

                        appendLog("##[endgroup]", TerminalColor.LOG_GRAY);

                        if (success) {
                            showToast("บิวด์แอปสำเร็จ! 🎉");
                            appendLog("\n##[group]🎉 งานหลังบิวด์: จัดเก็บไฟล์ระบบแอปพลิเคชัน", TerminalColor.SUGGEST_GREEN);
                            appendLog("✅ สำเร็จ: กระบวนการทำงานทั้งหมดเสร็จสิ้นโดยไม่มีข้อผิดพลาด", TerminalColor.SUGGEST_GREEN);
                            appendLog("📦 ไฟล์แอปที่ได้ (APK): " + (apkPath != null ? apkPath : "outputs/apk/debug/app-debug.apk"), TerminalColor.LOG_CYAN);
                            appendLog("##[endgroup]", TerminalColor.SUGGEST_GREEN);
                            
                            runOnUiThread(() -> { if (rvErrorPanel != null) rvErrorPanel.setVisibility(View.GONE); });
                        } else {
                            showToast("กระบวนการทำงานล้มเหลว");
                            appendLog("\n##[error] การทำงานหยุดช้าลงเนื่องจากการปิดตัวของระบบบิวด์อย่างกะทันหัน", TerminalColor.ERROR_RED);
                            
                            if (analyzer != null) {
                                analyzer.printSummary(new BuildSummaryAnalyzer.LogOutputListener() {
                                    @Override
                                    public void onAppendLog(String text, int color) {
                                        if (dialogPanelAdapter != null) tvConsole = dialogPanelAdapter.getTvConsole();
                                        appendColoredText(tvConsole, text, color);
                                    }
                                });
                            }
                            
                            final ParsedError err = analyzer.getLastError();
                            if (err != null) {
                                runOnUiThread(() -> {
                                    executeJumpToError(err);
                                });
                            }
                        }
                    }
                }
            );

            String githubToken = savedToken; 
            String projectName = currentProject.getProjectName();
            String repoUrl = "https://github.com/" + username + "/" + projectName + ".git";
            String packageName = "com.dev.ministudio"; 

            buildTask.startCloudBuild(githubToken, repoUrl, projectName, packageName); 
            buildTask.setAnalyzer(analyzer);
        }, 300);
    }

    private void executeJumpToError(final ParsedError errorItem) {
        if (errorItem == null || currentProject == null) return;

        try {
            java.io.File targetFile = new java.io.File(errorItem.file);
            if (!targetFile.isAbsolute()) {
                targetFile = new java.io.File(currentProject.getRootPath(), errorItem.file);
            }

            if (targetFile.exists()) {
                openFile(targetFile); 
                
                if (codeEditor != null) {
                    final int zeroBasedLine = Math.max(0, errorItem.line - 1); 
                    final int targetColumn = Math.max(0, errorItem.column);

                    codeEditor.postDelayed(() -> {
                        try {
                            if (codeEditor.getSearcher() != null) {
                                codeEditor.getSearcher().stopSearch();
                            }
                            codeEditor.jumpToLine(zeroBasedLine);            
                            codeEditor.setSelection(zeroBasedLine, targetColumn);
                            codeEditor.setSelectionRegion(zeroBasedLine, targetColumn, zeroBasedLine, targetColumn + 4);
                            
                            if (rvErrorPanel != null) {
                                rvErrorPanel.setVisibility(View.VISIBLE);
                            }
                            showToast("🚨 วาร์ปล็อกเป้าหมายพังในบรรทัดที่ " + errorItem.line + " สำเร็จครับ!");
                        } catch (Exception layoutEx) {
                            layoutEx.printStackTrace();
                        }
                    }, 200); 
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🛠️ เมทอดเก่าชุดใหญ่ถูกย้ายออกจากไฟล์นี้แล้ว โดยส่งผ่านตัวกระจายคำสั่งแทน
    public void openFilePicker() {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); 
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        startActivityForResult(android.content.Intent.createChooser(intent, "เลือกไฟล์ที่จะนำเข้า"), PICK_FILE_REQUEST_CODE);
    }

    public void openFile(File file) {
        if (projectTreeManager != null) {
            projectTreeManager.openFile(file);
        }
    }

    public void saveFile() {
        if (projectTreeManager != null) {
            projectTreeManager.saveFile();
        }
    }

    private void appendLog(final String text, final int color) {
        runOnUiThread(() -> {
            if (dialogPanelAdapter != null) {
                tvConsole = dialogPanelAdapter.getTvConsole();
            }
            if (tvConsole != null) {
                appendColoredText(tvConsole, text + "\n", color);
            }
        });
    }

    private void setupShortcutBar() { 
        LinearLayout shortcutBar = findViewById(R.id.shortcutBar); 
        if (shortcutBar == null) return;
        shortcutBar.removeAllViews();

        String[] shortcuts = {
            "↩", "↪", "{", "}", "[", "]", "(", ")", "<", ">", ";"
        };

        float density = getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, (int) (36 * density)
        );
        params.setMargins((int)(3 * density), (int)(2 * density), (int)(3 * density), (int)(2 * density));

        for (final String shortcut : shortcuts) {
            TextView btn = new TextView(this);
            btn.setText(shortcut);
            btn.setTextSize(15); 
            btn.setGravity(Gravity.CENTER);
            btn.setPadding((int)(10 * density), 0, (int)(10 * density), 0);
            btn.setTextColor(Color.parseColor("#B0B3B8")); 

            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(6 * density); 
            shape.setColor(Color.parseColor("#2D2D2D")); 
            btn.setBackground(shape);
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> {
                if (codeEditor.getCursor() != null) {
                    int line = codeEditor.getCursor().getLeftLine();
                    int column = codeEditor.getCursor().getLeftColumn();
                    codeEditor.getText().insert(line, column, shortcut);
                }
            });
            shortcutBar.addView(btn);
        }

        TextView btnAskAI = new TextView(this);
        btnAskAI.setText("🤖 ถาม AI");
        btnAskAI.setTextSize(14);
        btnAskAI.setGravity(Gravity.CENTER);
        btnAskAI.setPadding((int)(10 * density), 0, (int)(10 * density), 0);
        btnAskAI.setTextColor(Color.parseColor("#BB86FC")); 

        GradientDrawable aiShape = new GradientDrawable();
        aiShape.setCornerRadius(6 * density);
        aiShape.setColor(Color.parseColor("#251F35")); 
        btnAskAI.setBackground(aiShape);
        btnAskAI.setLayoutParams(params);

        btnAskAI.setOnClickListener(v -> {
            if (codeEditor == null || currentProject == null) return;

            showFullPanelDialog(1); 

            java.io.File currentFile = currentProject.getCurrentOpenFile();
            final String fileName = (currentFile != null) ? currentFile.getName() : "UnknownFile.java";
            final String currentCode = codeEditor.getText().toString();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                aiLayoutAnalyzer.analyzeCode(fileName, currentCode, new AiLayoutAnalyzer.OnAnalysisListener() {
                    @Override
                    public void onStart() {
                        runOnUiThread(() -> {
                            try {
                                android.webkit.WebView currentWeb = dialogPanelAdapter.getWebAiOutput();
                                if (currentWeb != null) {
                                    String tempHtml = AiHtmlFormatter.convertMarkdownToHtml("🤖 *MiniStudio AI กำลังวิเคราะห์โค้ด...*");
                                    currentWeb.loadDataWithBaseURL(null, tempHtml, "text/html", "utf-8", null);
                                }
                            } catch (Exception e) { e.printStackTrace(); }
                        });
                    }

                    @Override
                    public void onSuccess(final android.text.SpannableString formattedResult) {
                        runOnUiThread(() -> {
                            try {
                                android.webkit.WebView currentWeb = dialogPanelAdapter.getWebAiOutput();
                                chatHistory += "\n\n🤖 **ผลวิเคราะห์โค้ด (" + fileName + "):**\n" + formattedResult.toString();
                                if (currentWeb != null) {
                                    String htmlResult = AiHtmlFormatter.convertMarkdownToHtml(chatHistory);
                                    currentWeb.loadDataWithBaseURL(null, htmlResult, "text/html", "utf-8", null);
                                }
                            } catch (Exception e) { e.printStackTrace(); }
                        });
                    }

                    @Override
                    public void onError(final String errorMessage) {
                        runOnUiThread(() -> {
                            try {
                                android.webkit.WebView currentWeb = dialogPanelAdapter.getWebAiOutput();
                                chatHistory += "\n\n❌ **AI เกิดข้อผิดพลาดในการวิเคราะห์:** " + errorMessage;
                                if (currentWeb != null) {
                                    String htmlError = AiHtmlFormatter.convertMarkdownToHtml(chatHistory);
                                    currentWeb.loadDataWithBaseURL(null, htmlError, "text/html", "utf-8", null);
                                }
                            } catch (Exception e) { e.printStackTrace(); }
                        });
                    }
                });
            }, 500); 
        });

        shortcutBar.addView(btnAskAI); 
    }

    private void findAndHighlight() {
        String query = etFind.getText().toString();
        String content = codeEditor.getText().toString();
        if (query.isEmpty()) return;

        int index = content.indexOf(query, lastSearchIndex);
        if (index == -1) { index = content.indexOf(query, 0); lastSearchIndex = 0; }

        if (index != -1) {
            soraSelectLinear(index, index + query.length());
            lastSearchIndex = index + query.length();
        } else {
            showToast("Not found");
        }
    }

    private void soraSelectLinear(int startIdx, int endIdx) {
        try {
            String text = codeEditor.getText().toString();
            int startLine = 0, startCol = 0, endLine = 0, endCol = 0, currentIdx = 0;
            String[] lines = text.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                int lineLen = lines[i].length() + 1; 
                if (currentIdx + lineLen > startIdx && startLine == 0 && startCol == 0) {
                    startLine = i; startCol = startIdx - currentIdx;
                }
                if (currentIdx + lineLen > endIdx) {
                    endLine = i; endCol = endIdx - currentIdx; break;
                }
                currentIdx += lineLen;
            }
            final int sL = startLine; final int sC = startCol;
            final int eL = endLine; final int eC = endCol;
            runOnUiThread(() -> {
                codeEditor.setSelectionRegion(sL, sC, eL, eC);
                codeEditor.jumpToLine(sL); 
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void replaceText() {
        String target = etFind.getText().toString();
        String replacement = etReplace.getText().toString();
        if (target.isEmpty()) return;
        String content = codeEditor.getText().toString();
        codeEditor.setText(content.replaceFirst(java.util.regex.Pattern.quote(target), replacement));
        showToast("Replaced");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        MenuItem previewItem = menu.findItem(R.id.action_preview);
        if (previewItem != null) {
            previewItem.setTitle(isPreviewMode ? "ดูโค้ด (Code)" : "ดูตัวอย่าง (Preview)");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_build) { startCloudBuildPipeline(); return true; }
        if (id == R.id.action_preview) { toggleXmlPreview(); return true; }
        
        if (id == R.id.action_ai_settings) {
            startActivity(new Intent(this, AiSettingsActivity.class));
            return true;
        }
        
        if (id == R.id.action_search) {
            searchBar.setVisibility(searchBar.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void triggerTreeRefresh(FileNode parentNode) { 
        if (projectTreeManager != null) projectTreeManager.refreshFileTree(); 
    }

    private void setupTabLogic() {
        tabAdapter = new TabAdapter(currentProject, new TabAdapter.OnTabInterface() {
            @Override public void onTabClick(File file) { openFile(file); }
            @Override public void onTabClose(File file, int position) {}
        });
        tabRecyclerView.setAdapter(tabAdapter);
    }

    public void updateFilePathStatus(File file) {
        if (tvFilePath != null && file != null) tvFilePath.setText(file.getName());
    }

    public void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }
    
    private void appendColoredText(TextView tv, String text, int color) {
        if (tv == null) return;
        android.text.SpannableString spannable = new android.text.SpannableString(text);
        spannable.setSpan(new android.text.style.ForegroundColorSpan(color), 0, text.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.append(spannable);
        autoScrollTabContainer(tv);
    }

    private void autoScrollTabContainer(View innerTextView) {
        if (innerTextView == null) return;
        innerTextView.post(() -> {
            try {
                android.view.ViewParent currentParent = innerTextView.getParent();
                while (currentParent != null) {
                    if (currentParent instanceof ScrollView) {
                        ((ScrollView) currentParent).fullScroll(android.view.View.FOCUS_DOWN);
                        break;
                    }
                    currentParent = currentParent.getParent();
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    // 🌟 ระบบเปิดช่องทางการเชื่อมโยงข้อมูล (Getters สำหรับเรียกจากภายนอก)
    public ProjectModel getCurrentProject() { return currentProject; }
    public ProjectDialogManager getDialogManager() { return dialogManager; }
    public DrawerLayout getDrawerLayout() { return drawerLayout; }
    public CodeEditor getCodeEditor() { return codeEditor; }
    public TabAdapter getTabAdapter() { return tabAdapter; }
    public Handler getAutoSaveHandler() { return autoSaveHandler; }
    public Runnable getSaveRunnable() { return saveRunnable; }
    public PanelPagerAdapter getDialogPanelAdapter() { return dialogPanelAdapter; }

    public static class MenuOption {
        public String title;
        public int iconRes;
        public MenuOption(String title, int iconRes) {
            this.title = title;
            this.iconRes = iconRes;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aiLayoutAnalyzer != null) aiLayoutAnalyzer.shutdown();
    }
}
