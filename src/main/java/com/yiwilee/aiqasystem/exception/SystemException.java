package com.yiwilee.aiqasystem.exception;

/**
 * 系统级基础业务异常
 * 其他模块的自定义异常可以继承此类，便于全局拦截
 */
public class SystemException extends RuntimeException {

    public SystemException(String message) {
        super(message);
    }

    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }
}