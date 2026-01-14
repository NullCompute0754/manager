package me.ncexce.manager.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity @Getter @Setter
@Table(name = "commit_history")
public class CommitEntity {
    @Id
    private String id; // UUID
    private String parentId;
    private String message;
    private String author;
    private LocalDateTime createdAt;
    private String summaryHash; // 对整个提交内容的校验和
}