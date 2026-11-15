
package com.example.crud.service;

import com.example.crud.entity.SysUser;
import com.example.crud.repository.SysUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserRepository sysUserRepository;

    public CustomUserDetailsService(SysUserRepository sysUserRepository) {
        this.sysUserRepository = sysUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        SysUser sysUser = sysUserRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("用户不存在"));

        return User.withUsername(sysUser.getUsername())
                .password(sysUser.getPassword())
                .roles(sysUser.getRole())
                .build();
    }
}
