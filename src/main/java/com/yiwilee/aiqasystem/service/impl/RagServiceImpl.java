package com.yiwilee.aiqasystem.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiwilee.aiqasystem.common.MessageRole;
import com.yiwilee.aiqasystem.config.MilvusConfig;
import com.yiwilee.aiqasystem.exception.RagException;
import com.yiwilee.aiqasystem.model.entity.ChatMessage;
import com.yiwilee.aiqasystem.model.entity.DocumentChunk;
import com.yiwilee.aiqasystem.repository.DocumentChunkRepo;
import com.yiwilee.aiqasystem.service.ChatService;
import com.yiwilee.aiqasystem.service.RagService;
import com.yiwilee.aiqasystem.service.UserService;
import com.yiwilee.aiqasystem.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final VectorService vectorService;
    private final ChatService chatService;
    private final UserService userService;
    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final DocumentChunkRepo documentChunkRepo;
    private final MilvusConfig milvusConfig;
    private final JsonMapper jsonMapper; // 替换 Gson 为 Spring 原生支持的 Jackson

    // 注入我们刚才定义的线程池
    @Qualifier("ragTaskExecutor")
    private final Executor ragTaskExecutor;

    private static final Long SSE_TIMEOUT_MS = 180_000L; // 3分钟超时

    // 内部 Record 用于封装 RAG 检索结果
    private record RagContext(String contextText, List<String> referenceDocs) {}

    @Override
    public SseEmitter sendMessageStream(Long sessionId, Long userId, String userMessage, boolean enableRag) {

        // 1. 同步拦截与基础校验 (在 Tomcat 主线程中执行，安全上下文未丢失)
        List<String> userRoles = userService.getUserRoles(userId);
        // 注意：这里调用的是我们上一轮重构专门给后端用的 getInternalMessageHistory
        List<ChatMessage> history = chatService.getInternalMessageHistory(sessionId, userId);

        log.info("初始化 RAG 管道: SessionId={}, UserId={}, EnableRag={}", sessionId, userId, enableRag);

        // 2. 初始化 SSE 实例并绑定生命周期钩子 (防止内存泄露)
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        setupEmitterCallbacks(emitter, sessionId);

        // 3. 构建核心异步任务
        Runnable ragTask = () -> executeRagPipeline(emitter, sessionId, userId, userMessage, enableRag, userRoles, history);

        // 4. 将任务委托给安全上下文并扔进线程池运行
        ragTaskExecutor.execute(new DelegatingSecurityContextRunnable(ragTask));

        return emitter;
    }

    /**
     * 核心 RAG 管道调度流程
     */
    private void executeRagPipeline(SseEmitter emitter, Long sessionId, Long userId, String userMessage,
                                    boolean enableRag, List<String> userRoles, List<ChatMessage> history) {
        try {
            safeSendSse(emitter, "\n[系统提示：AI 正在检索与思考中...]\n");

            // 阶段 1：知识库检索
            RagContext ragContext = retrieveContext(userMessage, enableRag, userRoles);

            // 阶段 2：组装大语言模型 Prompt
            List<Message> springAiMessages = buildPromptMessages(userMessage, ragContext.contextText(), history, enableRag);

            // 阶段 3：大语言模型流式交互及持久化
            streamLlmResponseAndSave(emitter, sessionId, userMessage, springAiMessages, ragContext.referenceDocs());

        } catch (Exception e) {
            log.error("RAG 执行管道崩溃: SessionId={}", sessionId, e);
            handleSseError(emitter, "系统繁忙，大模型响应异常，请稍后重试");
        }
    }

    /**
     * 向量检索及文本拼装
     */
    private RagContext retrieveContext(String userMessage, boolean enableRag, List<String> userRoles) {
        if (!enableRag) {
            return new RagContext("", new ArrayList<>());
        }

        try {
            float[] queryVector = embeddingModel.embed(userMessage);
            List<VectorService.VectorSearchResult> searchResults = vectorService.searchWithRoles(
                    queryVector, milvusConfig.getTopK(), "chunk", userRoles
            );

            if (CollUtil.isEmpty(searchResults)) {
                return new RagContext("", new ArrayList<>());
            }

            List<Long> chunkIds = searchResults.stream().map(VectorService.VectorSearchResult::chunkId).toList();
            List<DocumentChunk> chunks = documentChunkRepo.findChunksWithDocumentByIds(chunkIds);

            String contextText = chunks.stream()
                    .map(DocumentChunk::getContent)
                    .collect(Collectors.joining("\n\n---\n\n"));

            List<String> referenceDocs = chunks.stream()
                    .map(c -> c.getDocument().getName() + " (第" + c.getPageNum() + "页)")
                    .distinct()
                    .toList();

            return new RagContext(contextText, referenceDocs);

        } catch (Exception e) {
            log.error("知识库向量检索失败", e);
            throw new RagException("检索向量数据库异常", e);
        }
    }

    /**
     * 构建 Spring AI 标准消息体
     */
    private List<Message> buildPromptMessages(String userMessage, String contextText, List<ChatMessage> history, boolean enableRag) {
        List<Message> messages = new ArrayList<>();

        // 系统人设
        String sysPrompt = "你是一个专业且严谨的基于私有知识库的 AI 问答助手。";
        if (enableRag && StrUtil.isNotBlank(contextText)) {
            sysPrompt += "\n请严格参考以下【知识库背景内容】来回答用户的问题。如果提供的资料中没有相关信息，请明确告知用户“根据知识库资料不足以回答该问题”，绝不要编造内容。\n"
                    + "【知识库背景内容】：\n" + contextText;
        }
        messages.add(new SystemMessage(sysPrompt));

        // 注入历史上下文
        for (ChatMessage h : history) {
            if (MessageRole.USER.getRole().equals(h.getMsgRole())) {
                messages.add(new UserMessage(h.getContent()));
            } else {
                messages.add(new AssistantMessage(h.getContent()));
            }
        }

        // 当前提问
        messages.add(new UserMessage(userMessage));
        return messages;
    }

    /**
     * 调用 LLM，流式推送，并在完成时落库
     */
    private void streamLlmResponseAndSave(SseEmitter emitter, Long sessionId, String userMessage,
                                          List<Message> springAiMessages, List<String> referenceDocs) {
        StringBuilder aiResponseBuilder = new StringBuilder();

        chatClient.prompt()
                .messages(springAiMessages)
                .stream()
                .content()
                .subscribe(
                        // onNext: 接收到大模型返回的代码片段
                        piece -> {
                            if (StrUtil.isNotBlank(piece)) {
                                aiResponseBuilder.append(piece);
                                try {
                                    safeSendSse(emitter, piece);
                                } catch (Exception e) {
                                    // 【核心断流防御】：一旦发生 IO 异常（用户关掉网页），立马抛出 RuntimeException
                                    // 借助 Reactor 机制，这会直接向上游抛出错误，强行中断大模型的 Token 推送，省钱省资源！
                                    throw new RuntimeException("客户端网络断开，中断生成", e);
                                }
                            }
                        },
                        // onError: 大模型调用失败 或 上面的中断发生
                        error -> {
                            log.warn("大模型流式响应中断或出错: {}", error.getMessage());
                            handleSseError(emitter, "AI 响应被中断或网络异常");
                        },
                        // onComplete: 大模型回答完毕，安全落库
                        () -> {
                            try {
                                String refDocsJson = jsonMapper.writeValueAsString(referenceDocs);
                                // 分别保存用户发言和 AI 回复
                                chatService.saveMessage(sessionId, MessageRole.USER, userMessage, null, 0);
                                chatService.saveMessage(sessionId, MessageRole.ASSISTANT, aiResponseBuilder.toString(), refDocsJson, 0);

                                safeSendSseEvent(emitter, "DONE", "[DONE]");
                                emitter.complete();
                                log.info("会话 {} 的问答流程全部结束并成功落库", sessionId);
                            } catch (Exception e) {
                                log.error("消息落库持久化失败", e);
                                handleSseError(emitter, "消息保存失败");
                            }
                        }
                );
    }

    // ---------------- 安全与工具方法 ----------------

    /**
     * 管理 SseEmitter 的生命周期钩子
     */
    private void setupEmitterCallbacks(SseEmitter emitter, Long sessionId) {
        emitter.onCompletion(() -> log.debug("SSE 连接正常结束: SessionId={}", sessionId));
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时: SessionId={}", sessionId);
            emitter.complete();
        });
        emitter.onError(e -> {
            log.error("SSE 连接遭遇严重异常: SessionId={}", sessionId, e);
            emitter.completeWithError(e);
        });
    }

    /**
     * 优雅发送普通的 SSE 文本内容
     */
    private void safeSendSse(SseEmitter emitter, String content) throws IOException {
        emitter.send(SseEmitter.event().data(content));
    }

    /**
     * 优雅发送带事件名称的 SSE 内容
     */
    private void safeSendSseEvent(SseEmitter emitter, String eventName, String content) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(content));
    }

    /**
     * 优雅处理并推送系统错误信息给前端
     */
    private void handleSseError(SseEmitter emitter, String errorMsg) {
        try {
            safeSendSseEvent(emitter, "error", "\n\n[系统异常：" + errorMsg + "]");
            emitter.complete();
        } catch (Exception ignored) {
            // 如果发送错误信息时客户端已经不在了，忽略即可
        }
    }

    @Override
    public String sendMessageSync(Long sessionId, Long userId, String userMessage, boolean enableRag) {
        throw new RagException("该接口已废弃，请使用流式接口 (/stream)");
    }
}