package com.yiwilee.aiqasystem.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * RAG 核心编排服务层
 * 负责调度 ChatService、VectorService、UserService 以及 Spring AI 大模型
 */
public interface RagService {

    /**
     * 核心方法 1：流式发送消息并获取 AI 回答 (带 RBAC 权限增强)
     * 场景：用户在聊天框发送问题，AI 像打字机一样一段一段返回答案
     *
     * @param sessionId 当前会话 ID
     * @param userId 当前提问的用户 ID (用于提取角色并进行向量库权限过滤)
     * @param userMessage 用户的提问内容
     * @param enableRag 是否开启知识库检索 (true-查阅资料后回答, false-纯大模型闲聊)
     * @return SseEmitter 用于将文字流式推送到前端
     */
    SseEmitter sendMessageStream(Long sessionId, Long userId, String userMessage, boolean enableRag);

    /**
     * 核心方法 2：普通(同步)发送消息
     * 场景：如果不使用流式输出，或者做单元测试、对外提供 API 接口时使用
     */
    String sendMessageSync(Long sessionId, Long userId, String userMessage, boolean enableRag);
}