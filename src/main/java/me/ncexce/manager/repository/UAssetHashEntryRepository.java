package me.ncexce.manager.repository;

import me.ncexce.manager.entity.UAssetHashEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UAssetHashEntryRepository extends JpaRepository<UAssetHashEntry, Long> {
    List<UAssetHashEntry> findByUassetId(Long uassetId);
}
