// 接收从前端传来的数据（用于注册或登录）
package com.example.demo.dto;

public class AuthRequest {
    private String username;
    private String password;
    private String email;
    private String fullName;
    
    // 默认构造函数
    public AuthRequest() {}
    
    // 构造函数
    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    // Getter 和 Setter 方法
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
}