package me.ncexce.manager.controller;

import lombok.RequiredArgsConstructor;
import me.ncexce.manager.entity.UAssetCommit;
import me.ncexce.manager.entity.UAssetFileCommit;
import me.ncexce.manager.entity.UAssetProject;
import me.ncexce.manager.service.MergeService;
import me.ncexce.manager.service.ProjectService;
import me.ncexce.manager.service.UAssetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/uasset")
@RequiredArgsConstructor
public class UAssetController {

    private final UAssetService uAssetService;
    private final ProjectService projectService;
    private final MergeService mergeService;

    /**
     * 上传文件到用户分支
     */
    @PostMapping("/upload/{projectId}/{userId}")
    public ResponseEntity<?> uploadFile(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) {
        uAssetService.uploadFile(projectId, userId, file);
        return ResponseEntity.ok().body("文件上传成功");
    }

    /**
     * 提交当前分支的更改
     */
    @PostMapping("/commit/{projectId}/{userId}")
    public ResponseEntity<UAssetCommit> commitChanges(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @RequestParam String message) {
        UAssetCommit commit = uAssetService.commitChanges(projectId, userId, message);
        return ResponseEntity.ok(commit);
    }

    /**
     * 获取用户分支的文件列表
     */
    @GetMapping("/files/{projectId}/{userId}")
    public ResponseEntity<List<UAssetFileCommit>> getUserBranchFiles(
            @PathVariable Long projectId,
            @PathVariable Long userId) {
        List<UAssetFileCommit> files = uAssetService.getUserBranchFiles(projectId, userId);
        return ResponseEntity.ok(files);
    }

    /**
     * 合并用户分支到master分支
     */
    @PostMapping("/merge/{projectId}/{sourceUserId}/{mergedByUserId}")
    public ResponseEntity<UAssetCommit> mergeToMaster(
            @PathVariable Long projectId,
            @PathVariable Long sourceUserId,
            @PathVariable Long mergedByUserId,
            @RequestParam String message) {
        UAssetCommit result = mergeService.mergeToMaster(projectId, sourceUserId, mergedByUserId, message);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取master分支和用户分支的diff
     */
    @GetMapping("/diff/{projectId}/{userId}")
    public ResponseEntity<Map<String, Object>> getDiff(
            @PathVariable Long projectId,
            @PathVariable Long userId) {
        Map<String, Object> diff = mergeService.getDiff(projectId, userId);
        return ResponseEntity.ok(diff);
    }

    /**
     * 创建新项目
     */
    @PostMapping("/project")
    public ResponseEntity<UAssetProject> createProject(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam Long createdByUserId) {
        UAssetProject project = projectService.createProject(name, description, createdByUserId);
        return ResponseEntity.ok(project);
    }

    /**
     * 获取所有项目
     */
    @GetMapping("/projects")
    public ResponseEntity<List<UAssetProject>> getAllProjects() {
        List<UAssetProject> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    /**
     * 获取项目详情
     */
    @GetMapping("/project/{id}")
    public ResponseEntity<UAssetProject> getProject(@PathVariable Long id) {
        UAssetProject project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }
}