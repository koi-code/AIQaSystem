package com.yiwilee.aiqasystem.security.filter;

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
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final HandlerExceptionResolver exceptionResolver;

    // 引入 AntPathMatcher 进行支持通配符的精确路径匹配
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 💡 核心修复：定义需要直接跳过 JWT 校验的白名单路径 (支持通配符)
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/v*/auth/**",
            "/error",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    );

    public JwtFilter(JwtUtils jwtUtils,
                     @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.jwtUtils = jwtUtils;
        this.exceptionResolver = exceptionResolver;
    }

    /**
     * 💡 核心修复：Spring Security 官方推荐的白名单放行方式。
     * 如果返回 true，请求将直接跳过当前的 doFilterInternal，不再处理 Token。
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return WHITE_LIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestURI));
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

                // 2. 构造轻量级虚拟 Principal，防止 SecurityUtils 转换失败
                SysUser lightUser = new SysUser();
                lightUser.setId(userId);
                lightUser.setUsername(username);
                UserPrincipal principal = new UserPrincipal(lightUser);

                // 3. 将 principal 放入上下文中
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
            // Token 异常交给全局异常处理器
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