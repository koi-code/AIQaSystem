package com.yiwilee.aiqasystem.repository;

import com.yiwilee.aiqasystem.model.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {

    /**
     * 根据会话 ID 分页查询聊天记录
     * 自动推导 SQL: SELECT * FROM chat_message WHERE session_id = ? ORDER BY create_time ASC/DESC LIMIT ?, ?
     */
    Page<ChatMessage> findBySessionId(Long sessionId, Pageable pageable);
}