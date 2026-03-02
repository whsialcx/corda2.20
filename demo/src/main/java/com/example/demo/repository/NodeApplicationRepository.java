package com.example.demo.repository;

import com.example.demo.entity.NodeApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NodeApplicationRepository extends JpaRepository<NodeApplication, Long> {

    /**
     * 检查是否存在相同组织（O）且状态为 PENDING 或 APPROVED 的申请
     */
    boolean existsByOrganizationAndStatusIn(String organization, List<String> statuses);

    /**
     * 根据状态查询申请列表
     */
    List<NodeApplication> findByStatus(String status);

    /**
     * 根据申请人用户名查询所有申请记录（新增方法，用于用户个人中心）
     */
    List<NodeApplication> findByApplicant(String applicant);
}