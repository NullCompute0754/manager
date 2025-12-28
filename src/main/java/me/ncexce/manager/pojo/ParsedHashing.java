package me.ncexce.manager.pojo;

import java.util.List;

public record ParsedHashing(
        String masterUUID,
        int auxByte1,
        int auxByte2,
        int entryCount,
        List<ParsedHashEntry> entries
) {}
