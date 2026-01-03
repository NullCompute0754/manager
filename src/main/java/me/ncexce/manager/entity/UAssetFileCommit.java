package me.ncexce.manager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "uasset_file_commit")
public class UAssetFileCommit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String filePath;

    private int tag;
    private int legacyVersion;
    private int legacyUE3Version;
    private int fileVersionUE4;
    private int fileVersionUE5;
    private int licenseeVersion;

    private String masterUUID;
    private int auxByte1;
    private int auxByte2;
    private int entryCount;

    private String assetLocation;
    private int nameCount;
    private int nameOffset;
    private int unkCount;
    private int unkOffset;

    @ManyToOne
    @JoinColumn(name = "commit_id")
    private UAssetCommit commit;

    @OneToMany(mappedBy = "uassetFileCommit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UAssetHashEntryCommit> hashEntries;

    @OneToMany(mappedBy = "uassetFileCommit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UAssetNameCommit> names;
}
