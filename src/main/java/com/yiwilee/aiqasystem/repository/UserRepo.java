package com.yiwilee.aiqasystem.repository;

import com.yiwilee.aiqasystem.model.entity.SysUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<SysUser, Long>, JpaSpecificationExecutor<SysUser> {

    /**
     * 核心业务：根据用户名查询用户（用于 Spring Security 登录逻辑）
     * 使用 Optional 包装，优雅处理用户不存在的情况，避免空指针异常
     */
    Optional<SysUser> findByUsername(String username);

    /**
     * 辅助业务：判断用户名是否已被注册
     */
    boolean existsByUsername(String username);

    Page<SysUser> findByUsernameContaining(String keyword, Pageable pageable);

    @Query("SELECT COUNT(u) FROM SysUser u JOIN u.roles r WHERE r.id = :roleId")
    long countUsersByRoleId(@Param("roleId") Long roleId);

    // 🌟 新增：使用 EntityGraph 告诉 JPA，这次查询顺便把 roles 和 roles 里面的 permissions 一起查出来！
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<SysUser> findWithRolesAndPermissionsByUsername(String username);

    // 如果你在 JwtFilter 里是用 ID 查的，同理加一个：
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<SysUser> findWithRolesAndPermissionsById(Long id);
}
