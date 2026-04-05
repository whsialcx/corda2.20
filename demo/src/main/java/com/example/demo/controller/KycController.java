package com.example.demo.controller;

import com.example.demo.entity.KycRecord;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.KycService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/kyc")
public class KycController {

    @Autowired
    private KycService kycService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 提交实名认证申请
     */
    @PostMapping("/submit")
    public Map<String, Object> submitKyc(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String username = request.get("username"); // 实际业务应从 Token 中取
        try {
            kycService.submitKyc(username, request);
            response.put("success", true);
            response.put("message", "实名认证已提交，请等待系统审核（预计5-20分钟）");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 查询实名认证状态
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus(@RequestParam String username) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isPresent()) {
            response.put("success", true);
            response.put("kycStatus", userOpt.get().getKycStatus());
            
            KycRecord record = kycService.getKycRecord(username);
            response.put("kycDetails", record);
        } else {
            response.put("success", false);
            response.put("message", "用户不存在");
        }
        return response;
    }
}
