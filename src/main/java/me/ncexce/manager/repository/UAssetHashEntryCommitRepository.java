package me.ncexce.manager.repository;

import me.ncexce.manager.entity.UAssetHashEntryCommit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// HashEntry Commit
@Repository
public interface UAssetHashEntryCommitRepository extends JpaRepository<UAssetHashEntryCommit, Long> {
    List<UAssetHashEntryCommit> findByUassetFileCommitId(Long fileCommitId);
}
