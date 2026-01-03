package me.ncexce.manager.repository;

import me.ncexce.manager.entity.UAssetNameCommit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Name Commit
@Repository
public interface UAssetNameCommitRepository extends JpaRepository<UAssetNameCommit, Long> {
    List<UAssetNameCommit> findByUassetFileCommitId(Long fileCommitId);
}
