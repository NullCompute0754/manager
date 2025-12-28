package me.ncexce.manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "uasset_name")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UAssetName {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String nameValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uasset_id")
    private UAssetFile uasset;
}
