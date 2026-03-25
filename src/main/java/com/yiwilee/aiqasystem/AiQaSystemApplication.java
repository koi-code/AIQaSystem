package com.yiwilee.aiqasystem;

import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {MilvusVectorStoreAutoConfiguration.class})
public class AiQaSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiQaSystemApplication.class, args);
    }

}
