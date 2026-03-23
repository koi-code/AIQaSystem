package com.yiwilee.aiqasystem.model.vo;

/**
 * 文档分块(切片)视图对象
 */
public record DocumentChunkVO(
        Long id,
        Integer chunkIndex, // 切片序号
        Integer pageNum,    // 所在的物理页码
        String content      // 切片的具体文本内容
) {}