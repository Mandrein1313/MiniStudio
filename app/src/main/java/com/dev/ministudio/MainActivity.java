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

    // Views สำหรับระบบ Bottom Console Panel
    private LinearLayout consolePanel;
    private ScrollView consoleScrollView;
    private boolean isConsoleMaximized = false; 
        
    // Controllers & Models
    private ProjectModel currentProject;

    // Utils
    private final Handler autoSaveHandler = new Handler(); 
    private Runnable saveRunnable;
    private int lastSearchIndex = 0;
    
    private float currentCodeFontSize = 14.0f; 

    private List<FileNode> masterFileList = new ArrayList<>();
    private FileTreeAdapter fileTreeAdapter;

    private BuildEnvironmentManager buildEnvManager;
    private File folderForImport = null;
    private int lastClickedPosition = -1; 
    private static final int PICK_FILE_REQUEST_CODE = 2026; 
    
    private ProjectDialogManager dialogManager;
    
    // 🤖 สลับมาเรียกใช้ตัวจัดการวิเคราะห์เลย์เอาต์ระดับสูงเพื่อความเสถียรและแก้ Code 400
    private com.dev.ministudio.AiLayoutAnalyzer aiLayoutAnalyzer; 
    
    private RecyclerView rvErrorPanel;
    
    // 🌟 ระบบ XML Preview กล่องและตัวแปรควบคุมสถานะ
    private FrameLayout previewContainer;
    private boolean isPreviewMode = false; 
    private String chatHistory = "";
    
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private PanelPagerAdapter panelAdapter;

    // คีย์เวิร์ดตัวควบคุมสลับหน้าจอตามดีไซน์ใหม่
    private TextView tvConsole;
    private TextView tvAiOutput;


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
        tvSaveStatus = findViewById(R.id.tvSaveStatus);
        tvFilePath = findViewById(R.id.tvFilePath); 
        
        // 🌟 1. ผูก ID ระบบสลับหน้าจอตามแบบใหม่
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        
        treeView = findViewById(R.id.treeView); 
        tabRecyclerView = findViewById(R.id.tabRecyclerView);
        
        tabRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        
        consolePanel = findViewById(R.id.consolePanel);
        consoleScrollView = findViewById(R.id.consoleScrollView);

        // 🌟 2. เปลี่ยนปุ่มล้างข้อมูล ให้ล้างทั้งสองแท็บไปเลยครับน้า
        findViewById(R.id.btnClearConsole).setOnClickListener(v -> {
            if (tvConsole != null) tvConsole.setText("");
            if (tvAiOutput != null) tvAiOutput.setText("");
        });
        
        findViewById(R.id.btnCloseConsole).setOnClickListener(v -> consolePanel.setVisibility(View.GONE));

        android.widget.ImageButton btnToggleExpand = findViewById(R.id.btnToggleExpand);
        if (btnToggleExpand != null) {
            btnToggleExpand.setOnClickListener(v -> {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) consolePanel.getLayoutParams();
                
                if (!isConsoleMaximized) {
                    params.height = LinearLayout.LayoutParams.MATCH_PARENT;
                    btnToggleExpand.setImageResource(android.R.drawable.ic_menu_delete); 
                    btnToggleExpand.setColorFilter(Color.parseColor("#FF5252")); 
                    isConsoleMaximized = true;
                } else {
                    int heightInDp = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 160, getResources().getDisplayMetrics()
                    );
                    params.height = heightInDp;
                    btnToggleExpand.setImageResource(android.R.drawable.ic_menu_compass); 
                    btnToggleExpand.setColorFilter(Color.parseColor("#FFB74D")); 
                    isConsoleMaximized = false;
                }
                consolePanel.setLayoutParams(params);
            });
        }

        findViewById(R.id.btnConsoleRun).setOnClickListener(v -> {
            startCloudBuildPipeline();
        });

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

        // --- 🌟 3. ปรับแต่งระบบส่งคำถามและแสดงผลลงแถบ AI แถบแยกใหม่โดยเฉพาะ ---
        android.widget.Button btnSendToAi = findViewById(R.id.btnSendToAi);
        android.widget.EditText etAiInput = findViewById(R.id.etAiInput);
        
        if (btnSendToAi != null && etAiInput != null) {
            btnSendToAi.setOnClickListener(v -> {
                String userQuestion = etAiInput.getText().toString();
                if (userQuestion.isEmpty()) {
                    if (tvAiOutput != null) {
                        tvAiOutput.append("\n⚠️ กรุณาพิมพ์คำถามก่อนครับ");
                        viewPager.setCurrentItem(1, true); // สลับไปแท็บ AI เพื่อโชว์คำเตือน
                    }
                    return;
                }
                
                // สลับหน้าจอรูดไปแท็บ AI ทันทีที่กดส่งคำถามครับน้า 🌟
                viewPager.setCurrentItem(1, true);
                
                if (tvAiOutput != null) {
                    tvAiOutput.append("\n\n👤 คุณ: " + userQuestion);
                }
                
                String fullPrompt = chatHistory + "\nผู้ใช้ถาม: " + userQuestion;
                
                aiLayoutAnalyzer.askAi(fullPrompt, new AiLayoutAnalyzer.OnAnalysisListener() {
                    @Override
                    public void onStart() {
                        if (tvAiOutput != null) {
                            tvAiOutput.append("\n🤖 AI กำลังคิด...");
                        }
                    }
                    
                    @Override
                    public void onSuccess(android.text.SpannableString formattedResult) {
                        if (tvAiOutput != null) {
                            tvAiOutput.append("\n🤖 AI: ");
                            tvAiOutput.append(formattedResult);
                        }
                        
                        chatHistory += "\nผู้ใช้: " + userQuestion + "\nAI: " + formattedResult.toString();
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        if (tvAiOutput != null) {
                            tvAiOutput.append("\n❌ AI ตอบไม่ได้: " + errorMessage);
                        }
                    }
                });
                
                etAiInput.setText(""); // ล้างช่องพิมพ์
            });
        }
    }

    private void setupLogic() {
        // 🤖 เริ่มการทำงานของคลาสแยกจัดการ AI ตัวใหม่
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
            initializeFileTree();
        }

        // 🌟 วางโค้ดระบบแท็บ Console & AI ต่อท้ายตรงนี้ได้เลยครับน้า 🎯 แก้ไขคลาสเรียกใช้ให้ตรงกับ Adapter
        panelAdapter = new PanelPagerAdapter(this);
        viewPager.setAdapter(panelAdapter);

        // สั่งเชื่อมความสัมพันธ์ระหว่างแท็บเมนูด้านบนกับหน้าแสดงผล ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Console");
            } else {
                tab.setText("AI");
            }
        }).attach();

        // ดักฟังก์ชันเพื่อป้อนตัวแปรให้ตรงจุดเมื่อวิวพร้อมแสดงผล
        viewPager.post(() -> {
            tvConsole = panelAdapter.getTvConsole();
            tvAiOutput = panelAdapter.getTvAiOutput();
        });
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
        
        // 🌟 แก้ไขจุดที่ 2: เปลี่ยนมาเคลียร์ข้อความในหน้าต่างแท็บ Console ตัวใหม่แทนตัวแปรเก่าครับ
        if (tvConsole != null) {
            tvConsole.setText(""); 
        }

        final BuildSummaryAnalyzer analyzer = new BuildSummaryAnalyzer();
        analyzer.clearErrors(); 
        
        final boolean[] isPipelineStopped = {false};

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
                        appendLog("\n##[error] การทำงานหยุดชะงักเนื่องจากการปิดตัวของระบบบิวด์อย่างกะทันหัน", TerminalColor.ERROR_RED);
                        
                        // 🌟 แก้ไขจุดที่ 3: เปลี่ยนมาพ่นสรุปวิเคราะห์ข้อผิดพลาดลงหน้าต่าง tvConsole ตัวใหม่แทนตัวแปรเก่าครับ
                        if (analyzer != null) {
                            analyzer.printSummary(new BuildSummaryAnalyzer.LogOutputListener() {
                                @Override
                                public void onAppendLog(String text, int color) {
                                    appendColoredText(tvConsole, text, color);
                                }
                            });
                        }
                        
                        final ParsedError err = analyzer.getLastError();
                        if (err != null) {
                            runOnUiThread(() -> executeJumpToError(err));
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
    }

    private void executeJumpToError(final ParsedError errorItem) {
        if (errorItem == null || currentProject == null) return;

        try {
            java.io.File targetFile = new java.io.File(errorItem.file);
            if (!targetFile.isAbsolute()) {
                targetFile = new java.io.File(currentProject.getRootPath(), errorItem.file);
            }

            appendLog("📂 กำลังตรวจสอบพิกัดไฟล์ในเครื่อง: " + targetFile.getAbsolutePath(), TerminalColor.LOG_GRAY);

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
                            codeEditor.jumpToLine(zeroBasedLine);
                        }
                    }, 200); 
                }
            } else {
                showToast("❌ ไม่พบตำแหน่งไฟล์นี้บนหน่วยความจำในเครื่องท่าน");
                appendLog("⚠️ ระบบไม่สามารถวาร์ปได้เนื่องจากหาพาธนี้ไม่พบในเครื่อง: " + targetFile.getAbsolutePath(), TerminalColor.ERROR_RED);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeFileTree() {
        if (currentProject == null) return;

        File projectRoot = new File(currentProject.getRootPath());
        masterFileList = FileSystemManager.loadRootDirectory(projectRoot);

        fileTreeAdapter = new FileTreeAdapter(this, masterFileList);
        treeView.setAdapter(fileTreeAdapter);

        treeView.setOnItemClickListener((parent, view, position, id) -> {
            FileNode selectedNode = masterFileList.get(position);

            if (selectedNode.isDirectory) {
                if (!selectedNode.isExpanded) {
                    selectedNode.isExpanded = true;
                    List<FileNode> children = FileSystemManager.loadChildren(selectedNode.file, selectedNode.depth);
                    masterFileList.addAll(position + 1, children);
                } else {
                    selectedNode.isExpanded = false;
                    int nextPosition = position + 1;
                    while (nextPosition < masterFileList.size() && masterFileList.get(nextPosition).depth > selectedNode.depth) {
                        masterFileList.remove(nextPosition);
                    }
                }
                fileTreeAdapter.notifyDataSetChanged();
                
            } else {
                String fileName = selectedNode.file.getName().toLowerCase();

                if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".webp")) {
                    dialogManager.showImageViewerDialog(selectedNode.file);
                } else {
                    fileTreeAdapter.setSelectedPosition(position);
                    currentProject.setCurrentOpenFile(selectedNode.file);
                    openFile(selectedNode.file);
                    
                    updateFilePathStatus(selectedNode.file);
                    
                    if (tabAdapter != null) {
                        tabAdapter.notifyDataSetChanged();
                        int pos = currentProject.getCurrentFileIndex();
                        if (pos != -1) {
                            tabRecyclerView.smoothScrollToPosition(pos);
                        }
                    }
                    drawerLayout.closeDrawers();
                }
            }
        });

        treeView.setOnItemLongClickListener((parent, view, position, id) -> {
            FileNode selectedNode = masterFileList.get(position);
            File currentFile = selectedNode.file;
            lastClickedPosition = position;

            com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
            
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_bottom_file_menu, null);
            bottomSheetDialog.setContentView(dialogView);

            TextView tvHeader = dialogView.findViewById(R.id.tvDialogHeader);
            LinearLayout menuContainer = dialogView.findViewById(R.id.menuContainer);

            tvHeader.setText(selectedNode.isDirectory ? "จัดการโฟลเดอร์: " + currentFile.getName() : "จัดการไฟล์: " + currentFile.getName());

            List<MenuOption> options = new ArrayList<>();
            options.add(new MenuOption("สร้างไฟล์ใหม่", android.R.drawable.ic_menu_add));
            options.add(new MenuOption("สร้างโฟลเดอร์ใหม่", android.R.drawable.ic_menu_preferences)); 
            options.add(new MenuOption("เปลี่ยนชื่อ", android.R.drawable.ic_menu_edit));
            options.add(new MenuOption("ลบ", android.R.drawable.ic_menu_delete));
            
            if (selectedNode.isDirectory) {
                options.add(new MenuOption("นำเข้าไฟล์ (Import)", android.R.drawable.ic_menu_share));
            }

            for (MenuOption option : options) {
                View itemView = getLayoutInflater().inflate(R.layout.dialog_menu_item, null);
                ImageView imgIcon = itemView.findViewById(R.id.menuIcon);
                TextView tvTitle = itemView.findViewById(R.id.menuTitle);

                tvTitle.setText(option.title);
                imgIcon.setImageResource(option.iconRes);

                itemView.setOnClickListener(v -> {
                    bottomSheetDialog.dismiss(); 
                    
                    if (option.title.equals("สร้างไฟล์ใหม่")) {
                        dialogManager.showCreateFileDialog(selectedNode.isDirectory ? currentFile : currentFile.getParentFile(), selectedNode.isDirectory ? selectedNode : findParentNode(selectedNode));
                    } else if (option.title.equals("สร้างโฟลเดอร์ใหม่")) {
                        dialogManager.showCreateFolderDialog(selectedNode.isDirectory ? currentFile : currentFile.getParentFile(), selectedNode.isDirectory ? selectedNode : findParentNode(selectedNode));
                    } else if (option.title.equals("เปลี่ยนชื่อ")) {
                        dialogManager.showRenameDialog(currentFile, selectedNode);
                    } else if (option.title.equals("ลบ")) {
                        dialogManager.showDeleteConfirmationDialog(currentFile.getName(), () -> {
                            boolean success = FileSystemManager.deleteFileOrFolder(currentFile);
                            if (success) {
                                showToast("ลบสำเร็จแล้ว");
                                masterFileList.remove(position);
                                if (fileTreeAdapter != null) {
                                    fileTreeAdapter.setSelectedPosition(-1);
                                    fileTreeAdapter.notifyDataSetChanged();
                                }
                            } else {
                                showToast("ลบไม่สำเร็จ");
                            }
                        });
                    } else if (option.title.equals("นำเข้าไฟล์ (Import)")) {
                        folderForImport = currentFile; 
                        openFilePicker(); 
                    }
                });
                menuContainer.addView(itemView);
            }
            bottomSheetDialog.show();
            return true;
        });
    }

    private FileNode findParentNode(FileNode childNode) {
        if (childNode == null || lastClickedPosition == -1) return null;
        for (int i = lastClickedPosition; i >= 0; i--) {
            FileNode potentialParent = masterFileList.get(i);
            if (potentialParent.isDirectory && potentialParent.depth < childNode.depth) {
                return potentialParent; 
            }
        }
        return null;
    }

    private void openFile(File file) {
        if (file == null || !file.exists()) return;

        try {
            autoSaveHandler.removeCallbacks(saveRunnable);

            if (currentProject != null) {
                if (!currentProject.getOpenedFiles().contains(file)) {
                    currentProject.getOpenedFiles().add(file);
                }
                currentProject.setCurrentOpenFile(file);
            }

            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            
            final String fileContent = sb.toString();

            runOnUiThread(() -> {
                if (codeEditor != null) {
                    codeEditor.setText(fileContent);
                    if (file.getName().endsWith(".xml")) {
                        // การรองรับในอนาคต
                    } else {
                        codeEditor.setEditorLanguage(new JavaLanguage());
                    }
                }
                
                updateFilePathStatus(file);
                
                if (tabAdapter != null) {
                    tabAdapter.notifyDataSetChanged();
                    int pos = currentProject.getCurrentFileIndex();
                    if (pos != -1 && tabRecyclerView != null) {
                        tabRecyclerView.smoothScrollToPosition(pos);
                    }
                }
                
                if (isPreviewMode && previewContainer != null) {
                    previewContainer.setVisibility(View.GONE);
                    if (codeEditor != null) codeEditor.setVisibility(View.VISIBLE);
                    isPreviewMode = false;
                    invalidateOptionsMenu();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            showToast("❌ เกิดข้อผิดพลาดในการโหลดไฟล์: " + e.getMessage());
        }
    }

    private void saveFile() {
        if (currentProject == null || currentProject.getCurrentOpenFile() == null) return;
        
        File fileToSave = currentProject.getCurrentOpenFile();
        try {
            FileOutputStream fos = new FileOutputStream(fileToSave);
            fos.write(codeEditor.getText().toString().getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    private void appendLog(final String text, final int color) {
        runOnUiThread(() -> {
            if (consolePanel != null && consolePanel.getVisibility() == View.GONE) {
                consolePanel.setVisibility(View.VISIBLE);
            }
            
            // 🌟 ปรับปรุงการต่อสาย Log ตอนคอมไพล์ให้พ่นเข้า tvConsole ของระบบแท็บตัวใหม่แทนตัวแปรเดิมครับ
            if (tvConsole != null) {
                tvConsole.setTextColor(color);
                tvConsole.append(text + "\n");
            }
            if (consoleScrollView != null) {
                consoleScrollView.post(() -> consoleScrollView.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    private void setupShortcutBar() { 
        LinearLayout shortcutBar = findViewById(R.id.shortcutBar); 
        if (shortcutBar == null) return;

        shortcutBar.removeAllViews();

        String[] shortcuts = {
            "↩", "↪", "{", "}", "[", "]", "(", ")", "<", ">", 
            ";", ",", ".", "=", "+", "-", "*", "/", 
            "_", "\"", "'", ":", "?", "!", "|", "&"
        };

        float density = getResources().getDisplayMetrics().density;
        int paddingHorizontal = (int) (10 * density); 

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            (int) (36 * density) 
        );
        params.setMargins((int)(3 * density), (int)(2 * density), (int)(3 * density), (int)(2 * density));
        params.gravity = Gravity.CENTER_VERTICAL;

        for (final String shortcut : shortcuts) {
            TextView btn = new TextView(this);
            btn.setText(shortcut);
            btn.setTextSize(15); 
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
            btn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            
            if (shortcut.equals("↩") || shortcut.equals("↪")) {
                btn.setTextColor(Color.parseColor("#FFFFFF")); 
            } else {
                btn.setTextColor(Color.parseColor("#B0B3B8")); 
            }

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(6 * density); 
            shape.setColor(Color.parseColor("#2D2D2D")); 

            int[][] states = new int[][] { new int[] {} };
            int[] colors = new int[] { Color.parseColor("#444444") };
            ColorStateList colorStateList = new ColorStateList(states, colors);
            RippleDrawable rippleDrawable = new RippleDrawable(colorStateList, shape, null);
            btn.setBackground(rippleDrawable);

            params.gravity = Gravity.CENTER_VERTICAL;
            btn.setLayoutParams(params);
            btn.setClickable(true);
            btn.setFocusable(true);

            btn.setOnClickListener(v -> {
                if (codeEditor == null) return;

                if (shortcut.equals("↩")) {
                    if (codeEditor.canUndo()) {
                        codeEditor.undo();
                    }
                } else if (shortcut.equals("↪")) {
                    if (codeEditor.canRedo()) {
                        codeEditor.redo();
                    }
                } else {
                    if (codeEditor.getCursor() != null) {
                        int line = codeEditor.getCursor().getLeftLine();
                        int column = codeEditor.getCursor().getLeftColumn();
                        codeEditor.getText().insert(line, column, shortcut);
                    }
                }
            });

            shortcutBar.addView(btn);
        }

        // 🤖 [จุดปรับปรุงลอจิกเรียกใช้คลาสแยกและลด Error 400]
        TextView btnAskAI = new TextView(this);
        btnAskAI.setText("🤖 ถาม AI");
        btnAskAI.setTextSize(14);
        btnAskAI.setGravity(Gravity.CENTER);
        btnAskAI.setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
        btnAskAI.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        btnAskAI.setTextColor(Color.parseColor("#BB86FC")); 

        GradientDrawable aiShape = new GradientDrawable();
        aiShape.setShape(GradientDrawable.RECTANGLE);
        aiShape.setCornerRadius(6 * density);
        aiShape.setColor(Color.parseColor("#251F35")); 
        
        RippleDrawable aiRipple = new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#443366")), aiShape, null);
        btnAskAI.setBackground(aiRipple);
        btnAskAI.setLayoutParams(params);
        btnAskAI.setClickable(true);
        btnAskAI.setFocusable(true);

        btnAskAI.setOnClickListener(v -> {
            if (codeEditor == null || currentProject == null) return;

            if (consolePanel != null) consolePanel.setVisibility(View.VISIBLE);
            
            // 🌟 แก้ไขจุดที่ 1: กดถาม AI ปุ๊บ สั่งให้ ViewPager เด้งรูดไปหน้าแท็บ AI ทันทีครับน้า
            if (viewPager != null) {
                viewPager.setCurrentItem(1, true);
            }
            
            java.io.File currentFile = currentProject.getCurrentOpenFile();
            String fileName = (currentFile != null) ? currentFile.getName() : "UnknownFile.java";
            String currentCode = codeEditor.getText().toString();

            aiLayoutAnalyzer.analyzeCode(fileName, currentCode, new AiLayoutAnalyzer.OnAnalysisListener() {
                @Override
                public void onStart() {
                    if (tvAiOutput != null) {
                        tvAiOutput.setText("🤖 MiniStudio AI กำลังวิเคราะห์โค้ด...");
                    }
                }

                @Override
                public void onSuccess(android.text.SpannableString formattedResult) {
                    if (tvAiOutput != null) {
                        tvAiOutput.setText(formattedResult); // แสดงผลความสวยงามแบบมีสีสันในแท็บ AI 🎨
                    }
                    if (consoleScrollView != null) {
                        consoleScrollView.post(() -> consoleScrollView.fullScroll(View.FOCUS_DOWN));
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (tvAiOutput != null) {
                        tvAiOutput.setText("❌ AI เกิดข้อผิดพลาดชั่วคราว: " + errorMessage);
                    }
                }
            });
        });

        shortcutBar.addView(btnAskAI); 
    }
    
    private void applyEditorFontSize(float sizeSp) {
        if (codeEditor != null) {
            codeEditor.setTextSize(sizeSp);
            showToast("Font size: " + (int)sizeSp + "sp");
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void findAndHighlight() {
        String query = etFind.getText().toString();
        String content = codeEditor.getText().toString();
        if (query.isEmpty()) return;

        int index = content.indexOf(query, lastSearchIndex);
        if (index == -1) {
            index = content.indexOf(query, 0);
            lastSearchIndex = 0;
        }

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
            int startLine = 0, startCol = 0;
            int endLine = 0, endCol = 0;
            int currentIdx = 0;
            String[] lines = text.split("\n", -1);
            
            for (int i = 0; i < lines.length; i++) {
                int lineLen = lines[i].length() + 1; 
                if (currentIdx + lineLen > startIdx && startLine == 0 && startCol == 0) {
                    startLine = i;
                    startCol = startIdx - currentIdx;
                }
                if (currentIdx + lineLen > endIdx) {
                    endLine = i;
                    endCol = endIdx - currentIdx;
                    break;
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
        String newContent = content.replaceFirst(java.util.regex.Pattern.quote(target), replacement);

        codeEditor.setText(newContent);
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
        
        if (id == R.id.action_build) {
            startCloudBuildPipeline();
            return true;
        }

        if (id == R.id.action_preview) {
            toggleXmlPreview();
            return true;
        }
        
        if(id == R.id.action_ai_settings){
            startActivity(
                new Intent(
                    this,
                    AiSettingsActivity.class 
                )
            );
            return true;
        }

        if (id == R.id.action_undo) { if (codeEditor.canUndo()) codeEditor.undo(); return true; } 
        if (id == R.id.action_redo) { if (codeEditor.canRedo()) codeEditor.redo(); return true; }
        if (id == R.id.action_search) {
            searchBar.setVisibility(searchBar.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void triggerTreeRefresh(FileNode parentNode) {
        if (parentNode != null) {
            int parentPos = masterFileList.indexOf(parentNode);
            if (parentPos != -1) {
                refreshSubFolder(parentPos, parentNode);
                return;
            }
        }
        refreshFileTree();
    }

    private void openFilePicker() {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); 
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        startActivityForResult(android.content.Intent.createChooser(intent, "เลือกไฟล์ที่จะนำเข้า"), PICK_FILE_REQUEST_CODE);
    }

    private void refreshSubFolder(int position, FileNode parentNode) {
        if (parentNode == null) return;
        int nextPosition = position + 1;
        while (nextPosition < masterFileList.size() && masterFileList.get(nextPosition).depth > parentNode.depth) {
            masterFileList.remove(nextPosition);
        }
        List<FileNode> children = FileSystemManager.loadChildren(parentNode.file, parentNode.depth);
        masterFileList.addAll(position + 1, children);
        parentNode.isExpanded = true;
        
        if (fileTreeAdapter != null) {
            fileTreeAdapter.notifyDataSetChanged();
        }
    }

    private void refreshFileTree() {
        if (currentProject != null) {
            File projectRoot = new File(currentProject.getRootPath());
            masterFileList.clear();
            masterFileList.addAll(FileSystemManager.loadRootDirectory(projectRoot));
            if (fileTreeAdapter != null) {
                fileTreeAdapter.setSelectedPosition(-1); 
                fileTreeAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            android.net.Uri fileUri = data.getData();
            try {
                String displayName = "imported_file_" + System.currentTimeMillis();
                android.database.Cursor cursor = getContentResolver().query(fileUri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) displayName = cursor.getString(nameIndex);
                    cursor.close();
                }

                File tempFile = new File(getCacheDir(), displayName);
                java.io.InputStream inputStream = getContentResolver().openInputStream(fileUri);
                java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();

                if (folderForImport != null) {
                    FileSystemManager.importFileToFolder(tempFile, folderForImport);
                    showToast("นำเข้าไฟล์ " + displayName + " เรียบร้อย!");
                    
                    if (lastClickedPosition != -1 && lastClickedPosition < masterFileList.size()) {
                        FileNode parentNode = masterFileList.get(lastClickedPosition);
                        triggerTreeRefresh(parentNode);
                    } else {
                        refreshFileTree();
                    }
                }
                tempFile.delete(); 
            } catch (Exception e) {
                e.printStackTrace();
                showToast("นำเข้าไฟล์ล้มเหลว: " + e.getMessage());
            }
        }
    }

    private void setupTabLogic() {
        if (currentProject == null) return;
        
        tabAdapter = new TabAdapter(currentProject, new TabAdapter.OnTabInterface() {
            @Override
            public void onTabClick(File file) {
                currentProject.setCurrentOpenFile(file);
                openFile(file);
                updateFilePathStatus(file);
                tabAdapter.notifyDataSetChanged();
            }

            @Override
            public void onTabClose(File file, int position) {
                currentProject.getOpenedFiles().remove(file);
                tabAdapter.notifyItemRemoved(position);
                tabAdapter.notifyItemRangeChanged(position, currentProject.getOpenedFiles().size());

                if (file.equals(currentProject.getCurrentOpenFile())) {
                    if (!currentProject.getOpenedFiles().isEmpty()) {
                        File nextFile = currentProject.getOpenedFiles().get(0);
                        currentProject.setCurrentOpenFile(nextFile);
                        openFile(nextFile);
                        updateFilePathStatus(nextFile);
                    } else {
                        currentProject.setCurrentOpenFile(null);
                        runOnUiThread(() -> codeEditor.setText(""));
                    }
                    tabAdapter.notifyDataSetChanged();
                }
            }
        });
        tabRecyclerView.setAdapter(tabAdapter);
        updateFilePathStatus(currentProject.getCurrentOpenFile());
    }

    private void updateFilePathStatus(File file) {
        if (tvFilePath == null) return;
        
        if (file == null || currentProject == null) {
            tvFilePath.setText("No file open");
            return;
        }

        String fullPath = file.getAbsolutePath();
        String projectRoot = currentProject.getRootPath(); 

        if (fullPath.startsWith(projectRoot)) {
            String relativePath = fullPath.substring(projectRoot.length());
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            tvFilePath.setText(relativePath);
        } else {
            tvFilePath.setText(file.getName());
        }
    }

    private void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    private static class MenuOption {
        String title;
        int iconRes;
        MenuOption(String title, int iconRes) {
            this.title = title;
            this.iconRes = iconRes;
        }
    }
    
    private void appendColoredText(TextView tv, String text, int color) {
        if (tv == null) return;
        android.text.SpannableString spannable = new android.text.SpannableString(text);
        spannable.setSpan(new android.text.style.ForegroundColorSpan(color), 
                          0, text.length(), 
                          android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        tv.append(spannable);
        
        if (consoleScrollView != null) {
            consoleScrollView.post(() -> consoleScrollView.fullScroll(android.view.View.FOCUS_DOWN));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aiLayoutAnalyzer != null) {
            aiLayoutAnalyzer.shutdown();
        }
    }
}
