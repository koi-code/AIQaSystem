package com.yiwilee.aiqasystem.constant;

import org.springframework.beans.factory.annotation.Value;

/**
 * 全局 API 版本路由常量
 * 统一管理所有接口的前缀，方便后续平滑升级到 v2, v3
 */
public interface ApiVersion {
    // 基础 API 前缀
    @Value("${aiqa.api.version-prefix}")
    String BASE_VERSION = "/api/v2";

}