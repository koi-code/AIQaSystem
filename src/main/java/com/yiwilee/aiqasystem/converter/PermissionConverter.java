package com.yiwilee.aiqasystem.converter;

import com.yiwilee.aiqasystem.model.entity.SysPermission;
import com.yiwilee.aiqasystem.model.vo.PermissionVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限（菜单）实体与视图对象转换器
 */
@Component
public class PermissionConverter {

    /**
     * 将 SysPermission 实体转换为 PermissionVO
     * * @param permission 权限实体
     * @return PermissionVO 权限视图对象
     */
    public PermissionVO toVO(SysPermission permission) {
        if (permission == null) {
            return null;
        }

        return new PermissionVO(
                permission.getId(),
                permission.getParentId(),
                permission.getPermName(),
                permission.getPermCode(),
                permission.getType(),
                permission.getPath(),
                // 【关键防坑】：这里必须初始化为 new ArrayList<>()，而不能用 Collections.emptyList()。
                // 因为返回的 VO 需要在 Service 层参与树形结构的组装（会调用 children.add() 方法）。
                new ArrayList<>()
        );
    }

    /**
     * 批量转换实体列表为 VO 列表
     * * @param permissions 实体列表
     * @return VO 列表
     */
    public List<PermissionVO> toVOList(List<SysPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }

        return permissions.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }
}