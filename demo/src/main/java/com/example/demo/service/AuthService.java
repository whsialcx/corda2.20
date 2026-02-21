package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.entity.VerificationToken;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.VerificationTokenRepository;
import com.example.demo.util.PasswordUtil;
import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VerificationTokenRepository tokenRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private PasswordUtil passwordUtil;
    
    @Value("${app.verification.token-expiry-hours:12}")
    private int tokenExpiryHours;
    
    @Transactional
    public AuthResponse register(AuthRequest request, String role) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            return new AuthResponse(false, "用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            return new AuthResponse(false, "邮箱已被注册");
        }
        
        // 创建用户对象
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordUtil.encodePassword(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setRole(role);
        
        // 根据角色决定是否直接激活
        if ("ADMIN".equals(role)) {
            // 管理员初始为未激活
            user.setEnabled(false);
            userRepository.save(user);
            
            // 生成验证令牌
            String token = UUID.randomUUID().toString();
            VerificationToken verificationToken = new VerificationToken(token, user, tokenExpiryHours);
            tokenRepository.save(verificationToken);
            
            // 发送审批邮件到超级管理员邮箱
            emailService.sendAdminVerificationEmail(user.getUsername(), token);
            
            return new AuthResponse(true, "管理员注册申请已提交，审批邮件已发送至超级管理员，请等待激活");
        } else {
            // 普通用户直接激活
            user.setEnabled(true);
            userRepository.save(user);
            return new AuthResponse(true, "普通用户注册成功", user.getUsername());
        }
    }
    
    public AuthResponse login(AuthRequest request, String role) {
        Optional<User> userOptional = userRepository.findByUsernameAndRole(request.getUsername(), role);
        
        if (userOptional.isEmpty()) {
            return new AuthResponse(false, "用户名、密码错误或角色不匹配");
        }
        
        User user = userOptional.get();
        
        // 检查账号是否已激活
        if (!user.isEnabled()) {
            return new AuthResponse(false, "账号尚未激活，请联系超级管理员审批");
        }
        
        if (passwordUtil.matches(request.getPassword(), user.getPassword())) {
            return new AuthResponse(true, "登录成功", user.getUsername());
        } else {
            return new AuthResponse(false, "用户名、密码错误或角色不匹配");
        }
    }
    
    @Transactional
    public String verifyAdminToken(String token) {
        Optional<VerificationToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return "无效的激活链接。";
        }
        
        VerificationToken verificationToken = tokenOpt.get();
        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return "激活链接已过期，请重新注册管理员账号。";
        }
        
        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        
        // 激活后删除令牌，防止重复使用
        tokenRepository.delete(verificationToken);
        
        return "管理员账号 [" + user.getUsername() + "] 已成功激活！现在可以使用该账号登录。";
    }
}