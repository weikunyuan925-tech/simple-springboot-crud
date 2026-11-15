package com.example.crud.service;

import com.example.crud.dto.LoginRequest;
import com.example.crud.dto.RegisterRequest;
import com.example.crud.entity.SysUser;
import com.example.crud.repository.SysUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(SysUserRepository sysUserRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.sysUserRepository = sysUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public void register(RegisterRequest request) {

        // 检查用户名是否已经注册
        if (sysUserRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 创建认证用户
        SysUser sysUser = new SysUser();

        sysUser.setUsername(request.getUsername());

        // 使用 BCrypt 加密密码，禁止保存明文密码
        sysUser.setPassword(passwordEncoder.encode(request.getPassword()));

        // 普通用户角色由服务端指定
        sysUser.setRole("USER");

        sysUserRepository.save(sysUser);
    }

    public String login(LoginRequest request) {
        SysUser sysUser = sysUserRepository.findByUsername(request.getUsername()).orElseThrow(() -> new BadCredentialsException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), sysUser.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        return jwtService.generateToken(sysUser.getUsername());
    }
}

