
package com.example.crud.controller;

import com.example.crud.dto.LoginRequest;
import com.example.crud.dto.RegisterRequest;
import com.example.crud.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest request) {

        authService.register(request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "注册成功");

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 用户登录
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request) {

        String token = authService.login(request);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 900);

        return ResponseEntity.ok(response);
    }
}
