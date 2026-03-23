package com.yiwilee.aiqasystem.config;

import io.milvus.param.IndexType;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "milvus")
public class MilvusConfig {
    /**
     * Milvus 服务器地址
     */
    private String uri = "http://localhost:19530";

    /**
     * 集合名称
     */
    private String collectionName = "aiqa_knowledge_base_v2";

    /**
    * 向量维度
    */
    private int dimension = 1024;

    /**
     * 索引类型
     */
    private String indexType = "IVF_FLAT";

    /**
     * 相似度计算方式
     */
    private String metricType = "COSINE";

    /**
     * 用户名：密码
    */
    @Value("${spring.ai.vectorstore.milvus.client.token}")
    private String token;

    private String defaultType = "chunk";

    private int topK = 5;

    @Bean
    public MilvusClientV2 milvusClientV2() {
        // 1、创建连接参数
        ConnectConfig connectConfig = ConnectConfig
                .builder()
                .uri(uri)
//                可选
//                .token(token)
                .build();
        // 2、创建客户端
        return new MilvusClientV2(connectConfig);
    }

    public IndexParam.IndexType getIndexType() {
        return IndexParam.IndexType.valueOf(indexType);
    }

    public IndexParam.MetricType getMetricType() {
        return IndexParam.MetricType.valueOf(metricType);
    }

}
