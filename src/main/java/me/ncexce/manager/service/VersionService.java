package me.ncexce.manager.service;

import lombok.RequiredArgsConstructor;
import me.ncexce.manager.entity.AssetEntity;
import me.ncexce.manager.entity.BranchEntity;
import me.ncexce.manager.entity.CommitEntity;
import me.ncexce.manager.entity.NameMapEntity;
import me.ncexce.manager.pojo.CommitRequest;
import me.ncexce.manager.pojo.ParsedUAsset;
import me.ncexce.manager.repository.AssetRepository;
import me.ncexce.manager.repository.BranchRepository;
import me.ncexce.manager.repository.CommitRepository;
import me.ncexce.manager.repository.NameMapRepository;
import me.ncexce.manager.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VersionService {

    private final CommitRepository commitRepo;
    private final AssetRepository assetRepo;
    private final NameMapRepository nameMapRepo;
    private final BranchRepository branchRepo;
    private final AssetService assetService;

    @Transactional(rollbackFor = Exception.class)
    public String createCommit(CommitRequest request) throws Exception {

        String operator = SecurityUtils.getCurrentUsername();

        // 1. 获取分支
        BranchEntity branch = branchRepo.findById(operator)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        // 2. 创建 Commit
        String commitId = UUID.randomUUID().toString();
        CommitEntity commit = new CommitEntity();
        commit.setId(commitId);
        commit.setParentId(branch.getHeadCommitId());
        commit.setAuthor(operator);
        commit.setMessage(request.message());
        commit.setCreatedAt(LocalDateTime.now());
        commitRepo.save(commit);

        // 3. 循环处理资产
        for (int i = 0; i < request.assets().size(); i++) {
            ParsedUAsset parsed = request.assets().get(i);
            String tempPath = request.paths().get(i);

            // 迁移文件
            String permanentPath = assetService.moveToPermanentStorage(tempPath, operator);

            // 保存 Asset 基础信息
            AssetEntity asset = new AssetEntity();
            asset.setCommitId(commitId);
            asset.setFileName(new File(permanentPath).getName());
            asset.setPhysicalPath(permanentPath);

            // 映射 Record 数据
            asset.setTag(parsed.tag());
            asset.setFileVersionUE5(parsed.fileVersionUE5());
            asset.setAssetLocation(parsed.assetLocation());
            asset.setHashingJson(assetService.toJson(parsed.hashing()));

            // 【关键】使用 saveAndFlush 确保立即获得数据库分配的 ID
            AssetEntity savedAsset = assetRepo.saveAndFlush(asset);

            // 保存 NameMap
            NameMapEntity nme = new NameMapEntity();
            nme.setAssetId(savedAsset.getId()); // 绑定 AssetId
            nme.setNameMapJson(assetService.toJson(parsed.names())); // 存入解析出的 List<String>
            nme.setEntryCount(parsed.nameCount());

            nameMapRepo.save(nme);
        }

        // 4. 移动 HEAD 指针
        branch.setHeadCommitId(commitId);
        branch.setUpdatedAt(LocalDateTime.now());
        branchRepo.save(branch);

        return commitId;
    }
}