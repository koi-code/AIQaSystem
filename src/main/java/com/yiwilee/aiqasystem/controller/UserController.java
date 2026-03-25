package com.yiwilee.aiqasystem.controller;

import com.yiwilee.aiqasystem.common.PageData;
import com.yiwilee.aiqasystem.common.Result;
import com.yiwilee.aiqasystem.constant.ApiVersion;
import com.yiwilee.aiqasystem.converter.UserConverter;
import com.yiwilee.aiqasystem.model.dto.UserCreateDTO;
import com.yiwilee.aiqasystem.model.entity.SysUser;
import com.yiwilee.aiqasystem.model.entity.UserPrincipal;
import com.yiwilee.aiqasystem.model.vo.UserVO;
import com.yiwilee.aiqasystem.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户资源管理控制器
 * 负责用户自身信息的获取，以及后台管理员对用户的 CRUD 操作
 */
@Slf4j
@RestController
@RequestMapping(ApiVersion.BASE_VERSION + "/users")
@RequiredArgsConstructor
@Tag(name = "02. 用户管理", description = "后台用户增删改查及角色分配接口")
public class UserController {

    private final UserService userService;
    private final UserConverter userConverter;

    // ==========================================
    // 1. 面向普通用户的接口 (仅需登录即可访问)
    // ==========================================

    @GetMapping("/me")
    @Operation(summary = "获取当前登录用户信息", description = "解析 Token 获取自身详细信息，杜绝越权查询")
    public Result<UserVO> getCurrentUser() {
        // 【核心安全点】：永远从服务端的 SecurityContext 中获取当前操作人
        Long currentUserId = extractCurrentUserId();

        UserVO userVO = userService.getUserById(currentUserId);
        return Result.success(userVO);
    }

    // ==========================================
    // 2. 面向管理员的接口 (通常需要特定角色才能访问)
    // ==========================================

    @GetMapping
    @Operation(summary = "分页查询用户列表", description = "支持按用户名模糊搜索")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')") // 预留权限控制注解
    public Result<PageData<UserVO>> pageUsers(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        // 调用 Service 获取分页数据
        return Result.success(PageData.of(userService.pageUsers(keyword, pageNum, pageSize)));
    }

    @PostMapping
    @Operation(summary = "管理员创建用户", description = "后台直接新增用户并指定初始角色和状态")
    public Result<UserVO> createUser(@Validated @RequestBody UserCreateDTO createDTO) {
        log.info("管理员正在后台创建新用户: {}", createDTO.username());

        UserVO userVO = userService.createUser(createDTO);

        return Result.success("用户创建成功", userVO);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "修改用户状态", description = "封禁或解封指定用户 (1-正常, 0-禁用)")
    public Result<Void> updateUserStatus(
            @PathVariable("id") Long userId,
            @Parameter(description = "状态值: 1-正常, 0-禁用") @RequestParam Integer status) {

        log.info("管理员修改用户 [{}] 状态为: {}", userId, status);
        userService.updateUserStatus(userId, status);

        return Result.success("状态更新成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "彻底删除用户", description = "级联删除该用户的所有聊天记录和向量文档，极其危险！")
    public Result<Void> deleteUser(@PathVariable("id") Long userId) {
        // 防止管理员把自己删了 (防呆设计)
        if (userId.equals(extractCurrentUserId())) {
            return Result.fail("非法操作：您不能删除您自己的账号！");
        }

        log.warn("管理员发起彻底删除用户 [{}] 的危险操作", userId);
        userService.deleteUser(userId);

        return Result.success("用户已被彻底删除", null);
    }

    @PostMapping("/{id}/roles")
    @Operation(summary = "为用户分配角色", description = "传入角色 ID 列表，全量覆盖用户的角色关联")
    public Result<Void> assignRolesToUser(
            @PathVariable("id") Long userId,
            @RequestBody List<Long> roleIds) {

        log.info("管理员为用户 [{}] 重新分配角色", userId);
        userService.assignRolesToUser(userId, roleIds);

        return Result.success("角色分配成功", null);
    }

    // ==========================================
    // 内部安全辅助方法
    // ==========================================

    /**
     * 从 Spring Security 上下文中安全提取当前登录用户的 ID
     */
    private Long extractCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getSysUser().getId();
        }
        log.error("尝试获取当前用户信息失败：SecurityContext 中无有效的 UserPrincipal");
        throw new AccessDeniedException("登录状态异常，请重新登录");
    }
}