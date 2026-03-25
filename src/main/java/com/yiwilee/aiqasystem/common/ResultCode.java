package com.yiwilee.aiqasystem.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工业级统一 API 响应状态码枚举
 * 规范：
 * 1. 基础 HTTP 状态码保留 (200, 400, 401, 403, 404, 500)
 * 2. 业务状态码采用 5 位数字: 模块标识(2位) + 具体错误(3位)
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // ==========================================
    // 基础通用状态码 (保留 HTTP 语义)
    // ==========================================
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "参数错误或业务校验失败"),
    UNAUTHORIZED(401, "暂未登录或 Token 已过期"),
    FORBIDDEN(403, "没有权限访问该资源"),
    NOT_FOUND(404, "请求的资源不存在"),
    INTERNAL_SERVER_ERROR(500, "系统内部异常，请联系管理员"),

    SERVER_ALIVE(20000, "服务器运行正常"),

    // ==========================================
    // [10xxx] 用户模块 (User)
    // ==========================================
    USER_NOT_FOUND(10001, "用户不存在"),
    USER_EXISTED(10002, "用户名已被注册，请更换"),
    USER_PASSWORD_ERROR(10003, "账号或密码错误"),
    USER_FORBIDDEN(10004, "账号已被封禁，请联系管理员"),
    USER_OLD_PASSWORD_ERROR(10005, "原密码错误"),
    USER_INFO_COMPLETE_FAILED(10006, "完善用户信息失败"),

    // ==========================================
    // [20xxx] 认证与安全模块 (Auth & Security)
    // ==========================================
    TOKEN_INVALID(20001, "无效的登录状态，请重新登录"),
    TOKEN_EXPIRED(20002, "登录已过期，请重新登录"),
    TOKEN_KICKED_OUT(20003, "您的账号已在其他设备登录"),
    CAPTCHA_ERROR(20004, "验证码错误或已失效"),

    // ==========================================
    // [30xxx] 核心业务模块 (AI 对话 & 文档知识库)
    // ==========================================
    DOCUMENT_NOT_FOUND(30001, "指定的文档不存在或已被删除"),
    DOCUMENT_PARSE_ERROR(30002, "文档解析失败，请检查文件格式"),
    DOCUMENT_SIZE_EXCEED(30003, "文档体积超出限制"),
    SESSION_NOT_FOUND(30004, "对话会话不存在"),
    QA_LIMIT_EXCEEDED(30005, "今日提问次数已达上限"),
    AI_SERVICE_UNAVAILABLE(30006, "AI 大模型服务暂不可用，请稍后再试"),
    AI_CONTENT_BLOCKED(30007, "提问或回答内容触发安全合规拦截"),

    // ==========================================
    // [50xxx] 第三方与系统底层依赖 (System & Third-Party)
    // ==========================================
    DB_OPERATION_ERROR(50001, "数据库操作失败"),
    OSS_UPLOAD_ERROR(50002, "文件存储服务上传失败"),
    MILVUS_CONNECT_ERROR(50003, "向量数据库连接失败");


    private final int code;
    private final String msg;
}