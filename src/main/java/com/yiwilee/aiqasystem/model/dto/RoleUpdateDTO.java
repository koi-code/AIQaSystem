package com.yiwilee.aiqasystem.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 注意：这里刻意去掉了 roleCode！
 * 角色代码作为系统底层的权限标识，一旦创建，绝对不允许通过更新接口修改。
 */
public record RoleUpdateDTO(
        @NotBlank(message = "角色名称不能为空")
        String roleName,

        String description
) {}