package com.yiwilee.aiqasystem.util;

import com.yiwilee.aiqasystem.model.entity.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 全局安全上下文工具类
 * 随时随地获取当前登录用户的身份信息，彻底杜绝从前端接收 userId
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户的 ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getSysUser().getId();
        }
        throw new AccessDeniedException("登录状态异常或已过期，请重新登录");
    }

    /**
     * 获取当前登录用户的完整用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUsername();
        }
        throw new AccessDeniedException("登录状态异常或已过期，请重新登录");
    }
}