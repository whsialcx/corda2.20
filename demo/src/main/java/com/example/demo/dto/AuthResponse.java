package com.example.demo.dto;

public class AuthResponse {
    private boolean success;
    private String message;
    private String username;
    private String token; // 后续可以添加 JWT token
    
    // 构造函数
    // 用于注册，或者登录失败的返回值
    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    //用于登录
    public AuthResponse(boolean success, String message, String username) {
        this.success = success;
        this.message = message;
        this.username = username;
    }
    
    // Getter 和 Setter 方法
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}