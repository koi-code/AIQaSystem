package com.yiwilee.aiqasystem.constant;

/**
 * 系统全局常量池
 */
public interface SystemConstants {

    // --- 角色相关常量 ---
    String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    String ROLE_ADMIN = "ROLE_ADMIN";
    String ROLE_USER = "ROLE_USER";

    // --- 业务逻辑强相关的特殊配置 ---
    /**
     * 新用户注册时的默认分配角色
     */
    String DEFAULT_REGISTER_ROLE = ROLE_USER;

    // --- 权限相关常量 ---
    String PERM_USER_ADD = "sys:user:add";
    String PERM_USER_DELETE = "sys:user:delete";
}