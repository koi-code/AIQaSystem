package com.yiwilee.aiqasystem.config;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import com.yiwilee.aiqasystem.config.properties.LlmProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.util.Map;

@Slf4j
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class AiConfig {

    /**
     * 动态构建 ChatClient
     * @param chatModelMap Spring 自动注入所有的 ChatModel (Key 为 Bean 名称)
     */
    @Bean
    public ChatClient chatClient(Map<String, ChatModel> chatModelMap, LlmProperties llmProperties) {
        // 拼装目标 Bean 的名称，例如 "openAi" + "ChatModel" = "openAiChatModel"
        String targetBeanName = llmProperties.activeProvider() + "ChatModel";

        ChatModel targetModel = chatModelMap.get(targetBeanName);
        if (targetModel == null) {
            log.error("当前容器中存在的 ChatModel: {}", chatModelMap.keySet());
            throw new IllegalArgumentException("无法找到激活的 ChatModel: " + targetBeanName + "。请检查 yml 中的 active-provider 配置是否拼写正确！");
        }

        log.info(">>>> 成功激活 ChatModel: {} <<<<", targetBeanName);
        return ChatClient.builder(targetModel).build();
    }

    /**
     * 动态暴露当前激活的 EmbeddingModel，并加上 @Primary
     * 这样在 RagServiceImpl 里直接注入 EmbeddingModel 时，拿到的就是这里路由出来的那一个
     */
    @Bean
    @Primary
    public EmbeddingModel primaryEmbeddingModel(Map<String, EmbeddingModel> embeddingModelMap, LlmProperties llmProperties) {
        // 拼装目标 Bean 的名称，例如 "openAi" + "EmbeddingModel" = "openAiEmbeddingModel"
        String targetBeanName = llmProperties.activeProvider() + "EmbeddingModel";

        EmbeddingModel targetModel = embeddingModelMap.get(targetBeanName);
        if (targetModel == null) {
            log.error("当前容器中存在的 EmbeddingModel: {}", embeddingModelMap.keySet());
            throw new IllegalArgumentException("无法找到激活的 EmbeddingModel: " + targetBeanName);
        }

        log.info(">>>> 成功激活 EmbeddingModel: {} <<<<", targetBeanName);
        return targetModel;
    }

    /**
     * 将 Tokenizer 注册为单例 Bean，避免在 Service 中硬编码和重复加载
     */
    @Bean
    public HuggingFaceTokenizer huggingFaceTokenizer(LlmProperties llmProperties) {
        String path = llmProperties.tokenizerPath();
        try {
            // 使用 ResourceLoader 可以同时支持 classpath: 和 file: 前缀
            ResourceLoader loader = new DefaultResourceLoader();
            Resource resource = loader.getResource(path);

            try (InputStream is = resource.getInputStream()) {
                HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance(is, null);
                log.info("本地 Tokenizer 词表加载成功！资源定位: {}", path);
                return tokenizer;
            }
        } catch (Exception e) {
            log.error("Tokenizer 加载失败，当前尝试路径: {}", path, e);
            throw new RuntimeException("系统初始化失败: Tokenizer 缺失");
        }
    }
}