package com.yiwilee.aiqasystem.model.vo;

import java.time.LocalDateTime;

/**
 * 左侧会话列表视图
 * 【性能核心】：绝对不包含 List<ChatMessage>，保证侧边栏加载极速！
 */
public record ChatSessionVO(
        Long id,
        String title,
        LocalDateTime updateTime // 前端根据这个字段显示 "2小时前"、"昨天" 等
) {}