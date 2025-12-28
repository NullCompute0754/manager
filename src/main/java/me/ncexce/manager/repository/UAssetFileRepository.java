package me.ncexce.manager.repository;

import me.ncexce.manager.entity.UAssetFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UAssetFileRepository extends JpaRepository<UAssetFile, Long> {
    List<UAssetFile> findByVersionId(Long versionId);
    Optional<UAssetFile> findByVersionIdAndFileName(Long versionId, String fileName);
}
