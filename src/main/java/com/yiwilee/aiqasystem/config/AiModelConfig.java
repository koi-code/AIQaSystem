package com.yiwilee.aiqasystem.config;

import com.yiwilee.aiqasystem.constant.AiConstants;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
/**
 * 设置 Spring AI 的（ChatClient, EmbeddingModel）模型
 */

public class AiModelConfig {

    @Bean
    @Primary
    public EmbeddingModel embeddingModel(@Qualifier(AiConstants.EMBEDDING_MODEL) EmbeddingModel embeddingModel) {
        return embeddingModel;
    }

    @Bean
    @Primary
    public ChatClient chatClient(@Qualifier(AiConstants.CHAT_MODEL) ChatModel chatModel) {
        return ChatClient.create(chatModel);
    }
}
