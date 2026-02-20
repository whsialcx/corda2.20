package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.HashMap;

@Service
public class CordaService {
    
    private final RestTemplate restTemplate;
    private String baseUrl;
    
    @Autowired
    public CordaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public CordaService(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    // 通用GET请求
    public ResponseEntity<String> get(String endpoint) {
        String url = baseUrl + endpoint;
        return restTemplate.getForEntity(url, String.class);
    }
    
    // 通用POST请求
    public ResponseEntity<String> post(String endpoint, Map<String, Object> body) {
        String url = baseUrl + endpoint;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(url, request, String.class);
    }
    
    // 1. 获取节点状态
    public ResponseEntity<String> getNodeStatus() {
        return get("/status");
    }
    
    // 2. 获取节点信息
    public ResponseEntity<String> getNodeInfo() {
        return get("/me");
    }
    
    // 3. 获取所有节点
    public ResponseEntity<String> getAllPeers() {
        return get("/peers");
    }
    
    // 4. 创建IOU
    public ResponseEntity<String> createIOU(Integer iouValue, String partyName) {
        String endpoint = "/create-iou";
        
        // 使用表单提交
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("iouValue", String.valueOf(iouValue));
        params.add("partyName", partyName);
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.postForEntity(baseUrl + endpoint, request, String.class);
    }
    
    // 5. 获取IOU列表
    public ResponseEntity<String> getIOUs() {
        return get("/ious");
    }
    
    // 6. 获取我的IOU
    public ResponseEntity<String> getMyIOUs() {
        return get("/my-ious");
    }
    
    // 7. 温度相关API
    
    // 记录温度
    public ResponseEntity<String> recordTemperature(Double temperature, Boolean isCritical, String receiver) {
        String endpoint = "/api/temperature/record";
        
        Map<String, Object> body = new HashMap<>();
        body.put("temperature", temperature);
        body.put("critical", isCritical);
        body.put("receiver", receiver);
        
        return post(endpoint, body);
    }
    
    // 查询温度记录
    public ResponseEntity<String> queryTemperatures(Boolean onlyCritical) {
        String endpoint = "/api/temperature/query";
        
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + endpoint);
        if (onlyCritical != null) {
            builder.queryParam("onlyCritical", onlyCritical);
        }
        
        URI uri = builder.build().toUri();
        return restTemplate.getForEntity(uri, String.class);
    }
    
    // 获取可用接收方
    public ResponseEntity<String> getAvailableReceivers() {
        return get("/api/temperature/receivers");
    }
    
    // 转移温度记录
    public ResponseEntity<String> transferTemperature(String linearId, String newReceiver) {
        String endpoint = "/api/temperature/transfer";
        
        Map<String, Object> body = new HashMap<>();
        body.put("linearId", linearId);
        body.put("newReceiver", newReceiver);
        
        return post(endpoint, body);
    }
    
    // 8. 其他节点信息
    public ResponseEntity<String> getServerTime() {
        return get("/servertime");
    }
    
    public ResponseEntity<String> getNodeAddresses() {
        return get("/addresses");
    }
    
    public ResponseEntity<String> getPlatformVersion() {
        return get("/platformversion");
    }
    
    public ResponseEntity<String> getNotaries() {
        return get("/notaries");
    }
    
    public ResponseEntity<String> getFlows() {
        return get("/flows");
    }
    
    public ResponseEntity<String> getStates() {
        return get("/states");
    }
}