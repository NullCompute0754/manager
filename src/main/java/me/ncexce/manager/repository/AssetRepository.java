package me.ncexce.manager.repository;

import me.ncexce.manager.entity.AssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<AssetEntity, Long> {
    List<AssetEntity> findByCommitId(String commitId);
}
