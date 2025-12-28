package me.ncexce.manager.service;

import lombok.RequiredArgsConstructor;

import me.ncexce.manager.entity.*;
import me.ncexce.manager.exceptions.InvalidCredentialsException;
import me.ncexce.manager.exceptions.UserNotFoundException;
import me.ncexce.manager.pojo.ParsedHashEntry;
import me.ncexce.manager.pojo.ParsedHashing;
import me.ncexce.manager.pojo.ParsedUAsset;
import me.ncexce.manager.repository.*;
import me.ncexce.manager.utils.UAssetParser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AssetService {
    private final UAssetProjectRepository projectRepo;
    private final UAssetFileRepository fileRepo;
    private final UAssetHashEntryRepository hashEntryRepo;
    private final UAssetNameRepository nameRepo;
    private final UAssetVersionRepository versionRepo;
    private final UAssetMergeHistoryRepository mergeHistoryRepo;
    private final UserRepository userRepo;

    public UserEntity getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new InvalidCredentialsException("No authentication found");

        String username = auth.getName();
        return userRepo.findByUsername(username).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Transactional
    public UAssetVersion uploadFile(Long projectId, File uassetFile) throws IOException, IllegalArgumentException {
        UserEntity user = getCurrentUser();
        long userId = user.getId();

        ParsedUAsset parsed = UAssetParser.parse(uassetFile);

        UAssetVersion parentVersion = versionRepo
                .findTopByProjectIdAndUserIdOrderByCreatedAtDesc(projectId, userId)
                .orElse(null);

        UAssetVersion newVersion = UAssetVersion.builder()
                .project(projectRepo.findById(projectId).orElseThrow())
                .versionName(generateNextVersionName(parentVersion))
                .parentVersion(parentVersion)
                .user(userRepo.findById(userId).orElseThrow())
                .branch(String.valueOf(userId))
                .createdAt(LocalDateTime.now())
                .build();
        versionRepo.save(newVersion);

        UAssetFile dbAsset = new UAssetFile();
        dbAsset.setTag(parsed.tag());
        dbAsset.setLegacyVersion(parsed.legacyVersion());
        dbAsset.setLegacyUE3Version(parsed.legacyUE3Version());
        dbAsset.setFileVersionUE4(parsed.fileVersionUE4());
        dbAsset.setFileVersionUE5(parsed.fileVersionUE5());
        dbAsset.setLicenseeVersion(parsed.licenceeVersion());
        dbAsset.setAssetLocation(parsed.assetLocation());


        ParsedHashing hashing = parsed.hashing();

        dbAsset.setMasterUUID(hashing.masterUUID());
        dbAsset.setAuxByte1(hashing.auxByte1());
        dbAsset.setAuxByte2(hashing.auxByte2());
        dbAsset.setEntryCount(hashing.entryCount());
        fileRepo.save(dbAsset);

        UAssetHashEntry dbEntry = new UAssetHashEntry();
        for(ParsedHashEntry e : hashing.entries()) {
            dbEntry.setEntryUUID(e.entryUUID());
            dbEntry.setEntryChecksum(e.entryChecksum());
            dbEntry.setUasset(dbAsset);
            hashEntryRepo.save(dbEntry);
        }

        UAssetName dbName = new UAssetName();
        for(String s : parsed.names()) {
            dbName.setNameValue(s);
            dbName.setUasset(dbAsset);
            nameRepo.save(dbName);
        }

        return newVersion;
    }

    private String generateNextVersionName(UAssetVersion previousVersion) {
        if (previousVersion == null) return "v1";
        String lastName = previousVersion.getVersionName();
        try {
            int v = Integer.parseInt(lastName.replaceAll("\\D+", ""));
            return "v" + (v + 1);
        } catch (Exception e) {
            return lastName + "_1";
        }
    }

    private void copyFilesAndChildren(UAssetVersion source, UAssetVersion target) {

    }
}
