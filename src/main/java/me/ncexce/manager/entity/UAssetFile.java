package me.ncexce.manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Data
@Table(name = "uasset_file")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UAssetFile {
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
    @JoinColumn(name = "version_id")
    private UAssetVersion version;

    @OneToMany(mappedBy = "uasset")
    private List<UAssetHashEntry> hashEntries;

    @OneToMany(mappedBy = "uasset")
    private List<UAssetName> names;
}
