package com.yiwilee.aiqasystem.model.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 返回给前端的用户基础信息
 * 【绝对不能包含 password 字段】
 */
public record UserVO(
        Long id,
        String username,
        Integer status,
        LocalDateTime createTime,
        List<String> roles // 直接返回前端 ["ROLE_ADMIN", "ROLE_TEACHER"] 方便前端做按钮级别的权限控制
) {}