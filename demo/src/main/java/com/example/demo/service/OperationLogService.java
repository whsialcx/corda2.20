package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class OperationLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(OperationLogService.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 记录操作日志
     */
    public void logOperation(String username, String operationType, String targetNode, 
                           Map<String, Object> parameters, String status, String errorMessage,
                           LocalDateTime startTime, LocalDateTime endTime, HttpServletRequest request) {
        
        try {
            String sql = """
                INSERT INTO operation_logs 
                (username, operation_type, target_node, parameters, status, error_message, 
                 start_time, end_time, duration_ms, ip_address, user_agent)
                VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            Long durationMs = null;
            if (startTime != null && endTime != null) {
                durationMs = java.time.Duration.between(startTime, endTime).toMillis();
            }
            
            String ipAddress = getClientIpAddress(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : null;
            String paramsJson = parameters != null ? mapToJson(parameters) : null;
            
            jdbcTemplate.update(sql, username, operationType, targetNode, paramsJson, 
                               status, errorMessage, startTime, endTime, durationMs, 
                               ipAddress, userAgent);
            
        } catch (Exception e) {
            logger.error("记录操作日志失败", e);
        }
    }
    
    /**
     * 记录节点状态
     */
    public void logNodeStatus(String nodeName, Map<String, Object> statusInfo) {
        try {
            String sql = """
                INSERT INTO node_status_history 
                (node_name, status, is_running, process_id, cpu_usage, memory_usage, 
                 last_start_time, last_stop_time, log_errors_count, directory_size, checks)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                """;
            
            jdbcTemplate.update(sql,
                nodeName,
                statusInfo.get("status"),
                statusInfo.get("running"),
                statusInfo.get("processId"),
                statusInfo.get("cpuUsage"),
                statusInfo.get("memoryUsage"),
                statusInfo.get("lastStartTime"),
                statusInfo.get("lastStopTime"),
                statusInfo.get("logErrorsCount"),
                statusInfo.get("directorySize"),
                mapToJson((Map<String, Object>) statusInfo.get("checks"))
            );
            
        } catch (Exception e) {
            logger.error("记录节点状态失败", e);
        }
    }
    
    /**
     * 保存节点配置
     */
    public void saveNodeConfiguration(String nodeName, Map<String, Object> config) {
        try {
            String sql = """
                INSERT INTO node_configurations 
                (node_name, p2p_port, rpc_port, admin_port, db_name, db_user, 
                 auto_ports, auto_db, node_config)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                ON CONFLICT (node_name) 
                DO UPDATE SET 
                    p2p_port = EXCLUDED.p2p_port,
                    rpc_port = EXCLUDED.rpc_port,
                    admin_port = EXCLUDED.admin_port,
                    db_name = EXCLUDED.db_name,
                    db_user = EXCLUDED.db_user,
                    auto_ports = EXCLUDED.auto_ports,
                    auto_db = EXCLUDED.auto_db,
                    node_config = EXCLUDED.node_config,
                    updated_at = CURRENT_TIMESTAMP
                """;
            
            jdbcTemplate.update(sql,
                nodeName,
                config.get("p2pPort"),
                config.get("rpcPort"),
                config.get("adminPort"),
                config.get("dbName"),
                config.get("dbUser"),
                config.get("autoPorts"),
                config.get("autoDb"),
                mapToJson(config)
            );
            
        } catch (Exception e) {
            logger.error("保存节点配置失败", e);
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return null;
        
        String[] headers = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", 
                           "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        return request.getRemoteAddr();
    }
    
    private String mapToJson(Map<String, Object> map) {
        if (map == null) return null;
        try {
            // 简化实现，实际项目中可以使用Jackson等JSON库
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":");
                if (entry.getValue() instanceof String) {
                    json.append("\"").append(entry.getValue()).append("\"");
                } else {
                    json.append(entry.getValue());
                }
                first = false;
            }
            json.append("}");
            return json.toString();
        } catch (Exception e) {
            logger.error("转换Map为JSON失败", e);
            return "{}";
        }
    }
}