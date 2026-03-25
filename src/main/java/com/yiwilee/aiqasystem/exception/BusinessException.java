package com.yiwilee.aiqasystem.exception;

import com.yiwilee.aiqasystem.common.ResultCode;
import lombok.Getter;

/**
 * 企业级通用业务异常基类
 * 在 Service 层校验逻辑不通过时，直接 throw new BusinessException(ResultCode.xxx)
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;
    private final String msg;

    /**
     * 直接使用状态码枚举构造
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
        this.msg = resultCode.getMsg();
    }

    /**
     * 覆盖枚举的默认提示语 (常用于拼接动态参数，如: "文档 [xxx.pdf] 解析失败")
     */
    public BusinessException(ResultCode resultCode, String customMsg) {
        super(customMsg);
        this.code = resultCode.getCode();
        this.msg = customMsg;
    }

    /**
     * 完全自定义 code 和 msg
     */
    public BusinessException(Integer code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }
}