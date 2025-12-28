package me.ncexce.manager.pojo;

import java.util.List;

public record ParsedUAsset(
        int tag,
        int legacyVersion,
        int legacyUE3Version,
        int fileVersionUE4,
        int fileVersionUE5,
        int licenceeVersion,
        ParsedHashing hashing,
        String assetLocation,
        int nameCount,
        int nameOffset,
        int unkCount,
        int unkOffset,
        List<String> names
) {}
