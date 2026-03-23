package com.yiwilee.aiqasystem.common;

import com.yiwilee.aiqasystem.common.Result;
import com.yiwilee.aiqasystem.common.ResultCode;
import com.yiwilee.aiqasystem.exception.ResourceNotFoundException;
import com.yiwilee.aiqasystem.exception.SystemException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.security.SignatureException;
import java.util.stream.Collectors;

/**
 * 负责拦截 Controller 层及向下抛出的所有异常，将其转化为标准的 Result 响应体，
 * 并配合合理的 HTTP 状态码返回给前端，同时进行分级日志记录。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==========================================
    // 1. 业务逻辑异常拦截 (HTTP 400 Bad Request)
    // ==========================================

    /**
     * 拦截所有派生自 SystemException 的自定义业务异常
     * (包含 UserException, RoleException, DocumentException, ChatException, RagException)
     */
    @ExceptionHandler(SystemException.class)
    public ResponseEntity<Result<Void>> handleSystemException(SystemException e) {
        // 业务异常通常是用户操作不当引起，使用 WARN 级别记录，不打印长堆栈，防止日志污染
        log.warn("业务规则阻断: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(ResultCode.BAD_REQUEST.getCode(), e.getMessage()));
    }

    // ==========================================
    // 2. 资源查找异常拦截 (HTTP 404 Not Found)
    // ==========================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Result<Void>> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn("请求资源不存在: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(ResultCode.NOT_FOUND.getCode(), e.getMessage()));
    }

    /**
     * 拦截 Spring MVC 接口路由或资源不存在异常 (HTTP 404)
     * 适用于 Spring Boot 3.2+ 新特性
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("接口路由或资源不存在: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(ResultCode.NOT_FOUND.getCode(), "您请求的接口路径不存在，请检查 URL 是否正确"));
    }

    // ==========================================
    // 3. 身份与权限异常拦截 (Spring Security)
    // ==========================================

    /**
     * 拦截登录时的账号密码错误 (HTTP 401)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Result<Void>> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("认证失败: 用户名或密码错误");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.fail(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误"));
    }

    /**
     * 拦截其他的认证异常（如 Token 失效/伪造）(HTTP 401)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthenticationException(AuthenticationException e) {
        log.warn("凭证无效或过期: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.fail(ResultCode.UNAUTHORIZED.getCode(), "登录凭证已失效，请重新登录"));
    }

    // ==========================================
    // 精细化 JWT 异常拦截
    // ==========================================

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Result<Void>> handleExpiredJwtException(ExpiredJwtException e) {
        log.warn("Token 已过期: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.fail(ResultCode.UNAUTHORIZED.getCode(), "登录凭证已过期，请重新登录"));
    }

    @ExceptionHandler({SignatureException.class, MalformedJwtException.class, UnsupportedJwtException.class})
    public ResponseEntity<Result<Void>> handleInvalidJwtException(Exception e) {
        log.warn("Token 被篡改或格式非法: {}", e.getMessage());
        // 安全防御：如果检测到篡改，也可以记录发起请求的 IP 放入黑名单
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.fail(ResultCode.UNAUTHORIZED.getCode(), "非法的登录凭证，拒绝访问"));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Result<Void>> handleGeneralJwtException(JwtException e) {
        log.warn("Token 校验失败: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.fail(ResultCode.UNAUTHORIZED.getCode(), "身份认证失败，请重新登录"));
    }

    /**
     * 拦截权限不足异常 (HTTP 403)
     * 例如：普通用户试图访问 @PreAuthorize("hasRole('ADMIN')") 的接口
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("越权访问被拦截: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.fail(ResultCode.FORBIDDEN.getCode(), "抱歉，您没有权限执行此操作"));
    }

    // ==========================================
    // 4. 参数校验异常拦截 (Validation) (HTTP 400)
    // ==========================================

    /**
     * 拦截 @RequestBody 中 DTO 对象的 @Validated 校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 提取所有的验证错误信息，用逗号拼接返回给前端展示
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("参数校验未通过(DTO): {}", errorMsg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(ResultCode.BAD_REQUEST.getCode(), errorMsg));
    }

    /**
     * 拦截 @RequestParam 或 @PathVariable 上的基础类型校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String errorMsg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        log.warn("参数校验未通过(路径/单参数): {}", errorMsg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(ResultCode.BAD_REQUEST.getCode(), errorMsg));
    }

    /**
     * 拦截表单提交 (form-data) 时的参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String errorMsg = e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("参数绑定失败: {}", errorMsg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(ResultCode.BAD_REQUEST.getCode(), errorMsg));
    }

    // ==========================================
    // 5. 框架/系统级防御拦截
    // ==========================================

    /**
     * 拦截文件上传超过限制异常 (HTTP 413)
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件上传体积超出限制: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Result.fail(413, "上传文件过大，请压缩后重试"));
    }

    /**
     * 拦截请求方法不支持异常 (如 GET 请求访问 POST 接口) (HTTP 405)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方式不支持: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.fail(405, "请求方法不支持: " + e.getMethod()));
    }

    // ==========================================
    // 6. 终极兜底拦截 (HTTP 500 Internal Server Error)
    // ==========================================

    /**
     * 拦截所有未预料到的 RuntimeException 或 Exception
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleAllUncaughtException(Exception e) {
        // 核心关注点：只有未知的异常才使用 ERROR 级别，并打印完整堆栈！
        log.error("系统发生未知严重异常: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ResultCode.INTERNAL_SERVER_ERROR));
    }
}