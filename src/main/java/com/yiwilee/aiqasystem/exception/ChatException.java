package com.yiwilee.aiqasystem.exception;

/**
 * 聊天会话模块专属业务异常
 */
public class ChatException extends SystemException {
    public ChatException(String message) {
        super(message);
    }
}