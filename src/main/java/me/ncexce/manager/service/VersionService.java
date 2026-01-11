package me.ncexce.manager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.ncexce.manager.entity.AssetEntity;
import me.ncexce.manager.entity.BranchEntity;
import me.ncexce.manager.entity.CommitEntity;
import me.ncexce.manager.entity.NameMapEntity;
import me.ncexce.manager.pojo.ParsedUAsset;
import me.ncexce.manager.repository.AssetRepository;
import me.ncexce.manager.repository.BranchRepository;
import me.ncexce.manager.repository.CommitRepository;
import me.ncexce.manager.repository.NameMapRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VersionService {
    private final CommitRepository commitRepo;
    private final AssetRepository assetRepo;
    private final NameMapRepository nameMapRepo;
    private final BranchRepository branchRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public String createCommit(String username, String message, List<ParsedUAsset> parsedAssets, List<String> paths) throws Exception {
        String commitId = UUID.randomUUID().toString();

        // 1. 获取当前分支
        BranchEntity branch = branchRepo.findById(username).orElseThrow();

        // 2. 创建 Commit 记录
        CommitEntity commit = new CommitEntity();
        commit.setId(commitId);
        commit.setParentId(branch.getHeadCommitId());
        commit.setAuthor(username);
        commit.setMessage(message);
        commit.setCreatedAt(LocalDateTime.now());
        commitRepo.save(commit);

        // 3. 遍历保存资产元数据
        for (int i = 0; i < parsedAssets.size(); i++) {
            ParsedUAsset p = parsedAssets.get(i);
            AssetEntity asset = new AssetEntity();
            // ... 映射 ParsedUAsset 到 AssetEntity ...
            asset.setCommitId(commitId);
            asset.setPhysicalPath(paths.get(i));
            asset.setHashingJson(objectMapper.writeValueAsString(p.hashing()));
            AssetEntity savedAsset = assetRepo.save(asset);

            // 4. 保存 NameMap
            NameMapEntity nme = new NameMapEntity();
            nme.setAssetId(savedAsset.getId());
            nme.setNameMapJson(objectMapper.writeValueAsString(p.names()));
            nme.setEntryCount(p.nameCount());
            nameMapRepo.save(nme);
        }

        // 5. 移动 Branch 指针
        branch.setHeadCommitId(commitId);
        branch.setUpdatedAt(LocalDateTime.now());
        branchRepo.save(branch);

        return commitId;
    }
}
