package com.yiwilee.aiqasystem.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "RAG 流式对话请求参数")
public record RagMessageDTO(

        @NotNull(message = "会话 ID 不能为空")
        @Schema(description = "当前对话所在的会话 ID", example = "1001")
        Long sessionId,

        @NotBlank(message = "提问内容不能为空")
        @Size(max = 5000, message = "提问内容过长，不能超过 5000 个字符")
        @Schema(description = "用户的提问内容", example = "什么是大语言模型的幻觉？")
        String userMessage,

        @Schema(description = "是否开启知识库检索增强 (默认开启)", example = "true")
        Boolean enableRag
) {
    // 提供一个紧凑的构造处理，如果前端未传 enableRag，则默认为 true
    public RagMessageDTO {
        if (enableRag == null) {
            enableRag = true;
        }
    }
}