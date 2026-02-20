package com.example.demo.service;

import com.example.demo.entity.CordaNode;
import com.example.demo.repository.CordaNodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CordaNodeManager {

    @Autowired
    private CordaNodeRepository nodeRepository;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 获取指定节点的服务 (动态实例化)
     */
    public CordaService getNodeService(String nodeName) {
        CordaNode node = nodeRepository.findByName(nodeName)
                .orElseThrow(() -> new IllegalArgumentException("未找到节点: " + nodeName));
        
        // 使用数据库中存储的 baseUrl 动态创建 CordaService
        return new CordaService(restTemplate, node.getBaseUrl());
    }

    /**
     * 获取所有节点名称
     */
    public String[] getNodeNames() {
        return nodeRepository.findAll().stream()
                .map(CordaNode::getName)
                .toArray(String[]::new);
    }

    public List<CordaNode> getAllNodeDetails() {
        return nodeRepository.findAll();
    }

    /**
     * 获取默认节点服务（如果有这个需求，可以指定一个默认的逻辑）
     */
    public CordaService getDefaultNodeService() {
        return getNodeService("partyA"); 
    }

    // --- 新增：用于在添加/删除节点时同步更新数据库 ---

    @Transactional
    public void saveNode(String name, String baseUrl) {
        CordaNode node = nodeRepository.findByName(name).orElse(new CordaNode());
        node.setName(name);
        node.setBaseUrl(baseUrl);
        nodeRepository.save(node);
    }

    @Transactional
    public void removeNode(String name) {
        // 1. 先尝试删除完全匹配的完整名称（针对后来通过页面动态添加并存入全名的节点）
        nodeRepository.findByName(name).ifPresent(node -> {
            nodeRepository.delete(node);
        });

        // 2. 如果传入的是 X.500 格式 (如 O=PartyE,L=Tokyo,C=JP)，提取短名称去数据库匹配
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("O=([^,]+)").matcher(name);
        if (matcher.find()) {
            String shortName = matcher.group(1); // 提取出 PartyE
            
            // 尝试删除大写开头的短名称 (如 PartyE)
            nodeRepository.findByName(shortName).ifPresent(node -> {
                nodeRepository.delete(node);
            });
            
            // 尝试删除小写开头的短名称 (如 partyE，这是系统初始化时默认存入的格式)
            String lowerFirstShortName = shortName.substring(0, 1).toLowerCase() + shortName.substring(1);
            nodeRepository.findByName(lowerFirstShortName).ifPresent(node -> {
                nodeRepository.delete(node);
            });
        }
    }
}
