package com.yiwilee.aiqasystem.repository;

import com.yiwilee.aiqasystem.model.entity.SysRole;
import com.yiwilee.aiqasystem.model.entity.SysUser;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepo extends JpaRepository<SysRole, Long>, JpaSpecificationExecutor<SysRole> {

    /**
     * 根据角色标识精确查询（例如：查询 ROLE_ADMIN）
     */
    Optional<SysRole> findByRoleCode(String roleCode);

    /**
     * 检查角色标识是否已存在，防止管理员添加重复角色
     */
    boolean existsByRoleCode(String roleCode);

    @NonNull
    Page<SysRole> findByRoleNameContaining(String keyword, Pageable pageable);
}