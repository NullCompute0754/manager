package me.ncexce.manager.repository;

import me.ncexce.manager.entity.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.lang.ScopedValue;

@Repository
public interface BranchRepository extends JpaRepository<BranchEntity, String> {
    boolean existsByName(String branchName);
}
