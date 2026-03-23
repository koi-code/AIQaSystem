package com.yiwilee.aiqasystem.exception;

/**
 * RAG 检索与生成模块专属业务异常
 */
public class RagException extends SystemException {
    public RagException(String message) {
        super(message);
    }
    public RagException(String message, Throwable cause) {
        super(message, cause);
    }
}