package com.yiwilee.aiqasystem.service.impl;

import com.yiwilee.aiqasystem.converter.PermissionConverter;
import com.yiwilee.aiqasystem.exception.ResourceNotFoundException;
import com.yiwilee.aiqasystem.exception.RoleException;
import com.yiwilee.aiqasystem.model.dto.PermissionCreateDTO;
import com.yiwilee.aiqasystem.model.dto.PermissionUpdateDTO;
import com.yiwilee.aiqasystem.model.entity.SysPermission;
import com.yiwilee.aiqasystem.model.vo.PermissionVO;
import com.yiwilee.aiqasystem.repository.PermissionRepo;
import com.yiwilee.aiqasystem.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepo permissionRepo;
    private final PermissionConverter permissionConverter; // 引入独立的转换器

    @Override
    @Transactional(readOnly = true)
    public Set<SysPermission> getAllPermissionsById(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(permissionRepo.findAllById(permissionIds));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionVO> getPermissionTree() {
        // 1. 全量查出所有权限节点
        List<SysPermission> allPermissions = permissionRepo.findAll();

        // 2. 利用 Converter 转化为 VO 列表
        List<PermissionVO> allVOs = permissionConverter.toVOList(allPermissions);

        // 3. 构建 O(1) 查找的哈希映射 (K: id, V: VO)，极大提升后续组装效率
        Map<Long, PermissionVO> voMap = allVOs.stream()
                .collect(Collectors.toMap(PermissionVO::id, vo -> vo));

        List<PermissionVO> tree = new ArrayList<>();

        // 4. 一次遍历组装树 (O(N) 复杂度)
        for (PermissionVO vo : allVOs) {
            if (vo.parentId() == 0L) {
                // 父节点为 0 即为根节点，直接加入顶层树
                tree.add(vo);
            } else {
                // 找到其父节点，并将自己塞入父节点的 children 列表中
                PermissionVO parent = voMap.get(vo.parentId());
                if (parent != null) {
                    parent.children().add(vo);
                } else {
                    log.warn("数据异常：发现孤儿权限节点: id={}, parentId={}", vo.id(), vo.parentId());
                }
            }
        }
        return tree;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionVO createPermission(PermissionCreateDTO createDTO) {
        // 业务防呆：如果 parentId 不为 0 (非根节点)，确保指定的父节点确实存在
        if (createDTO.parentId() != 0L && !permissionRepo.existsById(createDTO.parentId())) {
            throw new RoleException("指定的父节点 ID 不存在");
        }

        SysPermission permission = SysPermission.builder()
                .parentId(createDTO.parentId())
                .permName(createDTO.permName())
                .permCode(createDTO.permCode())
                .type(createDTO.type())
                .path(createDTO.path())
                .build();

        SysPermission saved = permissionRepo.save(permission);
        log.info("新增权限节点成功: 节点名称[{}]", saved.getPermName());

        return permissionConverter.toVO(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionVO updatePermission(Long id, PermissionUpdateDTO updateDTO) {
        SysPermission permission = permissionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到指定的权限节点: " + id));

        permission.setPermName(updateDTO.permName());
        permission.setPermCode(updateDTO.permCode());
        permission.setPath(updateDTO.path());

        SysPermission updated = permissionRepo.save(permission);
        log.info("更新权限节点成功: ID=[{}]", id);

        return permissionConverter.toVO(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(Long id) {
        // 防御性校验：如果有子节点，绝对不允许直接删除，防止导致树形结构崩溃
        boolean hasChildren = permissionRepo.existsByParentId(id);
        if (hasChildren) {
            log.warn("拒绝删除权限节点: ID=[{}], 原因: 存在级联子节点", id);
            throw new RoleException("该节点下包含子节点，请先删除底层的子节点！");
        }

        // TODO: 可在此处增加校验，检查当前权限是否被 SysRole 绑定。若绑定较多可拒绝删除。

        permissionRepo.deleteById(id);
        log.info("删除权限节点成功: ID=[{}]", id);
    }
}