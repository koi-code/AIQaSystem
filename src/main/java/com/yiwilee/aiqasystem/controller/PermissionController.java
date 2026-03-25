package com.yiwilee.aiqasystem.controller;

import com.yiwilee.aiqasystem.common.Result;
import com.yiwilee.aiqasystem.constant.ApiVersion;
import com.yiwilee.aiqasystem.model.dto.PermissionCreateDTO;
import com.yiwilee.aiqasystem.model.dto.PermissionUpdateDTO;
import com.yiwilee.aiqasystem.model.vo.PermissionVO;
import com.yiwilee.aiqasystem.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.BASE_VERSION+"/permissions")
@RequiredArgsConstructor
@Tag(name = "04. 权限菜单管理", description = "系统菜单、目录、按钮等权限节点的维护与树形结构获取")
public class PermissionController {

    private final PermissionService permissionService;

    // ==========================================
    // 💡 核心鉴权：以下所有操作仅限 ADMIN 角色访问！
    // 任何普通用户尝试调用，会在进入方法前被 Spring Security 强杀并返回 403
    // ==========================================

    @GetMapping("/tree")
    @Operation(summary = "获取全量权限树", description = "仅供管理员分配角色权限时使用")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<PermissionVO>> getPermissionTree() {
        List<PermissionVO> tree = permissionService.getPermissionTree();
        return Result.success(tree);
    }

    @PostMapping
    @Operation(summary = "新增权限节点", description = "支持添加顶级目录、级联菜单和页面内按钮")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PermissionVO> createPermission(@Validated @RequestBody PermissionCreateDTO createDTO) {
        PermissionVO permission = permissionService.createPermission(createDTO);
        return Result.success(permission);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新权限节点", description = "允许修改名称、前端路由和权限标识符")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PermissionVO> updatePermission(
            @PathVariable("id") Long id,
            @Validated @RequestBody PermissionUpdateDTO updateDTO) {
        PermissionVO permission = permissionService.updatePermission(id, updateDTO);
        return Result.success(permission);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除权限节点", description = "防御性设计：若存在关联的子节点时将拒绝删除操作")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deletePermission(@PathVariable("id") Long id) {
        permissionService.deletePermission(id);
        return Result.success(null);
    }
}