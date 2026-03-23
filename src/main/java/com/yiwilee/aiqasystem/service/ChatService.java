package com.yiwilee.aiqasystem.service;

import com.yiwilee.aiqasystem.common.MessageRole;
import com.yiwilee.aiqasystem.model.dto.ChatSessionUpdateDTO;
import com.yiwilee.aiqasystem.model.entity.ChatMessage;
import com.yiwilee.aiqasystem.model.vo.ChatMessageVO;
import com.yiwilee.aiqasystem.model.vo.ChatSessionVO;

import java.util.List;

/**
 * 智能问答会话业务逻辑接口
 * 负责管理用户的多轮对话上下文、消息落库及越权防护。
 */
public interface ChatService {

    /**
     * 为指定用户创建一个新的空白对话会话
     * @param userId 当前登录用户 ID
     * @return ChatSessionVO 新建的会话视图（只含基础信息，不含消息体）
     */
    ChatSessionVO createSession(Long userId);

    /**
     * 获取当前用户的所有会话列表（用于渲染左侧侧边栏）
     * 【性能优化】：只返回会话的基础信息和修改时间，绝对不携带庞大的历史消息体
     * @param userId 当前登录用户 ID
     * @return List<ChatSessionVO> 会话列表，按更新时间倒序排列
     */
    List<ChatSessionVO> getUserSessions(Long userId);

    /**
     * 更新会话标题
     * @param sessionId 会话主键 ID
     * @param userId    当前操作用户 ID (用于越权校验)
     * @param updateDTO 包含新标题的 DTO 对象
     * @return ChatSessionVO 更新后的会话视图
     * @throws com.yiwilee.aiqasystem.exception.ChatException 越权访问时抛出
     */
    ChatSessionVO updateSessionTitle(Long sessionId, Long userId, ChatSessionUpdateDTO updateDTO);

    /**
     * 删除单个指定会话及其包含的所有聊天记录
     * @param sessionId 会话主键 ID
     * @param userId    当前操作用户 ID (用于越权校验)
     */
    void deleteSession(Long sessionId, Long userId);

    /**
     * 批量清空指定用户的所有会话记录（如注销账号、清空历史）
     * @param userId 目标用户 ID
     * @return int 成功删除的会话数量
     */
    int deleteSessions(Long userId);

    /**
     * 供前端调用的：获取指定会话下的所有聊天记录 (VO)
     * @param sessionId 目标会话 ID
     * @param userId    当前操作用户 ID
     * @return List<ChatMessageVO> 脱敏且格式化后的聊天记录列表，按时间升序
     */
    List<ChatMessageVO> getSessionMessagesForFrontend(Long sessionId, Long userId);

    // ==========================================
    // 内部服务间调用方法 (供 RagService 使用，不向外暴露)
    // ==========================================

    /**
     * 内部调用：获取会话历史消息实体 (Entity)，用于组装大模型上下文 Prompt
     * @param sessionId 目标会话 ID
     * @param userId    当前操作用户 ID (严格校验，防止 RAG 被用于窃取他人对话)
     * @return List<ChatMessage> 原始消息实体列表
     */
    List<ChatMessage> getInternalMessageHistory(Long sessionId, Long userId);

    /**
     * 内部调用：保存单条聊天消息（用户提问或 AI 响应）
     * 注意：此方法会自动刷新所属 Session 的 updateTime 以置顶会话。
     * @param sessionId     所属会话 ID
     * @param messageRole       消息角色枚举 (USER/ASSISTANT/SYSTEM)
     * @param content       消息正文 (Markdown 文本)
     * @param referenceDocs RAG 检索到的引用文档 (JSON 字符串)
     * @param tokenUsage    Token 消耗量
     * @return ChatMessage  保存成功后的实体
     */
    ChatMessage saveMessage(Long sessionId, MessageRole messageRole, String content, String referenceDocs, Integer tokenUsage);
}