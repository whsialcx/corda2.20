package com.example.demo.config;

import com.example.demo.service.CordaService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "corda")
public class CordaConfig {
    
    private String nodeBaseUrl = "http://localhost:50005";
    private Map<String, String> nodes = new HashMap<>();
    
    // Getters and Setters
    public String getNodeBaseUrl() {
        return nodeBaseUrl;
    }
    
    public void setNodeBaseUrl(String nodeBaseUrl) {
        this.nodeBaseUrl = nodeBaseUrl;
    }
    
    public Map<String, String> getNodes() {
        return nodes;
    }
    
    public void setNodes(Map<String, String> nodes) {
        this.nodes = nodes;
    }
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    public CordaService cordaService(RestTemplate restTemplate) {
        return new CordaService(restTemplate, nodeBaseUrl);
    }
    
    @Bean
    public Map<String, CordaService> nodeServices(RestTemplate restTemplate) {
        Map<String, CordaService> services = new HashMap<>();
        nodes.forEach((nodeName, baseUrl) -> {
            services.put(nodeName, new CordaService(restTemplate, baseUrl));
        });
        return services;
    }
}