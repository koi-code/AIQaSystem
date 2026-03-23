package com.yiwilee.aiqasystem.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@Entity
@Table(name = "ai_chat_message")
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 多对一映射：多条消息属于一个会话
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    // 角色：user(用户), assistant(AI), system(系统提示词)
    @Column(name = "msg_role", length = 20, nullable = false)
    private String msgRole;

    // 消息内容，使用 TEXT 类型存储大段文本
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // RAG检索到的参考文档，存储 JSON 字符串
    @Column(name = "reference_docs", columnDefinition = "JSON")
    private String referenceDocs;

    @Column(name = "token_usage")
    private Integer tokenUsage = 0;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false, nullable = false)
    private LocalDateTime createTime;

//
//    // 消息在当前会话中的排序索引（从 0 开始递增）
//    @Column(name = "msg_index", nullable = false)
//    private Integer msgIndex;
}