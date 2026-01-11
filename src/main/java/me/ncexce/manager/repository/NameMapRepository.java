package me.ncexce.manager.repository;

import me.ncexce.manager.entity.NameMapEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NameMapRepository extends JpaRepository<NameMapEntity, Long> {
}
