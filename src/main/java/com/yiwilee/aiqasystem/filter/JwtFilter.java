package com.yiwilee.aiqasystem.filter;

import com.yiwilee.aiqasystem.model.entity.SysUser;
import com.yiwilee.aiqasystem.model.entity.UserPrincipal;
import com.yiwilee.aiqasystem.util.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final HandlerExceptionResolver exceptionResolver;

    public JwtFilter(JwtUtils jwtUtils,
                     @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.jwtUtils = jwtUtils;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            final String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            final String jwt = authHeader.substring(7);

            // 解析 JWT
            Claims claims = jwtUtils.extractAllClaims(jwt);
            String username = claims.getSubject();

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 1. 提取 userId 和 权限集合 (零查库)
                Long userId = claims.get("userId", Long.class);
                @SuppressWarnings("unchecked")
                List<String> authorityStrings = claims.get("authorities", List.class);

                List<SimpleGrantedAuthority> authorities = authorityStrings == null ? Collections.emptyList() :
                        authorityStrings.stream().map(SimpleGrantedAuthority::new).toList();

                // 2. 【核心修复】：构造轻量级虚拟 Principal，防止 SecurityUtils 转换失败
                SysUser lightUser = new SysUser();
                lightUser.setId(userId);
                lightUser.setUsername(username);
                UserPrincipal principal = new UserPrincipal(lightUser);

                // 3. 将 principal (而不是单纯的 username 字符串) 放入上下文中
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        authorities
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            filterChain.doFilter(request, response);

        } catch (JwtException e) {
            log.warn("JWT 校验失败 [{}]: {}", request.getRequestURI(), e.getMessage());
            exceptionResolver.resolveException(request, response, null, e);
        } catch (AuthenticationException e) {
            log.warn("Security 认证异常: {}", e.getMessage());
            exceptionResolver.resolveException(request, response, null, e);
        } catch (Exception e) {
            log.error("JWT 过滤器未知系统错误", e);
            exceptionResolver.resolveException(request, response, null, e);
        }
    }
}