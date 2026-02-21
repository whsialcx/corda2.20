package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    
    // 新增：根据用户名和角色查找用户
    Optional<User> findByUsernameAndRole(String username, String role);
    
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}