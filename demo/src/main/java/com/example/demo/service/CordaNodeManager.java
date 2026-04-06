package com.example.demo.service;

import com.example.demo.entity.CordaNode;
import com.example.demo.repository.CordaNodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CordaNodeManager {

    @Autowired
    private CordaNodeRepository nodeRepository;

    @Autowired
    private RestTemplate restTemplate;

    // 本地缓存：key = 节点名称（短名称或全名），value = 对应的 CordaService 实例
    private final Map<String, CordaService> serviceCache = new ConcurrentHashMap<>();

    /**
     * 获取指定节点的服务（优先从缓存获取，缓存未命中时查询数据库并缓存）
     */
public CordaService getNodeService(String nodeName) {
        return serviceCache.computeIfAbsent(nodeName, name -> {
            // 1. 优先尝试精确匹配 (数据库存什么就查什么)
            CordaNode node = nodeRepository.findByName(name).orElse(null);

            // 2. 如果精确匹配失败，且传入的 name 是短名称 (不包含 "=")
            if (node == null && !name.contains("=")) {
                String targetOrg = "O=" + name;
                
                // 在数据库所有节点中模糊匹配以 O=PartyTest 开头的节点
                node = nodeRepository.findAll().stream()
                        .filter(n -> n.getName().startsWith(targetOrg + ",") || n.getName().equals(targetOrg))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("未找到节点: " + name));
            } else if (node == null) {
                // 如果传入的是全名但没找到，直接抛错
                throw new IllegalArgumentException("未找到节点: " + name);
            }

            return new CordaService(restTemplate, node.getBaseUrl());
        });
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

    // --- 新增/删除节点时同步更新数据库并清理缓存 ---

    @Transactional
    public void saveNode(String name, String baseUrl) {
        CordaNode node = nodeRepository.findByName(name).orElse(new CordaNode());
        node.setName(name);
        node.setBaseUrl(baseUrl);
        nodeRepository.save(node);

        // 数据库更新后，清除该节点的缓存，保证下次获取时拉取最新数据
        serviceCache.remove(name);
    }

    @Transactional
    public void removeNode(String name) {
        // 1. 先尝试删除完全匹配的完整名称（针对后来通过页面动态添加并存入全名的节点）
        nodeRepository.findByName(name).ifPresent(node -> {
            nodeRepository.delete(node);
            serviceCache.remove(name);      // 清除精确名称缓存
        });

        // 2. 如果传入的是 X.500 格式 (如 O=PartyE,L=Tokyo,C=JP)，提取短名称去数据库匹配
        Matcher matcher = Pattern.compile("O=([^,]+)").matcher(name);
        if (matcher.find()) {
            String shortName = matcher.group(1); // 提取出 PartyE

            // 尝试删除大写开头的短名称 (如 PartyE)
            nodeRepository.findByName(shortName).ifPresent(node -> {
                nodeRepository.delete(node);
                serviceCache.remove(shortName);
            });

            // 尝试删除小写开头的短名称 (如 partyE，这是系统初始化时默认存入的格式)
            String lowerFirstShortName = shortName.substring(0, 1).toLowerCase() + shortName.substring(1);
            nodeRepository.findByName(lowerFirstShortName).ifPresent(node -> {
                nodeRepository.delete(node);
                serviceCache.remove(lowerFirstShortName);
            });
        }
    }

    /**
     * 可选：手动刷新整个缓存（例如在系统初始化或批量更新后调用）
     */
    public void clearCache() {
        serviceCache.clear();
    }
}
