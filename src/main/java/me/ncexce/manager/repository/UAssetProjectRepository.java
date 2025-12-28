package me.ncexce.manager.repository;

import me.ncexce.manager.entity.UAssetProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UAssetProjectRepository extends JpaRepository<UAssetProject, Long> {

}
