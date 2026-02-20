package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // 禁用 CSRF
            .authorizeHttpRequests(authz -> authz
            .requestMatchers("/api/nodes/**").permitAll() // 允许节点管理接口
            .requestMatchers("/**").permitAll()
            //     .requestMatchers("/api/auth/**").permitAll() // 认证接口公开
            //     .requestMatchers("/test.html").permitAll()   // 测试页面公开
            //     .requestMatchers("/dashboard.html").permitAll() // 允许仪表板访问
            //     .requestMatchers("/profile.html").permitAll()   // 允许个人资料页访问
            //     .requestMatchers("/settings.html").permitAll()  // 允许设置页访问
            //     .requestMatchers("/hello").permitAll()       // 测试接口公开
            //     .requestMatchers("/greet").permitAll()       // 测试接口公开
            //     .requestMatchers("/health").permitAll()      // 健康检查公开
            //     .requestMatchers("/css/**", "/js/**", "/images/**").permitAll() // 新增：允许静态资源
            //     .anyRequest().authenticated()                // 其他需要认证
            )
            .formLogin(form -> form.disable())               // 禁用表单登录
            .httpBasic(httpBasic -> httpBasic.disable());    // 禁用 HTTP Basic

        return http.build();
    }
}