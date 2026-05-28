package com.dev.ministudio.fs;

import com.dev.ministudio.model.FileNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileSystemManager {

    // Helper: จัดการการคัดกรองและเรียงลำดับไฟล์
    private static List<FileNode> processFileList(File[] files, int depth) {
        List<FileNode> nodes = new ArrayList<>();
        List<File> folders = new ArrayList<>();
        List<File> fileList = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                // กรองไฟล์ซ่อนและไฟล์ระบบ
                String name = file.getName();
                if (name.startsWith(".") || name.equals("TemporaryItems")) continue;
                
                if (file.isDirectory()) folders.add(file);
                else fileList.add(file);
            }

            // เรียงลำดับ A-Z
            folders.sort((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
            fileList.sort((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

            for (File f : folders) nodes.add(new FileNode(f, depth, true));
            for (File f : fileList) nodes.add(new FileNode(f, depth, false));
        }
        return nodes;
    }

    public static List<FileNode> loadRootDirectory(File rootDir) {
        if (rootDir != null && rootDir.exists() && rootDir.isDirectory()) {
            return processFileList(rootDir.listFiles(), 0);
        }
        return new ArrayList<>();
    }

    public static List<FileNode> loadChildren(File parentDir, int currentDepth) {
        return processFileList(parentDir.listFiles(), currentDepth + 1);
    }

    public static boolean createNewFolder(File parentDir, String folderName) {
        File newFolder = new File(parentDir, folderName);
        return !newFolder.exists() && newFolder.mkdirs();
    }

    public static boolean createNewFile(File parentDir, String fileName) {
        File newFile = new File(parentDir, fileName);
        try {
            return !newFile.exists() && newFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean renameFileOrFolder(File targetFile, String newName) {
        if (targetFile == null || !targetFile.exists()) return false;
        
        File renamedFile = new File(targetFile.getParentFile(), newName);
        // ตรวจสอบว่าชื่อใหม่ซ้ำกับที่มีอยู่หรือไม่
        return !renamedFile.exists() && targetFile.renameTo(renamedFile);
    }

    public static boolean deleteFileOrFolder(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteFileOrFolder(child);
                }
            }
        }
        return fileOrDirectory.delete();
    }

    public static void importFileToFolder(File sourceFile, File destDir) throws IOException {
        File destFile = new File(destDir, sourceFile.getName());
        
        try (FileChannel srcChannel = new FileInputStream(sourceFile).getChannel();
             FileChannel destChannel = new FileOutputStream(destFile).getChannel()) {
            destChannel.transferFrom(srcChannel, 0, srcChannel.size());
        }
    }
}
