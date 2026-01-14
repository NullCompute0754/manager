package me.ncexce.manager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity @Getter @Setter
@Table(name = "asset_metadata")
public class AssetEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String commitId;
    private String fileName;
    
    // 从 ParsedUAsset 映射的字段
    private int tag;
    private int fileVersionUE5;
    private String assetLocation;
    
    @Column(columnDefinition = "TEXT")
    private String hashingJson; // 将 ParsedHashing 序列化为 JSON
    
    private String physicalPath; // 服务器文件路径
}