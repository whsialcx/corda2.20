package com.example.demo.repository;

import com.example.demo.entity.KycRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface KycRecordRepository extends JpaRepository<KycRecord, Long> {
    Optional<KycRecord> findByUsername(String username);
    
    // 查找所有状态为 status，并且提交时间早于指定时间的记录（用于自动审核）
    List<KycRecord> findByStatusAndSubmitTimeBefore(String status, LocalDateTime time);
}
