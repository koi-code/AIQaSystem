package com.yiwilee.aiqasystem.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@Entity
@Table(name = "ai_chat_session")
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关联用户表，多个会话对应一个用户
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private SysUser user;

    @Column(length = 100)
    private String title = "新对话";

    @CreationTimestamp
    @Column(name = "create_time", updatable = false, nullable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    // 一对多映射：一个会话包含多条消息
    // mappedBy = "session" 表示关系由 AiChatMessage 端的 "session" 属性维护
    // cascade = CascadeType.ALL 允许级联操作（如删除会话时自动删除所有消息）
    // orphanRemoval = true 表示如果消息从集合中移除，数据库也会删除该记录
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();

    // 辅助方法：方便在代码中向会话添加消息，自动维护双向关系
    public void addMessage(ChatMessage message) {
        messages.add(message);
        message.setSession(this);
    }
}