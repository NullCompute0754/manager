package me.ncexce.manager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "uasset_commit")
public class UAssetCommit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private UAssetProject project;

    @ManyToOne
    @JoinColumn(name = "parent_commit_id")
    private UAssetCommit parentCommit;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user; // 分支对应 userId，master 为 null 或特定用户

    @Column(nullable = false)
    private String branch; // userId 或 "master"

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private String message; // commit 描述

    @Column(name = "commit_path") // 数据库字段名，可自定义
    private String commitPath;

    @OneToMany(mappedBy = "commit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UAssetFileCommit> files;
}
