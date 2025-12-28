package me.ncexce.manager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "uasset_merge_history")
public class UAssetMergeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_commit_id")
    private UAssetCommit sourceCommit;

    @ManyToOne
    @JoinColumn(name = "target_commit_id")
    private UAssetCommit targetCommit;

    @ManyToOne
    @JoinColumn(name = "result_commit_id")
    private UAssetCommit resultCommit;

    private LocalDateTime mergedAt;
}
