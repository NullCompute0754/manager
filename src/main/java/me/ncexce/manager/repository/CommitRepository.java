package me.ncexce.manager.repository;

import me.ncexce.manager.entity.CommitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommitRepository extends JpaRepository<CommitEntity, String> {
    List<CommitEntity> findByAuthorOrderByCreatedAtDesc(String author);
}
