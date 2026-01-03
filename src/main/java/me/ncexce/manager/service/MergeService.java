package me.ncexce.manager.service;

import lombok.RequiredArgsConstructor;
import me.ncexce.manager.entity.*;
import me.ncexce.manager.exceptions.FileOperationException;
import me.ncexce.manager.exceptions.ProjectNotFoundException;
import me.ncexce.manager.repository.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MergeService {

    private final UAssetProjectRepository projectRepository;
    private final UAssetCommitRepository commitRepository;
    private final UAssetMergeHistoryRepository mergeHistoryRepository;
    private final UserRepository userRepository;

    /**
     * 合并用户分支到master分支
     */
    public UAssetCommit mergeToMaster(Long projectId, Long sourceUserId, Long mergedByUserId, String mergeMessage) {
        // 验证项目存在
        UAssetProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("项目不存在"));

        // 获取master分支的最新commit
        UAssetCommit masterCommit = commitRepository
                .findTopByProjectIdAndBranchOrderByCreatedAtDesc(projectId, "master")
                .orElse(null);

        // 获取用户分支的最新commit
        UAssetCommit userCommit = commitRepository
                .findTopByProjectIdAndUserIdOrderByCreatedAtDesc(projectId, sourceUserId)
                .orElseThrow(() -> new RuntimeException("用户分支不存在"));

        // 获取执行合并的管理员
        UserEntity mergedBy = userRepository.findById(mergedByUserId)
                .orElseThrow(() -> new RuntimeException("管理员用户不存在"));

        // 创建合并结果commit
        UAssetCommit mergeResult = createMergeCommit(project, masterCommit, userCommit, mergeMessage);

        // 保存合并历史
        saveMergeHistory(masterCommit, userCommit, mergeResult, mergedBy);

        // 更新项目的master commit
        project.setMasterCommit(mergeResult);
        projectRepository.save(project);

        return mergeResult;
    }

    /**
     * 获取master分支和用户分支的diff
     */
    public Map<String, Object> getDiff(Long projectId, Long userId) {
        // 获取master分支的最新commit
        UAssetCommit masterCommit = commitRepository
                .findTopByProjectIdAndBranchOrderByCreatedAtDesc(projectId, "master")
                .orElse(null);

        // 获取用户分支的最新commit
        UAssetCommit userCommit = commitRepository
                .findTopByProjectIdAndUserIdOrderByCreatedAtDesc(projectId, userId)
                .orElseThrow(() -> new RuntimeException("用户分支不存在"));

        Map<String, Object> diffResult = new HashMap<>();

        // 比较文件差异
        List<Map<String, Object>> fileDiffs = compareFileDiffs(masterCommit, userCommit);
        diffResult.put("fileDiffs", fileDiffs);

        // 统计信息
        diffResult.put("addedFiles", countAddedFiles(masterCommit, userCommit));
        diffResult.put("modifiedFiles", countModifiedFiles(masterCommit, userCommit));
        diffResult.put("deletedFiles", countDeletedFiles(masterCommit, userCommit));

        return diffResult;
    }

    /**
     * 创建合并commit
     */
    private UAssetCommit createMergeCommit(UAssetProject project, UAssetCommit masterCommit, 
                                         UAssetCommit userCommit, String message) {
        UAssetCommit mergeCommit = new UAssetCommit();
        mergeCommit.setProject(project);
        mergeCommit.setParentCommit(masterCommit);
        mergeCommit.setBranch("master");
        mergeCommit.setCreatedAt(LocalDateTime.now());
        mergeCommit.setMessage(message);

        // 设置commit路径
        String commitPath = String.format("./uploads/commits/%d/merge-%s", 
                project.getId(), LocalDateTime.now().toString().replace(":", "-"));
        mergeCommit.setCommitPath(commitPath);

        // 合并文件（使用用户分支的文件覆盖master分支的文件）
        mergeFiles(masterCommit, userCommit, mergeCommit, commitPath);

        return commitRepository.save(mergeCommit);
    }

    /**
     * 合并文件
     */
    private void mergeFiles(UAssetCommit masterCommit, UAssetCommit userCommit, 
                           UAssetCommit mergeCommit, String commitPath) {
        try {
            Path mergeDir = Paths.get(commitPath);
            if (!Files.exists(mergeDir)) {
                Files.createDirectories(mergeDir);
            }

            // 获取master分支的文件映射
            Map<String, UAssetFileCommit> masterFiles = getFileMap(masterCommit);
            Map<String, UAssetFileCommit> userFiles = getFileMap(userCommit);

            // 合并策略：用户分支的文件覆盖master分支的同名文件
            for (Map.Entry<String, UAssetFileCommit> entry : userFiles.entrySet()) {
                String fileName = entry.getKey();
                UAssetFileCommit userFile = entry.getValue();

                // 复制文件到合并目录
                Path sourcePath = Paths.get(userFile.getFilePath());
                Path targetPath = mergeDir.resolve(fileName);
                Files.copy(sourcePath, targetPath);

                // 创建新的文件commit记录
                UAssetFileCommit mergedFile = cloneFileCommit(userFile);
                mergedFile.setCommit(mergeCommit);
                mergedFile.setFilePath(targetPath.toString());
            }

            // 保留master分支中独有的文件
            for (Map.Entry<String, UAssetFileCommit> entry : masterFiles.entrySet()) {
                String fileName = entry.getKey();
                if (!userFiles.containsKey(fileName)) {
                    UAssetFileCommit masterFile = entry.getValue();

                    // 复制文件到合并目录
                    Path sourcePath = Paths.get(masterFile.getFilePath());
                    Path targetPath = mergeDir.resolve(fileName);
                    Files.copy(sourcePath, targetPath);

                    // 创建新的文件commit记录
                    UAssetFileCommit mergedFile = cloneFileCommit(masterFile);
                    mergedFile.setCommit(mergeCommit);
                    mergedFile.setFilePath(targetPath.toString());
                }
            }

        } catch (IOException e) {
            throw new FileOperationException("文件合并失败: " + e.getMessage());
        }
    }

    /**
     * 克隆文件commit记录
     */
    private UAssetFileCommit cloneFileCommit(UAssetFileCommit source) {
        UAssetFileCommit clone = new UAssetFileCommit();
        clone.setFileName(source.getFileName());
        clone.setFilePath(source.getFilePath());
        clone.setTag(source.getTag());
        clone.setLegacyVersion(source.getLegacyVersion());
        clone.setLegacyUE3Version(source.getLegacyUE3Version());
        clone.setFileVersionUE4(source.getFileVersionUE4());
        clone.setFileVersionUE5(source.getFileVersionUE5());
        clone.setLicenseeVersion(source.getLicenseeVersion());
        clone.setMasterUUID(source.getMasterUUID());
        clone.setAuxByte1(source.getAuxByte1());
        clone.setAuxByte2(source.getAuxByte2());
        clone.setEntryCount(source.getEntryCount());
        clone.setAssetLocation(source.getAssetLocation());
        clone.setNameCount(source.getNameCount());
        clone.setNameOffset(source.getNameOffset());
        clone.setUnkCount(source.getUnkCount());
        clone.setUnkOffset(source.getUnkOffset());
        return clone;
    }

    /**
     * 获取文件映射
     */
    private Map<String, UAssetFileCommit> getFileMap(UAssetCommit commit) {
        Map<String, UAssetFileCommit> fileMap = new HashMap<>();
        if (commit != null && commit.getFiles() != null) {
            for (UAssetFileCommit file : commit.getFiles()) {
                fileMap.put(file.getFileName(), file);
            }
        }
        return fileMap;
    }

    /**
     * 保存合并历史
     */
    private void saveMergeHistory(UAssetCommit sourceCommit, UAssetCommit targetCommit, 
                                UAssetCommit resultCommit, UserEntity mergedBy) {
        UAssetMergeHistory mergeHistory = new UAssetMergeHistory();
        mergeHistory.setSourceCommit(sourceCommit);
        mergeHistory.setTargetCommit(targetCommit);
        mergeHistory.setResultCommit(resultCommit);
        mergeHistory.setMergedBy(mergedBy);
        mergeHistory.setMergedAt(LocalDateTime.now());
        mergeHistoryRepository.save(mergeHistory);
    }

    /**
     * 比较文件差异
     */
    private List<Map<String, Object>> compareFileDiffs(UAssetCommit masterCommit, UAssetCommit userCommit) {
        List<Map<String, Object>> diffs = new ArrayList<>();

        Map<String, UAssetFileCommit> masterFiles = getFileMap(masterCommit);
        Map<String, UAssetFileCommit> userFiles = getFileMap(userCommit);

        // 检查新增的文件
        for (Map.Entry<String, UAssetFileCommit> entry : userFiles.entrySet()) {
            String fileName = entry.getKey();
            if (!masterFiles.containsKey(fileName)) {
                Map<String, Object> diff = new HashMap<>();
                diff.put("fileName", fileName);
                diff.put("status", "ADDED");
                diff.put("userFile", entry.getValue());
                diffs.add(diff);
            }
        }

        // 检查修改的文件
        for (Map.Entry<String, UAssetFileCommit> entry : userFiles.entrySet()) {
            String fileName = entry.getKey();
            if (masterFiles.containsKey(fileName)) {
                UAssetFileCommit masterFile = masterFiles.get(fileName);
                UAssetFileCommit userFile = entry.getValue();
                
                if (!isFileContentEqual(masterFile, userFile)) {
                    Map<String, Object> diff = new HashMap<>();
                    diff.put("fileName", fileName);
                    diff.put("status", "MODIFIED");
                    diff.put("masterFile", masterFile);
                    diff.put("userFile", userFile);
                    diffs.add(diff);
                }
            }
        }

        // 检查删除的文件
        for (Map.Entry<String, UAssetFileCommit> entry : masterFiles.entrySet()) {
            String fileName = entry.getKey();
            if (!userFiles.containsKey(fileName)) {
                Map<String, Object> diff = new HashMap<>();
                diff.put("fileName", fileName);
                diff.put("status", "DELETED");
                diff.put("masterFile", entry.getValue());
                diffs.add(diff);
            }
        }

        return diffs;
    }

    /**
     * 检查文件内容是否相同（基于元数据）
     */
    private boolean isFileContentEqual(UAssetFileCommit file1, UAssetFileCommit file2) {
        return Objects.equals(file1.getMasterUUID(), file2.getMasterUUID()) &&
               file1.getEntryCount() == file2.getEntryCount() &&
               file1.getNameCount() == file2.getNameCount();
    }

    /**
     * 统计新增文件数量
     */
    private int countAddedFiles(UAssetCommit masterCommit, UAssetCommit userCommit) {
        Map<String, UAssetFileCommit> masterFiles = getFileMap(masterCommit);
        Map<String, UAssetFileCommit> userFiles = getFileMap(userCommit);
        
        int count = 0;
        for (Map.Entry<String, UAssetFileCommit> entry : userFiles.entrySet()) {
            if (!masterFiles.containsKey(entry.getKey())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 统计修改文件数量
     */
    private int countModifiedFiles(UAssetCommit masterCommit, UAssetCommit userCommit) {
        Map<String, UAssetFileCommit> masterFiles = getFileMap(masterCommit);
        Map<String, UAssetFileCommit> userFiles = getFileMap(userCommit);
        
        int count = 0;
        for (Map.Entry<String, UAssetFileCommit> entry : userFiles.entrySet()) {
            if (masterFiles.containsKey(entry.getKey()) && 
                !isFileContentEqual(masterFiles.get(entry.getKey()), entry.getValue())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 统计删除文件数量
     */
    private int countDeletedFiles(UAssetCommit masterCommit, UAssetCommit userCommit) {
        Map<String, UAssetFileCommit> masterFiles = getFileMap(masterCommit);
        Map<String, UAssetFileCommit> userFiles = getFileMap(userCommit);
        
        int count = 0;
        for (Map.Entry<String, UAssetFileCommit> entry : masterFiles.entrySet()) {
            if (!userFiles.containsKey(entry.getKey())) {
                count++;
            }
        }
        return count;
    }
}