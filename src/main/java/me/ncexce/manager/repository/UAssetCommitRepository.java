package me.ncexce.manager.repository;

import me.ncexce.manager.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Commit
@Repository
public interface UAssetCommitRepository extends JpaRepository<UAssetCommit, Long> {
    Optional<UAssetCommit> findTopByProjectIdAndBranchOrderByCreatedAtDesc(Long projectId, String branch);
    Optional<UAssetCommit> findTopByProjectIdAndUserIdOrderByCreatedAtDesc(Long projectId, Long userId);
    List<UAssetCommit> findByProjectIdAndUserId(Long projectId, Long userId);
}

// Merge History

