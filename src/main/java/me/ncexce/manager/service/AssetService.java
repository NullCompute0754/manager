package me.ncexce.manager.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.ncexce.manager.pojo.ParsedUAsset;
import me.ncexce.manager.utils.UAssetParser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParsedUAsset parseUploadedFile(File file) throws IOException {
        return UAssetParser.parse(file);
    }

    public String saveToStorage(MultipartFile file, String username) throws IOException {
        String path = "/storage/" + username + "/" + UUID.randomUUID() + ".uasset";
        File dest = new File(path);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);
        return path;
    }
}
