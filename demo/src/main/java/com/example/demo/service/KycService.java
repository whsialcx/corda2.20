package com.example.demo.service;

import com.example.demo.entity.KycRecord;
import com.example.demo.entity.User;
import com.example.demo.repository.KycRecordRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class KycService {

    @Autowired
    private KycRecordRepository kycRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void submitKyc(String username, Map<String, String> request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!"UNVERIFIED".equals(user.getKycStatus()) && !"REJECTED".equals(user.getKycStatus())) {
            throw new RuntimeException("实名认证正在审核中或已通过，无需重复提交");
        }

        String type = request.get("type"); // INDIVIDUAL 或 ENTERPRISE

        KycRecord record = kycRepository.findByUsername(username).orElse(new KycRecord());
        record.setUsername(username);
        record.setType(type);
        record.setName(request.get("name"));
        record.setIdNumber(request.get("idNumber"));
        
        if ("ENTERPRISE".equals(type)) {
            record.setLegalPerson(request.get("legalPerson"));
        } else {
            // 个人类型的身份证正反面（前端可以传Base64或者上传后的图片URL）
            record.setIdCardFrontUrl(request.get("idCardFrontUrl"));
            record.setIdCardBackUrl(request.get("idCardBackUrl"));
        }

        record.setStatus("PENDING");
        record.setSubmitTime(LocalDateTime.now());
        
        kycRepository.save(record);

        // 更新用户状态为审核中
        user.setKycStatus("PENDING");
        userRepository.save(user);
    }

    /**
     * 定时任务：模拟自动审核
     * 每隔 1 分钟执行一次，将提交超过 N 分钟（此处设为 5 分钟测试）的记录自动变更为 APPROVED
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoApproveKycRecords() {
        // 设置审核阈值（例如：提交时间早于 5 分钟前的自动通过）
        // 实际想设20分钟的话改为 minusMinutes(20)
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5); 
        
        List<KycRecord> pendingRecords = kycRepository.findByStatusAndSubmitTimeBefore("PENDING", threshold);
        
        for (KycRecord record : pendingRecords) {
            record.setStatus("APPROVED");
            record.setApproveTime(LocalDateTime.now());
            kycRepository.save(record);
            
            // 同步更新 User 表中的状态
            userRepository.findByUsername(record.getUsername()).ifPresent(user -> {
                user.setKycStatus("VERIFIED");
                userRepository.save(user);
            });
            
            System.out.println("自动审核通过了用户实名: " + record.getUsername());
        }
    }
    
    public KycRecord getKycRecord(String username) {
        return kycRepository.findByUsername(username).orElse(null);
    }
}
