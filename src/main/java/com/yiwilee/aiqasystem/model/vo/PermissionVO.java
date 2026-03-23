package com.yiwilee.aiqasystem.model.vo;

import java.util.List;

/**
 * 权限视图对象
 * 完美适配前端如 Element-UI / Ant Design 的 Tree 树形控件
 */
public record PermissionVO(
        Long id,
        Long parentId,
        String permName, // 菜单/按钮名称，如 "用户管理"
        String permCode, // 权限标识，如 "sys:user:add"
        Integer type,    // 菜单类型 (如: 1-左侧目录, 2-页面菜单, 3-页面内按钮)
        String path,     // 前端路由地址，如 "/admin/users"
        List<PermissionVO> children // 核心：包含它的所有子菜单
) {}