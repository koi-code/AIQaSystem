package com.yiwilee.aiqasystem.controller;

import com.yiwilee.aiqasystem.common.Result;
import com.yiwilee.aiqasystem.common.ResultCode;
import com.yiwilee.aiqasystem.constant.ApiVersion;
import com.yiwilee.aiqasystem.model.dto.ChatSessionUpdateDTO;
import com.yiwilee.aiqasystem.model.vo.ChatMessageVO;
import com.yiwilee.aiqasystem.model.vo.ChatSessionVO;
import com.yiwilee.aiqasystem.service.ChatService;
import com.yiwilee.aiqasystem.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能对话会话控制器
 * 负责侧边栏会话的 CRUD 以及对应聊天记录的拉取
 */
@Slf4j
@RestController
@RequestMapping(ApiVersion.BASE_VERSION+"/chat-sessions")
@RequiredArgsConstructor
@Tag(name = "05. 对话记录管理", description = "侧边栏多轮对话会话的 CRUD 及消息记录拉取")
public class ChatSessionController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "新建空白会话", description = "在左侧边栏创建一个默认标题的新会话")
    public Result<ChatSessionVO> createSession() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 [{}] 请求创建新对话会话", userId);

        ChatSessionVO sessionVO = chatService.createSession(userId);
        return Result.success("会话创建成功", sessionVO);
    }

    @GetMapping
    @Operation(summary = "获取当前用户的会话列表", description = "按最后活跃时间倒序排列，不含庞大的消息体数据，保证侧边栏秒开")
    public Result<List<ChatSessionVO>> getMySessions() {
        Long userId = SecurityUtils.getCurrentUserId();

        List<ChatSessionVO> sessions = chatService.getUserSessions(userId);
        if(sessions.isEmpty()) {
//            log.warn("{} 用户没有任何对话历史，将创建新的对话...", userId);
//            chatService.createSession(userId);
            return Result.success(ResultCode.NOT_FOUND.getCode(), "空会话记录");
        }
        return Result.success(sessions);
    }

    @PutMapping("/{id}/title")
    @Operation(summary = "重命名会话标题", description = "更新后该会话会自动置顶活跃时间")
    public Result<ChatSessionVO> updateSessionTitle(
            @PathVariable("id") Long sessionId,
            @Validated @RequestBody ChatSessionUpdateDTO updateDTO) { // DTO参数校验防火墙

        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 [{}] 请求修改会话 [{}] 标题为: {}", userId, sessionId, updateDTO.newTitle());

        // 传递 userId 到 Service 层进行 IDOR 水平越权拦截
        ChatSessionVO sessionVO = chatService.updateSessionTitle(sessionId, userId, updateDTO);
        return Result.success("标题更新成功", sessionVO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除指定会话")
    @PreAuthorize("hasRole('ADMIN') or @chatAuth.isOwner(#sessionId)")
    public Result<Void> deleteSession(@PathVariable("id") Long sessionId) {
        // 鉴权已经通过了，直接闭着眼睛删就行
        chatService.deleteSession(sessionId);
        return Result.success("会话已删除", null);
    }

//    @GetMapping("/{id}/messages")
//    @Operation(summary = "拉取会话的全部历史消息", description = "点击左侧会话时调用，返回按时间正序排列的详细问答")
//    public Result<List<ChatMessageVO>> getSessionMessages(@PathVariable("id") Long sessionId) {
//        Long userId = SecurityUtils.getCurrentUserId();
//
//        // 传递 userId 到 Service 层进行 IDOR 水平越权拦截，只返回脱敏的 VO
//        List<ChatMessageVO> messages = chatService.getSessionMessagesForFrontend(sessionId, userId);
//        return Result.success(messages);
//    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "拉取会话的全部历史消息", description = "已优化为分页查询，默认按时间倒序返回最新 50 条")
    @PreAuthorize("hasRole('ADMIN') or @chatAuth.isOwner(#sessionId)")
    public Result<Page<ChatMessageVO>> getSessionMessages(
            @PathVariable("id") Long sessionId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "50") int pageSize) { // 在这里设置默认阈值为 50

        Long userId = SecurityUtils.getCurrentUserId();

        // 传递分页参数到 Service 层，并返回 Spring 的 Page 包装对象
        Page<ChatMessageVO> messagesPage = chatService.getSessionMessagesForFrontend(sessionId, pageNum, pageSize);

        return Result.success(messagesPage);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "【管理员】获取指定用户的会话列表", description = "供后台管理员查看特定用户的全部对话历史")
    public Result<List<ChatSessionVO>> getUserSessionsByAdmin(@PathVariable("userId") Long targetUserId) {
        // 利用 SecurityUtils 判断当前登录者是否为管理员
        boolean isAdmin = SecurityUtils.isAdmin();

        List<ChatSessionVO> sessions = chatService.getUserSessionsByAdmin(targetUserId);
        return Result.success(sessions);
    }

    @GetMapping("/{id}/admin/messages")
    @Operation(summary = "【管理员】调取会话详情", description = "不受所有者限制，供后台管理或合规审查使用")
    // 核心：只允许 ADMIN 角色进入
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<ChatMessageVO>> getSessionMessagesByAdmin(
            @PathVariable("id") Long sessionId,
                    @RequestParam(defaultValue = "1") int pageNum,
    @RequestParam(defaultValue = "50") int pageSize) {

        // 直接调用 Service 逻辑，因为能进到这里的已经是合法的管理员
        Page<ChatMessageVO> messagesPage = chatService.getSessionMessagesForFrontend(sessionId, pageNum, pageSize);

        return Result.success(messagesPage);
    }
}