package com.yiwilee.aiqasystem.service;

import com.yiwilee.aiqasystem.model.dto.RoleCreateDTO;
import com.yiwilee.aiqasystem.model.dto.RoleUpdateDTO;
import com.yiwilee.aiqasystem.model.entity.SysRole;
import com.yiwilee.aiqasystem.model.vo.RoleVO;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 角色业务逻辑接口
 * 提供 RBAC 模型中角色的创建、权限分配及内部查询能力。
 */
public interface RoleService {

    /**
     * 获取系统中所有角色的列表，通常用于下拉框或分配角色的弹窗
     * @return List<RoleVO> 包含角色基本信息及所拥有的权限 ID 集合
     */
    List<RoleVO> getAllRoles();

    /**
     * 分页查询角色列表
     * @param keyword  模糊搜索关键词（匹配角色名），允许为 null
     * @param pageNum  当前页码（从 1 开始计算）
     * @param pageSize 每页数据条数
     * @return Page<RoleVO> 角色分页对象
     */
    Page<RoleVO> pageRoles(String keyword, int pageNum, int pageSize);

    /**
     * 创建全新角色
     * 防呆设计：系统会自动将传入的 roleCode 转换为大写并补齐 "ROLE_" 前缀
     * @param createDTO 包含角色名称、代码及描述的参数对象
     * @return RoleVO 新建的角色视图对象
     * @throws com.yiwilee.aiqasystem.exception.RoleException 若角色代码已被占用则抛出
     */
    RoleVO createRole(RoleCreateDTO createDTO);

    /**
     * 更新角色的基础信息（禁止修改系统级标识 roleCode）
     * @param roleId    目标角色的主键 ID
     * @param updateDTO 包含新角色名称和描述的参数对象
     * @return RoleVO 更新后的角色视图对象
     */
    RoleVO updateRole(Long roleId, RoleUpdateDTO updateDTO);

    /**
     * 删除闲置角色
     * @param roleId 目标角色的主键 ID
     * @return boolean 删除是否成功
     * @throws com.yiwilee.aiqasystem.exception.RoleException 若当前仍有用户绑定该角色，则拒绝删除并抛出异常
     */
    boolean deleteRole(Long roleId);

    /**
     * 为指定角色分配权限节点（全量覆盖）
     * @param roleId        目标角色的主键 ID
     * @param permissionIds 权限菜单/按钮的主键 ID 集合
     * @return RoleVO 刷新后的角色视图对象
     */
    RoleVO assignPermissionsToRole(Long roleId, List<Long> permissionIds);

    // ==========================================
    // 内部服务间调用方法 (允许返回 Entity 实体，不对 Controller 层暴露)
    // ==========================================

    /**
     * 获取系统默认的注册基础角色（通常为 ROLE_USER 或 ROLE_STUDENT）
     * @return SysRole 默认角色实体
     * @throws com.yiwilee.aiqasystem.exception.RoleException 系统未配置默认角色时抛出
     */
    SysRole getDefaultRole();

    /**
     * 根据角色代码精确查找角色实体
     * @param roleCode 角色代码（如 "ROLE_ADMIN"）
     * @return SysRole 角色实体对象
     * @throws com.yiwilee.aiqasystem.exception.ResourceNotFoundException 角色代码不存在时抛出
     */
    SysRole getByRoleCode(String roleCode);

    /**
     * 根据主键 ID 查找角色实体
     * @param id 角色的主键 ID
     * @return SysRole 角色实体对象
     * @throws com.yiwilee.aiqasystem.exception.ResourceNotFoundException ID 不存在时抛出
     */
    SysRole getByRoleId(Long id);

    /**
     * 工具方法：清洗和格式化前端传来的不规范角色字符串数组
     * 处理逻辑包含：去除首尾空格、去除多余双引号、按逗号拆分等
     * @param rawRoles 原始脏乱的角色字符串列表
     * @return List<String> 干净标准的角色代码列表
     */
    List<String> cleanAndParseRoles(List<String> rawRoles);
}