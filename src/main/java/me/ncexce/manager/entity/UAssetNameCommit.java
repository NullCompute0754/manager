package me.ncexce.manager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "uasset_name_commit")
public class UAssetNameCommit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nameValue;

    @ManyToOne
    @JoinColumn(name = "uasset_file_commit_id")
    private UAssetFileCommit uassetFileCommit;
}
