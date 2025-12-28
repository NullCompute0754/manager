package me.ncexce.manager.repository;

import me.ncexce.manager.entity.UAssetVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UAssetVersionRepository extends JpaRepository<UAssetVersion, Long> {
    Optional<UAssetVersion> findTopByProjectIdAndUserIdOrderByCreatedAtDesc(Long projectId, Long userId);
    Optional<UAssetVersion> findTopByProjectIdAndBranchOrderByCreatedAtDesc(Long projectId, String branch);
    List<UAssetVersion> findByProjectIdAndUserId(Long projectId, Long userId);
}
