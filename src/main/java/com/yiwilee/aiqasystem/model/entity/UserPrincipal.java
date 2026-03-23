package com.yiwilee.aiqasystem.model.entity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Spring Security 专属的用户身份凭证
 * 它将我们数据库里的 SysUser 实体，适配成 Spring Security 认识的 UserDetails 接口
 */
public class UserPrincipal implements UserDetails {

    private final SysUser sysUser;

    public UserPrincipal(SysUser sysUser) {
        this.sysUser = sysUser;
    }

    /**
     * 【权限核心】：在这里把数据库里的角色，转换成 Security 认识的权限对象
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 确保你的 SysRole 实体里有 getRoleCode() 方法，且值是 "ROLE_ADMIN" 这种格式
        return sysUser.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleCode()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return sysUser.getPassword();
    }

    @Override
    public String getUsername() {
        return sysUser.getUsername();
    }

    // ==========================================
    // 下面四个方法是控制账号状态的，配合你 SysUser 里的 status 字段完美联动
    // ==========================================

    @Override
    public boolean isAccountNonExpired() {
        return true; // 账户是否未过期 (这里写死 true，除非你的系统有定期强制改密码的需求)
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 账户是否未锁定
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 密码是否未过期
    }

    @Override
    public boolean isEnabled() {
        // 账户是否启用？根据你数据库里 sysUser.getStatus() 来判断
        // 假设 1 是正常，0 是禁用
        return sysUser.getStatus() != null && sysUser.getStatus() == 1;
    }

    // 预留一个暴露原生 SysUser 的方法，以后在 Controller 里获取当前登录用户的 ID 时会非常方便！
    public SysUser getSysUser() {
        return this.sysUser;
    }
}