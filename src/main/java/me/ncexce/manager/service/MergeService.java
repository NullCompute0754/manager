package me.ncexce.manager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.ncexce.manager.entity.AssetEntity;
import me.ncexce.manager.entity.BranchEntity;
import me.ncexce.manager.entity.CommitEntity;
import me.ncexce.manager.entity.NameMapEntity;
import me.ncexce.manager.exceptions.InvalidCredentialsException;
import me.ncexce.manager.repository.AssetRepository;
import me.ncexce.manager.repository.BranchRepository;
import me.ncexce.manager.repository.CommitRepository;
import me.ncexce.manager.repository.NameMapRepository;
import me.ncexce.manager.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MergeService {
    private final BranchRepository branchRepo;
    private final CommitRepository commitRepo;
    private final AssetRepository assetRepo;
    private final NameMapRepository nameMapRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void mergeToMaster(String userBranchName, String adminMessage) throws Exception {

        if(!SecurityUtils.isBusinessAdmin()) throw new InvalidCredentialsException("Not an administrator");

        String operator = SecurityUtils.getCurrentUsername();

        // 1. 获取分支信息
        BranchEntity master = branchRepo.findById("master")
                .orElseThrow(() -> new RuntimeException("Master branch not found"));
        BranchEntity userBranch = branchRepo.findById(userBranchName)
                .orElseThrow(() -> new RuntimeException("User branch not found"));

        // 2. 获取用户分支最新的提交数据
        String userHeadId = userBranch.getHeadCommitId();
        List<AssetEntity> userAssets = assetRepo.findByCommitId(userHeadId);

        // 3. 创建合并提交 (变基逻辑：Parent 指向当前 Master 的 HEAD)
        String newCommitId = UUID.randomUUID().toString();
        CommitEntity mergeCommit = new CommitEntity();
        mergeCommit.setId(newCommitId);
        mergeCommit.setParentId(master.getHeadCommitId()); // 关键：变基点
        mergeCommit.setAuthor(operator);
        mergeCommit.setMessage(adminMessage);
        mergeCommit.setCreatedAt(LocalDateTime.now());
        commitRepo.save(mergeCommit);

        // 4. 将用户分支的资产“克隆”到新提交下
        for (AssetEntity userAsset : userAssets) {
            // 获取对应的 NameMap
            NameMapEntity userMap = nameMapRepo.findById(userAsset.getId()).orElseThrow();

            // 创建新的 Asset 记录 (建立快照)
            AssetEntity newAsset = copyAsset(userAsset, newCommitId);
            AssetEntity savedAsset = assetRepo.save(newAsset);

            // 映射 NameMap
            NameMapEntity newMap = new NameMapEntity();
            newMap.setAssetId(savedAsset.getId());
            newMap.setNameMapJson(userMap.getNameMapJson());
            newMap.setEntryCount(userMap.getEntryCount());
            nameMapRepo.save(newMap);
        }

        // 5. 原子性移动 Master 指针
        master.setHeadCommitId(newCommitId);
        master.setUpdatedAt(LocalDateTime.now());
        branchRepo.save(master);
    }

    private AssetEntity copyAsset(AssetEntity source, String targetCommitId) {
        AssetEntity target = new AssetEntity();
        target.setCommitId(targetCommitId);
        target.setFileName(source.getFileName());
        target.setTag(source.getTag());
        target.setFileVersionUE5(source.getFileVersionUE5());
        target.setAssetLocation(source.getAssetLocation());
        target.setHashingJson(source.getHashingJson());
        target.setPhysicalPath(source.getPhysicalPath());
        return target;
    }
}
