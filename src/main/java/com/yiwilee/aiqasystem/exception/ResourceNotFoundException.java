package com.yiwilee.aiqasystem.exception;

/**
 * 资源不存在异常 (对应 HTTP 404)
 * 用于用户、角色、文档等记录在数据库中找不到的情况
 */
public class ResourceNotFoundException extends SystemException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}