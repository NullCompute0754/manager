package me.ncexce.manager.repository;

import me.ncexce.manager.entity.UAssetName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UAssetNameRepository extends JpaRepository<UAssetName, Long> {
    List<UAssetName> findByUassetId(Long uassetId);
}
