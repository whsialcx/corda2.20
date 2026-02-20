package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CordaExampleService {
    
    @Autowired
    private CordaNodeManager nodeManager;
    
    /**
     * 在所有节点中查询温度记录并汇总
     */
    public Map<String, Object> queryAllNodesTemperatures() {
        Map<String, Object> result = new HashMap<>();
        
        for (String nodeName : nodeManager.getNodeNames()) {
            try {
                CordaService service = nodeManager.getNodeService(nodeName);
                ResponseEntity<String> response = service.queryTemperatures(false);
                
                result.put(nodeName, Map.of(
                    "success", true,
                    "data", response.getBody()
                ));
            } catch (Exception e) {
                result.put(nodeName, Map.of(
                    "success", false,
                    "error", e.getMessage()
                ));
            }
        }
        
        return result;
    }
    
    /**
     * 在多个节点创建相同的IOU
     */
    public Map<String, Object> createIOUInMultipleNodes(Integer iouValue, String partyName, String... targetNodes) {
        Map<String, Object> results = new HashMap<>();
        
        for (String nodeName : targetNodes) {
            try {
                CordaService service = nodeManager.getNodeService(nodeName);
                ResponseEntity<String> response = service.createIOU(iouValue, partyName);
                
                results.put(nodeName, Map.of(
                    "success", true,
                    "transaction", response.getBody()
                ));
            } catch (Exception e) {
                results.put(nodeName, Map.of(
                    "success", false,
                    "error", e.getMessage()
                ));
            }
        }
        
        return results;
    }
    
    /**
     * 检查所有节点的健康状态
     */
    public Map<String, Object> checkAllNodesHealth() {
        Map<String, Object> healthStatus = new HashMap<>();
        
        for (String nodeName : nodeManager.getNodeNames()) {
            try {
                CordaService service = nodeManager.getNodeService(nodeName);
                ResponseEntity<String> statusResponse = service.getNodeStatus();
                ResponseEntity<String> timeResponse = service.getServerTime();
                
                healthStatus.put(nodeName, Map.of(
                    "healthy", true,
                    "status", statusResponse.getBody(),
                    "serverTime", timeResponse.getBody(),
                    "message", "节点运行正常"
                ));
            } catch (Exception e) {
                healthStatus.put(nodeName, Map.of(
                    "healthy", false,
                    "error", e.getMessage(),
                    "message", "节点连接失败"
                ));
            }
        }
        
        return healthStatus;
    }
}