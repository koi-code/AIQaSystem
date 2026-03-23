package com.yiwilee.aiqasystem.service.impl;

import com.yiwilee.aiqasystem.model.entity.SysUser;
import com.yiwilee.aiqasystem.model.entity.UserPrincipal;
import com.yiwilee.aiqasystem.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyUserDetailsService implements UserDetailsService {
    private final UserRepo userRepo;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        SysUser user = userRepo.findWithRolesAndPermissionsByUsername(username).orElseThrow(() ->new UsernameNotFoundException("找不到该用户"));
        return new UserPrincipal(user);
    }
}
