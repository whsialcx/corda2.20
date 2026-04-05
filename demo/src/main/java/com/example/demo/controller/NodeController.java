package com.example.demo.controller;

import com.example.demo.entity.CordaNode;
import com.example.demo.entity.NodeApplication;
import com.example.demo.entity.User;                 // 新增导入
import com.example.demo.repository.CordaNodeRepository;
import com.example.demo.repository.NodeApplicationRepository;
import com.example.demo.repository.UserRepository;  // 新增导入
import com.example.demo.service.CordaNodeManager;
import com.example.demo.service.PowerShellService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import java.util.zip.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/nodes")
public class NodeController {

    @Autowired
    private PowerShellService powerShellService;

    @Autowired
    private CordaNodeManager nodeManager;

    @Autowired
    private CordaNodeRepository cordaNodeRepository;

    @Autowired
    private NodeApplicationRepository nodeApplicationRepository;
    
    @Autowired
    private UserRepository userRepository;   // 新增：用于检查实名状态

    @GetMapping("/validate")
    public Map<String, Object> validateCordaProject() {
        Map<String, Object> response = new HashMap<>();
        PowerShellService.CordaProjectInfo projectInfo = powerShellService.getCordaProjectInfo();
        response.put("projectRoot", projectInfo.getProjectRoot());
        response.put("rootExists", projectInfo.isRootExists());
        response.put("buildGradleExists", projectInfo.isBuildGradleExists());
        response.put("scriptExists", projectInfo.isScriptExists());
        response.put("scriptPath", projectInfo.getScriptPath());
        response.put("valid", projectInfo.isRootExists() && projectInfo.isBuildGradleExists() && projectInfo.isScriptExists());
        return response;
    }

    @GetMapping("/list")
    public Map<String, Object> listNodes() {
        Map<String, Object> response = new HashMap<>();
        try {
            String[] nodeNamesArray = nodeManager.getNodeNames();
            List<String> nodes = java.util.Arrays.asList(nodeNamesArray);
            List<CordaNode> nodeDetails = nodeManager.getAllNodeDetails();

            response.put("success", true);
            response.put("nodes", nodes);
            response.put("nodeDetails", nodeDetails);
            response.put("count", nodes.size());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "从数据库获取节点列表时发生错误: " + e.getMessage());
        }
        return response;
    }

    /**
     * 原有添加节点接口（内部调用 doAddNode 执行实际逻辑）
     */
    @PostMapping("/add")
    public Map<String, Object> addNode(@RequestBody NodeRequest request) {
        return doAddNode(request);
    }

    /**
     * 核心添加节点逻辑（从原 addNode 提取，改为 List 参数传递）
     */
    private Map<String, Object> doAddNode(NodeRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!powerShellService.validateCordaProject()) {
                response.put("success", false);
                response.put("message", "Corda 项目配置验证失败，请检查配置");
                PowerShellService.CordaProjectInfo info = powerShellService.getCordaProjectInfo();
                response.put("projectInfo", info);
                return response;
            }

            // 构建参数列表
            List<String> args = new ArrayList<>();
            args.add("-NodeName");
            args.add(request.getNodeName());

            if (request.isAutoPorts()) {
                args.add("-AutoPorts");
            } else {
                if (request.getP2pPort() != null) {
                    args.add("-P2PPort");
                    args.add(String.valueOf(request.getP2pPort()));
                }
                if (request.getRpcPort() != null) {
                    args.add("-RPCPort");
                    args.add(String.valueOf(request.getRpcPort()));
                }
                if (request.getAdminPort() != null) {
                    args.add("-AdminPort");
                    args.add(String.valueOf(request.getAdminPort()));
                }
            }

            if (request.isAutoDb()) {
                args.add("-AutoDb");
            } else {
                if (request.getDbName() != null) {
                    args.add("-DbName");
                    args.add(request.getDbName());
                }
                if (request.getDbUser() != null) {
                    args.add("-DbUser");
                    args.add(request.getDbUser());
                }
            }

            PowerShellService.ProcessResult result = powerShellService.executePowerShellScript(args);

            if (result.isSuccess() && result.getExitCode() == 0) {
                String output = result.getOutput();
                int serverPort = extractServerPort(output);
                String baseUrl = "http://localhost:" + serverPort;
                nodeManager.saveNode(request.getNodeName(), baseUrl);

                response.put("success", true);
                response.put("message", "节点添加成功，并已同步至数据库");
                response.put("baseUrl", baseUrl);
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

    @PostMapping("/deploy")
    public Map<String, Object> deployNetwork() {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!powerShellService.validateCordaProject()) {
                response.put("success", false);
                response.put("message", "Corda 项目配置验证失败，请检查配置");
                return response;
            }

            PowerShellService.ProcessResult result = powerShellService.executeGradleDeploy();

            if (result.isSuccess() && result.getExitCode() == 0) {
                response.put("success", true);
                response.put("message", "gradlew deployNodes 执行成功");
                response.put("output", result.getOutput());
            } else {
                response.put("success", false);
                response.put("message", "gradlew deployNodes 执行失败");
                response.put("exitCode", result.getExitCode());
                response.put("output", result.getOutput());
                response.put("error", result.getError());
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "执行部署时发生错误: " + e.getMessage());
        }
        return response;
    }

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
            String fullNodeNameToRemove = requestedName;

            if (!requestedName.contains("=")) {
                List<String> allFullNames = powerShellService.getNodeNames();
                for (String fullName : allFullNames) {
                    Matcher m = Pattern.compile("O=([^,]+)").matcher(fullName);
                    if (m.find()) {
                        String orgName = m.group(1);
                        if (orgName.equalsIgnoreCase(requestedName)) {
                            fullNodeNameToRemove = fullName;
                            break;
                        }
                    }
                }
            }

            // 构建参数列表
            List<String> args = new ArrayList<>();
            args.add("-RemoveNode");
            args.add(fullNodeNameToRemove);

            PowerShellService.ProcessResult result = powerShellService.executePowerShellScript(args);

            if (result.isSuccess() && result.getExitCode() == 0) {
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

    @PostMapping("/start-all")
    public Map<String, Object> startAllNodes() {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!powerShellService.validateCordaProject()) {
                response.put("success", false);
                response.put("message", "Corda 项目配置验证失败，请检查配置");
                return response;
            }

            PowerShellService.ProcessResult result = powerShellService.executeRunnodesScript();

            if (result.isSuccess() && result.getExitCode() == 0) {
                response.put("success", true);
                response.put("message", "节点启动成功");
                response.put("output", result.getOutput());
            } else {
                response.put("success", false);
                response.put("message", "节点启动失败");
                response.put("exitCode", result.getExitCode());
                response.put("output", result.getOutput());
                response.put("error", result.getError());
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "执行启动脚本时发生错误: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/start")
    public Map<String, Object> startNode(@RequestBody StartNodeRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!powerShellService.validateCordaProject()) {
                response.put("success", false);
                response.put("message", "Corda 项目配置验证失败，请检查配置");
                return response;
            }

            PowerShellService.ProcessResult result = powerShellService.startNode(request.getNodeName());

            if (result.isSuccess() && result.getExitCode() == 0) {
                response.put("success", true);
                response.put("message", "节点启动成功");
                response.put("output", result.getOutput());
            } else {
                response.put("success", false);
                response.put("message", "节点启动失败");
                response.put("exitCode", result.getExitCode());
                response.put("output", result.getOutput());
                response.put("error", result.getError());
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "执行启动脚本时发生错误: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/stop")
    public Map<String, Object> stopNode(@RequestBody StartNodeRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!powerShellService.validateCordaProject()) {
                response.put("success", false);
                response.put("message", "Corda 项目配置验证失败，请检查配置");
                return response;
            }

            PowerShellService.ProcessResult result = powerShellService.stopNode(request.getNodeName());

            if (result.isSuccess() && result.getExitCode() == 0) {
                response.put("success", true);
                response.put("message", "节点停止成功");
                response.put("output", result.getOutput());
            } else {
                response.put("success", false);
                response.put("message", "节点停止失败");
                response.put("exitCode", result.getExitCode());
                response.put("output", result.getOutput());
                response.put("error", result.getError());
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "停止节点时发生错误: " + e.getMessage());
        }
        return response;
    }

    @GetMapping("/download/{nodeName}")
    public ResponseEntity<Object> downloadNodeZip(@PathVariable String nodeName) {
        try {
            // 1. 获取该节点部署后的物理路径
            File nodeDir = powerShellService.getNodeBuildDirectory(nodeName);

            // 2. 检查文件夹是否存在（即管理员是否已执行 deployNodes）
            if (!nodeDir.exists() || !nodeDir.isDirectory()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "节点文件尚未生成。请联系管理员执行‘部署网络’操作，或稍后再试。");
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(error); // 202 Accepted 表示请求已收到但处理未完成
            }

            // 3. 创建临时 Zip 文件 (Java 临时文件机制是跨平台的)
            Path tempZip = Files.createTempFile("corda-node-" + nodeDir.getName(), ".zip");
            
            // 4. 执行压缩逻辑
            zipDirectory(nodeDir.toPath(), tempZip);

            // 5. 构建响应流
            Resource resource = new FileSystemResource(tempZip.toFile());
            
            // 设置下载头，适配各种浏览器
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nodeDir.getName() + ".zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(tempZip.toFile().length())
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "打包失败: " + e.getMessage()));
        }
    }

    private void zipDirectory(Path sourcePath, Path zipPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            Files.walk(sourcePath).filter(path -> !Files.isDirectory(path)).forEach(path -> {
                ZipEntry zipEntry = new ZipEntry(sourcePath.relativize(path).toString());
                try {
                    zos.putNextEntry(zipEntry);
                    Files.copy(path, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException("压缩失败: " + path, e);
                }
            });
        }
    }

    // ==================== 新增申请/审核接口 ====================

    /**
     * 用户申请新节点
     */
    @PostMapping("/apply")
    public Map<String, Object> applyForNode(@RequestBody NodeApplyRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            // ================= 新增：实名认证拦截 =================
            String applicantUsername = request.getApplicant() != null ? request.getApplicant() : "unknown";
            User user = userRepository.findByUsername(applicantUsername).orElse(null);
            if (user == null || !"VERIFIED".equals(user.getKycStatus())) {
                response.put("success", false);
                response.put("message", "必须先通过实名验证才能申请节点");
                return response;
            }
            // ================================================

            // 1. 参数校验
            if (request.getO() == null || request.getO().trim().isEmpty() ||
                request.getL() == null || request.getL().trim().isEmpty() ||
                request.getC() == null || request.getC().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "O、L、C 均不能为空");
                return response;
            }

            // 2. 检查已激活节点中是否已存在相同的 O
            String oPattern = "O=" + request.getO() + ",";
            boolean existsInNodes = cordaNodeRepository.findAll().stream()
                    .anyMatch(node -> node.getName().startsWith(oPattern));

            // 3. 检查申请表中是否存在相同 O 且状态为 PENDING 或 APPROVED 的申请
            boolean existsInApps = nodeApplicationRepository
                    .existsByOrganizationAndStatusIn(request.getO(), List.of("PENDING", "APPROVED"));

            if (existsInNodes || existsInApps) {
                response.put("success", false);
                response.put("message", "组织名称 (O) '" + request.getO() + "' 已存在或正在审批中");
                return response;
            }

            // 4. 保存申请
            NodeApplication app = new NodeApplication(request.getO(), request.getL(), request.getC(),
                    applicantUsername);
            nodeApplicationRepository.save(app);

            response.put("success", true);
            response.put("message", "申请提交成功，等待管理员审核");
            response.put("applicationId", app.getId());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "申请提交失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 管理员审批通过申请，并实际生成节点
     */
    @PostMapping("/approve/{id}")
    public Map<String, Object> approveApplication(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            NodeApplication app = nodeApplicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("申请记录不存在"));

            if (!"PENDING".equals(app.getStatus())) {
                response.put("success", false);
                response.put("message", "该申请已被处理，无法再次审批");
                return response;
            }

            // 组装完整 X.500 名称
            String fullNodeName = String.format("O=%s,L=%s,C=%s",
                    app.getOrganization(), app.getLocality(), app.getCountry());

            // 构造添加节点所需的请求对象（使用 NodeRequest 内部类）
            NodeRequest nodeRequest = new NodeRequest();
            nodeRequest.setNodeName(fullNodeName);
            nodeRequest.setAutoPorts(true);
            nodeRequest.setAutoDb(true);

            // 调用核心添加逻辑（doAddNode 内部已改为 List 参数）
            Map<String, Object> addResult = doAddNode(nodeRequest);

            if ((boolean) addResult.get("success")) {
                // 更新申请状态
                app.setStatus("APPROVED");
                app.setReviewTime(LocalDateTime.now());
                nodeApplicationRepository.save(app);
                response.putAll(addResult);
                response.put("message", "节点已批准并生成成功");
            } else {
                response.put("success", false);
                response.put("message", "节点生成失败: " + addResult.get("message"));
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "审批失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 获取待审批申请列表
     */
    @GetMapping("/applications/pending")
    public Map<String, Object> getPendingApplications() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<NodeApplication> pendingApps = nodeApplicationRepository.findAll().stream()
                    .filter(app -> "PENDING".equals(app.getStatus()))
                    .collect(Collectors.toList());
            response.put("success", true);
            response.put("data", pendingApps);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取待审核列表失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 新增接口：获取当前用户的所有申请记录（包括审批中和已通过）
     */
    @GetMapping("/applications/my")
    public Map<String, Object> getMyApplications(@RequestParam String username) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 直接根据申请人查询数据库
            List<NodeApplication> apps = nodeApplicationRepository.findByApplicant(username);
            response.put("success", true);
            response.put("data", apps);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取列表失败: " + e.getMessage());
        }
        return response;
    }

    // ==================== 辅助方法 ====================

    private int extractServerPort(String output) {
        Pattern pattern = Pattern.compile("服务器端口=(\\d+)");
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 50008; // 默认端口
    }

    // ==================== 内部 DTO 类 ====================

    public static class StartNodeRequest {
        private String nodeName;
        public String getNodeName() { return nodeName; }
        public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    }

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

    /**
     * 节点申请请求体
     */
    public static class NodeApplyRequest {
        private String o;   // Organization
        private String l;   // Locality
        private String c;   // Country
        private String applicant; // 可选，实际应从认证上下文获取

        public String getO() { return o; }
        public void setO(String o) { this.o = o; }
        public String getL() { return l; }
        public void setL(String l) { this.l = l; }
        public String getC() { return c; }
        public void setC(String c) { this.c = c; }
        public String getApplicant() { return applicant; }
        public void setApplicant(String applicant) { this.applicant = applicant; }
    }
}
