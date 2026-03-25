package com.yiwilee.aiqasystem.controller;

import com.yiwilee.aiqasystem.common.PageData;
import com.yiwilee.aiqasystem.common.Result;
import com.yiwilee.aiqasystem.constant.ApiVersion;
import com.yiwilee.aiqasystem.model.vo.DocumentChunkVO;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库文档管理控制器
 * 负责私有知识库文档的物理上传、状态查询及级联清理
 */
@Slf4j
@RestController
@RequestMapping(ApiVersion.BASE_VERSION+"/documents")
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
    public Result<PageData<DocumentVO>> pageDocuments(
            @Parameter(description = "搜索关键词(匹配文件名)") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态: 0-等待, 1-处理中, 2-成功, 3-失败") @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        // 获取原始的 Page 对象
        Page<DocumentVO> documentVOPage = documentService.pageDocuments(keyword, status, pageNum, pageSize);

        // 使用 PageData.of() 包装，返回结构化、清爽的 JSON
        return Result.success(PageData.of(documentVOPage));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档", description = "同步清理本地物理文件、MySQL 切片数据及 Milvus 向量数据")
    // 💡 核心魔法：单资源操作前，直接通过注解拦截越权请求
    @PreAuthorize("hasRole('ADMIN') or @docAuth.isOwner(#id)")
    public Result<Void> deleteDocument(
            @Parameter(description = "文档主键 ID") @PathVariable("id") Long id) {

        // 鉴权已通过，直接调用 Service 执行删除，无需再传 userId
        documentService.deleteDocument(id);
        return Result.success("文档及关联向量数据已彻底删除", null);
    }

    // ==========================================
    // 列表分页查询
    // ==========================================
    @GetMapping("/list")
    @Operation(summary = "分页获取文档列表", description = "智能路由：普通用户获取个人文档；管理员若传 targetUserId 则获取指定用户文档，不传则获取全量。")
    public Result<PageData<DocumentVO>> getDocuments(
            @Parameter(description = "目标用户ID（仅管理员可用）") @RequestParam(required = false) Long targetUserId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();

        Long finalUserId = (isAdmin && targetUserId != null) ? targetUserId : (isAdmin ? null : currentUserId);

        Page<DocumentVO> page = documentService.pageDocumentsByUserId(finalUserId, pageNum, pageSize);
        return Result.success(PageData.of(page));
    }

    // ==========================================
    // 💡 附加接口 1：查看单文档详情
    // ==========================================
    @GetMapping("/{id}")
    @Operation(summary = "获取文档详情", description = "获取单个文档的元数据和状态信息")
    @PreAuthorize("hasRole('ADMIN') or @docAuth.isOwner(#id)")
    public Result<DocumentVO> getDocumentById(@PathVariable("id") Long documentId) {

        DocumentVO documentVO = documentService.getDocumentById(documentId);
        return Result.success(documentVO);
    }

    // ==========================================
    // 💡 附加接口 2：获取文档解析切片 (RAG 核心调试接口)
    // ==========================================
    @GetMapping("/{id}/chunks")
    @Operation(summary = "获取文档文本切片", description = "返回文档被拆分后的 Chunk 列表，常用于 RAG 检索质量调优和人工核对")
    @PreAuthorize("hasRole('ADMIN') or @docAuth.isOwner(#id)")
    public Result<List<DocumentChunkVO>> getDocumentChunks(@PathVariable("id") Long id) {

        // 同样干掉越权判断参数
        List<DocumentChunkVO> chunkVOs = documentService.getDocumentChunks(id);
        return Result.success(chunkVOs);
    }
}