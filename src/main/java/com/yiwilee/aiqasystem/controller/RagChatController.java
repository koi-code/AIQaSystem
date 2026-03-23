package com.yiwilee.aiqasystem.controller;

import com.yiwilee.aiqasystem.model.dto.RagMessageDTO;
import com.yiwilee.aiqasystem.service.RagService;
import com.yiwilee.aiqasystem.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 核心 RAG 问答流式引擎控制器
 * 负责接收用户提问，建立 SSE 长连接，并实现打字机效果的响应
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
@Tag(name = "06. 核心 AI 问答引擎", description = "基于 SSE (Server-Sent Events) 的流式问答枢纽")
public class RagChatController {

    private final RagService ragService;

    /**
     * 架构师警告：
     * 1. produces 必须强制定制为 TEXT_EVENT_STREAM_VALUE，告知网关和浏览器这是一个事件流。
     * 2. 这里绝对不能使用 Result<T> 包装，必须直接返回 SseEmitter 对象！
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发起流式 RAG 提问", description = "与大模型建立长连接，实现打字机流式响应 (SSE)")
    public SseEmitter streamChat(@Validated @RequestBody RagMessageDTO requestDTO) {

        // 1. 安全底线：从 SecurityContext 中强制提取 userId，杜绝伪造提问者身份
        Long userId = SecurityUtils.getCurrentUserId();

        log.info("用户 [{}] 在会话 [{}] 发起大模型流式提问，开启 RAG: {}",
                userId, requestDTO.sessionId(), requestDTO.enableRag());

        // 2. 将组装好的安全参数传递给底层的异步流式引擎
        // 这里的 emitter 会在 RagServiceImpl 的线程池中被异步写入数据
        return ragService.sendMessageStream(
                requestDTO.sessionId(),
                userId,
                requestDTO.userMessage(),
                requestDTO.enableRag()
        );
    }
}