package com.yiwilee.aiqasystem.service.impl;

import com.yiwilee.aiqasystem.constant.SystemConstants;
import com.yiwilee.aiqasystem.converter.RoleConverter;
import com.yiwilee.aiqasystem.exception.RoleException; // 自定义异常
import com.yiwilee.aiqasystem.exception.ResourceNotFoundException;
import com.yiwilee.aiqasystem.model.dto.RoleCreateDTO;
import com.yiwilee.aiqasystem.model.dto.RoleUpdateDTO;
import com.yiwilee.aiqasystem.model.entity.SysPermission;
import com.yiwilee.aiqasystem.model.entity.SysRole;
import com.yiwilee.aiqasystem.model.vo.RoleVO;
import com.yiwilee.aiqasystem.repository.PermissionRepo;
import com.yiwilee.aiqasystem.repository.RoleRepo;
import com.yiwilee.aiqasystem.repository.UserRepo;
import com.yiwilee.aiqasystem.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepo roleRepo;
    private final RoleConverter roleConverter;
    private final UserRepo userRepo;
    private final PermissionRepo permissionRepo;

    @Override
    @Transactional(readOnly = true)
    public List<RoleVO> getAllRoles() {
        return roleConverter.toVOList(roleRepo.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleVO> pageRoles(String keyword, int pageNum, int pageSize) {
        int actualNum = Math.max(0, pageNum - 1);
        Pageable pageable = PageRequest.of(actualNum, pageSize, Sort.by("id").descending());

        Page<SysRole> rolePage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            rolePage = roleRepo.findByRoleNameContaining(keyword.trim(), pageable);
        } else {
            rolePage = roleRepo.findAll(pageable);
        }

        return rolePage.map(roleConverter::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO createRole(RoleCreateDTO createDTO) {
        String finalRoleCode = formatRoleCode(createDTO.roleCode());

        if (roleRepo.existsByRoleCode(finalRoleCode)) {
            throw new RoleException("角色代码 [" + finalRoleCode + "] 已存在，请更换！");
        }

        SysRole role = SysRole.builder()
                .roleName(createDTO.roleName())
                .roleCode(finalRoleCode)
                .description(createDTO.description())
                .build();

        log.info("新建角色成功: {}", finalRoleCode);
        return roleConverter.toVO(roleRepo.save(role));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO updateRole(Long roleId, RoleUpdateDTO updateDTO) {
        SysRole role = getByRoleId(roleId);

        role.setRoleName(updateDTO.roleName());
        role.setDescription(updateDTO.description());

        log.info("更新角色成功: ID={}", roleId);
        return roleConverter.toVO(roleRepo.save(role));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRole(Long roleId) {
        SysRole role = getByRoleId(roleId);

        // 防御性校验：保护系统核心角色不被删除
        if (SystemConstants.DEFAULT_REGISTER_ROLE.equals(role.getRoleCode())) {
            throw new RoleException("系统默认基础角色，禁止删除！");
        }

        long userCount = userRepo.countUsersByRoleId(roleId);
        if (userCount > 0) {
            log.warn("拒绝删除角色 [{}], 关联用户数: {}", role.getRoleName(), userCount);
            throw new RoleException("无法删除：当前有 " + userCount + " 个用户绑定了此角色，请先解除绑定！");
        }

        roleRepo.delete(role);
        log.info("成功删除闲置角色: {}", role.getRoleName());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        SysRole role = getByRoleId(roleId);
        Set<SysPermission> targetPermissions = new HashSet<>(permissionRepo.findAllById(permissionIds));

        role.setPermissions(targetPermissions);
        log.info("为角色 [{}] 重新分配了 {} 个权限节点", role.getRoleName(), targetPermissions.size());

        return roleConverter.toVO(roleRepo.save(role));
    }

    @Override
    @Transactional(readOnly = true)
    public SysRole getDefaultRole() {
        return roleRepo.findByRoleCode(SystemConstants.DEFAULT_REGISTER_ROLE)
                .orElseThrow(() -> new RoleException("系统初始化异常：找不到默认基础角色配置！"));
    }

    @Override
    @Transactional(readOnly = true)
    public SysRole getByRoleCode(String roleCode) {
        String finalRoleCode = formatRoleCode(roleCode);
        return roleRepo.findByRoleCode(finalRoleCode)
                .orElseThrow(() -> new ResourceNotFoundException("找不到角色代码: " + finalRoleCode));
    }

    @Override
    @Transactional(readOnly = true)
    public SysRole getByRoleId(Long id) {
        return roleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到角色 ID: " + id));
    }

    // ---------------- 工具/辅助方法 ----------------

    private String formatRoleCode(String roleCode) {
        if (roleCode == null || roleCode.trim().isEmpty()) {
            throw new RoleException("角色代码不能为空");
        }
        String formatted = roleCode.trim().toUpperCase();
        return formatted.startsWith("ROLE_") ? formatted : "ROLE_" + formatted;
    }

    @Override
    public List<String> cleanAndParseRoles(List<String> rawRoles) {
        if (rawRoles == null || rawRoles.isEmpty()) {
            return Collections.singletonList(SystemConstants.DEFAULT_REGISTER_ROLE);
        }

        return rawRoles.stream()
                .filter(Objects::nonNull)
                .flatMap(role -> Arrays.stream(role.split(",")))
                .map(String::trim)
                .map(role -> role.replaceAll("^\"|\"$", "")) // 去除首尾双引号
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}