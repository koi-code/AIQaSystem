package com.yiwilee.aiqasystem.service;

import java.util.List;

/**
 * 向量数据库服务层
 * 专门负责与 Milvus 向量数据库交互，支持带 RBAC 权限的高维检索
 */
public interface VectorService {

    /**
     * 初始化向量集合 (Schema & Index)
     * 说明：系统启动时自动调用，确保向量表中存在 allowed_roles 这个元数据字段，并建立 HNSW 索引
     */
    void initCollection();

    /**
     * 插入单条向量数据 (带有权限思想钢印)
     *
     * @param businessId   关联的 MySQL 中 DocumentChunk(分块) 的 ID
     * @param vector       文本转换后的高维向量数组 (由大模型 Embedding 生成)
     * @param type         数据类型 (如: "chunk")
     * @param allowedRoles 允许访问的角色集合 (将被序列化为 JSON 存入 Metadata)
     * @return 插入状态 (成功返回 "SUCCESS"，失败返回 null)
     */
    String insertVector(Long businessId, float[] vector, String type, List<String> allowedRoles);

    /**
     * 批量删除向量数据
     * 场景：用户删除文件时，根据 MySQL 中的 chunkId 列表，去 Milvus 中进行物理双删
     *
     * @param chunkIds 关联的 MySQL Chunk ID 列表
     */
    void deleteByChunkIds(List<Long> chunkIds);

    /**
     * 向量相似度检索 (核心：严格的 RBAC 权限拦截)
     *
     * @param queryVector 用户提问转换成的高维向量
     * @param topK        返回最相似的前 K 个结果 (通常设为 3)
     * @param type        搜索的数据类型
     * @param userRoles   当前提问用户的合法角色集合 (拦截越权访问的底层保障)
     * @return 包含 chunkId 和相似度分数 (score) 的检索结果列表
     */
    List<VectorSearchResult> searchWithRoles(float[] queryVector, int topK, String type, List<String> userRoles);

    // ==========================================
    // 辅助数据结构 (DTO)
    // ==========================================
    record VectorSearchResult(Long chunkId, String type, float score) {}
}