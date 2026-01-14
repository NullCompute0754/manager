package me.ncexce.manager.controller;

import lombok.RequiredArgsConstructor;
import me.ncexce.manager.pojo.CommitHistoryItem;
import me.ncexce.manager.pojo.CommitRequest;
import me.ncexce.manager.pojo.ParsedUAsset;
import me.ncexce.manager.service.AssetService;
import me.ncexce.manager.service.MergeService;
import me.ncexce.manager.service.VersionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/asset")
@RequiredArgsConstructor
public class WorkspaceController {
    private final AssetService assetService;
    private final VersionService versionService;
    private final MergeService mergeService;

    // 返回一个包装类，包含解析结果和临时路径
    public record UploadResponse(ParsedUAsset parsed, String tempPath) {}
    
    // commit接口的响应包装类
    public record CommitResponse(String commitId) {}

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            String tempPath = assetService.saveToTemp(file);
            ParsedUAsset parsed = assetService.parseUploadedFile(new File(tempPath));

            // 关键：创建一个包含结果的 Response 对象
            UploadResponse resp = new UploadResponse(parsed, tempPath);

            // 使用你已经在 AssetService 里调教好的 objectMapper 进行序列化
            String jsonResult = assetService.toJson(resp);

            // 手动设置 Content-Type 为 application/json
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(jsonResult);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/commit")
    public ResponseEntity<String> commitWorkspace(@RequestBody CommitRequest request) {
        // Request 包含消息、用户名及已上传文件的路径
        try {
            String commitId = versionService.createCommit(request);
            
            // 创建响应对象
            CommitResponse response = new CommitResponse(commitId);
            
            // 使用assetService的toJson方法将响应对象序列化为JSON字符串
            String jsonResponse = assetService.toJson(response);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(jsonResponse);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/merge")
    public ResponseEntity<?> mergeToMaster(@RequestBody Map<String, String> payload) {
        String userBranchName = payload.get("userBranchName");
        String adminMessage = payload.getOrDefault("adminMessage", "Merged by Administrator");

        if (userBranchName == null || userBranchName.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Target branch name is required"));
        }

        try {
            // Service 内部会通过 SecurityUtils 自动校验 Administrator 权限
            mergeService.mergeToMaster(userBranchName, adminMessage);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Successfully merged [" + userBranchName + "] to master"
            ));
        } catch (me.ncexce.manager.exceptions.InvalidCredentialsException e) {
            // 权限不足返回 403
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // 其他错误返回 500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Merge failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{branchName}/history")
    public List<CommitHistoryItem> getBranchHistory(
            @PathVariable String branchName) {

        return versionService.getBranchHistory(branchName);
    }

    @PostMapping("/sethead")
    public ResponseEntity<String> setCurBranchHeadByShortHash(
            @RequestParam String branchName,
            @RequestParam String shortHash) {

        return ResponseEntity.ok(versionService.setCurBranchHeadByShortHash(branchName, shortHash));
    }

}