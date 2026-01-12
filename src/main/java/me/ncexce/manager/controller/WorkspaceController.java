package me.ncexce.manager.controller;

import lombok.RequiredArgsConstructor;
import me.ncexce.manager.pojo.CommitRequest;
import me.ncexce.manager.pojo.ParsedUAsset;
import me.ncexce.manager.service.AssetService;
import me.ncexce.manager.service.VersionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/api/asset")
@RequiredArgsConstructor
public class WorkspaceController {
    private final AssetService assetService;
    private final VersionService versionService;

    // 返回一个包装类，包含解析结果和临时路径
    public record UploadResponse(ParsedUAsset parsed, String tempPath) {}

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
            return ResponseEntity.ok(commitId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
