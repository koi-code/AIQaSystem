package com.yiwilee.aiqasystem.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger 3) 配置类
 */
@Configuration
public class SwaggerConfig {
    @Value("${aiqa.version:1.0.0}")
    private String appVersion;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // 1. 文档基础信息配置
                .info(new Info()
                        .title("AI-QA System 基于 RBAC 与 RAG 的智能问答系统 API")
                        .version(appVersion)
                        .description("提供用户管理、角色权限、知识库管理及大模型流式对话等核心接口。")
                        .contact(new Contact().name("Yiwi Lee").email("your.email@example.com")))

                // 2. 全局安全校验项 (指示所有的接口都需要 Bearer Token)
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))

                // 3. 安全模式配置 (定义右上角的 Authorize 按钮行为)
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .name("BearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("请先调用 /api/v1/auth/login 接口获取 Token，然后直接填入下框（无需手动加 Bearer 前缀）")));
    }
}