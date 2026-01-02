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
import java.util.List;
import java.util.Optional;

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
}