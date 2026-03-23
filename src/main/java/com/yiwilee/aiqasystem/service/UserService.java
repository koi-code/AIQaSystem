package com.yiwilee.aiqasystem.service;

import com.yiwilee.aiqasystem.model.dto.UserCreateDTO;
import com.yiwilee.aiqasystem.model.dto.UserLoginDTO;
import com.yiwilee.aiqasystem.model.dto.UserRegisterDTO;
import com.yiwilee.aiqasystem.model.vo.LoginTokenVO;
import com.yiwilee.aiqasystem.model.vo.UserVO;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 用户业务逻辑接口
 * 提供用户注册、登录、身份认证及后台管理的聚合服务。
 */
public interface UserService {

    /**
     * 普通用户开放注册
     * @param registerDTO 包含用户名和明文密码的注册参数
     * @return UserVO 脱敏后的新用户信息
     * @throws com.yiwilee.aiqasystem.exception.UserException 若用户名已存在则抛出
     */
    UserVO register(UserRegisterDTO registerDTO);

    /**
     * 用户登录并签发 JWT 令牌
     * @param loginDTO 包含用户名和明文密码的登录参数
     * @return LoginTokenVO 包含 JWT 字符串以及当前登录用户的基础脱敏信息
     * @throws org.springframework.security.authentication.BadCredentialsException 密码错误时抛出
     * @throws com.yiwilee.aiqasystem.exception.UserException 账号被禁用时抛出
     */
    LoginTokenVO login(UserLoginDTO loginDTO);

    /**
     * 根据用户 ID 查询用户信息
     * @param userId 目标用户的主键 ID
     * @return UserVO 脱敏后的用户信息
     * @throws com.yiwilee.aiqasystem.exception.ResourceNotFoundException 未找到时抛出
     */
    UserVO getUserById(Long userId);

    /**
     * 根据用户名查询用户信息
     * @param username 目标用户的唯一用户名
     * @return UserVO 脱敏后的用户信息
     * @throws com.yiwilee.aiqasystem.exception.ResourceNotFoundException 未找到时抛出
     */
    UserVO getUserByUsername(String username);

    /**
     * 获取指定用户所拥有的所有角色代码（Role Code）集合
     * @param userId 目标用户的主键 ID
     * @return List<String> 角色代码列表，例如 ["ROLE_ADMIN", "ROLE_USER"]
     */
    List<String> getUserRoles(Long userId);

    /**
     * 分页查询用户列表（后台管理端使用）
     * @param keyword  模糊搜索关键词（匹配用户名），允许为 null 或空字符串
     * @param pageNum  当前页码（前端通常从 1 开始计算）
     * @param pageSize 每页展示的数据条数
     * @return Page<UserVO> 包含总页数、总条数及脱敏用户列表的分页对象
     */
    Page<UserVO> pageUsers(String keyword, int pageNum, int pageSize);

    /**
     * 管理员在后台手动创建用户
     * @param createDTO 包含用户名、密码、初始状态及角色的参数
     * @return UserVO 脱敏后的新用户信息
     * @throws com.yiwilee.aiqasystem.exception.UserException 若用户名已被占用则抛出
     */
    UserVO createUser(UserCreateDTO createDTO);

    /**
     * 封禁或解封用户
     * @param userId 目标用户的主键 ID
     * @param status 目标状态（1-正常，0-禁用）
     */
    void updateUserStatus(Long userId, Integer status);

    /**
     * 彻底物理删除用户及其级联产生的所有系统数据（包含聊天记录、文档及向量库切片）
     * 警告：此操作不可逆！
     * @param userId 需要删除的用户 ID
     * @return boolean 删除是否成功
     */
    boolean deleteUser(Long userId);

    /**
     * 为用户重新分配角色（全量覆盖）
     * @param userId  目标用户的主键 ID
     * @param roleIds 需要分配的角色 ID 列表。若为空集合或 null，则清空该用户的所有角色
     */
    void assignRolesToUser(Long userId, List<Long> roleIds);
}