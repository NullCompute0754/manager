package me.ncexce.manager.service;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.ncexce.manager.pojo.ParsedUAsset;
import me.ncexce.manager.utils.UAssetParser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class AssetService {

    // 直接实例化，不依赖外部 Bean，不使用 registerModule
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AssetService() {
        // 让 Jackson 能够识别 Record 里的字段（Record 没有传统的 Getter/Setter）
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    private final String TEMP_DIR = "storage/temp/";
    private final String UPLOAD_DIR = "storage/assets/";

    /**
     * 第一步：存临时区
     */
    public String saveToTemp(MultipartFile file) throws IOException {
        // 1. 获取项目根目录的绝对路径 (这样就不会跑进 AppData/Local/Temp 了)
        String rootPath = System.getProperty("user.dir");
        String tempDirRelative = "storage/temp/";

        // 2. 构造物理文件的绝对路径
        File dir = new File(rootPath, tempDirRelative);
        if (!dir.exists()) {
            boolean created = dir.mkdirs(); // 显式创建目录
            System.out.println("目录不存在，创建结果: " + created);
        }

        String tempFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File targetFile = new File(dir, tempFileName);

        // 3. 执行传输 (现在 targetFile 是类似 D:/project/storage/temp/... 的绝对路径)
        file.transferTo(targetFile.getAbsoluteFile());

        return targetFile.getAbsolutePath();
    }

    /**
     * 第二步：解析
     */
    public ParsedUAsset parseUploadedFile(File file) throws IOException {
        return UAssetParser.parse(file);
    }

    /**
     * 第三步：正式迁移
     */
    public String moveToPermanentStorage(String tempPath, String username) throws IOException {
        File tempFile = new File(tempPath);
        if (!tempFile.exists()) {
            throw new FileNotFoundException("临时文件不存在: " + tempPath);
        }

        String fileName = tempFile.getName();
        String userDirPath = UPLOAD_DIR + username + "/";
        File userDir = new File(userDirPath);
        if (!userDir.exists()) userDir.mkdirs();

        File permanentFile = new File(userDir, fileName);

        // 使用 NIO 的 Files.move，零拷贝
        Files.move(tempFile.toPath(), permanentFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);

        return permanentFile.getAbsolutePath();
    }

    // 辅助方法：将 Record 转为 JSON 存库
    public String toJson(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsString(obj);
    }
}