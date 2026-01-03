package me.ncexce.manager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "uasset_hash_entry_commit")
public class UAssetHashEntryCommit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entryUUID;
    private int entryChecksum;

    @ManyToOne
    @JoinColumn(name = "uasset_file_commit_id")
    private UAssetFileCommit uassetFileCommit;
}
