package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CordaConfig {
    
    /**
     * 声明 RestTemplate Bean
     * 供 CordaNodeManager 在动态实例化 CordaService 时使用，用于发起底层的 HTTP 请求
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    // ---------------------------------------------------------
    // 以下代码已被删除，因为现在由数据库 (CordaNodeManager) 接管路由：
    // 1. 删除了 @ConfigurationProperties(prefix = "corda")
    // 2. 删除了 private String nodeBaseUrl; 和 private Map<String, String> nodes;
    // 3. 删除了 public CordaService cordaService(...) 
    // 4. 删除了 public Map<String, CordaService> nodeServices(...)
    // ---------------------------------------------------------
}