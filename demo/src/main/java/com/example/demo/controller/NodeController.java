package com.example.demo.controller;

import com.example.demo.service.PowerShellService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.demo.service.CordaNodeManager;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@RestController
@RequestMapping("/api/nodes")// 所有接口以/api/nodes开头
public class NodeController 
{
    @Autowired
    private PowerShellService powerShellService;

    @Autowired
    private CordaNodeManager nodeManager;
    
    //验证 Corda 项目配置
    @GetMapping("/validate")
    public Map<String, Object> validateCordaProject() 
    {
        Map<String, Object> response = new HashMap<>();//构建响应
        
        PowerShellService.CordaProjectInfo projectInfo = powerShellService.getCordaProjectInfo();
        
        response.put("projectRoot", projectInfo.getProjectRoot());// 项目根目录
        response.put("rootExists", projectInfo.isRootExists());
        response.put("buildGradleExists", projectInfo.isBuildGradleExists());//build.gradle是否存在
        response.put("scriptExists", projectInfo.isScriptExists());//脚本是否存在
        response.put("scriptPath", projectInfo.getScriptPath());
        response.put("valid", projectInfo.isRootExists() && projectInfo.isBuildGradleExists() && projectInfo.isScriptExists());
        
        return response;
    }

    @GetMapping("/list")
    public Map<String, Object> listNodes() {
        Map<String, Object> response = new HashMap<>();
        try {
            // 1. 直接从数据库拉取节点名称数组 (耗时从数百毫秒降至几毫秒)
            String[] nodeNamesArray = nodeManager.getNodeNames();
            java.util.List<String> nodes = java.util.Arrays.asList(nodeNamesArray);
            
            // 2. 获取完整的节点对象列表（如果你后续前端想要直接拿到 baseUrl，可以用这个）
            // 需要在开头引入：import com.example.demo.entity.CordaNode;
            java.util.List<com.example.demo.entity.CordaNode> nodeDetails = nodeManager.getAllNodeDetails();

            response.put("success", true);
            // 保持向前兼容，维持原有的 nodes 字段结构
            response.put("nodes", nodes); 
            // 额外返回详细数据，供前端展示更多信息（如 API 端口等）
            response.put("nodeDetails", nodeDetails);
            response.put("count", nodes.size());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "从数据库获取节点列表时发生错误: " + e.getMessage());
        }
        return response;
    }

    
    @PostMapping("/add")
    public Map<String, Object> addNode(@RequestBody NodeRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!powerShellService.validateCordaProject()) {
                response.put("success", false);
                response.put("message", "Corda 项目配置验证失败，请检查配置");
                PowerShellService.CordaProjectInfo info = powerShellService.getCordaProjectInfo();
                response.put("projectInfo", info);
                return response;
            }
            
            StringBuilder arguments = new StringBuilder();
            // 修改点1: 去掉 NodeName 的双引号
            arguments.append("-NodeName ").append(request.getNodeName());
            
            if (request.isAutoPorts()) {
                arguments.append(" -AutoPorts");
            } else {
                if (request.getP2pPort() != null) 
                    arguments.append(" -P2PPort ").append(request.getP2pPort());
                if (request.getRpcPort() != null) 
                    arguments.append(" -RPCPort ").append(request.getRpcPort());
                if (request.getAdminPort() != null) 
                    arguments.append(" -AdminPort ").append(request.getAdminPort());
            }
            
            if (request.isAutoDb()) {
                arguments.append(" -AutoDb");
            } else {
                // 修改点2: 去掉 DbName 和 DbUser 的双引号
                if (request.getDbName() != null) 
                    arguments.append(" -DbName ").append(request.getDbName());
                if (request.getDbUser() != null) 
                    arguments.append(" -DbUser ").append(request.getDbUser());
            }
            
            PowerShellService.ProcessResult result = 
                powerShellService.executePowerShellScript(arguments.toString());
            
            if (result.isSuccess() && result.getExitCode() == 0) {
                // 1. 从 PowerShell 脚本输出中解析分配的服务器端口
                String output = result.getOutput();
                int serverPort = extractServerPort(output);
                
                // 2. 拼接完整的 baseUrl (假设你的节点服务器运行在 localhost)
                // 如果你的应用已经在 Linux 服务器上，这里可能需要换成具体的服务器 IP 或使用 127.0.0.1
                String baseUrl = "http://localhost:" + serverPort;
                
                // 3. 将节点信息同步保存到 PostgreSQL 数据库
                nodeManager.saveNode(request.getNodeName(), baseUrl);

                response.put("success", true);
                response.put("message", "节点添加成功，并已同步至数据库");
                response.put("baseUrl", baseUrl); // 返回给前端参考
                response.put("output", output);
            } else {
                response.put("success", false);
                response.put("message", "节点添加失败");
                response.put("error", result.getError());
                response.put("output", result.getOutput());
                response.put("exitCode", result.getExitCode());
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "执行脚本时发生错误: " + e.getMessage());
        }
        
        return response;
    }
    // 用于构造网络
    @PostMapping("/deploy")
    public Map<String, Object> deployNetwork() 
    {
        Map<String, Object> response = new HashMap<>();
        try 
        {
            if (!powerShellService.validateCordaProject()) 
            {
                response.put("success", false);
                response.put("message", "Corda 项目配置验证失败，请检查配置");
                return response;
            }

            PowerShellService.ProcessResult result = powerShellService.executeGradleDeploy();

            if (result.isSuccess() && result.getExitCode() == 0) 
            {
                response.put("success", true);
                response.put("message", "gradlew deployNodes 执行成功");
                response.put("output", result.getOutput());
            } 
            else
            {
                response.put("success", false);
                response.put("message", "gradlew deployNodes 执行失败");
                response.put("exitCode", result.getExitCode());
                response.put("output", result.getOutput());
                response.put("error", result.getError());
            }
        } 
        catch (Exception e) 
        {
            response.put("success", false);
            response.put("message", "执行部署时发生错误: " + e.getMessage());
        }
        return response;
    }
    // 删除节点
    @PostMapping("/remove")
    public Map<String, Object> removeNode(@RequestBody RemoveNodeRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!powerShellService.validateCordaProject()) {
                response.put("success", false);
                response.put("message", "Corda 项目配置验证失败，请检查配置");
                return response;
            }
            
            String requestedName = request.getNodeName();
            String fullNodeNameToRemove = requestedName; // 默认使用传过来的名字
            
            // 【核心补全逻辑】：如果传过来的名字不包含 "="，说明是短名称 (如 partyA 或 PartyA)
            if (!requestedName.contains("=")) {
                // 获取 build.gradle 中所有的全名
                java.util.List<String> allFullNames = powerShellService.getNodeNames();
                for (String fullName : allFullNames) {
                    // 使用正则提取全名中的组织名，例如从 O=PartyA,L=London 提取 PartyA
                    Matcher m = Pattern.compile("O=([^,]+)").matcher(fullName);
                    if (m.find()) {
                        String orgName = m.group(1);
                        // 忽略大小写匹配 (partyA 等于 PartyA)
                        if (orgName.equalsIgnoreCase(requestedName)) {
                            fullNodeNameToRemove = fullName; // 找到全名，进行替换
                            break;
                        }
                    }
                }
            }
            
            String arguments = "-RemoveNode " + fullNodeNameToRemove;
            
            PowerShellService.ProcessResult result = 
                powerShellService.executePowerShellScript(arguments);
            
            if (result.isSuccess() && result.getExitCode() == 0) {
                // 脚本执行成功后，从 PostgreSQL 数据库中同步删除该节点记录
                // 这里传入原始的 request.getNodeName() (如 partyA)，让 JPA 删掉数据库里的短名记录
                nodeManager.removeNode(request.getNodeName());

                response.put("success", true);
                response.put("message", "节点删除成功，并已从数据库中移除");
                response.put("output", result.getOutput());
            } else {
                response.put("success", false);
                response.put("message", "节点删除失败");
                response.put("error", result.getError());
                response.put("output", result.getOutput());
                response.put("exitCode", result.getExitCode());
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "执行脚本时发生错误: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/start-all")//启动所有节点
    public Map<String, Object> startAllNodes() 
    {
        Map<String, Object> response = new HashMap<>();
        try 
        {
            if (!powerShellService.validateCordaProject()) 
            {
                response.put("success", false);
                response.put("message", "Corda 项目配置验证失败，请检查配置");
                return response;
            }

            // 执行 runnodes.bat脚本来启动所有节点
            PowerShellService.ProcessResult result = powerShellService.executeRunnodesScript();

            if (result.isSuccess() && result.getExitCode() == 0) {
                response.put("success", true);
                response.put("message", "节点启动成功");
                response.put("output", result.getOutput());
            } 
            else 
            {
                response.put("success", false);
                response.put("message", "节点启动失败");
                response.put("exitCode", result.getExitCode());
                response.put("output", result.getOutput());
                response.put("error", result.getError());
            }
        } 
        catch (Exception e) 
        {
            response.put("success", false);
            response.put("message", "执行启动脚本时发生错误: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/start")//启动某一个节点
    public Map<String, Object> startNode(@RequestBody StartNodeRequest request) 
    {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!powerShellService.validateCordaProject()) 
            {
                response.put("success", false);
                response.put("message", "Corda 项目配置验证失败，请检查配置");
                return response;
            }

            PowerShellService.ProcessResult result = powerShellService.startNode(request.getNodeName());

            if (result.isSuccess() && result.getExitCode() == 0) 
            {
                response.put("success", true);
                response.put("message", "节点启动成功");
                response.put("output", result.getOutput());
            } 
            else 
            {
                response.put("success", false);
                response.put("message", "节点启动失败");
                response.put("exitCode", result.getExitCode());
                response.put("output", result.getOutput());
                response.put("error", result.getError());
            }
        }
        catch (Exception e) 
        {
            response.put("success", false);
            response.put("message", "执行启动脚本时发生错误: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/stop")//终止节点
    public Map<String, Object> stopNode(@RequestBody StartNodeRequest request) 
    {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!powerShellService.validateCordaProject()) 
            {
                response.put("success", false);
                response.put("message", "Corda 项目配置验证失败，请检查配置");
                return response;
            }

            PowerShellService.ProcessResult result = powerShellService.stopNode(request.getNodeName());

            if (result.isSuccess() && result.getExitCode() == 0) {
                response.put("success", true);
                response.put("message", "节点停止成功");
                response.put("output", result.getOutput());
            } 
            else 
            {
                response.put("success", false);
                response.put("message", "节点停止失败");
                response.put("exitCode", result.getExitCode());
                response.put("output", result.getOutput());
                response.put("error", result.getError());
            }
        } 
        catch (Exception e) 
        {
            response.put("success", false);
            response.put("message", "停止节点时发生错误: " + e.getMessage());
        }
        return response;
    }

    private int extractServerPort(String output) {
        // 匹配 add_node.ps1 中的输出格式: "服务器端口=50008"
        Pattern pattern = Pattern.compile("服务器端口=(\\d+)");
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        // 如果没有匹配到（比如手动指定了端口没有走自动分配逻辑），可以返回一个默认占位端口，或抛出异常
        return 50008; 
    }
    
    public static class StartNodeRequest 
    {
        private String nodeName;

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }
    }
        
    
    // 请求 DTO 类
    public static class NodeRequest {
        private String nodeName;
        private Integer p2pPort;
        private Integer rpcPort;
        private Integer adminPort;
        private String dbName;
        private String dbUser;
        private boolean autoPorts = false;
        private boolean autoDb = false;
        
        // Getters and Setters
        public String getNodeName() { return nodeName; }
        public void setNodeName(String nodeName) { this.nodeName = nodeName; }
        
        public Integer getP2pPort() { return p2pPort; }
        public void setP2pPort(Integer p2pPort) { this.p2pPort = p2pPort; }
        
        public Integer getRpcPort() { return rpcPort; }
        public void setRpcPort(Integer rpcPort) { this.rpcPort = rpcPort; }
        
        public Integer getAdminPort() { return adminPort; }
        public void setAdminPort(Integer adminPort) { this.adminPort = adminPort; }
        
        public String getDbName() { return dbName; }
        public void setDbName(String dbName) { this.dbName = dbName; }
        
        public String getDbUser() { return dbUser; }
        public void setDbUser(String dbUser) { this.dbUser = dbUser; }
        
        public boolean isAutoPorts() { return autoPorts; }
        public void setAutoPorts(boolean autoPorts) { this.autoPorts = autoPorts; }
        
        public boolean isAutoDb() { return autoDb; }
        public void setAutoDb(boolean autoDb) { this.autoDb = autoDb; }
    }
    
    public static class RemoveNodeRequest {
        private String nodeName;
        
        public String getNodeName() { return nodeName; }
        public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    }
}
