package com.example.demo.entity;

import jakarta.persistence.*; // 如果使用的是较低版本的Spring Boot，可能需要换成 javax.persistence.*

@Entity
@Table(name = "corda_nodes")
public class CordaNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // 节点名称，例如：partyA, partyB

    @Column(nullable = false)
    private String baseUrl; // 节点的 Web API 基础地址，例如：http://localhost:50005

    // 无参构造
    public CordaNode() {}

    public CordaNode(String name, String baseUrl) {
        this.name = name;
        this.baseUrl = baseUrl;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
}