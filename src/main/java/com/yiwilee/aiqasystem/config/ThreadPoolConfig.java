package com.yiwilee.aiqasystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    @Bean(name = "ragTaskExecutor")
    public Executor ragTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // 核心线程数（视你的服务器 CPU 核心数而定）
        executor.setMaxPoolSize(50);  // 最大并发线程数
        executor.setQueueCapacity(200); // 队列容量：如果前 50 个线程都在忙，新请求放入队列
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("Rag-Worker-");
        // 拒绝策略：当队列满了，由调用者（Tomcat 主线程）直接执行，防止消息丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}