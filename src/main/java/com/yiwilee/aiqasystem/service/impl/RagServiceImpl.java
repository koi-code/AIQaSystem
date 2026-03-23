package com.yiwilee.aiqasystem.service.impl;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.yiwilee.aiqasystem.common.MessageRole;
import com.yiwilee.aiqasystem.config.MilvusConfig;
import com.yiwilee.aiqasystem.config.properties.LlmProperties;
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
import java.util.LinkedList;
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
    private final JsonMapper jsonMapper;

    private final HuggingFaceTokenizer tokenizer;
    private final LlmProperties llmProperties;

    @Qualifier("ragTaskExecutor")
    private final Executor ragTaskExecutor;

    private static final Long SSE_TIMEOUT_MS = 180_000L;

    // 内部 Record 用于封装 RAG 检索结果
    private record RagContext(String contextText, List<String> referenceDocs) {}

    // 内部 Record 用于封装组装好的 Prompt 和计算出的准确 Token 数
    private record PromptContext(List<Message> messages, int totalInputTokens) {}

    @Override
    public SseEmitter sendMessageStream(Long sessionId, Long userId, String userMessage, boolean enableRag) {
        List<String> userRoles = userService.getUserRoles(userId);
        List<ChatMessage> history = chatService.getInternalMessageHistory(sessionId, userId);

        log.info("初始化 RAG 管道: SessionId={}, UserId={}, EnableRag={}", sessionId, userId, enableRag);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        setupEmitterCallbacks(emitter, sessionId);

        Runnable ragTask = () -> executeRagPipeline(emitter, sessionId, userId, userMessage, enableRag, userRoles, history);
        ragTaskExecutor.execute(new DelegatingSecurityContextRunnable(ragTask));

        return emitter;
    }

    private void executeRagPipeline(SseEmitter emitter, Long sessionId, Long userId, String userMessage,
                                    boolean enableRag, List<String> userRoles, List<ChatMessage> history) {
        try {
            safeSendSse(emitter, "\n[系统提示：AI 正在检索与思考中...]\n");

            // 阶段 1：知识库检索
            RagContext ragContext = retrieveContext(userMessage, enableRag, userRoles);

            // 阶段 2：带有滑动截断机制的 Prompt 组装 (获取组装后的消息和精确 Token)
            PromptContext promptContext = buildPromptMessagesWithTruncation(userMessage, ragContext.contextText(), history, enableRag);

            // 阶段 3：流式交互及持久化 (传入计算好的 Input Token)
            streamLlmResponseAndSave(emitter, sessionId, userMessage, promptContext, ragContext.referenceDocs());

        } catch (Exception e) {
            log.error("RAG 执行管道崩溃: SessionId={}", sessionId, e);
            handleSseError(emitter, "系统繁忙，大模型响应异常，请稍后重试");
        }
    }

    private RagContext retrieveContext(String userMessage, boolean enableRag, List<String> userRoles) {
        if (!enableRag) return new RagContext("", new ArrayList<>());
        try {
            float[] queryVector = embeddingModel.embed(userMessage);
            List<VectorService.VectorSearchResult> searchResults = vectorService.searchWithRoles(
                    queryVector, milvusConfig.getTopK(), "chunk", userRoles
            );
            if (CollUtil.isEmpty(searchResults)) return new RagContext("", new ArrayList<>());

            List<Long> chunkIds = searchResults.stream().map(VectorService.VectorSearchResult::chunkId).toList();
            List<DocumentChunk> chunks = documentChunkRepo.findChunksWithDocumentByIds(chunkIds);

            String contextText = chunks.stream().map(DocumentChunk::getContent).collect(Collectors.joining("\n\n---\n\n"));
            List<String> referenceDocs = chunks.stream().map(c -> c.getDocument().getName() + " (第" + c.getPageNum() + "页)").distinct().toList();
            return new RagContext(contextText, referenceDocs);
        } catch (Exception e) {
            log.error("知识库向量检索失败", e);
            throw new RagException("检索向量数据库异常", e);
        }
    }


    private PromptContext buildPromptMessagesWithTruncation(String userMessage, String contextText, List<ChatMessage> history, boolean enableRag) {
        List<Message> finalMessages = new ArrayList<>();
        int currentTotalTokens = 0;

        // 1. 系统人设与 RAG 知识背景
        String sysPrompt = "你是一个专业且严谨的基于私有知识库的 AI 问答助手。";
        if (enableRag && StrUtil.isNotBlank(contextText)) {
            sysPrompt += "\n请严格参考以下【知识库背景内容】来回答用户的问题。如果提供的资料中没有相关信息，请明确告知用户“根据知识库资料不足以回答该问题”，绝不要编造内容。\n"
                    + "【知识库背景内容】：\n" + contextText;
        }
        int sysTokens = countTokens(sysPrompt);
        currentTotalTokens += sysTokens;

        // 2. 当前用户最新提问
        int userTokens = countTokens(userMessage);
        currentTotalTokens += userTokens;

        // 3. 计算留给历史对话的 Token 预算 (从配置属性类中获取)
        int availableHistoryTokens = llmProperties.maxContextTokens() - llmProperties.reservedOutputTokens() - sysTokens - userTokens;

        if (availableHistoryTokens <= 0) {
            log.warn("当前提问与检索内容已占满 Token 预算 ({} Tokens)，被迫清空所有历史上下文！", currentTotalTokens);
            finalMessages.add(new SystemMessage(sysPrompt));
            finalMessages.add(new UserMessage(userMessage));
            return new PromptContext(finalMessages, currentTotalTokens);
        }

        // 4. 倒序遍历历史记录进行滑动截断
        LinkedList<Message> retainedHistory = new LinkedList<>();
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage h = history.get(i);
            int msgTokens = countTokens(h.getContent());

            if (availableHistoryTokens >= msgTokens) {
                availableHistoryTokens -= msgTokens;
                currentTotalTokens += msgTokens;

                // 插入队列头部，恢复时间正序
                if (MessageRole.USER.getRole().equals(h.getMsgRole())) {
                    retainedHistory.addFirst(new UserMessage(h.getContent()));
                } else {
                    retainedHistory.addFirst(new AssistantMessage(h.getContent()));
                }
            } else {
                log.info("触发 Token 动态截断：历史记录已达到容量上限，丢弃了最早的 {} 条对话", i + 1);
                break;
            }
        }

        // 5. 按顺序组装：System -> History -> Current User
        finalMessages.add(new SystemMessage(sysPrompt));
        finalMessages.addAll(retainedHistory);
        finalMessages.add(new UserMessage(userMessage));

        return new PromptContext(finalMessages, currentTotalTokens);
    }

    /**
     * 辅助方法：调用本地分词器计算纯文本 Token 数量
     */
    private int countTokens(String text) {
        if (StrUtil.isBlank(text)) return 0;
        return tokenizer.encode(text).getTokens().length;
    }

    private void streamLlmResponseAndSave(SseEmitter emitter, Long sessionId, String userMessage,
                                          PromptContext promptContext, List<String> referenceDocs) {
        StringBuilder aiResponseBuilder = new StringBuilder();

        chatClient.prompt()
                .messages(promptContext.messages()) // 传入截断后的安全消息队列
                .stream()
                .content()
                .subscribe(
                        piece -> {
                            if (StrUtil.isNotBlank(piece)) {
                                aiResponseBuilder.append(piece);
                                try {
                                    safeSendSse(emitter, piece);
                                } catch (Exception e) {
                                    throw new RuntimeException("客户端网络断开，中断生成", e);
                                }
                            }
                        },
                        error -> {
                            log.warn("大模型流式响应中断或出错: {}", error.getMessage());
                            handleSseError(emitter, "AI 响应被中断或网络异常");
                        },
                        () -> {
                            try {
                                String refDocsJson = jsonMapper.writeValueAsString(referenceDocs);

                                // 精确计算 AI 回复的 Output Token
                                String aiFullResponse = aiResponseBuilder.toString();
                                int outputTokens = countTokens(aiFullResponse);

                                // 用户消息落库：记录 Input Token (即系统词 + 历史词 + 提问词)
                                chatService.saveMessage(sessionId, MessageRole.USER, userMessage, null, promptContext.totalInputTokens());

                                // AI 消息落库：记录 Output Token
                                chatService.saveMessage(sessionId, MessageRole.ASSISTANT, aiFullResponse, refDocsJson, outputTokens);

                                safeSendSseEvent(emitter, "DONE", "[DONE]");
                                emitter.complete();
                                log.info("会话 {} 结束，Input Tokens: {}, Output Tokens: {}",
                                        sessionId, promptContext.totalInputTokens(), outputTokens);
                            } catch (Exception e) {
                                log.error("消息落库持久化失败", e);
                                handleSseError(emitter, "消息保存失败");
                            }
                        }
                );
    }

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

    private void safeSendSse(SseEmitter emitter, String content) throws IOException {
        emitter.send(SseEmitter.event().data(content));
    }

    private void safeSendSseEvent(SseEmitter emitter, String eventName, String content) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(content));
    }

    private void handleSseError(SseEmitter emitter, String errorMsg) {
        try {
            safeSendSseEvent(emitter, "error", "\n\n[系统异常：" + errorMsg + "]");
            emitter.complete();
        } catch (Exception ignored) {}
    }

    @Override
    public String sendMessageSync(Long sessionId, Long userId, String userMessage, boolean enableRag) {
        throw new RagException("该接口已废弃，请使用流式接口 (/stream)");
    }
}