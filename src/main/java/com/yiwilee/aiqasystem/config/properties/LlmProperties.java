package com.yiwilee.aiqasystem.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 集中管理大模型上下文与 Tokenizer 相关的动态配置
 */
@ConfigurationProperties(prefix = "aiqa.llm")
public record LlmProperties(
        String activeProvider,
        int maxContextTokens,
        int reservedOutputTokens,
        String tokenizerPath
) {
}