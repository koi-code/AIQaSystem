package com.yiwilee.aiqasystem.service.impl;

import com.yiwilee.aiqasystem.common.MessageRole;
import com.yiwilee.aiqasystem.converter.ChatConverter;
import com.yiwilee.aiqasystem.exception.ChatException;
import com.yiwilee.aiqasystem.exception.ResourceNotFoundException;
import com.yiwilee.aiqasystem.model.dto.ChatSessionUpdateDTO;
import com.yiwilee.aiqasystem.model.entity.ChatMessage;
import com.yiwilee.aiqasystem.model.entity.ChatSession;
import com.yiwilee.aiqasystem.model.vo.ChatMessageVO;
import com.yiwilee.aiqasystem.model.vo.ChatSessionVO;
import com.yiwilee.aiqasystem.repository.ChatMessageRepo;
import com.yiwilee.aiqasystem.repository.ChatSessionRepo;
import com.yiwilee.aiqasystem.repository.UserRepo;
import com.yiwilee.aiqasystem.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepo messageRepo;
    private final ChatSessionRepo sessionRepo;
    private final UserRepo userRepo;

    private final ChatConverter converter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSessionVO createSession(Long userId) {
        ChatSession chatSession = ChatSession.builder()
                .user(userRepo.getReferenceById(userId)) // 使用 GetReferenceById 延迟加载，提升性能
                .title("新对话")
                .messages(new ArrayList<>())
                .build();

        ChatSession savedSession = sessionRepo.save(chatSession);
        log.info("用户 {} 创建了新会话: {}", userId, savedSession.getId());

        return converter.toSessionVO(savedSession);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionVO> getUserSessions(Long userId) {
        // 要求 ChatSessionRepo 中已定义 findAllByUserIdOrderByUpdateTimeDesc
        List<ChatSession> sessions = sessionRepo.findAllByUserIdOrderByUpdateTimeDesc(userId);

        // 核心性能点：只映射 VO 属性，绝不触发 messages 的懒加载
        return sessions.stream()
                .map(converter::toSessionVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSessionVO updateSessionTitle(Long sessionId, Long userId, ChatSessionUpdateDTO updateDTO) {
        ChatSession session = getAndCheckSession(sessionId, userId);

        session.setTitle(updateDTO.newTitle());
        session.setUpdateTime(LocalDateTime.now()); // 刷新修改时间，使之前排

        session = sessionRepo.save(session);
        log.info("会话 {} 标题已更新为: {}", sessionId, updateDTO.newTitle());

        return converter.toSessionVO(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId, Long userId) {
        ChatSession session = getAndCheckSession(sessionId, userId);
        // JPA 的 cascade = CascadeType.ALL 会自动级联删除对应的 ChatMessage
        sessionRepo.delete(session);
        log.info("用户 {} 删除了会话: {}", userId, sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSessions(Long userId) {
        int count = sessionRepo.bulkDeleteByUserId(userId);
        log.info("用户 {} 清空了所有会话，共计 {} 条", userId, count);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageVO> getSessionMessagesForFrontend(Long sessionId, Long userId) {
        ChatSession session = getAndCheckSession(sessionId, userId);

        // 严格保证前端获得的消息是按时间升序排列的（历史在上面，最新在下面）
        return session.getMessages().stream()
                .sorted(Comparator.comparing(ChatMessage::getCreateTime))
                .map(converter::toMessageVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getInternalMessageHistory(Long sessionId, Long userId) {
        ChatSession session = getAndCheckSession(sessionId, userId);

        // 保证内部组装大模型上下文时顺序不乱
        return session.getMessages().stream()
                .sorted(Comparator.comparing(ChatMessage::getCreateTime))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage saveMessage(Long sessionId, MessageRole messageRole, String content, String referenceDocs, Integer tokenUsage) {
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("会话已失效或不存在"));

        int actualTokenUsage = (tokenUsage != null) ? tokenUsage : 0;

        ChatMessage message = ChatMessage.builder()
                .session(session)
                .msgRole(messageRole.getRole())
                .content(content)
                .referenceDocs(referenceDocs)
                .tokenUsage(actualTokenUsage)
                .build();

        // 每次发消息必须刷新外层 Session 的更新时间，确保该会话在左侧列表顶部活跃
        session.setUpdateTime(LocalDateTime.now());
        sessionRepo.save(session);

        return messageRepo.save(message);
    }

    // ---------------- 内部安全与工具方法 ----------------

    /**
     * 获取会话并进行水平越权(IDOR)严格校验
     */
    private ChatSession getAndCheckSession(Long sessionId, Long userId) {
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("会话已失效或不存在"));

        if (!Objects.equals(session.getUser().getId(), userId)) {
            log.warn("越权拦截：用户 {} 试图非法访问会话 {}", userId, sessionId);
            throw new ChatException("非法操作：您无权访问该会话！");
        }

        return session;
    }

    /* 已实现了对应 converter 工具类*/

//    @Deprecated
//    private ChatSessionVO convertToSessionVO(ChatSession session) {
//        return new ChatSessionVO(
//                session.getId(),
//                session.getTitle(),
//                session.getUpdateTime()
//        );
//    }
//
//    @Deprecated
//    private ChatMessageVO convertToMessageVO(ChatMessage message) {
//        return new ChatMessageVO(
//                message.getId(),
//                message.getMsgRole(),
//                message.getContent(),
//                message.getReferenceDocs(),
//                message.getCreateTime()
//        );
//    }
}