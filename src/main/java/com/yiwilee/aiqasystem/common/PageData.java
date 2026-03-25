package com.yiwilee.aiqasystem.common;

import org.springframework.data.domain.Page;
import java.util.List;

/**
 * 统一分页响应体
 * 解决 Spring Data Page 直接序列化的警告，并提供干净的 JSON 结构
 */
public record PageData<T>(
        List<T> list,      // 数据列表
        long total,        // 总记录数
        int pageNum,       // 当前页码
        int pageSize,      // 每页大小
        int totalPages     // 总页数
) {
    /**
     * 静态转换工具：将 Spring Data 的 Page 对象转换为自定义的 PageData
     */
    public static <T> PageData<T> of(Page<T> page) {
        return new PageData<>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber() + 1, // Spring 内部从 0 开始，前端通常从 1 开始
                page.getSize(),
                page.getTotalPages()
        );
    }
}