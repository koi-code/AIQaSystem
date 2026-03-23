package com.yiwilee.aiqasystem.model.vo;

import java.util.List;

public record RoleVO(
        Long id,
        String roleName,
        String roleCode,
        String description,
        // 这里只返回权限的 ID 列表。前端在打开“分配权限”弹窗时，
        // 可以根据这个 List 自动把权限树上的对应节点打上 ✅ 勾。
        List<Long> permissionIds
) {}