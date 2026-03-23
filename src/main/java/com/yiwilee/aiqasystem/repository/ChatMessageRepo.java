package com.yiwilee.aiqasystem.repository;

import com.yiwilee.aiqasystem.model.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {
}
