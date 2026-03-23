package com.yiwilee.aiqasystem.controller;

import com.yiwilee.aiqasystem.common.Result;
import com.yiwilee.aiqasystem.model.vo.DocumentVO;
import com.yiwilee.aiqasystem.service.DocumentService;
import com.yiwilee.aiqasystem.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库文档管理控制器
 * 负责私有知识库文档的物理上传、状态查询及级联清理
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "04. 知识库管理", description = "私有知识库文档的上传、解析及展示")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传并解析文档", description = "物理存盘 -> 文本切片 -> 大模型 Embedding -> 写入 Milvus。该接口耗时较长，前端建议展示进度条。")
    public Result<DocumentVO> uploadDocument(
            @Parameter(description = "待上传的物理文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "允许访问的角色(可多选，如 ROLE_STUDENT)") @RequestParam(value = "allowedRoles", required = false) List<String> allowedRoles) {

        // 核心安全点：从 Security 上下文获取当前操作人 ID
        Long uploaderId = SecurityUtils.getCurrentUserId();

        log.info("用户 [{}] 发起文件上传请求: {}, 文件大小: {} bytes",
                uploaderId, file.getOriginalFilename(), file.getSize());

        // 交给 Service 层执行耗时的上传、解析与向量化流程
        DocumentVO documentVO = documentService.uploadAndParseDocument(file, allowedRoles, uploaderId);

        return Result.success("文件上传并解析成功", documentVO);
    }

    @GetMapping
    @Operation(summary = "分页查询文档库", description = "支持按文件名模糊搜索及文档解析状态过滤")
    public Result<Page<DocumentVO>> pageDocuments(
            @Parameter(description = "搜索关键词(匹配文件名)") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态: 0-等待, 1-处理中, 2-成功, 3-失败") @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        // 调用 Service 获取脱敏后的分页 VO 数据
        Page<DocumentVO> documentVOPage = documentService.pageDocuments(keyword, status, pageNum, pageSize);

        return Result.success(documentVOPage);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档", description = "同步清理本地物理文件、MySQL 切片数据及 Milvus 向量数据")
    public Result<Void> deleteDocument(
            @Parameter(description = "文档主键 ID") @PathVariable("id") Long documentId) {

        // 核心防越权点：提取当前用户 ID 传给 Service
        Long userId = SecurityUtils.getCurrentUserId();

        log.warn("用户 [{}] 尝试删除知识库文件 [{}]", userId, documentId);

        // Service 层内部会校验该文档是否属于当前用户（或当前用户是否为超管）
        documentService.deleteDocument(documentId, userId);

        return Result.success("文档及关联向量数据已彻底删除", null);
    }
}