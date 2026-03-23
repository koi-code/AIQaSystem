package com.yiwilee.aiqasystem.model.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档/知识库视图对象
 * 【安全核心】：坚决不暴露 filePath 物理路径！
 * 【性能核心】：坚决不包含 List<DocumentChunk>，确保列表加载速度！
 */
public record DocumentVO(
        Long id,
        String name,             // 显示名称
        String originalName,     // 原始文件名
        String fileType,         // 文件类型 (pdf, docx)
        Long fileSize,           // 文件大小 (字节，前端可自行转换为 KB/MB)
        Integer status,          // 状态: 0-等待, 1-处理中, 2-成功, 3-失败
        String summary,          // AI 生成的摘要
        String errorMsg,         // 失败时的报错信息
        String uploaderName,     // 上传人的名字 (扁平化处理，不返回整个 SysUser 对象)
        List<String> allowedRoles, // 允许访问的角色
        LocalDateTime createTime
) {}