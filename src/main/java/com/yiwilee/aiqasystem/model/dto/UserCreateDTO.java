package com.yiwilee.aiqasystem.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 接收管理员在后台手动创建用户的参数
 */
public record UserCreateDTO(
        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "初始密码不能为空")
        String password,

        @NotNull(message = "用户状态不能为空")
        Integer status, // 1-正常, 0-禁用

        @NotBlank(message = "必须指定初始角色")
        String roleCode // 例如: "ROLE_STUDENT"
) {}