// 定义数据，对应数据库中的users表
package com.example.demo.entity;

import jakarta.persistence.*; //JPA注解，用于将Java对象映射到数据库表的规范
import java.time.LocalDateTime;

@Entity// 用户实体类，表示它对应数据库中的一张表
@Table(name = "users")// 数据库表名是users
public class User {
    @Id// 标记主键
    @GeneratedValue(strategy = GenerationType.IDENTITY)// 指定主键的生成策略
    private Long id;
    
    @Column(unique = true, nullable = false)//指定列名和特性，这里是唯一且非空
    private String username;
    
    @Column(nullable = false)//非空
    private String password;
    
    @Column(unique = true)//唯一
    private String email;
    
    private String fullName;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;//创建时间
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;//更新时间
    
    // 构造函数
    public User() {}
    
    public User(String username, String password, String email, String fullName) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getter 和 Setter 方法，将private的属性暴露出来，以便其他类使用
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}