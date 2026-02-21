package com.example.demo.controller;

import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.AuthResponse;
import com.example.demo.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    // ================= 普通用户接口 =================
    
    @PostMapping("/user/register")
    public AuthResponse userRegister(@RequestBody AuthRequest request) {
        return authService.register(request, "USER");
    }
    
    @PostMapping("/user/login")
    public AuthResponse userLogin(@RequestBody AuthRequest request) {
        return authService.login(request, "USER");
    }
    
    // ================= 管理员接口 =================
    
    @PostMapping("/admin/register")
    public AuthResponse adminRegister(@RequestBody AuthRequest request) {
        return authService.register(request, "ADMIN");
    }
    
    @PostMapping("/admin/login")
    public AuthResponse adminLogin(@RequestBody AuthRequest request) {
        return authService.login(request, "ADMIN");
    }
    
    // ================= 管理员激活验证接口 =================
    @GetMapping("/admin/verify")
    public String verifyAdmin(@RequestParam("token") String token) {
        return authService.verifyAdminToken(token);
    }
    
    // ================= 公共接口 =================
    
    @GetMapping("/check-username")
    public String checkUsername(@RequestParam String username) {
        return "Username check endpoint";
    }
}