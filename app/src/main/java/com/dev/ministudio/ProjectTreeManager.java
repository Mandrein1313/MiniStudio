package com.dev.ministudio;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dev.ministudio.fs.FileSystemManager;
import com.dev.ministudio.model.FileNode;
import com.dev.ministudio.model.ProjectModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import io.github.rosemoe.sora.langs.java.JavaLanguage;

public class ProjectTreeManager {

    private final MainActivity activity;
    private final ListView treeView;
    private List<FileNode> masterFileList = new ArrayList<>();
    private FileTreeAdapter fileTreeAdapter;
    private int lastClickedPosition = -1;
    private File folderForImport = null;

    public ProjectTreeManager(MainActivity activity, ListView treeView) {
        this.activity = activity;
        this.treeView = treeView;
    }

    public void initializeFileTree() {
        ProjectModel currentProject = activity.getCurrentProject();
        if (currentProject == null) return;

        File projectRoot = new File(currentProject.getRootPath());
        masterFileList = FileSystemManager.loadRootDirectory(projectRoot);

        fileTreeAdapter = new FileTreeAdapter(activity, masterFileList);
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
                    activity.getDialogManager().showImageViewerDialog(selectedNode.file);
                } else {
                    fileTreeAdapter.setSelectedPosition(position);
                    currentProject.setCurrentOpenFile(selectedNode.file);
                    openFile(selectedNode.file);
                    activity.getDrawerLayout().closeDrawers();
                }
            }
        });

        treeView.setOnItemLongClickListener((parent, view, position, id) -> {
            FileNode selectedNode = masterFileList.get(position);
            File currentFile = selectedNode.file;
            lastClickedPosition = position;

            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
            View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_bottom_file_menu, null);
            bottomSheetDialog.setContentView(dialogView);

            TextView tvHeader = dialogView.findViewById(R.id.tvDialogHeader);
            LinearLayout menuContainer = dialogView.findViewById(R.id.menuContainer);

            tvHeader.setText(selectedNode.isDirectory ? "จัดการโฟลเดอร์: " + currentFile.getName() : "จัดการไฟล์: " + currentFile.getName());

            List<MainActivity.MenuOption> options = new ArrayList<>();
            options.add(new MainActivity.MenuOption("สร้างไฟล์ใหม่", android.R.drawable.ic_menu_add));
            options.add(new MainActivity.MenuOption("สร้างโฟลเดอร์ใหม่", android.R.drawable.ic_menu_preferences)); 
            options.add(new MainActivity.MenuOption("เปลี่ยนชื่อ", android.R.drawable.ic_menu_edit));
            options.add(new MainActivity.MenuOption("ลบ", android.R.drawable.ic_menu_delete));
            
            if (selectedNode.isDirectory) {
                options.add(new MainActivity.MenuOption("นำเข้าไฟล์ (Import)", android.R.drawable.ic_menu_share));
            }

            for (MainActivity.MenuOption option : options) {
                View itemView = activity.getLayoutInflater().inflate(R.layout.dialog_menu_item, null);
                ImageView imgIcon = itemView.findViewById(R.id.menuIcon);
                TextView tvTitle = itemView.findViewById(R.id.menuTitle);

                tvTitle.setText(option.title);
                imgIcon.setImageResource(option.iconRes);

                itemView.setOnClickListener(v -> {
                    bottomSheetDialog.dismiss(); 
                    if (option.title.equals("สร้างไฟล์ใหม่")) {
                        activity.getDialogManager().showCreateFileDialog(selectedNode.isDirectory ? currentFile : currentFile.getParentFile(), selectedNode.isDirectory ? selectedNode : findParentNode(selectedNode));
                    } else if (option.title.equals("สร้างโฟลเดอร์ใหม่")) {
                        activity.getDialogManager().showCreateFolderDialog(selectedNode.isDirectory ? currentFile : currentFile.getParentFile(), selectedNode.isDirectory ? selectedNode : findParentNode(selectedNode));
                    } else if (option.title.equals("เปลี่ยนชื่อ")) {
                        activity.getDialogManager().showRenameDialog(currentFile, selectedNode);
                    } else if (option.title.equals("ลบ")) {
                        activity.getDialogManager().showDeleteConfirmationDialog(currentFile.getName(), () -> {
                            boolean success = FileSystemManager.deleteFileOrFolder(currentFile);
                            if (success) {
                                activity.showToast("ลบสำเร็จแล้ว");
                                masterFileList.remove(position);
                                if (fileTreeAdapter != null) {
                                    fileTreeAdapter.setSelectedPosition(-1);
                                    fileTreeAdapter.notifyDataSetChanged();
                                }
                            } else {
                                activity.showToast("ลบไม่สำเร็จ");
                            }
                        });
                    } else if (option.title.equals("นำเข้าไฟล์ (Import)")) {
                        folderForImport = currentFile; 
                        activity.openFilePicker(); 
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

    public void refreshFileTree() {
        ProjectModel currentProject = activity.getCurrentProject();
        if (currentProject != null) {
            File projectRoot = new File(currentProject.getRootPath());
            masterFileList.clear(); 
            masterFileList.addAll(FileSystemManager.loadRootDirectory(projectRoot));
            if (fileTreeAdapter != null) fileTreeAdapter.notifyDataSetChanged();
        }
    }

    public void openFile(File file) {
        if (file == null || !file.exists()) return;
        ProjectModel currentProject = activity.getCurrentProject();

        try {
            activity.getAutoSaveHandler().removeCallbacks(activity.getSaveRunnable());

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

            activity.runOnUiThread(() -> {
                if (activity.getCodeEditor() != null) {
                    activity.getCodeEditor().setText(fileContent);
                    activity.getCodeEditor().setEditorLanguage(new JavaLanguage());
                }
                
                activity.updateFilePathStatus(file);
                if (activity.getTabAdapter() != null) activity.getTabAdapter().notifyDataSetChanged();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveFile() {
        ProjectModel currentProject = activity.getCurrentProject();
        if (currentProject == null || currentProject.getCurrentOpenFile() == null) return;
        File fileToSave = currentProject.getCurrentOpenFile();
        try {
            FileOutputStream fos = new FileOutputStream(fileToSave);
            fos.write(activity.getCodeEditor().getText().toString().getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }
}
