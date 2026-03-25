package com.yiwilee.aiqasystem.service;

import com.yiwilee.aiqasystem.model.dto.PermissionCreateDTO;
import com.yiwilee.aiqasystem.model.dto.PermissionUpdateDTO;
import com.yiwilee.aiqasystem.model.entity.SysPermission;
import com.yiwilee.aiqasystem.model.vo.PermissionVO;

import java.util.List;
import java.util.Set;

/**
 * 权限（菜单/按钮）业务逻辑接口
 * 管理系统内的细粒度访问控制资源，提供菜单树构建及基础的维护功能。
 */
public interface PermissionService {

    /**
     * 根据给定的一组权限 ID，批量查询对应的权限实体集合
     * 此方法主要供内部 RoleService 绑定权限时调用，确保脏数据和无效 ID 被过滤。
     *
     * @param permissionIds 权限节点的主键 ID 列表（通常由前端 Tree 树形组件勾选产生）
     * @return Set<SysPermission> 去重后的合法权限实体集合，若传入 null 或空列表则返回空 Set
     */
    Set<SysPermission> getAllPermissionsById(List<Long> permissionIds);

    /**
     * 获取全量权限菜单树
     * 将底层扁平化的权限数据，通过内存哈希映射（O(N) 复杂度）组装成具有层级关系的树形结构。
     * 主要用于前端动态路由菜单的渲染，以及角色分配权限时的级联选择器。
     *
     * @return List<PermissionVO> 嵌套的权限视图对象列表，顶级节点的 parentId 默认约定为 0
     */
    List<PermissionVO> getPermissionTree();

    /**
     * 创建新的权限/菜单节点
     * 支持创建目录、页面菜单或页面内按钮。系统会校验传入的 parentId 是否合法。
     *
     * @param createDTO 包含权限基本信息的创建载体 (Record)
     * @return PermissionVO 包含生成 ID 的新建权限视图对象
     * @throws com.yiwilee.aiqasystem.exception.RoleException 当指定的父节点 ID 不存在时抛出
     */
    PermissionVO createPermission(PermissionCreateDTO createDTO);

    /**
     * 更新现有的权限/菜单节点信息
     * 允许修改节点名称、前端路由路径及权限标识符。注意：修改权限标识符可能导致前端依赖该标识符的按钮失效。
     *
     * @param id        待更新的权限节点主键 ID
     * @param updateDTO 包含修改内容的更新载体 (Record)
     * @return PermissionVO 更新后的权限视图对象
     * @throws com.yiwilee.aiqasystem.exception.ResourceNotFoundException 当指定 ID 的节点不存在时抛出
     */
    PermissionVO updatePermission(Long id, PermissionUpdateDTO updateDTO);

    /**
     * 删除指定的权限/菜单节点
     * 具备防御性删除机制：在执行删除前，会强制校验该节点下是否还存在子节点。
     * * @param id 待删除的权限节点主键 ID
     * @throws com.yiwilee.aiqasystem.exception.RoleException 当该节点下仍有子节点时，拒绝删除并抛出异常
     */
    void deletePermission(Long id);
}