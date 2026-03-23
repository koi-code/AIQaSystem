package com.yiwilee.aiqasystem.model.vo;

/**
 * 登录成功后返回给前端的完整凭证
 */
public record LoginTokenVO(
        String token,      // JWT 令牌字符串
        UserVO userInfo    // 用户的基本信息
) {}