package com.yiwilee.aiqasystem.security;

import com.yiwilee.aiqasystem.repository.ChatSessionRepo;
import com.yiwilee.aiqasystem.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 细粒度数据权限校验器 (ABAC)
 * 供 @PreAuthorize 注解在路由进入 Controller 前进行动态调用
 */
@Component("chatAuth") // 给组件起个简短的名字，方便在注解中调用
@RequiredArgsConstructor
public class ChatAuth {

    private final ChatSessionRepo chatSessionRepo;

    /**
     * 判断当前登录用户是否为该会话的拥有者
     */
    public boolean isOwner(Long sessionId) {
        // 1. 获取当前登录用户的 ID
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 2. 从数据库轻量级查询该会话的所属用户 ID
        Long ownerId = chatSessionRepo.findUserIdBySessionId(sessionId);

        // 3. 如果会话不存在，放行交给 Service 去报 404，或者在这里直接返回 false 报 403
        if (ownerId == null) {
            return false;
        }

        // 4. 比对是否为本人
        return currentUserId.equals(ownerId);
    }
}