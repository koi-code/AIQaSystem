package com.yiwilee.aiqasystem.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 权限/菜单创建参数
 */
public record PermissionCreateDTO(
        @NotNull(message = "父节点ID不能为空，根节点请传 0")
        Long parentId,

        @NotBlank(message = "权限名称不能为空")
        String permName,

        String permCode,

        @NotNull(message = "菜单类型不能为空")
        Integer type, // 1-目录, 2-菜单, 3-按钮

        String path
) {}