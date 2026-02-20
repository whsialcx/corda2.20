// 处理用户认证相关的业务逻辑
package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.PasswordUtil;
import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service //主要的业务服务组件
public class AuthService 
{
    
    // @Autowired 
    // private UserRepository userRepository;
    @Autowired // 依赖注入，自动装配UserRepository实例
    private UserRepository userRepository; // 相当于系统帮我们创建了一个对象，但是我们直接使用这个对象，不需要自己去new，系统会帮我们进行管理
    
    @Autowired
    private PasswordUtil passwordUtil;
    
    public AuthResponse register(AuthRequest request) 
    {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) 
        {
            return new AuthResponse(false, "用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) 
        {
            return new AuthResponse(false, "邮箱已被注册");
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordUtil.encodePassword(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        
        userRepository.save(user);// 保存到数据库
        
        return new AuthResponse(true, "注册成功", user.getUsername());
    }
    //登录方法
    public AuthResponse login(AuthRequest request) {
        Optional<User> userOptional = userRepository.findByUsername(request.getUsername());
        
        if (userOptional.isEmpty()) 
        {
            return new AuthResponse(false, "用户名或密码错误");
        }
        
        User user = userOptional.get();
        
        if (passwordUtil.matches(request.getPassword(), user.getPassword())) 
        {
            return new AuthResponse(true, "登录成功", user.getUsername());
        } 
        else 
        {
            return new AuthResponse(false, "用户名或密码错误");
        }
    }
}