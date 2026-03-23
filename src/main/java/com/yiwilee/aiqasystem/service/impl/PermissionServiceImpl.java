package com.yiwilee.aiqasystem.service.impl;

import com.yiwilee.aiqasystem.model.entity.SysPermission;
import com.yiwilee.aiqasystem.repository.PermissionRepo;
import com.yiwilee.aiqasystem.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepo permissionRepo;

    @Override
    @Transactional(readOnly = true) // 只读事务，提高查询性能
    public Set<SysPermission> getAllPermissionsById(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(permissionRepo.findAllById(permissionIds));
    }
}