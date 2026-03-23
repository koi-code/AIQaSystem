package com.yiwilee.aiqasystem.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Data
@Configuration
@ConfigurationProperties(prefix = "aiqa.milvus")
public class MilvusConfig {

    private String uri;
    private String collectionName;
    private int dimension;
    private String indexType;
    private String metricType;
    private String defaultType;
    private int topK;
    private String token; // 统一从 aiqa.milvus.token 获取

    @Bean
    public MilvusClientV2 milvusClientV2() {
        ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder().uri(uri);

        // 如果有鉴权 token 才配置
        if (StringUtils.hasText(token)) {
            builder.token(token);
        }

        return new MilvusClientV2(builder.build());
    }

    public IndexParam.IndexType getIndexTypeEnum() {
        return IndexParam.IndexType.valueOf(indexType);
    }

    public IndexParam.MetricType getMetricTypeEnum() {
        return IndexParam.MetricType.valueOf(metricType);
    }
}