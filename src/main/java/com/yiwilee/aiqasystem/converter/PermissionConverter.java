package com.yiwilee.aiqasystem.converter;

import com.yiwilee.aiqasystem.model.entity.SysPermission;
import com.yiwilee.aiqasystem.model.vo.PermissionVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PermissionConverter {

    /**
     * 单个对象转换
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
                new ArrayList<>() // 务必初始化为空列表，防止前端读取 children 时报 null 异常
        );
    }

    /**
     * 【企业级核心】：将平铺的数据库记录，组装成带有层级关系的树形菜单！
     */
    public List<PermissionVO> buildTree(List<SysPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 全部转换为 VO
        List<PermissionVO> allVOs = permissions.stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        // 2. 将所有的 VO 按 ID 放入 Map 中，方便后续快速查找父节点
        Map<Long, PermissionVO> voMap = allVOs.stream()
                .collect(Collectors.toMap(PermissionVO::id, vo -> vo));

        List<PermissionVO> tree = new ArrayList<>();

        // 3. 遍历组装
        for (PermissionVO vo : allVOs) {
            // 假设 parentId == 0 代表它是最顶层的根目录
            if (vo.parentId() == 0L) {
                tree.add(vo);
            } else {
                // 如果它有爸爸，就把自己塞进爸爸的 children 列表里
                PermissionVO parent = voMap.get(vo.parentId());
                if (parent != null) {
                    parent.children().add(vo);
                }
            }
        }

        return tree;
    }
}