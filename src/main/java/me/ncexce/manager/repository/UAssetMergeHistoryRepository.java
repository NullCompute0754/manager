package me.ncexce.manager.repository;

import me.ncexce.manager.entity.UAssetMergeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UAssetMergeHistoryRepository extends JpaRepository<UAssetMergeHistory, Long> {
    List<UAssetMergeHistory> findByTargetCommitId(Long commitId);
    List<UAssetMergeHistory> findBySourceCommitId(Long commitId);
}
