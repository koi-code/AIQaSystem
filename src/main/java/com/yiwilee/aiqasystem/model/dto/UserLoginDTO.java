package com.yiwilee.aiqasystem.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 接收用户登录请求参数
 */
public record UserLoginDTO(
        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        String password
) {}