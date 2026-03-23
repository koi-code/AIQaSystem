package com.yiwilee.aiqasystem.model.vo;

import java.time.LocalDateTime;

/**
 * 单条聊天记录视图
 */
public record ChatMessageVO(
        Long id,
        String msgRole,       // 角色："user" 或 "assistant"
        String content,       // 具体的 Markdown 文本内容
        String referenceDocs, // RAG 引用文档的 JSON 字符串 (前端可以解析成带链接的 Tag)
        LocalDateTime createTime
) {}