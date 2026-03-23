package com.yiwilee.aiqasystem.exception;

/**
 * 知识库/文档模块专属业务异常
 */
public class DocumentException extends SystemException {
    public DocumentException(String message) {
        super(message);
    }

    public DocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}