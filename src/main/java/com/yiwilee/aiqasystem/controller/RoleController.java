package com.yiwilee.aiqasystem.controller;

import com.yiwilee.aiqasystem.common.PageData;
import com.yiwilee.aiqasystem.common.Result;
import com.yiwilee.aiqasystem.constant.ApiVersion;
import com.yiwilee.aiqasystem.model.dto.RoleCreateDTO;
import com.yiwilee.aiqasystem.model.dto.RoleUpdateDTO;
import com.yiwilee.aiqasystem.model.vo.RoleVO;
import com.yiwilee.aiqasystem.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiVersion.BASE_VERSION+"/roles")
@RequiredArgsConstructor // 架构最佳实践：使用构造器注入依赖，保证不可变性
@Tag(name = "03. 角色权限管理", description = "RBAC 模型中的角色维护及权限节点绑定")
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/all")
    @Operation(summary = "获取所有角色集合", description = "通常用于用户管理页面的下拉选择框")
    public Result<List<RoleVO>> getAllRoles() {
        List<RoleVO> roles = roleService.getAllRoles();
        return Result.success(roles);
    }

    @GetMapping
    @Operation(summary = "分页查询角色列表", description = "支持按角色名称模糊搜索")
    public Result<PageData<RoleVO>> pageRoles(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<RoleVO> pageData = roleService.pageRoles(keyword, pageNum, pageSize);
        return Result.success(PageData.of(pageData));
    }

    @PostMapping
    @Operation(summary = "创建新角色", description = "防呆设计：角色代码会自动转大写并拼装 ROLE_ 前缀")
    public Result<RoleVO> createRole(@Validated @RequestBody RoleCreateDTO createDTO) {
        RoleVO createdRole = roleService.createRole(createDTO);
        return Result.success(createdRole);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新角色信息", description = "仅允许更新名称和描述，角色代码禁止修改")
    public Result<RoleVO> updateRole(
            @PathVariable("id") Long roleId,
            @Validated @RequestBody RoleUpdateDTO updateDTO) {
        RoleVO updatedRole = roleService.updateRole(roleId, updateDTO);
        return Result.success(updatedRole);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除闲置角色", description = "若角色已被用户绑定则拒绝删除")
    public Result<Void> deleteRole(@PathVariable("id") Long roleId) {
        roleService.deleteRole(roleId);
        // 删除操作通常不返回具体数据，返回 success 空载体即可
        return Result.success(null);
    }

    @PostMapping("/{id}/permissions")
    @Operation(summary = "为角色分配权限菜单", description = "传入权限节点 ID 列表，全量覆盖。传空数组 [] 表示清空该角色所有权限")
    public Result<RoleVO> assignPermissions(
            @PathVariable("id") Long roleId,
            // 加上 @Validated 和 @NotNull，确保前端哪怕要清空权限，也必须传一个空数组 [] 而不是不传
            @Validated @NotNull(message = "权限ID列表不能为空，若要清空请传空数组") @RequestBody List<Long> permissionIds) {

        RoleVO updatedRole = roleService.assignPermissionsToRole(roleId, permissionIds);
        return Result.success(updatedRole);
    }
}