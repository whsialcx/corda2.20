package com.example.demo.controller;

import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.AuthResponse;
import com.example.demo.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController  //标记为REXT API，用来接受和处理HTTP请求
@RequestMapping("/api/auth")// 基础路径
public class AuthController 
{
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/register")// 处理注册请求
    public AuthResponse register(@RequestBody AuthRequest request) {
        return authService.register(request);
    }
    
    @PostMapping("/login")// 处理登录请求
    public AuthResponse login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }
    
    // 检查用户名是否可用
    @GetMapping("/check-username")
    public String checkUsername(@RequestParam String username) {
        // 这里可以添加更复杂的检查逻辑
        return "Username check endpoint";
    }
}