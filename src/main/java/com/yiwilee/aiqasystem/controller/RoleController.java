package com.yiwilee.aiqasystem.controller;

import com.yiwilee.aiqasystem.common.Result;
import com.yiwilee.aiqasystem.model.dto.RoleCreateDTO;
import com.yiwilee.aiqasystem.model.dto.RoleUpdateDTO;
import com.yiwilee.aiqasystem.model.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "03. 角色权限管理", description = "RBAC 模型中的角色维护及权限节点绑定")
public class RoleController {

    @GetMapping("/all")
    @Operation(summary = "获取所有角色集合", description = "通常用于用户管理页面的下拉选择框")
    public Result<List<RoleVO>> getAllRoles() {
        // TODO: 调用 roleService.getAllRoles
        return null;
    }

    @GetMapping
    @Operation(summary = "分页查询角色列表", description = "支持按角色名称模糊搜索")
    public Result<Page<RoleVO>> pageRoles(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        // TODO: 调用 roleService.pageRoles
        return null;
    }

    @PostMapping
    @Operation(summary = "创建新角色", description = "防呆设计：角色代码会自动转大写并拼装 ROLE_ 前缀")
    public Result<RoleVO> createRole(@Validated @RequestBody RoleCreateDTO createDTO) {
        // TODO: 调用 roleService.createRole
        return null;
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新角色信息", description = "仅允许更新名称和描述，角色代码禁止修改")
    public Result<RoleVO> updateRole(
            @PathVariable("id") Long roleId,
            @Validated @RequestBody RoleUpdateDTO updateDTO) {
        // TODO: 调用 roleService.updateRole
        return null;
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除闲置角色", description = "若角色已被用户绑定则拒绝删除")
    public Result<Void> deleteRole(@PathVariable("id") Long roleId) {
        // TODO: 调用 roleService.deleteRole
        return null;
    }

    @PostMapping("/{id}/permissions")
    @Operation(summary = "为角色分配权限菜单", description = "传入权限节点 ID 列表，全量覆盖")
    public Result<RoleVO> assignPermissions(
            @PathVariable("id") Long roleId,
            @RequestBody List<Long> permissionIds) {
        // TODO: 调用 roleService.assignPermissionsToRole
        return null;
    }
}