package com.example.demo.config;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordUtil passwordUtil;

    @Override
    public void run(String... args) throws Exception {
        // 1. 初始化超级管理员账号
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordUtil.encodePassword("admin123")); // 默认密码
            admin.setEmail("admin@example.com");
            admin.setFullName("系统管理员");
            admin.setRole("ADMIN");
            admin.setEnabled(true);         // 直接激活，跳过邮件审批
            admin.setKycStatus("VERIFIED"); // 设为已实名，方便测试
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(admin);
            System.out.println("✅ 【系统初始化】默认管理员账号创建成功 -> 账号: admin | 密码: admin123");
        }

        // 2. 初始化普通用户账号
        if (!userRepository.existsByUsername("testuser")) {
            User user = new User();
            user.setUsername("testuser");
            user.setPassword(passwordUtil.encodePassword("user123")); // 默认密码
            user.setEmail("user@example.com");
            user.setFullName("测试用户");
            user.setRole("USER");
            user.setEnabled(true);          // 普通用户默认就是 true
            user.setKycStatus("VERIFIED");  // 设为已实名，方便测试申请节点
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(user);
            System.out.println("✅ 【系统初始化】默认测试用户创建成功 -> 账号: testuser | 密码: user123");
        }
    }
}