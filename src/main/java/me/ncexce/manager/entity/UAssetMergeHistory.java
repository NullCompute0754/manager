package me.ncexce.manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "uasset_merge_history")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UAssetMergeHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_version_id")
    private UAssetVersion sourceVersion;

    @ManyToOne
    @JoinColumn(name = "target_version_id")
    private UAssetVersion targetVersion;

    @ManyToOne
    @JoinColumn(name = "result_version_id")
    private UAssetVersion resultVersion;

    private LocalDateTime mergedAt;
}
