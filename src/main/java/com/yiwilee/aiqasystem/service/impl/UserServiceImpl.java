package com.yiwilee.aiqasystem.service.impl;

import com.yiwilee.aiqasystem.constant.SystemConstants;
import com.yiwilee.aiqasystem.converter.UserConverter;
import com.yiwilee.aiqasystem.exception.ResourceNotFoundException;
import com.yiwilee.aiqasystem.exception.UserException;
import com.yiwilee.aiqasystem.model.dto.UserCreateDTO;
import com.yiwilee.aiqasystem.model.dto.UserLoginDTO;
import com.yiwilee.aiqasystem.model.dto.UserRegisterDTO;
import com.yiwilee.aiqasystem.model.entity.SysRole;
import com.yiwilee.aiqasystem.model.entity.SysUser;
import com.yiwilee.aiqasystem.model.entity.UserPrincipal;
import com.yiwilee.aiqasystem.model.vo.LoginTokenVO;
import com.yiwilee.aiqasystem.model.vo.UserVO;
import com.yiwilee.aiqasystem.repository.UserRepo;
import com.yiwilee.aiqasystem.service.ChatService;
import com.yiwilee.aiqasystem.service.DocumentService;
import com.yiwilee.aiqasystem.service.RoleService;
import com.yiwilee.aiqasystem.service.UserService;
import com.yiwilee.aiqasystem.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepo userRepo;
    private final RoleService roleService;
    private final ChatService chatService;
    private final DocumentService documentService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserConverter userConverter;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(UserRegisterDTO registerDTO) {
        log.info("接收到新用户注册请求: {}", registerDTO.username());
        checkUsernameUnique(registerDTO.username());

        SysRole defaultRole = roleService.getDefaultRole();

        SysUser sysUser = SysUser.builder()
                .username(registerDTO.username())
                .password(passwordEncoder.encode(registerDTO.password()))
                .status(1) // 注册默认正常状态
                .roles(Collections.singleton(defaultRole))
                .build();

        return userConverter.toVO(userRepo.save(sysUser));
    }

    @Override
    public LoginTokenVO login(UserLoginDTO loginDTO) {
        // 1. 提交给 Security 进行密码比对 (失败会自动抛出 BadCredentialsException)
        UsernamePasswordAuthenticationToken authReq =
                new UsernamePasswordAuthenticationToken(loginDTO.username(), loginDTO.password());
        Authentication authentication = authenticationManager.authenticate(authReq);

        // 2. 校验通过，提取用户信息
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        SysUser sysUser = userPrincipal.getSysUser();

        // 3. 拦截被禁用的用户
        if (sysUser.getStatus() == 0) {
            log.warn("被禁用用户尝试登录: {}", loginDTO.username());
            throw new UserException("账号已被禁用，请联系管理员");
        }

        // 4. 生成 Token 并封装完整的 LoginTokenVO
        String token = jwtUtils.generateToken(userPrincipal);
        UserVO userVO = userConverter.toVO(sysUser);

        log.info("用户登录成功: {}", loginDTO.username());
        return new LoginTokenVO(token, userVO);
    }

    @Override
    @Transactional(readOnly = true)
    public UserVO getUserById(Long userId) {
        return userConverter.toVO(findSysUserById(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserVO getUserByUsername(String username) {
        SysUser sysUser = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("找不到用户: " + username));
        return userConverter.toVO(sysUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getUserRoles(Long userId) {
        return findSysUserById(userId).getRoles().stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserVO> pageUsers(String keyword, int pageNum, int pageSize) {
        int actualPageNum = Math.max(0, pageNum - 1);
        Pageable pageable = PageRequest.of(actualPageNum, pageSize, Sort.by("id").descending());

        Page<SysUser> userPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            userPage = userRepo.findByUsernameContaining(keyword.trim(), pageable);
        } else {
            userPage = userRepo.findAll(pageable);
        }

        // 核心重构：将 Entity 的 Page 映射为 VO 的 Page，杜绝数据泄露
        return userPage.map(userConverter::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO createUser(UserCreateDTO createDTO) {
        log.info("管理员后台创建用户: {}", createDTO.username());
        checkUsernameUnique(createDTO.username());

        SysRole role = roleService.getByRoleCode(createDTO.roleCode());

        SysUser sysUser = SysUser.builder()
                .username(createDTO.username())
                .password(passwordEncoder.encode(createDTO.password()))
                .status(createDTO.status())
                .roles(Collections.singleton(role))
                .build();

        return userConverter.toVO(userRepo.save(sysUser));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status) {
        SysUser sysUser = findSysUserById(userId);
        sysUser.setStatus(status);
        userRepo.save(sysUser);
        log.info("管理员更新了用户状态: [userId={}, status={}]", userId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long userId) {
        SysUser sysUser = findSysUserById(userId);
        log.info("准备级联删除用户: {}", sysUser.getUsername());

        int deletedSessions = chatService.deleteSessions(userId);
        int docsDeleted = documentService.deleteDocuments(userId);

        userRepo.delete(sysUser);
        log.info("用户 [{}] 及其关联数据清理完毕(会话:{}, 文档:{})", sysUser.getUsername(), deletedSessions, docsDeleted);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRolesToUser(Long userId, List<Long> roleIds) {
        SysUser sysUser = findSysUserById(userId);

        if (roleIds == null || roleIds.isEmpty()) {
            sysUser.getRoles().clear();
        } else {
            Set<SysRole> newRoles = roleIds.stream()
                    .map(roleService::getByRoleId)
                    .collect(Collectors.toSet());
            sysUser.setRoles(newRoles);
        }

        userRepo.save(sysUser);
        log.info("重新分配用户角色成功: 用户={}, 角色数量={}", sysUser.getUsername(), sysUser.getRoles().size());
    }

    // ---------------- 内部辅助方法 ----------------

    private SysUser findSysUserById(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在 (ID: " + userId + ")"));
    }

    private void checkUsernameUnique(String username) {
        if (userRepo.existsByUsername(username)) {
            throw new UserException("用户名 [" + username + "] 已被占用");
        }
    }
}