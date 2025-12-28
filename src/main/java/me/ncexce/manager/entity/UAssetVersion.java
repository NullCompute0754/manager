package me.ncexce.manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "uasset_version")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UAssetVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private UAssetProject project;

    private String versionName; // v1, v2 等

    @ManyToOne
    @JoinColumn(name = "parent_version_id")
    private UAssetVersion parentVersion;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user; // 用户分支标识

    @Column(nullable = false)
    private String branch; // master 或 userId

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 一个版本对应多个文件
    @OneToMany(mappedBy = "version")
    private List<UAssetFile> files;
}
