package me.ncexce.manager.pojo;

public record CommitHistoryItem(
        String commitId,
        String message,
        String createdAt,
        String author
) {}
