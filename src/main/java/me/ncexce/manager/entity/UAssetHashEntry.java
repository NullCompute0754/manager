package me.ncexce.manager.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Data
@Table(name = "uasset_hash_entry")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UAssetHashEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entryUUID;
    private int entryChecksum;

    @ManyToOne
    @JoinColumn(name = "uasset_id")
    private UAssetFile uasset;
}
