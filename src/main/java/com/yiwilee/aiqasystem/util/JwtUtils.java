package com.yiwilee.aiqasystem.util;

import com.yiwilee.aiqasystem.config.properties.JwtProperties;
import com.yiwilee.aiqasystem.model.entity.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtProperties jwtProperties;
    private Key signKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecretKey());
        this.signKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT 签名密钥已成功初始化");
    }

    /**
     * 生成 Token，注入用户名、userId和角色权限
     */
    public String generateToken(UserDetails principal) {
        Map<String, Object> claims = new HashMap<>();

        List<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // 将 userId 和 权限列表 直接塞进 JWT 的载荷中
        if (principal instanceof UserPrincipal userPrincipal) {
            claims.put("userId", userPrincipal.getSysUser().getId());
        }
        claims.put("authorities", authorities);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(principal.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpirationTime()))
                .signWith(signKey, SignatureAlgorithm.HS256) // 修复：统一使用初始化的 signKey
                .compact();
    }

    /**
     * 一次性解析 Token 并返回所有 Claims
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signKey) // 修复：统一使用初始化的 signKey
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}