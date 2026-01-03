package me.ncexce.manager.service;

import lombok.RequiredArgsConstructor;
import me.ncexce.manager.entity.*;
import me.ncexce.manager.exceptions.FileOperationException;
import me.ncexce.manager.exceptions.ProjectNotFoundException;
import me.ncexce.manager.pojo.ParsedHashEntry;
import me.ncexce.manager.pojo.ParsedHashing;
import me.ncexce.manager.pojo.ParsedUAsset;
import me.ncexce.manager.repository.*;
import me.ncexce.manager.utils.UAssetParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UAssetService {

    private final UAssetProjectRepository projectRepository;
    private final UAssetCommitRepository commitRepository;
    private final UAssetFileCommitRepository fileCommitRepository;
    private final UAssetHashEntryCommitRepository hashEntryCommitRepository;
    private final UAssetNameCommitRepository nameCommitRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.base-path:./uploads}")
    private String baseUploadPath;

    /**
     * 上传文件到用户分支
     */
    public void uploadFile(Long projectId, Long userId, MultipartFile file) {
        try {
            // 验证项目存在
            UAssetProject project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ProjectNotFoundException("项目不存在"));

            // 获取或创建用户分支的最新commit
            UAssetCommit latestCommit = getOrCreateUserBranchCommit(projectId, userId);

            // 创建上传目录
            String userBranchPath = String.format("%s/%d/%d", baseUploadPath, projectId, userId);
            Path uploadDir = Paths.get(userBranchPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 保存文件到上传目录
            String fileName = file.getOriginalFilename();
            Path filePath = uploadDir.resolve(fileName);
            file.transferTo(filePath.toFile());

            // 解析uasset文件元数据
            ParsedUAsset parsedUAsset = UAssetParser.parse(filePath.toFile());

            // 保存文件元数据到数据库
            saveFileMetadata(latestCommit, fileName, filePath.toString(), parsedUAsset);

        } catch (IOException e) {
            throw new FileOperationException("文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            throw new FileOperationException("文件处理失败: " + e.getMessage());
        }
    }

    /**
     * 提交当前分支的更改
     */
    public UAssetCommit commitChanges(Long projectId, Long userId, String commitMessage) {
        // 获取当前用户分支的最新commit
        UAssetCommit currentCommit = getOrCreateUserBranchCommit(projectId, userId);

        // 创建新的commit
        UAssetCommit newCommit = new UAssetCommit();
        newCommit.setProject(currentCommit.getProject());
        newCommit.setParentCommit(currentCommit);
        newCommit.setUser(currentCommit.getUser());
        newCommit.setBranch(currentCommit.getBranch());
        newCommit.setCreatedAt(LocalDateTime.now());
        newCommit.setMessage(commitMessage);

        // 设置commit路径（用于恢复）
        String commitPath = String.format("%s/commits/%d/%s", baseUploadPath, projectId, 
                LocalDateTime.now().toString().replace(":", "-"));
        newCommit.setCommitPath(commitPath);

        // 复制当前分支的文件到commit目录
        copyFilesToCommit(currentCommit, newCommit, commitPath);

        return commitRepository.save(newCommit);
    }

    /**
     * 获取用户分支的最新commit，如果不存在则创建
     */
    private UAssetCommit getOrCreateUserBranchCommit(Long projectId, Long userId) {
        Optional<UAssetCommit> latestCommitOpt = commitRepository
                .findTopByProjectIdAndUserIdOrderByCreatedAtDesc(projectId, userId);

        if (latestCommitOpt.isPresent()) {
            return latestCommitOpt.get();
        }

        // 创建新的用户分支commit
        UAssetProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("项目不存在"));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        UAssetCommit newCommit = new UAssetCommit();
        newCommit.setProject(project);
        newCommit.setUser(user);
        newCommit.setBranch(userId.toString()); // 使用userId作为分支名
        newCommit.setCreatedAt(LocalDateTime.now());
        newCommit.setMessage("初始提交");

        return commitRepository.save(newCommit);
    }

    /**
     * 保存文件元数据到数据库
     */
    private void saveFileMetadata(UAssetCommit commit, String fileName, String filePath, ParsedUAsset parsedUAsset) {
        // 创建文件commit记录
        UAssetFileCommit fileCommit = new UAssetFileCommit();
        fileCommit.setFileName(fileName);
        fileCommit.setFilePath(filePath);
        fileCommit.setTag(parsedUAsset.tag());
        fileCommit.setLegacyVersion(parsedUAsset.legacyVersion());
        fileCommit.setLegacyUE3Version(parsedUAsset.legacyUE3Version());
        fileCommit.setFileVersionUE4(parsedUAsset.fileVersionUE4());
        fileCommit.setFileVersionUE5(parsedUAsset.fileVersionUE5());
        fileCommit.setLicenseeVersion(parsedUAsset.licenseeVersion());
        fileCommit.setMasterUUID(parsedUAsset.hashing().masterUUID());
        fileCommit.setAuxByte1(parsedUAsset.hashing().auxByte1());
        fileCommit.setAuxByte2(parsedUAsset.hashing().auxByte2());
        fileCommit.setEntryCount(parsedUAsset.hashing().entryCount());
        fileCommit.setAssetLocation(parsedUAsset.assetLocation());
        fileCommit.setNameCount(parsedUAsset.nameCount());
        fileCommit.setNameOffset(parsedUAsset.nameOffset());
        fileCommit.setUnkCount(parsedUAsset.unkCount());
        fileCommit.setUnkOffset(parsedUAsset.unkOffset());
        fileCommit.setCommit(commit);

        // 保存文件commit
        fileCommit = fileCommitRepository.save(fileCommit);

        // 保存hash entries
        saveHashEntries(fileCommit, parsedUAsset.hashing().entries());

        // 保存names
        saveNames(fileCommit, parsedUAsset.names());
    }

    /**
     * 保存hash entries
     */
    private void saveHashEntries(UAssetFileCommit fileCommit, List<ParsedHashEntry> entries) {
        if (entries != null) {
            for (ParsedHashEntry entry : entries) {
                UAssetHashEntryCommit hashEntry = new UAssetHashEntryCommit();
                hashEntry.setEntryUUID(entry.entryUUID());
                hashEntry.setEntryChecksum(entry.entryChecksum());
                hashEntry.setUassetFileCommit(fileCommit);
                hashEntryCommitRepository.save(hashEntry);
            }
        }
    }

    /**
     * 保存names
     */
    private void saveNames(UAssetFileCommit fileCommit, List<String> names) {
        if (names != null) {
            for (String name : names) {
                UAssetNameCommit nameCommit = new UAssetNameCommit();
                nameCommit.setNameValue(name);
                nameCommit.setUassetFileCommit(fileCommit);
                nameCommitRepository.save(nameCommit);
            }
        }
    }

    /**
     * 复制文件到commit目录
     */
    private void copyFilesToCommit(UAssetCommit sourceCommit, UAssetCommit targetCommit, String commitPath) {
        try {
            Path commitDir = Paths.get(commitPath);
            if (!Files.exists(commitDir)) {
                Files.createDirectories(commitDir);
            }

            // 复制当前分支的所有文件到commit目录
            List<UAssetFileCommit> files = sourceCommit.getFiles();
            if (files != null) {
                for (UAssetFileCommit file : files) {
                    Path sourcePath = Paths.get(file.getFilePath());
                    Path targetPath = commitDir.resolve(file.getFileName());
                    Files.copy(sourcePath, targetPath);
                }
            }
        } catch (IOException e) {
            throw new FileOperationException("文件复制失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户分支的文件列表
     */
    public List<UAssetFileCommit> getUserBranchFiles(Long projectId, Long userId) {
        Optional<UAssetCommit> latestCommit = commitRepository
                .findTopByProjectIdAndUserIdOrderByCreatedAtDesc(projectId, userId);
        
        if (latestCommit.isPresent()) {
            return latestCommit.get().getFiles();
        }
        return new ArrayList<>();
    }

    /**
     * 删除用户分支的文件
     */
    public boolean deleteFileFromBranch(Long projectId, Long userId, Long fileId) {
        try {
            // 获取文件记录
            UAssetFileCommit fileCommit = fileCommitRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("文件不存在"));

            // 验证文件属于当前用户分支
            UAssetCommit commit = fileCommit.getCommit();
            if (commit.getUser().getId() != userId || 
                commit.getProject().getId() != projectId) {
                throw new RuntimeException("无权删除此文件");
            }

            // 从文件系统删除文件
            Path filePath = Paths.get(fileCommit.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }

            // 从数据库删除相关记录
            // 先删除关联的hash entries和names
            if (fileCommit.getHashEntries() != null) {
                hashEntryCommitRepository.deleteAll(fileCommit.getHashEntries());
            }
            if (fileCommit.getNames() != null) {
                nameCommitRepository.deleteAll(fileCommit.getNames());
            }

            // 删除文件记录
            fileCommitRepository.delete(fileCommit);

            return true;

        } catch (Exception e) {
            throw new FileOperationException("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件的详细元信息
     */
    public Map<String, Object> getFileMetadata(Long fileId) {
        UAssetFileCommit fileCommit = fileCommitRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("文件不存在"));

        Map<String, Object> metadata = new HashMap<>();

        // 基本文件信息
        metadata.put("id", fileCommit.getId());
        metadata.put("fileName", fileCommit.getFileName());
        metadata.put("filePath", fileCommit.getFilePath());
        metadata.put("uploadTime", fileCommit.getCommit().getCreatedAt());

        // uasset文件头信息
        metadata.put("tag", fileCommit.getTag());
        metadata.put("legacyVersion", fileCommit.getLegacyVersion());
        metadata.put("legacyUE3Version", fileCommit.getLegacyUE3Version());
        metadata.put("fileVersionUE4", fileCommit.getFileVersionUE4());
        metadata.put("fileVersionUE5", fileCommit.getFileVersionUE5());
        metadata.put("licenseeVersion", fileCommit.getLicenseeVersion());

        // Hashing信息
        Map<String, Object> hashingInfo = new HashMap<>();
        hashingInfo.put("masterUUID", fileCommit.getMasterUUID());
        hashingInfo.put("auxByte1", fileCommit.getAuxByte1());
        hashingInfo.put("auxByte2", fileCommit.getAuxByte2());
        hashingInfo.put("entryCount", fileCommit.getEntryCount());
        metadata.put("hashing", hashingInfo);

        // 位置信息
        metadata.put("assetLocation", fileCommit.getAssetLocation());
        metadata.put("nameCount", fileCommit.getNameCount());
        metadata.put("nameOffset", fileCommit.getNameOffset());
        metadata.put("unkCount", fileCommit.getUnkCount());
        metadata.put("unkOffset", fileCommit.getUnkOffset());

        // Hash entries列表
        if (fileCommit.getHashEntries() != null) {
            List<Map<String, Object>> hashEntries = fileCommit.getHashEntries().stream()
                    .map((UAssetHashEntryCommit entry) -> {
                        Map<String, Object> entryInfo = new HashMap<>();
                        entryInfo.put("entryUUID", entry.getEntryUUID());
                        entryInfo.put("entryChecksum", entry.getEntryChecksum());
                        return entryInfo;
                    })
                    .collect(Collectors.toList());
            metadata.put("hashEntries", hashEntries);
        }

        // Names列表（前10个，避免数据过大）
        if (fileCommit.getNames() != null) {
            List<String> names = fileCommit.getNames().stream()
                    .map((UAssetNameCommit nameCommit) -> nameCommit.getNameValue())
                    .limit(10)
                    .collect(Collectors.toList());
            metadata.put("names", names);
            metadata.put("totalNameCount", fileCommit.getNames().size());
        }

        // 文件大小
        try {
            Path path = Paths.get(fileCommit.getFilePath());
            if (Files.exists(path)) {
                metadata.put("fileSize", Files.size(path));
                metadata.put("fileSizeFormatted", formatFileSize(Files.size(path)));
            }
        } catch (IOException e) {
            metadata.put("fileSize", 0);
            metadata.put("fileSizeFormatted", "未知");
        }

        return metadata;
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