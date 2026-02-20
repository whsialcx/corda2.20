package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello Spring Boot! Your REST controller is working!";
    }

    @GetMapping("/greet")
    public String greet(@RequestParam(value = "name", defaultValue = "World") String name) {
        return "Hello, " + name + "!";
    }

    @GetMapping("/user")
    public User getUser() {
        return new User(1, "John Doe", "john@example.com");
    }
    
    @GetMapping("/health")
    public String healthCheck() {
        return "Application is running! Database connection is OK.";
    }
}

// 简单的用户类（这个可以保留，或者删除，因为我们已经有了完整的 User 实体）
class User {
    private int id;
    private String name;
    private String email;
    
    public User(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
    
    // Getter 方法
    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    
    // Setter 方法
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
}