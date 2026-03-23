package com.yiwilee.aiqasystem.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 外部配置绑定类
 * 对应 application.yml 中的 security.jwt 节点
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
    /**
     * 必须是 256-bit 以上的 Base64 编码字符串
     */
    private String secretKey;

    /**
     * Token 有效期（单位：毫秒）
     */
    private Long expirationTime;
}