package com.yiwilee.aiqasystem.service;

import com.yiwilee.aiqasystem.model.vo.DocumentChunkVO;
import com.yiwilee.aiqasystem.model.vo.DocumentVO;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库文档业务逻辑接口
 * 负责文档的物理上传、文本解析、向量化(Embedding)以及生命周期管理。
 */
public interface DocumentService {

    /**
     * 上传、解析并向量化文档 (核心 RAG 预处理链路)
     * @param file         前端传来的物理文件
     * @param allowedRoles 允许访问该文档的角色列表 (用于 Milvus 向量防越权)
     * @param uploaderId   上传者的用户 ID
     * @return DocumentVO 包含基础信息和当前处理状态的视图对象
     * @throws com.yiwilee.aiqasystem.exception.DocumentException 文件为空或解析失败时抛出
     */
    DocumentVO uploadAndParseDocument(MultipartFile file, List<String> allowedRoles, Long uploaderId);

    /**
     * 分页查询当前系统的文档列表
     * @param keyword  搜索关键词 (匹配文档名)
     * @param status   文档状态 (0-等待, 1-处理中, 2-成功, 3-失败)，可为 null
     * @param pageNum  当前页码
     * @param pageSize 每页条数
     * @return Page<DocumentVO> 脱敏后的文档视图列表
     */
    Page<DocumentVO> pageDocuments(String keyword, Integer status, int pageNum, int pageSize);

    /**
     * 删除指定的单个文档 (包含 MySQL 数据、物理硬盘文件、Milvus 向量)
     * @param documentId 文档的主键 ID
     * @return boolean   是否删除成功
     */
    boolean deleteDocument(Long documentId);

    /**
     * 批量清空某用户上传的所有文档 (包含关联的物理文件和向量)
     * 通常在注销用户或清空知识库时调用。
     * @param userId 目标用户 ID
     * @return int 成功清理的文档总数
     */
    int deleteDocuments(Long userId);
    /**
     * 根据用户 ID 分页查询私人知识库文档
     * 包含水平越权（IDOR）安全防护
     *
     * @param targetUserId  要查询的目标用户 ID, 或用户自己的 ID
     * @param pageNum       页码
     * @param pageSize      每页条数
     * @return 分页的文档视图对象
     */
    Page<DocumentVO> pageDocumentsByUserId(Long targetUserId, int pageNum, int pageSize);

    // ==========================================
    // 💡 架构师建议追加的实用接口
    // ==========================================

    /**
     * 获取单个文档的详细信息
     */
    DocumentVO getDocumentById(Long documentId);

    /**
     * 获取文档解析后的文本切片（Chunk）列表，用于 RAG 调试和人工核验
     */
    List<DocumentChunkVO> getDocumentChunks(Long documentId);

    /**
     * 重新解析解析失败的文档
     * @param documentId 目标文档 ID
     */
    void reparseDocument(Long documentId);


}