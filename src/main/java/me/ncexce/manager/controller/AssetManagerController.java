package me.ncexce.manager.controller;

import lombok.RequiredArgsConstructor;
import me.ncexce.manager.entity.UAssetFileCommit;
import me.ncexce.manager.service.UAssetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/asset-manager")
@RequiredArgsConstructor
public class AssetManagerController {

    private final UAssetService uAssetService;

    /**
     * 获取用户分支的文件列表（包含元信息摘要）
     */
    @GetMapping("/files/{projectId}/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getFilesWithMetadata(
            @PathVariable Long projectId,
            @PathVariable Long userId) {
        
        List<UAssetFileCommit> files = uAssetService.getUserBranchFiles(projectId, userId);
        
        // 转换为前端需要的格式
        List<Map<String, Object>> fileList = files.stream().map(file -> {
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("id", file.getId());
            fileInfo.put("fileName", file.getFileName());
            fileInfo.put("filePath", file.getFilePath());
            fileInfo.put("uploadTime", file.getCommit().getCreatedAt());
            
            // 获取文件大小
            try {
                java.nio.file.Path path = java.nio.file.Paths.get(file.getFilePath());
                long fileSize = java.nio.file.Files.size(path);
                fileInfo.put("fileSize", fileSize);
                fileInfo.put("fileSizeFormatted", formatFileSize(fileSize));
            } catch (Exception e) {
                fileInfo.put("fileSize", 0);
                fileInfo.put("fileSizeFormatted", "未知");
            }
            
            // 元信息摘要（用于hover显示）
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("masterUUID", file.getMasterUUID());
            metadata.put("entryCount", file.getEntryCount());
            metadata.put("nameCount", file.getNameCount());
            metadata.put("fileVersionUE5", file.getFileVersionUE5());
            metadata.put("assetLocation", file.getAssetLocation());
            
            fileInfo.put("metadata", metadata);
            return fileInfo;
        }).toList();
        
        return ResponseEntity.ok(fileList);
    }

    /**
     * 上传文件到用户分支
     */
    @PostMapping("/upload/{projectId}/{userId}")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) {
        
        uAssetService.uploadFile(projectId, userId, file);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "文件上传成功");
        response.put("fileName", file.getOriginalFilename());
        response.put("fileSize", file.getSize());
        response.put("fileSizeFormatted", formatFileSize(file.getSize()));
        
        return ResponseEntity.ok(response);
    }

    /**
     * 删除用户分支的文件
     */
    @DeleteMapping("/file/{projectId}/{userId}/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @PathVariable Long fileId) {
        
        boolean deleted = uAssetService.deleteFileFromBranch(projectId, userId, fileId);
        
        Map<String, Object> response = new HashMap<>();
        if (deleted) {
            response.put("success", true);
            response.put("message", "文件删除成功");
        } else {
            response.put("success", false);
            response.put("message", "文件删除失败");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取文件的详细元信息（用于hover显示）
     */
    @GetMapping("/file/metadata/{fileId}")
    public ResponseEntity<Map<String, Object>> getFileMetadata(@PathVariable Long fileId) {
        Map<String, Object> metadata = uAssetService.getFileMetadata(fileId);
        return ResponseEntity.ok(metadata);
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
}