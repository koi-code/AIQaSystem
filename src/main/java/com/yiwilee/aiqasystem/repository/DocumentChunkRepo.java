package com.yiwilee.aiqasystem.repository;

import com.yiwilee.aiqasystem.model.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentChunkRepo extends JpaRepository<DocumentChunk, Long> {
    @Query("SELECT c FROM DocumentChunk c JOIN FETCH c.document WHERE c.id IN :ids")
    List<DocumentChunk> findChunksWithDocumentByIds(@Param("ids") List<Long> ids);
    // 【新增 1】：根据单个文档 ID 查询所有的切片
    List<DocumentChunk> findByDocumentId(Long documentId);

    // 【新增 2】：根据多个文档 ID 批量查询切片
    List<DocumentChunk> findByDocumentIdIn(List<Long> documentIds);
}
