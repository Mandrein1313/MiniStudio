package com.dev.ministudio;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

public class ProjectListActivity extends AppCompatActivity {

    private ArrayList<String> projects = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private DrawerLayout drawerLayout;
    private FloatingActionsMenu fabMenu;
    private FloatingActionButton fabCreate;
    private FloatingActionButton fabGithub;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#1E1E1E"));
        setContentView(R.layout.activity_project_list);

        ListView listView = findViewById(R.id.projectListView);
        Toolbar toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);

        fabMenu = findViewById(R.id.multiple_actions);
        fabCreate = findViewById(R.id.action_create);
        fabGithub = findViewById(R.id.action_github);

        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, android.R.string.ok, android.R.string.cancel);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        // 1. ตั้งค่าปุ่ม Fab ผ่านเมธอดแยก (สะอาดและดูดีขึ้น)
        setupFabButtons();
        // 2. โหลดข้อมูลต่างๆ
        checkPermissions();
        refreshProjectList();
        // 3. ตั้งค่า Adapter
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, projects) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(android.graphics.Color.WHITE);
                text.setTextSize(18);
                text.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.sym_def_app_icon, 0, 0, 0);
                text.setCompoundDrawablePadding(30);
                return view;
            }
        };
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(ProjectListActivity.this, MainActivity.class);
            intent.putExtra("projectName", projects.get(position));
            startActivity(intent);
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            String projectName = projects.get(position);
            new AlertDialog.Builder(ProjectListActivity.this)
                .setTitle("ลบโปรเจกต์")
                .setMessage("คุณต้องการลบ " + projectName + " ใช่หรือไม่?")
                .setPositiveButton("ลบ", (dialog, which) -> {
                    deleteRecursive(new File("/sdcard/MiniStudio/" + projectName));
                    projects.remove(position);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(ProjectListActivity.this, "ลบแล้ว", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("ยกเลิก", null)
                .show();
            return true;
        });

        // 4. ระบบตรวจสอบ GitHub
        SharedPreferences prefs = getSharedPreferences("GitHubPrefs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("is_github_setup", false)) {
            new android.os.Handler().postDelayed(this::showGitHubSettingsDialog, 600);
        }
    }

    private void setupFabButtons() {
        fabCreate.setOnClickListener(v -> {
            showCreateProjectDialog(); // เรียกหน้าต่างสร้างโปรเจกต์
            fabMenu.collapse();
        });

        fabGithub.setOnClickListener(v -> {
            importFromGitHub(); // เรียกฟังก์ชันนำเข้า (น้าไปเขียนต่อด้านล่างครับ)
            fabMenu.collapse();
        });
    }

    private void importFromGitHub() {
        // TODO: น้าใส่โค้ดสำหรับดึงโปรเจกต์จาก GitHub ไว้ที่นี่ครับ
        Toast.makeText(this, "ฟังก์ชันนำเข้าจาก GitHub กำลังพัฒนา...", Toast.LENGTH_SHORT).show();
    }

    // --- เมธอดส่วนที่เหลือคงเดิม ---
    private void refreshProjectList() {
        projects.clear();
        File root = new File("/sdcard/MiniStudio");
        if (!root.exists()) root.mkdirs();
        File[] files = root.listFiles();
        if (files != null) {
            for (File f : files) if (f.isDirectory()) projects.add(f.getName());
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

 
    // 🟢 หน้าต่างสร้างโปรเจกต์แบบ Advance เพิ่มตัวเลือก Language และ Minimum SDK (ดีไซน์พรีเมียมดาร์กโมด)
    private void showCreateProjectDialog() {
    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
    LinearLayout mainLayout = new LinearLayout(this);
    mainLayout.setOrientation(LinearLayout.VERTICAL);
    int paddingPx = (int) (20 * getResources().getDisplayMetrics().density);
    mainLayout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
    mainLayout.setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"));

    // --- ระบบไอคอน ---
    final int[] currentSelectedIcon = {R.drawable.ic_launcher};
    android.widget.ImageView iconPreview = new android.widget.ImageView(this);
    iconPreview.setImageResource(currentSelectedIcon[0]);
    int size = (int) (60 * getResources().getDisplayMetrics().density);
    LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(size, size);
    imgParams.gravity = android.view.Gravity.CENTER;
    imgParams.bottomMargin = 20;
    iconPreview.setLayoutParams(imgParams);
    iconPreview.setOnClickListener(v -> IconPickerDialogHelper.show(this, resId -> {
        currentSelectedIcon[0] = resId;
        iconPreview.setImageResource(resId);
    }));
    mainLayout.addView(iconPreview);

    // --- ช่องกรอกข้อมูล ---
    final android.widget.EditText etName = new android.widget.EditText(this);
    etName.setHint("Project Name");
    etName.setTextColor(-1);
    etName.setHintTextColor(android.graphics.Color.GRAY);
    mainLayout.addView(etName);

    final android.widget.EditText etPkg = new android.widget.EditText(this);
    etPkg.setHint("Package Name");
    etPkg.setTextColor(-1);
    etPkg.setHintTextColor(android.graphics.Color.GRAY);
    mainLayout.addView(etPkg);

    // --- Spinner (Lang & SDK) ---
    final android.widget.Spinner spinLang = new android.widget.Spinner(this);
    spinLang.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Java", "Kotlin"}));
    mainLayout.addView(spinLang);

    final android.widget.Spinner spinSdk = new android.widget.Spinner(this);
    spinSdk.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"API 21", "API 23", "API 26"}));
    mainLayout.addView(spinSdk);

    // --- ปุ่ม Create ---
    android.widget.Button btnCreate = new android.widget.Button(this);
    btnCreate.setText("CREATE");
    btnCreate.setOnClickListener(v -> {
        // เรียกเมธอดสร้างโปรเจกต์โดยส่ง icon ไปด้วย
        createNewProject(
            etName.getText().toString(), 
            etPkg.getText().toString(), 
            spinLang.getSelectedItem().toString(), 
            spinSdk.getSelectedItem().toString(), 
            currentSelectedIcon[0] // <--- ตรงนี้คือการส่งไอคอน
        );
        refreshProjectList();
    });
    mainLayout.addView(btnCreate);

    builder.setView(mainLayout).show();
}

    // 🌟 เมธอดเวอร์ชันอัปเกรด: รับค่าตัวแปรภาษาและ SDK มาจำแนกเขียนโค้ดและสร้างโฟลเดอร์จริง
    private void createNewProject(String projectName, String packageName, String language, String minSdkVersionString) {
        String rootPath = "/sdcard/MiniStudio/" + projectName;
        
        // 1. นำค่าภาษามาสลับโฟลเดอร์ Source Code ให้ตรงตามไวยากรณ์ (src/main/java หรือ src/main/kotlin)
        String langFolder = language.toLowerCase(); // คืนค่าเป็น "java" หรือ "kotlin"
        String sourceDirPath = rootPath + "/app/src/main/" + langFolder + "/" + packageName.replace(".", "/");
        
        String[] folders = {
            sourceDirPath,
            rootPath + "/app/src/main/res/layout",
            rootPath + "/app/src/main/res/values",
            rootPath + "/app/src/main/res/drawable",
            rootPath + "/app/src/main/res/mipmap-hdpi",
            rootPath + "/app/src/main/res/mipmap-mdpi",
            rootPath + "/app/src/main/res/mipmap-xhdpi",
            rootPath + "/app/src/main/res/mipmap-xxhdpi",
            rootPath + "/app/src/main/res/mipmap-xxxhdpi"
        };

        for (String path : folders) {
            File f = new File(path);
            if (!f.exists()) f.mkdirs();
        }

        // 2. นำข้อความ SDK มาแกะเอาเฉพาะตัวเลข API ด้วย Regular Expression
        String minSdkDigits = minSdkVersionString.replaceAll("[^0-9]", "");
        int minSdk = Integer.parseInt(minSdkDigits.length() > 2 ? minSdkDigits.substring(0, 2) : minSdkDigits);

        // 3. สร้างไฟล์ AndroidManifest.xml
        String manifest = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
            "    <application \n" +
            "        android:label=\"" + projectName + "\"\n" +
            "        android:theme=\"@style/AppTheme\">\n" + 
            "        <activity android:name=\".MainActivity\" android:exported=\"true\">\n" +
            "            <intent-filter>\n" +
            "                <action android:name=\"android.intent.action.MAIN\" />\n" +
            "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
            "            </intent-filter>\n" +
            "        </activity>\n" +
            "    </application>\n" +
            "</manifest>";
        writeFile(rootPath + "/app/src/main/AndroidManifest.xml", manifest);

        // 4. สร้าง Resource Files (Layout, Strings, Colors, Styles)
        String layout = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"match_parent\"\n" +
            "    android:gravity=\"center\" \n" +
            "    android:orientation=\"vertical\">\n" +
            "    <TextView\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:text=\"Hello MiniStudio (" + language + ")!\" />\n" +
            "</LinearLayout>";
        writeFile(rootPath + "/app/src/main/res/layout/activity_main.xml", layout);

        String stringsXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n" +
            "    <string name=\"app_name\">" + projectName + "</string>\n</resources>";
        writeFile(rootPath + "/app/src/main/res/values/strings.xml", stringsXml);

        String colorsXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n" +
            "    <color name=\"purple_500\">#FF6200EE</color>\n" +
            "    <color name=\"purple_700\">#FF3700B3</color>\n" +
            "    <color name=\"teal_200\">#FF03DAC5</color>\n" +
            "</resources>";
        writeFile(rootPath + "/app/src/main/res/values/colors.xml", colorsXml);

        String stylesXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n" +
            "    <style name=\"AppTheme\" parent=\"Theme.MaterialComponents.DayNight.NoActionBar\">\n" +
            "        <item name=\"colorPrimary\">@color/purple_500</item>\n" +
            "    </style>\n</resources>";
        writeFile(rootPath + "/app/src/main/res/values/styles.xml", stylesXml);
        
        // 5. คัดแยกการเจนไฟล์ซอร์สโค้ดเริ่มต้น
        if ("Kotlin".equals(language)) {
            String kotlinCode = "package " + packageName + "\n\n" +
                "import android.app.Activity\n" +
                "import android.os.Bundle\n" +
                "import " + packageName + ".R\n\n" + 
                "class MainActivity : Activity() {\n" +
                "    override fun onCreate(savedInstanceState: Bundle?) {\n" +
                "        super.onCreate(savedInstanceState)\n" +
                "        setContentView(R.layout.activity_main)\n" +
                "    }\n" +
                "}";
            writeFile(sourceDirPath + "/MainActivity.kt", kotlinCode);
        } else {
            String javaCode = "package " + packageName + ";\n\n" +
                "import android.app.Activity;\n" +
                "import android.os.Bundle;\n" +
                "import " + packageName + ".R;\n\n" + 
                "public class MainActivity extends Activity {\n" +
                "    @Override\n" +
                "    protected void onCreate(Bundle savedInstanceState) { \n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        setContentView(R.layout.activity_main);\n" +
                "    }\n" +
                "}";
            writeFile(sourceDirPath + "/MainActivity.java", javaCode);
        }

        // 6. ส่งโครงสร้างและเตรียมค่าสำหรับส่งไปคอมไพล์บน GitHub CI/CD
        BuildEnvironmentManager envManager = new BuildEnvironmentManager(this);
        envManager.prepareGitHubWorkflow(rootPath, projectName, packageName, language, minSdk);
    }


    private void writeFile(String path, String content) {
        try {
            File file = new File(path);
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes("UTF-8"));
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_project_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_global_github_settings) {
            showGitHubSettingsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showGitHubSettingsDialog() {
        SharedPreferences prefs = getSharedPreferences("GitHubPrefs", Context.MODE_PRIVATE);
        String savedUsername = prefs.getString("username", "");
        String savedEmail = prefs.getString("email", "");
        String savedToken = prefs.getString("token", "");

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        int paddingPx = (int) (24 * getResources().getDisplayMetrics().density);
        mainLayout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        mainLayout.setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"));

        LinearLayout titleLayout = new LinearLayout(this);
        titleLayout.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        titleLayout.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⚙️ ตั้งค่าบัญชี GitHub Sync");
        tvTitle.setTextColor(android.graphics.Color.WHITE);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
        titleLayout.addView(tvTitle);
        mainLayout.addView(titleLayout);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("ข้อมูลนี้จะถูกบันทึกเพื่อใช้ส่งซอร์สโค้ดโปรเจกต์ขึ้นไปบิวด์บนคลาวด์อัตโนมัติ");
        tvDesc.setTextColor(android.graphics.Color.parseColor("#8E8E93"));
        tvDesc.setTextSize(13);
        tvDesc.setLineSpacing(0, 1.2f);
        tvDesc.setPadding(0, 0, 0, (int) (20 * getResources().getDisplayMetrics().density));
        mainLayout.addView(tvDesc);

        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        boxParams.bottomMargin = (int) (14 * getResources().getDisplayMetrics().density);

        android.graphics.drawable.GradientDrawable inputStyle = new android.graphics.drawable.GradientDrawable();
        inputStyle.setColor(android.graphics.Color.parseColor("#252526"));
        inputStyle.setCornerRadius((int) (8 * getResources().getDisplayMetrics().density));
        inputStyle.setStroke((int) (1 * getResources().getDisplayMetrics().density), android.graphics.Color.parseColor("#3F3F46"));

        int inputPadding = (int) (12 * getResources().getDisplayMetrics().density);

        // --- GitHub Username ---
        TextView labelUsername = new TextView(this);
        labelUsername.setText("GitHub Username");
        labelUsername.setTextColor(android.graphics.Color.parseColor("#D4D4D8"));
        labelUsername.setTextSize(13);
        labelUsername.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
        mainLayout.addView(labelUsername);

        final EditText etUsername = new EditText(this);
        etUsername.setHint("ระบุชื่อผู้ใช้ GitHub");
        etUsername.setHintTextColor(android.graphics.Color.parseColor("#52525B"));
        etUsername.setText(savedUsername);
        etUsername.setTextColor(android.graphics.Color.WHITE);
        etUsername.setTextSize(14);
        etUsername.setBackground(inputStyle.getConstantState().newDrawable());
        etUsername.setPadding(inputPadding, inputPadding, inputPadding, inputPadding);
        mainLayout.addView(etUsername, boxParams);

        // --- GitHub Email ---
        TextView labelEmail = new TextView(this);
        labelEmail.setText("GitHub Email");
        labelEmail.setTextColor(android.graphics.Color.parseColor("#D4D4D8"));
        labelEmail.setTextSize(13);
        labelEmail.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
        mainLayout.addView(labelEmail);

        final EditText etEmail = new EditText(this);
        etEmail.setHint("ระบุอีเมลที่ผูกกับ GitHub");
        etEmail.setHintTextColor(android.graphics.Color.parseColor("#52525B"));
        etEmail.setText(savedEmail);
        etEmail.setTextColor(android.graphics.Color.WHITE);
        etEmail.setTextSize(14);
        etEmail.setBackground(inputStyle.getConstantState().newDrawable());
        etEmail.setPadding(inputPadding, inputPadding, inputPadding, inputPadding);
        mainLayout.addView(etEmail, boxParams);

        // --- Personal Access Token ---
        TextView labelToken = new TextView(this);
        labelToken.setText("Personal Access Token (Classic)");
        labelToken.setTextColor(android.graphics.Color.parseColor("#D4D4D8"));
        labelToken.setTextSize(13);
        labelToken.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
        mainLayout.addView(labelToken);

        final EditText etToken = new EditText(this);
        etToken.setHint("วางโทเค็นสิทธิ์เข้าถึง (ghp_...)");
        etToken.setHintTextColor(android.graphics.Color.parseColor("#52525B"));
        etToken.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etToken.setText(savedToken);
        etToken.setTextColor(android.graphics.Color.WHITE);
        etToken.setTextSize(14);
        etToken.setBackground(inputStyle.getConstantState().newDrawable());
        etToken.setPadding(inputPadding, inputPadding, inputPadding, inputPadding);
        mainLayout.addView(etToken, boxParams);

        final androidx.appcompat.app.AlertDialog dialog = builder.setView(mainLayout).create();

        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(android.view.Gravity.END);
        LinearLayout.LayoutParams btnLayoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLayoutParams.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
        buttonLayout.setLayoutParams(btnLayoutParams);

        android.widget.Button btnCancel = new android.widget.Button(this, null, 0, android.R.style.Widget_Material_Button_Borderless);
        btnCancel.setText("ยกเลิก");
        btnCancel.setTextColor(android.graphics.Color.parseColor("#A1A1AA"));
        btnCancel.setTextSize(14);
        btnCancel.setAllCaps(false);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        buttonLayout.addView(btnCancel);

        android.widget.Button btnSave = new android.widget.Button(this, null, 0, android.R.style.Widget_Material_Button_Borderless);
        btnSave.setText("บันทึกข้อมูล");
        btnSave.setTextColor(android.graphics.Color.WHITE);
        btnSave.setTextSize(14);
        btnSave.setAllCaps(false);
        
        android.graphics.drawable.GradientDrawable saveBtnBg = new android.graphics.drawable.GradientDrawable();
        saveBtnBg.setColor(android.graphics.Color.parseColor("#248A3D"));
        saveBtnBg.setCornerRadius((int) (6 * getResources().getDisplayMetrics().density));
        btnSave.setBackground(saveBtnBg);
        
        LinearLayout.LayoutParams saveBtnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) (40 * getResources().getDisplayMetrics().density));
        saveBtnParams.leftMargin = (int) (12 * getResources().getDisplayMetrics().density);
        btnSave.setLayoutParams(saveBtnParams);
        btnSave.setPadding((int) (16 * getResources().getDisplayMetrics().density), 0, (int) (16 * getResources().getDisplayMetrics().density), 0);    
		
        btnSave.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String token = etToken.getText().toString().trim();

            if (username.isEmpty() || token.isEmpty()) {
                Toast.makeText(this, "❌ กรุณากรอก Username และ Token", Toast.LENGTH_LONG).show();
                return;
            }

            prefs.edit()
                .putString("username", username)
                .putString("email", email)
                .putString("token", token)
                .putBoolean("is_github_setup", true)
                .apply();

            Toast.makeText(this, "💾 บันทึกการตั้งค่าสำเร็จ", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        buttonLayout.addView(btnSave);
        mainLayout.addView(buttonLayout);

        if (dialog.getWindow() != null) {
            android.graphics.drawable.GradientDrawable dialogBg = new android.graphics.drawable.GradientDrawable();
            dialogBg.setColor(android.graphics.Color.parseColor("#1E1E1E"));
            dialogBg.setCornerRadius((int) (14 * getResources().getDisplayMetrics().density));
            dialog.getWindow().setBackgroundDrawable(dialogBg);
        }

        dialog.show();
    }
    

}
