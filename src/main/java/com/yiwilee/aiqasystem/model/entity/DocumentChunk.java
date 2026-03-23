package com.yiwilee.aiqasystem.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "kb_document_chunk")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 多对一：多个分块属于同一个文档
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex; // 当前分块在原文档中的顺序索引 (0, 1, 2...)

    @Column(name = "page_num")
    private Integer pageNum; // 当前分块所属的 PDF/Word 页码 (用于给前端展示引用页码)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 切分出来的纯文本片段 (约 500-1000 字)

    // ==========================================
    // 向量库关联 (核心关联点)
    // ==========================================
    @Column(name = "vector_id", length = 64)
    private String vectorId; // 对应 Milvus 或 Chroma 中的主键 ID

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}