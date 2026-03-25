package com.yiwilee.aiqasystem.controller;

import com.yiwilee.aiqasystem.common.Result;
import com.yiwilee.aiqasystem.common.ResultCode;
import com.yiwilee.aiqasystem.constant.ApiVersion;
import com.yiwilee.aiqasystem.model.dto.UserLoginDTO;
import com.yiwilee.aiqasystem.model.dto.UserRegisterDTO;
import com.yiwilee.aiqasystem.model.vo.LoginTokenVO;
import com.yiwilee.aiqasystem.model.vo.UserVO;
import com.yiwilee.aiqasystem.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 身份认证网关
 * 负责处理不需要 Token 即可访问的开放接口（登录、注册）
 */
@Slf4j
@RestController
@RequestMapping(ApiVersion.BASE_VERSION+"/auth")
@RequiredArgsConstructor
@Tag(name = "01. 身份认证", description = "提供用户登录、注册等公开访问接口")
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "验证账号密码，返回 JWT 令牌及用户基础信息")
    public Result<LoginTokenVO> login(@Validated @RequestBody UserLoginDTO loginDTO) {
        log.info("接收到用户登录请求: [{}]", loginDTO.username());

        // 调用 Service 层执行具体的认证逻辑并生成 Token 视图对象
        LoginTokenVO tokenVO = userService.login(loginDTO);

        log.info("用户 [{}] 登录成功，已签发 Token", loginDTO.username());
        return Result.success("登录成功", tokenVO);
    }

    @PostMapping("/register")
    @Operation(summary = "开放注册", description = "普通用户前端自行注册账号，默认分配基础角色")
    public Result<UserVO> register(@Validated @RequestBody UserRegisterDTO registerDTO) {
        log.info("接收到新用户注册请求: [{}]", registerDTO.username());

        // 调用 Service 层执行注册逻辑
        UserVO userVO = userService.register(registerDTO);

        log.info("新用户 [{}] 注册成功", registerDTO.username());
        return Result.success("注册成功", userVO);
    }

    @PostMapping("/health")
    @Operation(summary = "检测服务器状态（连通性）", description = "检测后端服务器是否运行正常，测试前后端的连通性")
    public ResponseEntity<Result<String>> pingHealth(@RequestParam("someData") String someData) {
        return ResponseEntity.ok(Result.success(ResultCode.SERVER_ALIVE.getCode(), ResultCode.SERVER_ALIVE.getMsg(), someData));
    }
}