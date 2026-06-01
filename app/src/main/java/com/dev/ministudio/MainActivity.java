package com.dev.ministudio;

import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue; 
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView; 
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// 🛠️ เพิ่มการ Import ระบบดึงข้อมูล SharedPreferences 
import android.content.Context;
import android.content.SharedPreferences;

// เพิ่มการ Import คลาสระบบสี กราฟิก และปุ่มกดที่ขาดหายไป
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;

// นำเข้าแพ็คเกจระดับสูงของ Sora Editor เข้าสู่โปรเจกต์
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
    private TextView tvConsoleLog;
    private boolean isConsoleMaximized = false; // ตัวแปรเช็คว่าคอนโซลเต็มจออยู่หรือไม่
        
    // Controllers & Models
    private ProjectModel currentProject;

    // Utils
    private final Handler autoSaveHandler = new Handler(); 
    private Runnable saveRunnable;
    private int lastSearchIndex = 0;
    
    // ตัวแปรควบคุมขนาดฟอนต์ (เริ่มต้น 14.0f ตามมาตรฐาน)
    private float currentCodeFontSize = 14.0f; 

    // ระบบกางกิ่งไม้สไตล์ AndroidIDE
    private List<FileNode> masterFileList = new ArrayList<>();
    private FileTreeAdapter fileTreeAdapter;

    // ตัวจัดการเชื่อมต่อสิ่งแวดล้อม
    private BuildEnvironmentManager buildEnvManager;
    private File folderForImport = null;
    private int lastClickedPosition = -1; 
    private static final int PICK_FILE_REQUEST_CODE = 2026; 
    
    // ตัวจัดการกล่องไดอะล็อกแยกส่วนที่คุณกำหนด
    private ProjectDialogManager dialogManager;

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
        
        treeView = findViewById(R.id.treeView); 
        tabRecyclerView = findViewById(R.id.tabRecyclerView);
        
        tabRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        
        // เริ่มการผูกมัดหน้าจอชุดใหม่ตามดีไซน์โมเดิร์น
        consolePanel = findViewById(R.id.consolePanel);
        consoleScrollView = findViewById(R.id.consoleScrollView);
        tvConsoleLog = findViewById(R.id.tvConsoleLog);

        // เครื่องหมาย ✕ เล็กๆ บนแถบ กดแล้วจะล้างหน้าจอเหมือนปุ่ม Clear เดิม
        findViewById(R.id.btnClearConsole).setOnClickListener(v -> tvConsoleLog.setText(""));
        
        // แฟ้มสีเขียวฝั่งซ้าย กดเพื่อพับปิดหน้าต่างคอนโซลลงไปข้างล่าง
        findViewById(R.id.btnCloseConsole).setOnClickListener(v -> consolePanel.setVisibility(View.GONE));

        // 1. แทรกระบบขยาย-ย่อหน้าจอ Console เข้าไปตรงนี้ (เชื่อมโยงกับปุ่มใน XML)
        android.widget.ImageButton btnToggleExpand = findViewById(R.id.btnToggleExpand);
        if (btnToggleExpand != null) {
            btnToggleExpand.setOnClickListener(v -> {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) consolePanel.getLayoutParams();
                
                if (!isConsoleMaximized) {
                    // ขยายความสูงเต็มหน้าจอ (MATCH_PARENT)
                    params.height = LinearLayout.LayoutParams.MATCH_PARENT;
                    btnToggleExpand.setImageResource(android.R.drawable.ic_menu_delete); // เปลี่ยนไอคอนระบบชั่วคราวให้รู้ว่าย่อลงได้
                    btnToggleExpand.setColorFilter(Color.parseColor("#FF5252")); // เปลี่ยนสีปุ่มเป็นโทนแดงส้ม
                    isConsoleMaximized = true;
                } else {
                    // หดกลับมาเหลือขนาดปกติ 160dp
                    int heightInDp = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 160, getResources().getDisplayMetrics()
                    );
                    params.height = heightInDp;
                    btnToggleExpand.setImageResource(android.R.drawable.ic_menu_compass); // เปลี่ยนกลับเป็นไอคอนเข็มทิศ
                    btnToggleExpand.setColorFilter(Color.parseColor("#FFB74D")); // เปลี่ยนกลับเป็นสีส้มเหลือง
                    isConsoleMaximized = false;
                }
                
                // อัปเดตเลย์เอาต์หน้าจอทันที
                consolePanel.setLayoutParams(params);
            });
        }

        // ฟังก์ชันพิเศษ: ปุ่มรันสามเหลี่ยมสีเขียวที่ย้ายมาไว้ข้างคอนโซลแบบในภาพตัวอย่าง
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
    }
    
    private void setupLogic() {
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
    }

    // 🐙 เมทอดรันบิวด์บนคลาวด์: เวอร์ชันแก้ไขดึงค่าจาก SharedPreferences สำเร็จแล้ว 🌟
    private void startCloudBuildPipeline() {
        if (currentProject == null) {
            showToast("กรุณาเปิดโปรเจกต์ก่อนทำการรัน");
            return;
        }

        // 🛠️ แก้ไขจุดที่ 1: ดึงค่าบัญชีผู้ใช้จาก SharedPreferences ที่บันทึกมาจากหน้าแรก
        SharedPreferences prefs = getSharedPreferences("GitHubPrefs", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "");
        String savedToken = prefs.getString("token", "");

        // ตรวจสอบความถูกต้อง หากยังไม่ได้กรอกข้อมูลให้เด้งเตือนทันที
        if (username.isEmpty() || savedToken.isEmpty()) {
            showToast("❌ ยังไม่ได้ตั้งค่าบัญชี GitHub กรุณาตั้งค่าที่ปุ่มฟันเฟืองหน้าแรกก่อนครับ");
            return;
        }

        saveFile(); 
        if (tvConsoleLog != null) {
            tvConsoleLog.setText(""); 
        }

        // เรียกใช้สมองกลวิเคราะห์บั๊กตัวใหม่ที่เราแยกคลาสออกมา
        final BuildSummaryAnalyzer analyzer = new BuildSummaryAnalyzer();
        final boolean[] isPipelineStopped = {false};

        // สั่งเริ่มกลุ่มคำสั่งตั้งค่าเริ่มต้น
        appendLog("##[group]เริ่มขั้นตอนการตั้งค่า & ตรวจสอบโปรเจกต์เบื้องต้น", TerminalColor.LOG_GRAY); 
        appendLog("🔔 [กำลังจัดเตรียมสภาพแวดล้อม...] เริ่มทำงานระบบ Workflow สำเร็จ", TerminalColor.LOG_WHITE);
        appendLog("📂 ที่อยู่โปรเจกต์ (Root Path): " + currentProject.getRootPath(), TerminalColor.BORDER_BLUE); 
        appendLog("##[endgroup]", TerminalColor.LOG_GRAY);

        // ✅ แก้ไขสอดคล้องตัวแปรดั้งเดิม: ลบ buildEnvManager ออก เพื่อให้เหลือพารามิเตอร์ 3 ตัวตรงตาม Constructor
        BuildTaskManager buildTask = new BuildTaskManager(
            MainActivity.this, 
            currentProject.getRootPath(),
            new BuildTaskManager.BuildListener() {
                
                @Override 
                public void onLogAppend(String text, int color) { 
                    if (isPipelineStopped[0]) return;

                    // 🛠️ ส่งบรรทัด Log ไปให้คลาสสรุปวิเคราะห์ หากเจอเงื่อนไขผิดพลาดร้ายแรง ฟังก์ชันจะรีเทิร์นกลับมาเป็น true
                    boolean hasFailed = analyzer.analyzeLine(text, color, new BuildSummaryAnalyzer.LogOutputListener() {
                        @Override
                        public void onAppendLog(String logText, int logColor) {
                            appendLog(logText, logColor); // พ่นกล่องข้อความลง Console หลัก
                        }
                    });

                    if (hasFailed) {
                        isPipelineStopped[0] = true;
                        showToast("💥 บิวด์ล้มเหลว! (Exit Code 1)");
                        return;
                    }

                    // จัดการสีของลีดเดอร์ Log ทั่วไปตามสไตล์ความสะอาดสายตา
                    String lowerText = text.toLowerCase();
                    if (color == Color.GREEN || lowerText.contains("success")) {
                        appendLog(text, TerminalColor.SUGGEST_GREEN); 
                    } else if (color == Color.YELLOW) {
                        appendLog(text, TerminalColor.TARGET_YELLOW); 
                    } else if (color == Color.CYAN) {
                        appendLog(text, TerminalColor.LOG_CYAN); 
                    } else if (color == Color.RED || lowerText.contains("error:") || lowerText.contains("failed:")) {
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
                    } else {
                        showToast("กระบวนการทำงานล้มเหลว");
                        appendLog("\n##[error] การทำงานหยุดชะงักเนื่องจากการปิดตัวของระบบบิวด์อย่างกะทันหัน", TerminalColor.ERROR_RED);
                        
                        // 🌟 ดึงค่าจาก Regex Parser มาแสดงพิกัดที่พัง
                        final ParsedError err = analyzer.getLastError();
                        if (err != null) {
                            appendLog("\n======================================", TerminalColor.DETAIL_RED);
                            appendLog("📍 พิกัดโค้ดพัง: " + err.file + " (บรรทัดที่ " + err.line + ")", TerminalColor.DETAIL_RED);
                            appendLog("💬 ข้อความพัง: " + err.message, TerminalColor.TARGET_YELLOW);
                            appendLog("======================================", TerminalColor.DETAIL_RED);
                            
                            // 🚀 สั่งรันคำสั่งเปิดไฟล์ที่พังขึ้นหน้าจออัตโนมัติบน UI Thread
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        // 🛠️ แก้ไขบรรทัดที่ 328: แปลง String ให้เป็นวัตถุ File ก่อนส่งเข้าเมทอด
                                        java.io.File targetFile = new java.io.File(currentProject.getRootPath(), err.file);
                                        if (targetFile.exists()) {
                                            openFile(targetFile);
                                            showToast("📂 เปิดไฟล์ที่พังให้เรียบร้อยแล้ว! (บรรทัดที่ " + err.line + ")");
                                        } else {
                                            // สำรองไว้เผื่อกรณีพิกัดเป็นแบบ Full Path มาอยู่แล้ว
                                            openFile(new java.io.File(err.file));
                                            showToast("📂 เปิดไฟล์ที่พังให้เรียบร้อยแล้ว! (บรรทัดที่ " + err.line + ")");
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                }
            }
        );
        
        // 🛠️ แก้ไขจุดที่ 2: ผูกข้อมูลและประกอบลิงก์ URL อัตโนมัติจากฐานข้อมูลแอปแทนค่า Static ข้อความไทยเดิม
        String githubToken = savedToken; 
        String projectName = currentProject.getProjectName();
        String repoUrl = "https://github.com/" + username + "/" + projectName + ".git";
        String packageName = "com.dev.ministudio"; 

        buildTask.startCloudBuild(githubToken, repoUrl, projectName, packageName); 
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
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            reader.close();
            
            final String fileContent = sb.toString();
            runOnUiThread(() -> {
                codeEditor.setText(fileContent);
            });
        } catch (Exception e) {
            Toast.makeText(this, "Read Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveFile() {
        if (currentProject == null || currentProject.getCurrentOpenFile() == null) return;
        try {
            FileOutputStream fos = new FileOutputStream(currentProject.getCurrentOpenFile());
            fos.write(codeEditor.getText().toString().getBytes());
            fos.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void appendLog(final String text, final int color) {
        runOnUiThread(() -> {
            if (consolePanel != null && consolePanel.getVisibility() == View.GONE) {
                consolePanel.setVisibility(View.VISIBLE);
            }
            if (tvConsoleLog != null) {
                tvConsoleLog.setTextColor(color);
                tvConsoleLog.append(text + "\n");
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_build) {
            startCloudBuildPipeline();
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
}
