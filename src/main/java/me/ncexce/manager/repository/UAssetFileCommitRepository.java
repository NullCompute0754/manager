package me.ncexce.manager.repository;

import me.ncexce.manager.entity.UAssetFileCommit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// 文件 Commit
@Repository
public interface UAssetFileCommitRepository extends JpaRepository<UAssetFileCommit, Long> {
    List<UAssetFileCommit> findByCommitId(Long commitId);
    Optional<UAssetFileCommit> findByCommitIdAndFileName(Long commitId, String fileName);
}
