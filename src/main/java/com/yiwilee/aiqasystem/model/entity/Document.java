package com.yiwilee.aiqasystem.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "kb_document")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor // kb = knowledge base (知识库)
public class Document {

    // --- 状态常量定义 (推荐抽取到 Enum 中，这里为直观写在实体里) ---
    public static final Integer STATUS_PENDING = 0;    // 等待解析
    public static final Integer STATUS_PROCESSING = 1; // 解析中
    public static final Integer STATUS_COMPLETED = 2;  // 解析成功
    public static final Integer STATUS_FAILED = 3;     // 解析失败

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name; // 文档显示名称 (不带后缀)

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName; // 原始文件名 (带后缀)

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath; // 物理存储路径或 OSS URL

    @Column(name = "file_type", length = 50)
    private String fileType; // 文件后缀类型 (如: pdf, docx)

    @Column(name = "file_size")
    private Long fileSize; // 文件大小(字节)

    @Column(nullable = false)
    private Integer status = STATUS_PENDING; // 解析状态

    @Column(columnDefinition = "TEXT")
    private String summary; // 文档摘要 (由大模型在解析完毕后自动生成)

    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg; // 如果解析失败，记录报错堆栈

    // ==========================================
    // 核心改造点：RBAC 权限关联
    // ==========================================

    // 记录上传此文档的管理员
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private SysUser uploader;

    // 允许访问此文档的角色集合 (如: ["ROLE_STUDENT", "ROLE_TEACHER"])
    // 使用 Hibernate 提供的注解，自动实现 JSON 到 List 的序列化与反序列化
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_roles", columnDefinition = "JSON")
    private List<String> allowedRoles = new ArrayList<>();

    // ==========================================
    // 审计字段
    // ==========================================
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    // 级联关系：一个文档包含多个分块 (孤儿删除机制：文档删了，分块全删)
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentChunk> chunks = new ArrayList<>();
}