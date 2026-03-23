package com.yiwilee.aiqasystem.service;

import com.yiwilee.aiqasystem.model.entity.SysPermission;
import java.util.List;
import java.util.Set;

/**
 * 权限（菜单/按钮）业务逻辑接口
 * 管理系统内的细粒度访问控制资源。
 */
public interface PermissionService {

    /**
     * 根据给定的一组权限 ID，批量查询对应的权限实体集合
     * 此方法主要供内部 RoleService 绑定权限时调用，确保脏数据和无效 ID 被过滤。
     * * @param permissionIds 权限节点的主键 ID 列表（通常由前端 Tree 树形组件勾选产生）
     * @return Set<SysPermission> 去重后的合法权限实体集合，若传入 null 或空列表则返回空 Set
     */
    Set<SysPermission> getAllPermissionsById(List<Long> permissionIds);
}