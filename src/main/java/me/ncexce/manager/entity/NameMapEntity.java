package me.ncexce.manager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity @Getter @Setter
@Table(name = "name_map_storage")
public class NameMapEntity {
    @Id
    private Long assetId; // 对应 AssetEntity 的 ID
    
    @Column(columnDefinition = "LONGTEXT")
    private String nameMapJson; // List<String> 序列化
    
    private int entryCount;
}