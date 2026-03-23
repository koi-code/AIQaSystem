package com.yiwilee.aiqasystem.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一 API 响应状态码枚举
 * 遵循 HTTP 状态码语义，便于前端进行统一拦截
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "参数错误或业务校验失败"),
    UNAUTHORIZED(401, "暂未登录或 Token 已过期"),
    FORBIDDEN(403, "没有权限访问该资源"),
    NOT_FOUND(404, "请求的资源不存在"),
    INTERNAL_SERVER_ERROR(500, "系统内部异常，请联系管理员");

    private final int code;
    private final String msg;
}