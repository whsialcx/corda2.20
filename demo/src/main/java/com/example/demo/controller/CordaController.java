package com.example.demo.controller;

import com.example.demo.service.CordaNodeManager;
import com.example.demo.service.CordaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/corda")
public class CordaController {
    
    @Autowired
    private CordaNodeManager nodeManager;
    
    /**
     * 测试所有节点的连接状态
     */
    @GetMapping("/test-all-nodes")
    public ResponseEntity<Map<String, Object>> testAllNodes() {
        Map<String, Object> result = new HashMap<>();
        
        for (String nodeName : nodeManager.getNodeNames()) {
            try {
                CordaService service = nodeManager.getNodeService(nodeName);
                ResponseEntity<String> response = service.getNodeStatus();
                result.put(nodeName, Map.of(
                    "status", "connected",
                    "response", response.getBody(),
                    "httpStatus", response.getStatusCode().value()
                ));
            } catch (Exception e) {
                result.put(nodeName, Map.of(
                    "status", "error",
                    "message", e.getMessage()
                ));
            }
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 在指定节点创建IOU
     */
    @PostMapping("/{nodeName}/create-iou")
    public ResponseEntity<Map<String, Object>> createIOU(
            @PathVariable String nodeName,
            @RequestParam Integer iouValue,
            @RequestParam String partyName) {
        
        try {
            CordaService service = nodeManager.getNodeService(nodeName);
            ResponseEntity<String> response = service.createIOU(iouValue, partyName);
            
            Map<String, Object> result = new HashMap<>();
            result.put("node", nodeName);
            result.put("status", response.getStatusCode().value());
            result.put("response", response.getBody());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("node", nodeName);
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 查询指定节点的IOU
     */
    @GetMapping("/{nodeName}/ious")
    public ResponseEntity<Map<String, Object>> getIOUs(@PathVariable String nodeName) {
        try {
            CordaService service = nodeManager.getNodeService(nodeName);
            ResponseEntity<String> response = service.getIOUs();
            
            Map<String, Object> result = new HashMap<>();
            result.put("node", nodeName);
            result.put("status", response.getStatusCode().value());
            result.put("data", response.getBody());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("node", nodeName);
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 在指定节点记录温度
     */
    @PostMapping("/{nodeName}/record-temperature")
    public ResponseEntity<Map<String, Object>> recordTemperature(
            @PathVariable String nodeName,
            @RequestParam Double temperature,
            @RequestParam Boolean isCritical,
            @RequestParam String receiver) {
        
        try {
            CordaService service = nodeManager.getNodeService(nodeName);
            ResponseEntity<String> response = service.recordTemperature(temperature, isCritical, receiver);
            
            Map<String, Object> result = new HashMap<>();
            result.put("node", nodeName);
            result.put("status", response.getStatusCode().value());
            result.put("response", response.getBody());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("node", nodeName);
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 查询指定节点的温度记录
     */
    @GetMapping("/{nodeName}/temperatures")
    public ResponseEntity<Map<String, Object>> queryTemperatures(
            @PathVariable String nodeName,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyCritical) {
        
        try {
            CordaService service = nodeManager.getNodeService(nodeName);
            ResponseEntity<String> response = service.queryTemperatures(onlyCritical);
            
            Map<String, Object> result = new HashMap<>();
            result.put("node", nodeName);
            result.put("status", response.getStatusCode().value());
            result.put("data", response.getBody());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("node", nodeName);
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取指定节点的可用接收方
     */
    @GetMapping("/{nodeName}/receivers")
    public ResponseEntity<Map<String, Object>> getReceivers(@PathVariable String nodeName) {
        try {
            CordaService service = nodeManager.getNodeService(nodeName);
            ResponseEntity<String> response = service.getAvailableReceivers();
            
            Map<String, Object> result = new HashMap<>();
            result.put("node", nodeName);
            result.put("status", response.getStatusCode().value());
            result.put("data", response.getBody());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("node", nodeName);
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取指定节点的基本信息
     */
    @GetMapping("/{nodeName}/info")
    public ResponseEntity<Map<String, Object>> getNodeInfo(@PathVariable String nodeName) {
        try {
            CordaService service = nodeManager.getNodeService(nodeName);
            
            Map<String, Object> info = new HashMap<>();
            info.put("nodeName", nodeName);
            
            // 获取多个信息
            info.put("status", service.getNodeStatus().getBody());
            info.put("identity", service.getNodeInfo().getBody());
            info.put("serverTime", service.getServerTime().getBody());
            info.put("peers", service.getAllPeers().getBody());
            
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("node", nodeName);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{nodeName}/status")
    public ResponseEntity<Map<String, Object>> getNodeStatus(@PathVariable String nodeName) {
        try {
            CordaService service = nodeManager.getNodeService(nodeName);
            ResponseEntity<String> response = service.getNodeStatus();
            
            Map<String, Object> result = new HashMap<>();
            result.put("node", nodeName);
            result.put("status", response.getStatusCode().value());
            result.put("response", response.getBody());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("node", nodeName);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{nodeName}/servertime")
    public ResponseEntity<Map<String, Object>> getServerTime(@PathVariable String nodeName) {
        try {
            CordaService service = nodeManager.getNodeService(nodeName);
            ResponseEntity<String> response = service.getServerTime();
            
            Map<String, Object> result = new HashMap<>();
            result.put("node", nodeName);
            result.put("status", response.getStatusCode().value());
            result.put("response", response.getBody());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("node", nodeName);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{nodeName}/addresses")
    public ResponseEntity<Map<String, Object>> getAddresses(@PathVariable String nodeName) {
        try {
            CordaService service = nodeManager.getNodeService(nodeName);
            ResponseEntity<String> response = service.getNodeAddresses();
            
            Map<String, Object> result = new HashMap<>();
            result.put("node", nodeName);
            result.put("status", response.getStatusCode().value());
            result.put("response", response.getBody());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("node", nodeName);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{nodeName}/platformversion")
    public ResponseEntity<Map<String, Object>> getPlatformVersion(@PathVariable String nodeName) {
        try {
            CordaService service = nodeManager.getNodeService(nodeName);
            ResponseEntity<String> response = service.getPlatformVersion();
            
            Map<String, Object> result = new HashMap<>();
            result.put("node", nodeName);
            result.put("status", response.getStatusCode().value());
            result.put("response", response.getBody());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("node", nodeName);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{nodeName}/notaries")
    public ResponseEntity<Map<String, Object>> getNotaries(@PathVariable String nodeName) {
        try {
            CordaService service = nodeManager.getNodeService(nodeName);
            ResponseEntity<String> response = service.getNotaries();
            
            Map<String, Object> result = new HashMap<>();
            result.put("node", nodeName);
            result.put("status", response.getStatusCode().value());
            result.put("response", response.getBody());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("node", nodeName);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{nodeName}/flows")
    public ResponseEntity<Map<String, Object>> getFlows(@PathVariable String nodeName) {
        try {
            CordaService service = nodeManager.getNodeService(nodeName);
            ResponseEntity<String> response = service.getFlows();
            
            Map<String, Object> result = new HashMap<>();
            result.put("node", nodeName);
            result.put("status", response.getStatusCode().value());
            result.put("response", response.getBody());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("node", nodeName);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{nodeName}/states")
    public ResponseEntity<Map<String, Object>> getStates(@PathVariable String nodeName) {
        try {
            CordaService service = nodeManager.getNodeService(nodeName);
            ResponseEntity<String> response = service.getStates();
            
            Map<String, Object> result = new HashMap<>();
            result.put("node", nodeName);
            result.put("status", response.getStatusCode().value());
            result.put("response", response.getBody());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("node", nodeName);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{nodeName}/peers")
    public ResponseEntity<Map<String, Object>> getAllPeers(@PathVariable String nodeName) {
        try {
            CordaService service = nodeManager.getNodeService(nodeName);
            ResponseEntity<String> response = service.getAllPeers();
            
            Map<String, Object> result = new HashMap<>();
            result.put("node", nodeName);
            result.put("status", response.getStatusCode().value());
            result.put("response", response.getBody());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("node", nodeName);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{nodeName}/my-ious")
    public ResponseEntity<Map<String, Object>> getMyIOUs(@PathVariable String nodeName) {
        try {
            CordaService service = nodeManager.getNodeService(nodeName);
            ResponseEntity<String> response = service.getMyIOUs();
            
            Map<String, Object> result = new HashMap<>();
            result.put("node", nodeName);
            result.put("status", response.getStatusCode().value());
            result.put("response", response.getBody());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("node", nodeName);
            return ResponseEntity.badRequest().body(error);
        }
    }
}