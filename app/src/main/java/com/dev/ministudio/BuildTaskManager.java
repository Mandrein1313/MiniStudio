package com.dev.ministudio;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.json.JSONArray;
import org.json.JSONObject;

public class BuildTaskManager {

    public interface BuildListener {
        void onLogAppend(String text, int color);
        void onBuildStarted();
        void onBuildFinished(boolean success, String apkPath);
    }

    private final Context context; 
    private final String projectPath;
    private final BuildEnvironmentManager envManager; 
    private final BuildListener listener;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final int COLOR_INFO = Color.parseColor("#4FC3F7");
    private final int COLOR_SUCCESS = Color.parseColor("#81C784");
    private final int COLOR_ERROR = Color.parseColor("#FF8A80");
    private final int COLOR_WARNING = Color.parseColor("#FFB74D");

    public BuildTaskManager(Context context, String projectPath, BuildListener listener) {
        this.context = context;
        this.projectPath = projectPath;
        this.envManager = new BuildEnvironmentManager(context);
        this.listener = listener;
    }

    public void startCloudBuild(final String githubToken, final String repoUrl, final String projectName, final String packageName) {
        postUiEvent(BuildListener::onBuildStarted);
        
        new Thread(() -> {
            try {
                sendProgress("🚀 เริ่มต้นกระบวนการเชื่อมต่อและเตรียมซอร์สโค้ด...\n", COLOR_INFO);
                File projectDir = new File(projectPath);
                
                // จัดแจงเขียน .gitignore เพื่อความสะอาดของโปรเจกต์
                createGitIgnore(projectDir);

                // สั่งระบบสร้างสคริปต์ขั้นตอนทำงานอัตโนมัติบน GitHub (Workflows)
                envManager.prepareGitHubWorkflow(projectPath, projectName, packageName, "Java", 21);

                sendProgress("📦 กำลังทำการส่งซอร์สโค้ดขึ้นสู่ GitHub Remote...\n", COLOR_INFO);
                
                Git git;
                File gitDir = new File(projectDir, ".git");
                if (!gitDir.exists()) {
                    git = Git.init().setDirectory(projectDir).call();
                } else {
                    git = Git.open(projectDir);
                }

                git.add().addFilepattern(".").call();
                git.commit().setMessage("Cloud Build Request - " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).call();

                StoredConfig config = git.getRepository().getConfig();
                config.setString("remote", "origin", "url", repoUrl);
                config.save();

                UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(githubToken, "");
                PushCommand push = git.push();
                push.setCredentialsProvider(credentials);
                push.setForce(true);
                push.setRemote("origin");
                push.add("master").add("main");
                push.call();

                sendProgress("✅ อัปโหลดซอร์สโค้ดสำเร็จเรียบร้อย! กำลังปลุกระบบคลาวด์บิวด์...\n", COLOR_SUCCESS);
                
                // ดึงพิกัดรายละเอียดของ Repository ออกมาใช้งาน
                String repoPath = repoUrl.replace("https://github.com/", "").replace(".git", "");
                
                // เริ่มติดตามและดูสถานะความคืบหน้าของกระบวนการบิวด์
                monitorWorkflowRuns(githubToken, repoPath);

            } catch (Exception e) {
                sendProgress("❌ เกิดข้อผิดพลาดในระบบการนำส่งข้อมูล: " + e.getMessage() + "\n", COLOR_ERROR);
                postUiEvent(l -> l.onBuildFinished(false, null));
            }
        }).start();
    }

    private void monitorWorkflowRuns(String token, String repoPath) {
        try {
            String urlStr = "https://api.github.com/repos/" + repoPath + "/actions/runs?per_page=1";
            sendProgress("⏳ กำลังรอคิวและจัดเตรียมตู้คอนเทนเนอร์บิวด์บนคลาวด์...\n", COLOR_INFO);

            long startTime = System.currentTimeMillis();
            long runId = -1;

            //  แก้ไขตรงนี้: เปลี่ยนจาก 60000 เป็น 180000 (ขยายเวลารอเพิ่มเป็น 3 นาที)
            while (System.currentTimeMillis() - startTime < 180000) {
                Thread.sleep(5000);
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray runs = json.getJSONArray("workflow_runs");
                    if (runs.length() > 0) {
                        JSONObject latestRun = runs.getJSONObject(0);
                        runId = latestRun.getLong("id");
                        String status = latestRun.getString("status");
                        
                        sendProgress("⚡ สถานะไปป์ไลน์ล่าสุด: [" + status.toUpperCase() + "]\n", COLOR_WARNING);
                        
                        if ("completed".equals(status)) {
                            String conclusion = latestRun.getString("conclusion");
                            if ("success".equals(conclusion)) {
                                sendProgress("🎉 บิวด์สำเร็จสมบูรณ์! กำลังนำเข้าไฟล์ APK ลงสู่ตัวเครื่อง...\n", COLOR_SUCCESS);
                                postUiEvent(l -> l.onBuildFinished(true, ""));
                            } else {
                                sendProgress("❌ บิวด์ล้มเหลว! กำลังสืบค้นพิกัดข้อผิดพลาดจาก Log บนเซิร์ฟเวอร์...\n", COLOR_ERROR);
                                fetchAndParseBuildLogs(token, repoPath, runId);
                                postUiEvent(l -> l.onBuildFinished(false, null));
                            }
                            return;
                        }
                    }
                }
            }
            sendProgress("⏳ หมดเวลาเชื่อมต่อเซิร์ฟเวอร์ (Timeout)\n", COLOR_ERROR);
            postUiEvent(l -> l.onBuildFinished(false, null));
        } catch (Exception e) {
            sendProgress("❌ มีปัญหาในการเชื่อมต่อระบบตรวจสอบสถานะ: " + e.getMessage() + "\n", COLOR_ERROR);
            postUiEvent(l -> l.onBuildFinished(false, null));
        }
    }

    private void fetchAndParseBuildLogs(String token, String repoPath, long runId) {
        try {
            // 1. ตรวจค้นและหา Job ID ที่ทำงานพลาด
            String jobsUrl = "https://api.github.com/repos/" + repoPath + "/actions/runs/" + runId + "/jobs";
            HttpURLConnection conn = (HttpURLConnection) new URL(jobsUrl).openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + token);
            
            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                JSONArray jobs = json.getJSONArray("jobs");
                if (jobs.length() > 0) {
                    JSONObject job = jobs.getJSONObject(0);
                    long jobId = job.getLong("id");

                    // 2. เรียกดาวน์โหลด Log ทั้งหมดของ Job นั้นออกมาสแกนแบบเรียลไทม์
                    String logUrl = "https://api.github.com/repos/" + repoPath + "/actions/jobs/" + jobId + "/logs";
                    HttpURLConnection logConn = (HttpURLConnection) new URL(logUrl).openConnection();
                    logConn.setRequestProperty("Authorization", "Bearer " + token);

                    if (logConn.getResponseCode() == 200) {
                        BufferedReader logReader = new BufferedReader(new InputStreamReader(logConn.getInputStream()));
                        
                        // 🌟 นำออบเจกต์ตัวสแกนใหม่มาเตรียมวิเคราะห์
                        BuildSummaryAnalyzer analyzer = new BuildSummaryAnalyzer();
                        
                        while ((line = logReader.readLine()) != null) {
                            // นำ Log แต่ละบรรทัดเข้าสู่ตัวประมวลผล
                            // 🛑 ตรวจเช็คค่าคืนกลับ (Return Value): หากเป็น true สั่งตัดตอนและหยุดดึงข้อมูลทันที!
                            boolean shouldStop = analyzer.analyzeLine(line, COLOR_WARNING, (txt, col) -> sendProgress(txt, col));
                            
                            if (shouldStop) {
                                break; // สั่งหยุดดึง Log บรรทัดที่เหลือเพื่อล็อกผลลัพธ์แรกสุดไว้
                            }
                        }
                        logReader.close();

                        // 🌟 แสดงกรอบหน้าต่างคำแนะนำการแก้ไขให้ผู้ใช้งานทราบพิกัดแบบแม่นยำตรงตาม GitHub Actions
                        analyzer.printSummary((txt, col) -> sendProgress(txt, col));
                        return;
                    }
                }
            }
            sendProgress("❌ ไม่สามารถดึงประวัติการทำงานจากเซิร์ฟเวอร์ GitHub มาประมวลผลได้\n", COLOR_ERROR);
        } catch (Exception e) {
            sendProgress("❌ เกิดปัญหาการแปลงโครงสร้าง Log: " + e.getMessage() + "\n", COLOR_ERROR);
        }
    }

    private void createGitIgnore(File projectDir) {
        try {
            File gitIgnoreFile = new File(projectDir, ".gitignore");
            String content = ".gradle/\nbuild/\napp/build/\n*.iml\nlocal.properties\n";
            try (FileOutputStream fos = new FileOutputStream(gitIgnoreFile)) {
                fos.write(content.getBytes("UTF-8"));
            }
        } catch (Exception ignored) {}
    }

    private void sendProgress(final String text, final int color) {
        uiHandler.post(() -> listener.onLogAppend(text, color));
    }

    private interface UiEventAction {
        void run(BuildListener listener);
    }
    
    private void postUiEvent(final UiEventAction action) {
        uiHandler.post(() -> action.run(listener));
    }
}
