package com.yiwilee.aiqasystem.common;

import com.yiwilee.aiqasystem.exception.BusinessException;
import com.yiwilee.aiqasystem.exception.DisabledException;
import com.yiwilee.aiqasystem.exception.ResourceNotFoundException;
import com.yiwilee.aiqasystem.exception.SystemException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.security.SignatureException;
import java.util.stream.Collectors;

/**
 * 负责拦截 Controller 层及向下抛出的所有异常，将其转化为标准的 Result 响应体
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==========================================
    // 1. 业务逻辑异常拦截 (核心升级点)
    // ==========================================

    /**
     * 拦截统一的 BusinessException
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        // 打印业务日志时，带上具体的错误码，方便通过日志排查是哪里阻断的
        log.warn("业务规则阻断: [Code:{}] {}", e.getCode(), e.getMessage());
        return ResponseEntity.ok(Result.fail(e.getCode(), e.getMessage()));
    }

    /**
     * 兼容老代码抛出的 SystemException
     */
    @ExceptionHandler(SystemException.class)
    public ResponseEntity<Result<Void>> handleSystemException(SystemException e) {
        log.warn("老业务规则阻断: {}", e.getMessage());
        return ResponseEntity.ok(Result.fail(ResultCode.BAD_REQUEST.getCode(), e.getMessage()));
    }

    // ==========================================
    // 2. 资源查找异常拦截
    // ==========================================
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Result<Void>> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn("请求资源不存在: {}", e.getMessage());
        return ResponseEntity.ok(Result.fail(ResultCode.NOT_FOUND.getCode(), e.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("接口路由或资源不存在: {}", e.getMessage());
        return ResponseEntity.ok(Result.fail(ResultCode.NOT_FOUND.getCode(), "您请求的接口路径不存在，请检查 URL 是否正确"));
    }

    // ==========================================
    // 3. 身份与权限异常拦截 (Spring Security)
    // ==========================================
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Result<Void>> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("认证失败: 用户名或密码错误");
        // 自动映射为密码错误状态码
        return ResponseEntity.ok(Result.fail(ResultCode.USER_PASSWORD_ERROR));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Result<Void>> handleDisabledException(DisabledException e) {
        log.warn("用户已被禁用");
        return ResponseEntity.ok(Result.fail(ResultCode.USER_FORBIDDEN));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthenticationException(AuthenticationException e) {
        log.warn("凭证无效或过期: {}", e.getMessage());
        return ResponseEntity.ok(Result.fail(ResultCode.TOKEN_INVALID));
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Result<Void>> handleExpiredJwtException(ExpiredJwtException e) {
        log.warn("Token 已过期: {}", e.getMessage());
        return ResponseEntity.ok(Result.fail(ResultCode.TOKEN_EXPIRED));
    }

    @ExceptionHandler({SignatureException.class, MalformedJwtException.class, UnsupportedJwtException.class})
    public ResponseEntity<Result<Void>> handleInvalidJwtException(Exception e) {
        log.warn("Token 被篡改或格式非法: {}", e.getMessage());
        return ResponseEntity.ok(Result.fail(ResultCode.TOKEN_INVALID));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Result<Void>> handleGeneralJwtException(JwtException e) {
        log.warn("Token 校验失败: {}", e.getMessage());
        return ResponseEntity.ok(Result.fail(ResultCode.TOKEN_INVALID));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("越权访问被拦截: {}", e.getMessage());
        return ResponseEntity.ok(Result.fail(ResultCode.FORBIDDEN));
    }

    // ==========================================
    // 4. 参数校验异常拦截 (Validation) - 保持不变
    // ==========================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验未通过(DTO): {}", errorMsg);
        return ResponseEntity.ok(Result.fail(ResultCode.BAD_REQUEST.getCode(), errorMsg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String errorMsg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验未通过(路径/单参数): {}", errorMsg);
        return ResponseEntity.ok(Result.fail(ResultCode.BAD_REQUEST.getCode(), errorMsg));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String errorMsg = e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", errorMsg);
        return ResponseEntity.ok(Result.fail(ResultCode.BAD_REQUEST.getCode(), errorMsg));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("参数解析失败(JSON格式化错误): {}", e.getMessage());
        return ResponseEntity.ok(Result.fail(ResultCode.BAD_REQUEST.getCode(), "请求参数格式错误，请检查 JSON 数据"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {}", e.getName());
        return ResponseEntity.ok(Result.fail(ResultCode.BAD_REQUEST.getCode(), "参数类型错误: [" + e.getName() + "]"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("缺少必填参数: {}", e.getParameterName());
        return ResponseEntity.ok(Result.fail(ResultCode.BAD_REQUEST.getCode(), "缺少必填参数: " + e.getParameterName()));
    }

    // ==========================================
    // 5. 框架/系统级防御拦截 - 保持不变
    // ==========================================
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件上传体积超出限制: {}", e.getMessage());
        // 大文件拦截也应当返回 200 让前端处理
        return ResponseEntity.ok(Result.fail(ResultCode.DOCUMENT_SIZE_EXCEED));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方式不支持: {}", e.getMethod());
        return ResponseEntity.ok(Result.fail(405, "请求方法不支持: " + e.getMethod()));
    }

    @ExceptionHandler({ClientAbortException.class, HttpMessageNotWritableException.class, IOException.class})
    public void handleClientAbortException(Exception ex) {
        String message = ex.getMessage();
        if (message != null && (message.contains("Connection reset by peer") || message.contains("Broken pipe"))) {
            return;
        }
    }

    // ==========================================
    // 6. 终极兜底拦截 (系统内部致命错误)
    // ==========================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleAllUncaughtException(Exception e) {
        log.error("系统发生未知严重异常: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ResultCode.INTERNAL_SERVER_ERROR));
    }
}