package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "node_applications")
public class NodeApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String organization;   // X.500 中的 O（如 PartyB）

    @Column(nullable = false)
    private String locality;       // X.500 中的 L（如 New York）

    @Column(nullable = false)
    private String country;        // X.500 中的 C（如 US）

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    private String applicant;      // 申请人用户名（可从认证上下文获取）

    private LocalDateTime applyTime = LocalDateTime.now();

    private LocalDateTime reviewTime;

    // 无参构造
    public NodeApplication() {}

    // 全参构造（不含自动生成字段）
    public NodeApplication(String organization, String locality, String country, String applicant) {
        this.organization = organization;
        this.locality = locality;
        this.country = country;
        this.applicant = applicant;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApplicant() { return applicant; }
    public void setApplicant(String applicant) { this.applicant = applicant; }

    public LocalDateTime getApplyTime() { return applyTime; }
    public void setApplyTime(LocalDateTime applyTime) { this.applyTime = applyTime; }

    public LocalDateTime getReviewTime() { return reviewTime; }
    public void setReviewTime(LocalDateTime reviewTime) { this.reviewTime = reviewTime; }
}