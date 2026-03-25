package com.yiwilee.aiqasystem.repository;

import com.yiwilee.aiqasystem.model.entity.SysPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionRepo extends JpaRepository<SysPermission, Long>, JpaSpecificationExecutor<SysPermission> {

    /**
     * 根据父级ID查询子菜单/按钮（用于在后端构建菜单树）
     */
    List<SysPermission> findByParentId(Long parentId);

    /**
     * 根据权限类型查询（例如：type=1或2 只查出能显示在页面左侧的目录和菜单）
     */
    List<SysPermission> findByTypeIn(List<Integer> types);

    /**
     * 这是一个非常实用的自定义查询：直接根据用户ID，跨三张表查出该用户拥有的所有权限标识。
     * 虽然你可以用 user.getRoles().getPermissions() 这种对象导航来拿，但在有些极端的性能优化场景下，直接写一条 SQL 查出来会更快。
     */
    @Query("SELECT DISTINCT p.permCode FROM SysPermission p " +
            "JOIN SysRole r ON p IN elements(r.permissions) " +
            "JOIN SysUser u ON r IN elements(u.roles) " +
            "WHERE u.id = :userId AND p.permCode IS NOT NULL")
    List<String> findPermCodesByUserId(@Param("userId") Long userId);

    /**
     * 检查是否存在以指定 ID 为父节点的子节点
     * 业务场景：用于在删除菜单/权限节点前进行防御性校验，防止出现“孤儿节点”。
     * Spring Data JPA 会自动将其解析为类似如下的 SQL:
     * SELECT COUNT(*) FROM sys_permission WHERE parent_id = ? LIMIT 1
     *
     * @param parentId 父节点 ID
     * @return 如果存在至少一个子节点返回 true，否则返回 false
     */
    boolean existsByParentId(Long parentId);

    // 如果后续业务需要根据 permCode 查询节点，可以取消下面这行的注释：
    // Optional<SysPermission> findByPermCode(String permCode);
}
