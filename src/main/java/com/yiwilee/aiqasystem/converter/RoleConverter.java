package com.yiwilee.aiqasystem.converter;

import com.yiwilee.aiqasystem.model.entity.SysPermission;
import com.yiwilee.aiqasystem.model.entity.SysRole;
import com.yiwilee.aiqasystem.model.vo.RoleVO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RoleConverter {

    public RoleVO toVO(SysRole role) {
        if (role == null) {
            return null;
        }

        // 提取该角色拥有的所有权限 ID
        List<Long> permissionIds = Collections.emptyList();
        if (role.getPermissions() != null && !role.getPermissions().isEmpty()) {
            permissionIds = role.getPermissions().stream()
                    .map(SysPermission::getId)
                    .collect(Collectors.toList());
        }

        return new RoleVO(
                role.getId(),
                role.getRoleName(),
                role.getRoleCode(),
                role.getDescription(),
                permissionIds
        );
    }

    public List<RoleVO> toVOList(List<SysRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }
}