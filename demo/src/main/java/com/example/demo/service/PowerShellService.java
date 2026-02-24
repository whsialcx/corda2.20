package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PowerShellService {
    
    private static final Logger logger = LoggerFactory.getLogger(PowerShellService.class);
    
    @Value("${script.add-node-path:./add_node.ps1}")
    private String scriptPath;
    
    @Value("${corda.project.root:./../scripts}")
    private String cordaProjectRoot;
    
    // 判断操作系统
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
    
    // 获取PowerShell命令 - 跨平台支持
    private String getPowerShellCommand() {
        return isWindows() ? "powershell.exe" : "pwsh";
    }

    // 获取项目的绝对根路径
    private String getProjectRootPath() {
        try {
            String currentDir = System.getProperty("user.dir");
            logger.info("当前工作目录: {}", currentDir);
            
            File baseDir = new File(currentDir);
            File projectRoot = resolveRelativePath(baseDir, cordaProjectRoot);
            
            logger.info("解析后的Corda项目路径: {}", projectRoot.getAbsolutePath());
            logger.info("路径是否存在: {}", projectRoot.exists());
            
            return projectRoot.getAbsolutePath();
        } catch (Exception e) {
            logger.warn("获取项目根路径失败，使用配置路径: {}", cordaProjectRoot, e);
            return cordaProjectRoot;
        }
    }
    
    // 解析相对路径
    private File resolveRelativePath(File baseDir, String path) {
        if (path == null || path.trim().isEmpty()) {
            return baseDir;
        }
        
        if (path.startsWith(".")) {
            File resolved = new File(baseDir, path);
            if (!resolved.exists() && path.startsWith("./..")) {
                File parent = baseDir.getParentFile();
                if (parent != null) {
                    String remainingPath = path.substring(4);
                    return new File(parent, remainingPath);
                }
            }
            return resolved;
        } else {
            return new File(path);
        }
    }

    // 获取脚本的绝对路径
    private String getScriptAbsolutePath() {
        try {
            String projectRoot = getProjectRootPath();
            File projectRootDir = new File(projectRoot);
            
            if (scriptPath.startsWith(".")) {
                File scriptFile = new File(projectRootDir, scriptPath.substring(1));
                logger.info("脚本路径: {}", scriptFile.getAbsolutePath());
                logger.info("脚本是否存在: {}", scriptFile.exists());
                return scriptFile.getAbsolutePath();
            } else {
                logger.info("脚本路径: {}", scriptPath);
                return scriptPath;
            }
        } catch (Exception e) {
            logger.warn("获取脚本路径失败，使用配置路径: {}", scriptPath, e);
            return scriptPath;
        }
    }
    
    // 在项目根执行 gradlew clean deployNodes
    public ProcessResult executeGradleDeploy() {
        String projectRoot = getProjectRootPath();
        File projectRootDir = new File(projectRoot);
        
        logger.info("执行Gradle部署，项目目录: {}", projectRootDir.getAbsolutePath());
        logger.info("目录是否存在: {}", projectRootDir.exists());
        
        if (!projectRootDir.exists()) {
            logger.error("Corda 项目根目录不存在: {}", projectRoot);
            return new ProcessResult(-1, "", "Corda 项目根目录不存在: " + projectRoot, false);
        }

        try {
            logger.info("在 {} 执行: {} clean deployNodes", 
                projectRootDir.getAbsolutePath(), 
                isWindows() ? "gradlew.bat" : "./gradlew");

            ProcessBuilder pb = new ProcessBuilder();
            if (isWindows()) {
                pb.command("cmd.exe", "/c", "gradlew.bat", "clean", "deployNodes");
            } else {
                pb.command("./gradlew", "clean", "deployNodes");
            }
            pb.directory(projectRootDir);
            pb.redirectErrorStream(true);

            Process proc = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.info("[gradle] {}", line);
                }
            }

            boolean finished = proc.waitFor(10, TimeUnit.MINUTES);
            int exitCode = finished ? proc.exitValue() : -1;

            logger.info("gradlew 执行完成，退出码: {}", exitCode);
            return new ProcessResult(exitCode, output.toString(), "", finished);

        } catch (IOException | InterruptedException e) {
            logger.error("执行 gradlew 失败", e);
            Thread.currentThread().interrupt();
            return new ProcessResult(-1, "", "执行 gradlew 时发生错误: " + e.getMessage(), false);
        }
    }

    public List<String> getNodeNames() {
        List<String> nodes = new ArrayList<>();
        String projectRoot = getProjectRootPath();
        File projectRootDir = new File(projectRoot);
        File buildGradle = new File(projectRootDir, "build.gradle");
        
        logger.info("查找节点名称，build.gradle路径: {}", buildGradle.getAbsolutePath());
        logger.info("build.gradle是否存在: {}", buildGradle.exists());
        
        if (!buildGradle.exists()) {
            logger.warn("未找到 build.gradle 文件: {}", buildGradle.getAbsolutePath());
            return nodes;
        }
        
        try {
            String content = Files.readString(buildGradle.toPath(), StandardCharsets.UTF_8);
            Pattern p = Pattern.compile("name\\s+\"([^\"]+)\"");
            Matcher m = p.matcher(content);
            while (m.find()) {
                String nodeName = m.group(1);
                logger.info("找到节点: {}", nodeName);
                nodes.add(nodeName);
            }
        } catch (IOException e) {
            logger.error("读取 build.gradle 时出错", e);
        }
        return nodes;
    }
    
    /**
     * 执行 PowerShell 脚本，参数以 List<String> 形式传递（跨平台安全）
     */
    public ProcessResult executePowerShellScript(List<String> args) {
        try {
            // 确保工作目录存在
            String projectRoot = getProjectRootPath();
            File projectRootDir = new File(projectRoot);
            if (!projectRootDir.exists()) {
                logger.error("Corda 项目根目录不存在: {}", projectRoot);
                return new ProcessResult(-1, "", "Corda 项目根目录不存在: " + projectRoot, false);
            }
            
            // 获取脚本绝对路径
            String fullScriptPath = getScriptAbsolutePath();
            File scriptFile = new File(fullScriptPath);
            if (!scriptFile.exists()) {
                logger.error("PowerShell 脚本不存在: {}", fullScriptPath);
                return new ProcessResult(-1, "", "脚本文件不存在: " + fullScriptPath, false);
            }
            
            // 构建命令列表
            List<String> command = new ArrayList<>();
            String powerShellCmd = getPowerShellCommand();
            command.add(powerShellCmd);
            
            if (isWindows()) {
                command.add("-ExecutionPolicy");
                command.add("Bypass");
            }
            command.add("-File");
            command.add(fullScriptPath);
            
            // 添加传入的参数
            if (args != null) {
                command.addAll(args);
            }
            
            logger.info("执行命令: {}", String.join(" ", command));
            logger.info("工作目录: {}", projectRootDir.getAbsolutePath());
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(projectRootDir);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.info("PowerShell 输出: {}", line);
                }
            }
            
            // 等待完成（超时5分钟）
            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            int exitCode = finished ? process.exitValue() : -1;
            
            logger.info("PowerShell 脚本执行完成，退出码: {}", exitCode);
            
            return new ProcessResult(
                exitCode,
                output.toString(),
                "",  // 错误已合并到输出流
                finished && exitCode == 0
            );
            
        } catch (IOException | InterruptedException e) {
            logger.error("执行 PowerShell 脚本失败", e);
            Thread.currentThread().interrupt();
            return new ProcessResult(-1, "", "执行脚本时发生错误: " + e.getMessage(), false);
        }
    }
    
    // 验证配置
    public boolean validateCordaProject() {
        String projectRoot = getProjectRootPath();
        File projectRootDir = new File(projectRoot);
        
        logger.info("验证Corda项目，路径: {}", projectRoot);
        logger.info("目录是否存在: {}", projectRootDir.exists());
        
        if (!projectRootDir.exists()) {
            logger.error("Corda 项目根目录不存在: {}", projectRoot);
            return false;
        }
        
        File buildGradle = new File(projectRootDir, "build.gradle");
        logger.info("build.gradle路径: {}", buildGradle.getAbsolutePath());
        logger.info("build.gradle是否存在: {}", buildGradle.exists());
        
        if (!buildGradle.exists()) {
            logger.error("在 Corda 项目根目录中找不到 build.gradle 文件: {}", projectRoot);
            return false;
        }
        
        String scriptPath = getScriptAbsolutePath();
        File scriptFile = new File(scriptPath);
        logger.info("脚本路径: {}", scriptPath);
        logger.info("脚本是否存在: {}", scriptFile.exists());
        
        if (!scriptFile.exists()) {
            logger.error("PowerShell 脚本不存在: {}", scriptFile.getAbsolutePath());
            return false;
        }
        
        logger.info("Corda 项目验证成功: {}", projectRoot);
        return true;
    }
    
    // 获取 Corda 项目信息
    public CordaProjectInfo getCordaProjectInfo() {
        String projectRoot = getProjectRootPath();
        File projectRootDir = new File(projectRoot);
        File buildGradle = new File(projectRootDir, "build.gradle");
        File scriptFile = new File(getScriptAbsolutePath());
        
        return new CordaProjectInfo(
            projectRoot,
            projectRootDir.exists(),
            buildGradle.exists(),
            scriptFile.exists(),
            scriptFile.getAbsolutePath()
        );
    }
    
    // 启动所有节点 - Ubuntu 版本
    public ProcessResult executeRunnodesScript() {
        String projectRoot = getProjectRootPath();
        File projectRootDir = new File(projectRoot);
        
        if (!projectRootDir.exists()) {
            logger.error("Corda 项目根目录不存在: {}", projectRoot);
            return new ProcessResult(-1, "", "Corda 项目根目录不存在: " + projectRoot, false);
        }

        File runnodesScript = new File(projectRootDir, "build/nodes/runnodes");
        if (!runnodesScript.exists()) {
            runnodesScript = new File(projectRootDir, "build/nodes/runnodes.bat");
            if (!runnodesScript.exists()) {
                logger.error("runnodes 脚本不存在: {}", runnodesScript.getAbsolutePath());
                return new ProcessResult(-1, "", "runnodes 脚本不存在: " + runnodesScript.getAbsolutePath(), false);
            }
        }
        
        try {
            logger.info("执行 runnodes 脚本: {}", runnodesScript.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder();
            if (isWindows()) {
                pb.command("cmd.exe", "/c", "runnodes.bat");
            } else {
                pb.command("bash", runnodesScript.getAbsolutePath());
            }
            pb.directory(new File(projectRootDir, "build/nodes"));
            pb.redirectErrorStream(true);

            Process proc = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.info("[runnodes] {}", line);
                }
            }
            
            boolean finished = proc.waitFor(10, TimeUnit.SECONDS);
            int exitCode = finished ? proc.exitValue() : 0;
            
            logger.info("runnodes 脚本执行完成，退出码: {}", exitCode);
            return new ProcessResult(exitCode, output.toString(), "", finished);
        } catch (IOException | InterruptedException e) {
            logger.error("执行 runnodes 脚本失败", e);
            Thread.currentThread().interrupt();
            return new ProcessResult(-1, "", "执行 runnodes 脚本时发生错误: " + e.getMessage(), false);
        }
    }

    // 启动指定节点 - Ubuntu 版本
    public ProcessResult startNode(String nodeName) {
        String projectRoot = getProjectRootPath();
        File projectRootDir = new File(projectRoot);
        
        if (!projectRootDir.exists()) {
            logger.error("Corda 项目根目录不存在: {}", projectRoot);
            return new ProcessResult(-1, "", "Corda 项目根目录不存在: " + projectRoot, false);
        }

        File nodeDir = new File(projectRootDir, "build/nodes/" + nodeName);
        if (!nodeDir.exists()) {
            logger.error("节点目录不存在: {}", nodeDir.getAbsolutePath());
            return new ProcessResult(-1, "", "节点目录不存在: " + nodeDir.getAbsolutePath(), false);
        }

        File startScript = new File(nodeDir, "startNode");
        if (!startScript.exists()) {
            startScript = new File(nodeDir, "startNode.bat");
        }
        
        try {
            logger.info("启动节点 {} 使用脚本: {}", nodeName, startScript.getAbsolutePath());
            
            ProcessBuilder pb = new ProcessBuilder();
            if (isWindows()) {
                pb.command("cmd.exe", "/c", "startNode.bat");
            } else {
                pb.command("bash", startScript.getAbsolutePath());
            }
            pb.directory(nodeDir);
            pb.redirectErrorStream(true);
            
            Process proc = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.info("[node-{}] {}", nodeName, line);
                }
            }
            
            boolean finished = proc.waitFor(5, TimeUnit.SECONDS);
            int exitCode = finished ? proc.exitValue() : 0;
            
            logger.info("节点 {} 启动完成，退出码: {}", nodeName, exitCode);
            return new ProcessResult(exitCode, output.toString(), "", finished);
        } catch (IOException | InterruptedException e) {
            logger.error("启动节点失败", e);
            Thread.currentThread().interrupt();
            return new ProcessResult(-1, "", "启动节点时发生错误: " + e.getMessage(), false);
        }
    }
    
    // 停止指定节点 - Ubuntu 版本
    public ProcessResult stopNode(String nodeName) {
        String projectRoot = getProjectRootPath();
        File projectRootDir = new File(projectRoot);
        
        if (!projectRootDir.exists()) {
            logger.error("Corda 项目根目录不存在: {}", projectRoot);
            return new ProcessResult(-1, "", "Corda 项目根目录不存在: " + projectRoot, false);
        }

        File nodeDir = new File(projectRootDir, "build/nodes/" + nodeName);
        if (!nodeDir.exists()) {
            logger.error("节点目录不存在: {}", nodeDir.getAbsolutePath());
            return new ProcessResult(-1, "", "节点目录不存在: " + nodeDir.getAbsolutePath(), false);
        }

        try {
            if (isWindows()) {
                // Windows 版本
                String nodePathPattern = nodeDir.getAbsolutePath().replace("\\", "\\\\");
                String psCmd = "Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -and $_.CommandLine -match '" +
                                nodePathPattern +
                                "' } | Select-Object -ExpandProperty ProcessId | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue; Write-Output (\"Killed process $_\") }";
                ProcessBuilder pb = new ProcessBuilder();
                pb.command("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", psCmd);
                pb.directory(projectRootDir);
                pb.redirectErrorStream(true);
                
                Process proc = pb.start();
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        logger.info("[stop-node] {}", line);
                    }
                }
                
                boolean finished = proc.waitFor(2, TimeUnit.MINUTES);
                int exitCode = finished ? proc.exitValue() : -1;
                
                if (exitCode == 0) {
                    logger.info("停止节点 {} 操作完成，exitCode={}", nodeName, exitCode);
                    return new ProcessResult(0, output.toString(), "", true);
                } else {
                    logger.warn("停止节点 {} 操作返回非 0 退出码: {}", nodeName, exitCode);
                    return new ProcessResult(exitCode, output.toString(), "停止节点时 exitCode != 0", false);
                }
            } else {
                // Ubuntu 版本 - 使用 pkill 命令
                String nodePath = nodeDir.getAbsolutePath();
                String[] cmd = {"bash", "-c", 
                    "ps aux | grep -i corda | grep \"" + nodePath + "\" | grep -v grep | awk '{print $2}' | xargs kill -9"};
                
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(projectRootDir);
                pb.redirectErrorStream(true);
                
                Process proc = pb.start();
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        logger.info("[stop-node] {}", line);
                    }
                }
                
                boolean finished = proc.waitFor(30, TimeUnit.SECONDS);
                int exitCode = finished ? proc.exitValue() : 0;
                
                logger.info("停止节点 {} 完成，退出码: {}", nodeName, exitCode);
                return new ProcessResult(exitCode, output.toString(), "", true);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("停止节点失败", e);
            Thread.currentThread().interrupt();
            return new ProcessResult(-1, "", "停止节点时发生错误: " + e.getMessage(), false);
        }
    }

    // 内部类
    public static class ProcessResult {
        private final int exitCode;
        private final String output;
        private final String error;
        private final boolean success;
        
        public ProcessResult(int exitCode, String output, String error, boolean success) {
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
            this.success = success;
        }
        
        public int getExitCode() { return exitCode; }
        public String getOutput() { return output; }
        public String getError() { return error; }
        public boolean isSuccess() { return success; }
    }
    
    public static class CordaProjectInfo {
        private final String projectRoot;
        private final boolean rootExists;
        private final boolean buildGradleExists;
        private final boolean scriptExists;
        private final String scriptPath;
        
        public CordaProjectInfo(String projectRoot, boolean rootExists, boolean buildGradleExists, 
                               boolean scriptExists, String scriptPath) {
            this.projectRoot = projectRoot;
            this.rootExists = rootExists;
            this.buildGradleExists = buildGradleExists;
            this.scriptExists = scriptExists;
            this.scriptPath = scriptPath;
        }
        
        public String getProjectRoot() { return projectRoot; }
        public boolean isRootExists() { return rootExists; }
        public boolean isBuildGradleExists() { return buildGradleExists; }
        public boolean isScriptExists() { return scriptExists; }
        public String getScriptPath() { return scriptPath; }
    }
}