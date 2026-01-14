package me.ncexce.manager.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity @Getter @Setter
@Table(name = "branch")
public class BranchEntity {
    @Id
    private String name; // master, user_a
    private String headCommitId;
    private boolean isMaster;
    private LocalDateTime updatedAt;
}
