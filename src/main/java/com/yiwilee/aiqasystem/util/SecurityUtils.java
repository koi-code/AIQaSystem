package com.yiwilee.aiqasystem.util;

import com.yiwilee.aiqasystem.model.entity.SysUser;
import com.yiwilee.aiqasystem.model.entity.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 全局安全上下文工具类
 * 随时随地获取当前登录用户的身份信息与权限，彻底杜绝从前端接收 userId，防范越权漏洞。
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户的 ID
     */
    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * 获取当前登录用户的完整用户名
     */
    public static String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }

    /**
     * 场景：当 Service 层不仅需要 ID，还需要用户的其他字段（如昵称、头像）时，直接拿，省去一次查库。
     */
    public static SysUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getSysUser();
        }
        throw new AccessDeniedException("登录状态异常或已过期，请重新登录");
    }

    /**
     * 场景：在代码逻辑中做硬编码的权限分流（比如：管理员查全部，普通人查自己）
     *
     * @param roleCode 角色代码，例如 "ROLE_ADMIN"
     * @return boolean 是否拥有该角色
     */
    public static boolean hasRole(String roleCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 防呆设计：检查是否为空或未认证 (例如匿名用户)
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return false;
        }

        // 遍历 Security 框架缓存的权限列表进行精确匹配
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(roleCode));
    }

    /**
     * 场景：最高频的权限判断，单独抽离让代码更优雅。
     * * @return boolean 是否为管理员
     */
    public static boolean isAdmin() {
        // 常量最好定义在 SystemConstants 中，这里硬编码仅为演示
        return hasRole("ROLE_ADMIN");
    }
}