package com.yiwilee.aiqasystem.converter;

import com.yiwilee.aiqasystem.model.entity.SysRole;
import com.yiwilee.aiqasystem.model.entity.SysUser;
import com.yiwilee.aiqasystem.model.vo.UserVO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User 模块的对象转换器
 */
@Component
public class UserConverter {

    /**
     * 将 SysUser 实体转换为给前端展示的 UserVO
     */
    public UserVO toVO(SysUser user) {
        if (user == null) {
            return null;
        }

        // 安全提取用户的角色列表
        List<String> roleCodes = Collections.emptyList();
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            roleCodes = user.getRoles().stream()
                    .map(SysRole::getRoleCode)
                    .collect(Collectors.toList());
        }

        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getStatus(),
                user.getCreateTime(),
                roleCodes
        );
    }

    /**
     * 将 SysUser 实体列表批量转换为 UserVO 列表
     */
    public List<UserVO> toVOList(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        return users.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }
}