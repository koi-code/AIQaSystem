package com.yiwilee.aiqasystem.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RoleCreateDTO(
        @NotBlank(message = "角色名称不能为空")
        String roleName,

        @NotBlank(message = "角色代码不能为空")
        // 防呆设计：提醒前端传入时符合大写规范（虽然后端 Service 里我们做了兜底）
        @Pattern(regexp = "^[A-Z_]+$", message = "角色代码只能包含大写字母和下划线")
        String roleCode,

        String description
) {}