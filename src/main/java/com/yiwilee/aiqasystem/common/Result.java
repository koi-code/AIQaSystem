package com.yiwilee.aiqasystem.common;

import io.milvus.param.R;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 工业级统一 API 响应包装类
 * 强制约束所有 Controller 的返回值格式，对前端极其友好
 */
@Data
@Schema(description = "统一 API 响应包装体")
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "业务状态码 (200表示成功，其他表示失败)", example = "200")
    private Integer code;

    @Schema(description = "响应提示信息", example = "操作成功")
    private String msg;

    @Schema(description = "实际响应数据 (泛型)")
    private T data;

    @Schema(description = "服务器响应时间戳 (毫秒级)", example = "1710000000000")
    private Long timestamp;

    /**
     * 架构师约束：私有化构造函数，强制要求通过静态工厂方法创建实例
     */
    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    private Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // ==========================================
    // 成功响应 (Success) 静态工厂方法
    // ==========================================
    /**
     * 成功：无返回数据 (常用于 DELETE/PUT 操作)
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), null);
    }

    /**
     * 成功：携带返回数据 (常用于 GET/POST 操作)
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    /**
     * 成功：自定义提示信息并携带数据
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), msg, data);
    }

    /**
     * 成功：自定义提示信息并携带数据
     */
    public static <T> Result<T> success(Integer code, String msg) {
        return new Result<>(ResultCode.SUCCESS.getCode(), msg, null);
    }

    /**
     * 成功：供测试服务器是否存活等方法使用
     * 提供精准的状态码
     */
    public static <T> Result<T> success(Integer code, String msg, T data) {
        return new Result<>(code, msg, data);
    }

    // ==========================================
    // 失败响应 (Fail) 静态工厂方法
    // ==========================================

    /**
     * 失败：使用默认的 500 内部错误
     */
    public static <T> Result<T> fail() {
        return new Result<>(ResultCode.INTERNAL_SERVER_ERROR.getCode(), ResultCode.INTERNAL_SERVER_ERROR.getMsg(), null);
    }

    /**
     * 失败：指定错误提示信息 (默认 400 状态码，常用于业务校验不通过)
     */
    public static <T> Result<T> fail(String msg) {
        return new Result<>(ResultCode.BAD_REQUEST.getCode(), msg, null);
    }

    /**
     * 失败：精确指定状态码和错误信息 (常供全局异常处理器调用)
     */
    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    /**
     * 失败：直接传入 ResultCode 枚举
     */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMsg(), null);
    }
}