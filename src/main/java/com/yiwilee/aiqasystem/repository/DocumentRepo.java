package com.yiwilee.aiqasystem.repository;

import com.yiwilee.aiqasystem.model.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepo extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {
    @Query("SELECT d FROM Document d WHERE d.uploader.id = :userId")
    List<Document> findByUserId(@Param("userId") Long userId);

    /**
     * 新增：根据上传者（用户）ID分页查询文档
     * 自动推导 SQL: SELECT * FROM document WHERE uploader_id = ? ORDER BY create_time DESC
     */
    Page<Document> findByUploaderId(Long uploaderId, Pageable pageable);
}