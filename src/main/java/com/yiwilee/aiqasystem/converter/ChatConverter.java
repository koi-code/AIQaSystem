package com.yiwilee.aiqasystem.converter;

import com.yiwilee.aiqasystem.model.entity.ChatMessage;
import com.yiwilee.aiqasystem.model.entity.ChatSession;
import com.yiwilee.aiqasystem.model.vo.ChatMessageVO;
import com.yiwilee.aiqasystem.model.vo.ChatSessionVO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChatConverter {

    // ==========================================
    // 会话 (Session) 转换
    // ==========================================
    public ChatSessionVO toSessionVO(ChatSession session) {
        if (session == null) {
            return null;
        }
        return new ChatSessionVO(
                session.getId(),
                session.getTitle(),
                session.getUpdateTime()
        );
    }

    public List<ChatSessionVO> toSessionVOList(List<ChatSession> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return Collections.emptyList();
        }
        return sessions.stream()
                .map(this::toSessionVO)
                .collect(Collectors.toList());
    }

    // ==========================================
    // 消息 (Message) 转换
    // ==========================================
    public ChatMessageVO toMessageVO(ChatMessage message) {
        if (message == null) {
            return null;
        }
        return new ChatMessageVO(
                message.getId(),
                message.getMsgRole(),
                message.getContent(),
                message.getReferenceDocs(), // 如果为 null，前端就不展示引用区域
                message.getCreateTime()
        );
    }

    public List<ChatMessageVO> toMessageVOList(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }
        return messages.stream()
                .map(this::toMessageVO)
                .collect(Collectors.toList());
    }
}