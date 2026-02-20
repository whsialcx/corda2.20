package net.corda.samples.example.webserver;

import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.example.states.TemperatureState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/temperature")
public class TemperatureController {
    private static final Logger logger = LoggerFactory.getLogger(TemperatureController.class);
    
    private final CordaRPCOps proxy;
    private final Party self;

    public TemperatureController(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.self = proxy.nodeInfo().getLegalIdentities().get(0);
    }

    // 1. 手动记录温度数据
    @PostMapping("/record")
    public ResponseEntity<Map<String, Object>> recordTemperature(
            @RequestBody TemperatureRecordRequest request) {
        
        try {
            logger.info("尝试解析接收方: {}", request.getReceiver());
            
            // 方法1: 使用精确匹配
            Set<Party> receiverParties = proxy.partiesFromName(request.getReceiver(), false);
            
            // 方法2: 如果精确匹配失败，尝试模糊匹配
            if (receiverParties.isEmpty()) {
                logger.info("精确匹配失败，尝试模糊匹配...");
                receiverParties = proxy.partiesFromName(request.getReceiver(), true);
            }
            
            // 方法3: 如果还找不到，尝试去掉空格匹配
            if (receiverParties.isEmpty()) {
                logger.info("模糊匹配失败，尝试去掉空格匹配...");
                // 获取网络中的所有Party
                List<Party> allParties = new ArrayList<>();
                proxy.networkMapSnapshot().forEach(node -> 
                    allParties.addAll(node.getLegalIdentities()));
                
                for (Party party : allParties) {
                    String partyName = party.getName().toString();
                    // 去掉空格比较
                    String requestedName = request.getReceiver().replaceAll("\\s+", "");
                    String actualName = partyName.replaceAll("\\s+", "");
                    
                    if (actualName.equalsIgnoreCase(requestedName)) {
                        receiverParties.add(party);
                        logger.info("找到匹配的Party: {} -> {}", partyName, request.getReceiver());
                        break;
                    }
                }
            }
            
            if (receiverParties.isEmpty()) {
                logger.error("无法找到接收方: {}", request.getReceiver());
                
                // 获取所有可用的接收方
                List<String> availableReceivers = proxy.networkMapSnapshot().stream()
                    .flatMap(node -> node.getLegalIdentities().stream())
                    .filter(party -> !party.equals(self))
                    .map(party -> party.getName().toString())
                    .collect(java.util.stream.Collectors.toList());
                
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "status", "error",
                        "message", "未找到接收方: " + request.getReceiver(),
                        "availableReceivers", availableReceivers,
                        "suggestion", "请从 availableReceivers 列表中选择一个接收方"
                    ));
            }
            
            Party receiver = receiverParties.iterator().next();
            logger.info("成功解析接收方: {} -> {}", request.getReceiver(), receiver.getName());
            
            // 尝试执行温度Flow
            SignedTransaction tx = proxy.startTrackedFlowDynamic(
                    net.corda.samples.example.flows.TemperatureFlows.CreateTemperatureFlow.class,
                    request.getTimestamp() != null ? request.getTimestamp() : Instant.now(),
                    request.getTemperature(),
                    request.isCritical(),
                    receiver
            ).getReturnValue().get();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("transactionId", tx.getId().toString());
            response.put("message", "温度记录创建成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("记录温度失败", e);
            
            // 修复：避免 e.getMessage() 为 null
            String errorMessage = e.getMessage();
            if (errorMessage == null) {
                errorMessage = e.getClass().getSimpleName();
            }
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "status", "error", 
                        "message", errorMessage,
                        "errorType", e.getClass().getName()
                    ));
        }
    }

    // 2. 查询温度记录
    @GetMapping("/query")
    public ResponseEntity<Map<String, Object>> queryTemperatures(
            @RequestParam(defaultValue = "false") boolean onlyCritical) {
        
        try {
            // 先检查是否有温度Flow
            String allFlows = proxy.registeredFlows().toString();
            if (!allFlows.contains("Temperature")) {
                logger.warn("TemperatureFlow未部署，返回空列表");
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("count", 0);
                response.put("records", new ArrayList<>());
                response.put("message", "TemperatureFlow未部署，返回空列表");
                return ResponseEntity.ok(response);
            }
            
            List<StateAndRef<TemperatureState>> states = proxy.startTrackedFlowDynamic(
                    net.corda.samples.example.flows.TemperatureFlows.QueryTemperaturesFlow.class,
                    onlyCritical
            ).getReturnValue().get();

            List<Map<String, Object>> records = states.stream().map(state -> {
                TemperatureState temp = state.getState().getData();
                Map<String, Object> record = new HashMap<>();
                record.put("timestamp", temp.getTimestamp().toString());
                record.put("temperature", temp.getTemperature());
                record.put("critical", temp.isCritical());
                record.put("owner", temp.getOwner().getName().toString());
                record.put("receiver", temp.getReceiver().getName().toString());
                record.put("linearId", temp.getLinearId().toString());
                record.put("stateRef", state.getRef().toString());
                return record;
            }).collect(java.util.stream.Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", records.size());
            response.put("records", records);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("查询温度记录失败", e);
            
            String errorMessage = e.getMessage();
            if (errorMessage == null) {
                errorMessage = e.getClass().getSimpleName();
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "status", "error", 
                        "message", errorMessage
                    ));
        }
    }

    // 3. 转移温度记录
    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transferTemperature(
            @RequestBody TransferRequest request) {
        
        try {
            // 解析新接收方
            Set<Party> newReceiverParties = proxy.partiesFromName(request.getNewReceiver(), false);
            
            if (newReceiverParties.isEmpty()) {
                // 尝试模糊匹配
                newReceiverParties = proxy.partiesFromName(request.getNewReceiver(), true);
            }
            
            if (newReceiverParties.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "status", "error", 
                        "message", "未找到接收方: " + request.getNewReceiver()
                    ));
            }
            
            Party newReceiver = newReceiverParties.iterator().next();
            
            SignedTransaction tx = proxy.startTrackedFlowDynamic(
                    net.corda.samples.example.flows.TemperatureFlows.TransferTemperatureFlow.class,
                    request.getLinearId(),
                    newReceiver
            ).getReturnValue().get();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("transactionId", tx.getId().toString());
            response.put("message", "温度记录转移成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("转移温度记录失败", e);
            
            String errorMessage = e.getMessage();
            if (errorMessage == null) {
                errorMessage = e.getClass().getSimpleName();
            }
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "status", "error", 
                        "message", errorMessage
                    ));
        }
    }

    // 4. 获取可用的接收方列表
    @GetMapping("/receivers")
    public ResponseEntity<Map<String, Object>> getAvailableReceivers() {
        try {
            List<String> receivers = proxy.networkMapSnapshot().stream()
                    .flatMap(node -> node.getLegalIdentities().stream())
                    .filter(party -> !party.equals(self)) // 排除自己
                    .map(party -> party.getName().toString())
                    .collect(java.util.stream.Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("receivers", receivers);
            response.put("self", self.getName().toString());
            response.put("count", receivers.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取接收方列表失败", e);
            
            String errorMessage = e.getMessage();
            if (errorMessage == null) {
                errorMessage = e.getClass().getSimpleName();
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "status", "error", 
                        "message", errorMessage
                    ));
        }
    }
    
    // 5. 添加测试端点：验证Party解析
    @GetMapping("/test/party-resolve")
    public ResponseEntity<Map<String, Object>> testPartyResolve(@RequestParam String partyName) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("requestedName", partyName);
            
            // 精确匹配
            Set<Party> exactMatches = proxy.partiesFromName(partyName, false);
            result.put("exactMatchCount", exactMatches.size());
            result.put("exactMatches", exactMatches.stream()
                .map(p -> p.getName().toString())
                .collect(java.util.stream.Collectors.toList()));
            
            // 模糊匹配
            Set<Party> fuzzyMatches = proxy.partiesFromName(partyName, true);
            result.put("fuzzyMatchCount", fuzzyMatches.size());
            result.put("fuzzyMatches", fuzzyMatches.stream()
                .map(p -> p.getName().toString())
                .collect(java.util.stream.Collectors.toList()));
            
            // 网络中的所有Party
            List<String> allParties = proxy.networkMapSnapshot().stream()
                .flatMap(node -> node.getLegalIdentities().stream())
                .map(p -> p.getName().toString())
                .collect(java.util.stream.Collectors.toList());
            result.put("allParties", allParties);
            
            // 去掉空格匹配
            String normalizedRequest = partyName.replaceAll("\\s+", "");
            List<String> normalizedMatches = allParties.stream()
                .filter(p -> p.replaceAll("\\s+", "").equalsIgnoreCase(normalizedRequest))
                .collect(java.util.stream.Collectors.toList());
            result.put("normalizedMatchCount", normalizedMatches.size());
            result.put("normalizedMatches", normalizedMatches);
            
            result.put("status", "success");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("测试Party解析失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // 请求DTO类
    public static class TemperatureRecordRequest {
        private Double temperature;
        private Boolean critical;
        private String receiver;
        private Instant timestamp;

        // getters and setters
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        public Boolean isCritical() { return critical; }
        public void setCritical(Boolean critical) { this.critical = critical; }
        public String getReceiver() { return receiver; }
        public void setReceiver(String receiver) { this.receiver = receiver; }
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
        
        @Override
        public String toString() {
            return "TemperatureRecordRequest{temperature=" + temperature + 
                   ", critical=" + critical + 
                   ", receiver='" + receiver + "'" + 
                   ", timestamp=" + timestamp + "}";
        }
    }

    public static class TransferRequest {
        private String linearId;
        private String newReceiver;

        // getters and setters
        public String getLinearId() { return linearId; }
        public void setLinearId(String linearId) { this.linearId = linearId; }
        public String getNewReceiver() { return newReceiver; }
        public void setNewReceiver(String newReceiver) { this.newReceiver = newReceiver; }
    }
}