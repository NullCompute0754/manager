package me.ncexce.manager.pojo;

import java.util.List;

public record CommitRequest(
        String username,
        String message,
        List<ParsedUAsset> assets, // 关键：这是前端已经拿到的解析结果
        List<String> paths         // 资产在服务器上的临时物理路径
) {}