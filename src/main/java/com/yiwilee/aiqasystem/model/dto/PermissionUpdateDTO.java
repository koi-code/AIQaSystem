package com.yiwilee.aiqasystem.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 权限/菜单更新参数
 */
public record PermissionUpdateDTO(
        @NotBlank(message = "权限名称不能为空")
        String permName,

        String permCode,

        String path
) {}