#!/usr/bin/env pwsh
[CmdletBinding(DefaultParameterSetName="Help")]
param(
    [Parameter(Mandatory=$true, ParameterSetName="AddNode")]
    [string]$NodeName,

    [Parameter(Mandatory=$true, ParameterSetName="RemoveNode")]
    [string]$RemoveNode,

    [Parameter(ParameterSetName="AddNode")]
    [int]$P2PPort,

    [Parameter(ParameterSetName="AddNode")]
    [int]$RPCPort,

    [Parameter(ParameterSetName="AddNode")]
    [int]$AdminPort,

    [Parameter(ParameterSetName="AddNode")]
    [string]$DbName,

    [Parameter(ParameterSetName="AddNode")]
    [string]$DbUser,

    [Parameter(ParameterSetName="AddNode")]
    [switch]$AutoPorts,

    [Parameter(ParameterSetName="AddNode")]
    [switch]$AutoDb,

    # 独立的帮助参数集
    [Parameter(ParameterSetName="Help")]
    [switch]$Help
)

# --- 颜色定义 ---
$ESC = [char]27
$RED = "$ESC[91m"
$GREEN = "$ESC[92m"
$YELLOW = "$ESC[93m"
$BLUE = "$ESC[94m"
$NC = "$ESC[0m"
# --- 日志函数 ---
function Write-Info { param([string]$message) Write-Host "${BLUE}[INFO]${NC} $message" }
function Write-Success { param([string]$message) Write-Host "${GREEN}[SUCCESS]${NC} $message" }
function Write-Warning { param([string]$message) Write-Host "${YELLOW}[WARNING]${NC} $message" }
function Write-Error { param([string]$message) Write-Host "${RED}[ERROR]${NC} $message" }

function Show-Usage {
    Write-Host "用法: ./add_node.ps1 [选项]"
    Write-Host ""
    Write-Host "添加节点模式:"
    Write-Host "  -NodeName      (必需) 节点名称 (例如: 'O=PartyE,L=Tokyo,C=JP')"
    Write-Host "  -P2PPort       P2P端口号 (例如: 10012)"
    Write-Host "  -RPCPort       RPC端口号 (例如: 10013)"
    Write-Host "  -AdminPort     Admin端口号 (例如: 10053)"
    Write-Host "  -DbName        数据库名称 (例如: corda_party_e)"
    Write-Host "  -DbUser        数据库用户 (例如: user_e)"
    Write-Host "  -AutoPorts     自动分配未被占用的端口号"
    Write-Host "  -AutoDb        根据节点名称自动生成数据库配置"
    Write-Host ""
    Write-Host "删除节点模式:"
    Write-Host "  -RemoveNode    (必需) 要删除的节点名称"
    Write-Host ""
    Write-Host "通用选项:"
    Write-Host "  -Help          显示此帮助信息"
    Write-Host ""
    Write-Host "示例:"
    Write-Host "  # 添加节点 - 手动指定所有参数"
    Write-Host "  ./add_node.ps1 -NodeName 'O=PartyE,L=Tokyo,C=JP' -P2PPort 10012 -RPCPort 10013 -AdminPort 10053 -DbName corda_party_e -DbUser user_e"
    Write-Host ""
    Write-Host "  # 添加节点 - 自动分配端口和数据库配置"
    Write-Host "  ./add_node.ps1 -NodeName 'O=PartyE,L=Tokyo,C=JP' -AutoPorts -AutoDb"
    Write-Host ""
    Write-Host "  # 删除节点"
    Write-Host "  ./add_node.ps1 -RemoveNode 'O=PartyE,L=Tokyo,C=JP'"
    Write-Host ""
    Write-Host "快速参考:" -ForegroundColor Green
    Write-Host "  # 快速添加节点"
    Write-Host "  ./add_node.ps1 -NodeName 'O=PartyE,L=Tokyo,C=JP' -AutoPorts -AutoDb"
    Write-Host ""
    Write-Host "  # 快速删除节点"
    Write-Host "  ./add_node.ps1 -RemoveNode 'O=PartyE,L=Tokyo,C=JP'"
    Write-Host ""
}

#节点检测函数
function Test-NodeExists 
{
    param([string]$NodeName)
    
    Write-Info "检查节点 '$NodeName' 是否已存在..."
    
    $buildGradleFile = "build.gradle"
    if (-not (Test-Path $buildGradleFile)) 
    {
        Write-Warning "找不到 $buildGradleFile 文件，跳过节点存在性检查"
        return $false
    }
    
    $content = Get-Content $buildGradleFile -Raw -Encoding UTF8
    # 匹配所有节点的name
    $pattern = 'name\s+"([^"]+)"'
    $matches = [regex]::Matches($content, $pattern)
    
    $existingNodes = @()
    foreach ($match in $matches) 
    {
        $existingNodeName = $match.Groups[1].Value
        $existingNodes += $existingNodeName
        if ($existingNodeName -eq $NodeName) 
        {
            Write-Error "节点 '$NodeName' 已存在于 build.gradle 中！"
            return $true
        }
    }
    Write-Info "现有节点列表: $($existingNodes -join ', ')"
    return $false
}

# 检查要删除的节点是否存在
function Test-NodeToRemoveExists 
{
    param([string]$NodeName)
    
    Write-Info "检查要删除的节点 '$NodeName' 是否存在..."
    
    $buildGradleFile = "build.gradle"
    if (-not (Test-Path $buildGradleFile)) 
    {
        Write-Error "找不到 $buildGradleFile 文件"
        return $false
    }
    $content = Get-Content $buildGradleFile -Raw -Encoding UTF8
    $pattern = 'name\s+"([^"]+)"'
    $matches = [regex]::Matches($content, $pattern)
    $existingNodes = @()
    foreach ($match in $matches) 
    {
        $existingNodeName = $match.Groups[1].Value
        $existingNodes += $existingNodeName
        if ($existingNodeName -eq $NodeName) 
        {
            Write-Info "找到节点 '$NodeName'，可以删除"
            return $true
        }
    }
    Write-Error "节点 '$NodeName' 不存在于 build.gradle 中！"
    Write-Info "现有节点列表: $($existingNodes -join ', ')"
    return $false
}

#端口检测函数 - Ubuntu兼容版本
function Test-PortInUse 
{
    param([int]$Port)
    try 
    {
        # 使用 netstat 命令检查端口占用
        $result = netstat -tuln 2>$null | Select-String ":$Port\s"
        if ($result) {
            return $true
        }
        
        # 使用 lsof 命令作为备选检查
        $result = lsof -i:$Port 2>$null
        if ($result) {
            return $true
        }
        
        return $false
    } 
    catch 
    {
        return $false
    }
}

# 获取下一个可用端口函数
function Get-NextAvailablePort 
{
    param([int]$StartPort)
    $port = $StartPort
    while (Test-PortInUse -Port $port) 
    {
        Write-Info "端口 $port 已被占用，尝试下一个..."
        $port++
        if ($port -gt 65535) { throw "在起始端口 $StartPort 之后找不到可用端口" }
    }
    return $port
}

function Auto-AssignPorts 
{
    Write-Info "正在自动分配端口..."
    
    $content = Get-Content "build.gradle" -Raw -Encoding UTF8
    
    # 提取所有已配置的端口（关键修复：强制使用 @() 包裹结果，确保即使为空或只有1个结果，也绝对是数组）
    $p2pMatches = [regex]::Matches($content, 'p2p(?:Port\s+|Address\s+".*?:)(\d+)"?')
    $allP2pPorts = @($p2pMatches | ForEach-Object { [int]$_.Groups[1].Value })
    
    $rpcMatches = [regex]::Matches($content, 'address\("172\.18\.44\.66:(\d+)"\)')
    $allRpcPorts = @($rpcMatches | ForEach-Object { [int]$_.Groups[1].Value })
    
    $adminMatches = [regex]::Matches($content, 'adminAddress\("172\.18\.44\.66:(\d+)"\)')
    $allAdminPorts = @($adminMatches | ForEach-Object { [int]$_.Groups[1].Value })
    
    # 所有已使用的端口（前面加上 @() 确保它是以数组拼接的模式运行，而不是数学相加）
    $allUsedPorts = @() + $allP2pPorts + $allRpcPorts + $allAdminPorts
    Write-Info "现有配置中的端口: $($allUsedPorts -join ', ')"
    
    function Get-TrulyAvailablePort 
    {
        param([int]$StartPort, [array]$ExcludePorts)
        $port = $StartPort
        while ($port -in $ExcludePorts -or (Test-PortInUse -Port $port)) 
        {
            Write-Info "端口 $port 已被占用或已配置，尝试下一个..."
            $port++
            if ($port -gt 65535) { throw "在起始端口 $StartPort 之后找不到可用端口" }
        }
        return $port
    }
    
    # 关键修复：强制将提取的最后端口转为 [int] 整数型，确保后面的 + 1 是数学加法，而不是数组追加
    [int]$lastP2PPort = if ($allP2pPorts.Count -gt 0) { $allP2pPorts | Sort-Object -Descending | Select-Object -First 1 } else { 10000 }
    [int]$lastRPCPort = if ($allRpcPorts.Count -gt 0) { $allRpcPorts | Sort-Object -Descending | Select-Object -First 1 } else { 10000 }
    [int]$lastAdminPort = if ($allAdminPorts.Count -gt 0) { $allAdminPorts | Sort-Object -Descending | Select-Object -First 1 } else { 7000 }
    
    $script:P2PPort = Get-TrulyAvailablePort -StartPort ($lastP2PPort + 1) -ExcludePorts $allUsedPorts
    $script:RPCPort = Get-TrulyAvailablePort -StartPort ([Math]::Max($lastRPCPort + 1, $script:P2PPort + 1)) -ExcludePorts $allUsedPorts
    $script:AdminPort = Get-TrulyAvailablePort -StartPort ([Math]::Max($lastAdminPort + 1, $script:RPCPort + 40)) -ExcludePorts $allUsedPorts
    
    Write-Success "自动分配的端口: P2P=$P2PPort, RPC=$RPCPort, Admin=$AdminPort"
}
# 自动生成数据库配置
function Auto-GenerateDbConfig 
{
    Write-Info "正在自动生成数据库配置..."
    if ($NodeName -match 'O=Party([A-Z])') 
    {
        $nodeLetter = $Matches[1].ToLower()
    } elseif ($NodeName -match 'O=([^,]+)') 
    {
        $nodeLetter = ($Matches[1] -replace '^Party', '').ToLower()
    } 
    else 
    {
        throw "无法从节点名称 '$NodeName' 中提取组织名称"
    }
    $script:DbName = "corda_party_$nodeLetter"
    $script:DbUser = "user_$nodeLetter"
    Write-Success "自动生成的数据库配置: DB=$DbName, User=$DbUser"
}

# 添加
function Validate-Input-Add 
{
    Write-Info "验证输入参数（添加节点模式）..."

    if (Test-NodeExists -NodeName $NodeName) 
    {
        Write-Error "无法添加节点：节点 '$NodeName' 已存在！请使用不同的节点名称。"
        exit 1
    }
    
    if ($AutoPorts) 
    {
        Auto-AssignPorts
    } 
    else 
    {
        if (-not $P2PPort -or -not $RPCPort -or -not $AdminPort) 
        {
            Write-Error "必须手动提供 P2PPort, RPCPort, AdminPort, 或使用 -AutoPorts 标志。"
            Show-Usage
            exit 1
        }
    }
    if ($AutoDb) 
    {
        Auto-GenerateDbConfig
    } 
    else 
    {
        if (-not $DbName -or -not $DbUser) 
        {
            Write-Error "必须手动提供 DbName 和 DbUser, 或使用 -AutoDb 标志。"
            Show-Usage
            exit 1
        }
    }
}

# 删除
function Validate-Input-Remove 
{
    Write-Info "验证输入参数（删除节点模式）..."

    if (-not (Test-NodeToRemoveExists -NodeName $RemoveNode)) 
    {
        Write-Error "无法删除节点：节点 '$RemoveNode' 不存在！"
        exit 1
    }
}

function Write-Utf8File 
{
    param(
        [string]$Path,
        [string]$Content
    )
    # 使用 .NET 方法写入无 BOM 的 UTF-8 文件
    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($Path, $Content, $utf8NoBom)
}

function Modify-BuildGradle 
{
    Write-Info "正在修改 build.gradle 文件..."
    $buildGradleFile = "build.gradle"
    if (-not (Test-Path $buildGradleFile)) 
    {
        Write-Error "找不到 $buildGradleFile 文件"
        exit 1
    }
    # 备份
    $backupFile = "build.gradle.backup.$(Get-Date -Format 'yyyyMMddHHmmss')"
    Copy-Item $buildGradleFile $backupFile
    Write-Info "已备份 $buildGradleFile 到 $backupFile"
    # 创建新节点配置的文本块
    $newNodeConfig = @" 
    node {
        name "$NodeName"
        p2pAddress "172.18.44.66:$P2PPort"
        rpcSettings {
            address("172.18.44.66:$RPCPort")
            adminAddress("172.18.44.66:$AdminPort")
        }
        rpcUsers = [[ user: "user1", "password": "test", "permissions": ["ALL"]]]
        extraConfig = [
             dataSourceProperties: [
                 dataSourceClassName: "org.postgresql.ds.PGSimpleDataSource",
                 "dataSource.url": "jdbc:postgresql://172.18.44.66:5432/$DbName",
                 "dataSource.user": "$DbUser",
                 "dataSource.password": "123456"
             ]
        ]
    }
"@

    $content = Get-Content $buildGradleFile -Raw -Encoding UTF8

    # 匹配最后一个节点块，然后在之后插入新节点
    $pattern = '(?s)(.*)(\n    node \{.*?\n    \})(.*)'

    if ($content -match $pattern) 
    {
        $before = $matches[1]
        $lastNode = $matches[2]
        $after = $matches[3]

        # 在最后一个节点之后插入新节点
        $newContent = $before + $lastNode + "`n" + $newNodeConfig + $after
        Write-Utf8File -Path $buildGradleFile -Content $newContent
        Write-Success "已成功将节点 '$NodeName' 添加到 $buildGradleFile"
    } 
    else 
    {
        Write-Error "无法在 $buildGradleFile 中找到合适的插入位置。请手动添加节点配置。"
        Write-Host "需要手动添加的配置内容:"
        Write-Host $newNodeConfig
        exit 1
    }
}


# 更新 clients/build.gradle 文件，添加新的服务器启动任务
function Update-ClientsBuildGradle {
    Write-Info "正在更新 clients/build.gradle 文件..."
    
    $clientsBuildFile = "clients/build.gradle"
    if (-not (Test-Path $clientsBuildFile)) {
        Write-Error "找不到 $clientsBuildFile 文件"
        return
    }
    
    # 备份
    $backupFile = "clients/build.gradle.backup.$(Get-Date -Format 'yyyyMMddHHmmss')"
    Copy-Item $clientsBuildFile $backupFile
    Write-Info "已备份 $clientsBuildFile 到 $backupFile"
    
    $content = Get-Content $clientsBuildFile -Raw -Encoding UTF8
    
    # 从节点名称中提取组织名
    if ($NodeName -match 'O=([^,]+)') {
        $partyName = $Matches[1]
    } else {
        Write-Warning "无法从节点名称中提取组织名，使用默认格式"
        $partyName = "Party$($NodeName -replace '[^a-zA-Z0-9]', '')"
    }
    
    # 构建新的任务
    # 分配新的服务器端口（从 50008 开始递增）
    $existingServerPorts = @()
    if ($content -match '--server\.port=(\d+)') {
        $existingServerPorts = [regex]::Matches($content, '--server\.port=(\d+)') | ForEach-Object { [int]$_.Groups[1].Value }
    }
    
    # 查找下一个可用的服务器端口
    $serverPort = 50008
    while ($serverPort -in $existingServerPorts) {
        $serverPort++
    }
    
    # 构建任务名称（移除特殊字符）
    $taskName = "run$($partyName -replace '[^a-zA-Z0-9]', '')Server"
    
    # 检查任务是否已存在
    if ($content -match "task $taskName\(") {
        Write-Info "任务 $taskName 已存在，跳过创建"
        return
    }
    
    $newTask = @"
    
task $taskName(type: JavaExec, dependsOn: assemble) {
    classpath = sourceSets.main.runtimeClasspath
    main = 'net.corda.samples.example.webserver.Starter'
    args '--server.port=$serverPort', '--config.rpc.host=172.18.44.66', '--config.rpc.port=$RPCPort', '--config.rpc.username=user1', '--config.rpc.password=test'
}
"@
    
    # 找到最后一个任务的位置，在其后插入新任务
    $pattern = '(?s)(task runParty[A-Z]Server\(type: JavaExec, dependsOn: assemble\) \{.*?\n\})'
    
    if ($content -match $pattern) {
        $lastTask = $matches[0]
        $newContent = $content -replace [regex]::Escape($lastTask), "$lastTask$newTask"
        Write-Utf8File -Path $clientsBuildFile -Content $newContent
        Write-Success "已成功将任务 $taskName 添加到 $clientsBuildFile"
        Write-Info "新节点服务器配置: 服务器端口=$serverPort, RPC端口=$RPCPort"
    } else {
        Write-Warning "无法找到现有任务模式，将新任务添加到文件末尾"
        $newContent = $content.Trim() + $newTask
        Write-Utf8File -Path $clientsBuildFile -Content $newContent
        Write-Success "已成功将任务 $taskName 添加到 $clientsBuildFile"
    }
}

# 从 clients/build.gradle 中删除服务器启动任务
function Remove-TaskFromClientsBuildGradle {
    Write-Info "正在从 clients/build.gradle 中删除节点 '$RemoveNode' 的服务器任务..."
    
    $clientsBuildFile = "clients/build.gradle"
    if (-not (Test-Path $clientsBuildFile)) {
        Write-Warning "找不到 $clientsBuildFile 文件，跳过此步骤"
        return
    }
    
    # 从节点名称中提取组织名
    if ($RemoveNode -match 'O=([^,]+)') {
        $partyName = $Matches[1]
    } else {
        Write-Warning "无法从节点名称中提取组织名"
        $partyName = "Party$($RemoveNode -replace '[^a-zA-Z0-9]', '')"
    }
    
    # 构建任务名称（移除特殊字符）
    $taskName = "run$($partyName -replace '[^a-zA-Z0-9]', '')Server"
    
    $content = Get-Content $clientsBuildFile -Raw -Encoding UTF8
    
    # 查找并删除任务
    $pattern = "(?s)task $taskName\(type: JavaExec, dependsOn: assemble\) \{.*?\n\}"
    
    if ($content -match $pattern) {
        $matchedTask = $matches[0]
        $newContent = $content -replace [regex]::Escape($matchedTask), ""
        # 清理多余的空行
        $newContent = $newContent -replace "(`r`n){3,}", "`r`n`r`n"
        Write-Utf8File -Path $clientsBuildFile -Content $newContent
        Write-Success "已成功从 $clientsBuildFile 中删除任务 $taskName"
    } else {
        Write-Info "任务 $taskName 未找到，无需删除"
    }
}

function Remove-NodeFromBuildGradle 
{
    Write-Info "正在从 build.gradle 文件中删除节点 '$RemoveNode'..."
    $buildGradleFile = "build.gradle"
    if (-not (Test-Path $buildGradleFile)) 
    {
        Write-Error "找不到 $buildGradleFile 文件"
        exit 1
    }
    # 备份
    $backupFile = "build.gradle.backup.$(Get-Date -Format 'yyyyMMddHHmmss')"
    Copy-Item $buildGradleFile $backupFile
    Write-Info "已备份 $buildGradleFile 到 $backupFile"
    $content = Get-Content $buildGradleFile -Raw -Encoding UTF8
    Write-Info "使用方法1: 正则表达式匹配删除节点块..."
    $pattern = "(?s)(\s+node\s*\{[^{}]*?name\s+`"$([regex]::Escape($RemoveNode))`"[^{}]*(?:\{[^{}]*\}[^{}]*)*\s*\})"
    Write-Info "使用正则表达式模式搜索节点块..."
    if ($content -match $pattern)
    {
        $matchedContent = $matches[1]
        Write-Info "找到匹配的节点块，内容长度: $($matchedContent.Length)"
        Write-Info "匹配内容: $matchedContent"
        $newContent = $content -replace [regex]::Escape($matchedContent), ""
        $newContent = $newContent -replace "(`r`n){3,}", "`r`n`r`n"
        Write-Utf8File -Path $buildGradleFile -Content $newContent
        Write-Success "已成功从 $buildGradleFile 中删除节点 '$RemoveNode'"
        # 验证删除结果
        $updatedContent = Get-Content $buildGradleFile -Raw -Encoding UTF8
        $remainingNodes = [regex]::Matches($updatedContent, 'name\s+"([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
        Write-Info "删除后剩余节点: $($remainingNodes -join ', ')"
        return
    }
    Write-Info "正则表达式匹配失败，使用方法2: 逐行处理删除节点..."
    $lines = Get-Content $buildGradleFile -Encoding UTF8
    $newLines = @()
    $inTargetNode = $false
    $braceCount = 0
    $nodeStarted = $false
    for ($i = 0; $i -lt $lines.Count; $i++) 
    {
        $line = $lines[$i]
        # 检查是否进入目标节点块
        if (-not $inTargetNode -and $line -match "name\s+`"$([regex]::Escape($RemoveNode))`"") 
        {
            Write-Info "找到目标节点名称行: $line"
            $inTargetNode = $true
            $nodeStarted = $true
            for ($j = $i - 1; $j -ge 0; $j--) 
            {
                if ($lines[$j] -match '^\s*node\s*\{') 
                {
                    Write-Info "回溯找到节点块开始于行: $j - $($lines[$j])"
                    # 从节点开始行重新开始处理
                    $newLines = $newLines[0..($j-1)]
                    $i = $j - 1  # 从节点开始行重新开始
                    $braceCount = 0
                    break
                }
            }
            continue
        }
        
        if ($inTargetNode) 
        {
            $openBraces = ($line -split '\{' | Measure-Object).Count - 1
            $closeBraces = ($line -split '\}' | Measure-Object).Count - 1
            $braceCount += $openBraces - $closeBraces
            Write-Info "在节点块中: 行 $i - $line (braceCount: $braceCount)"
            if ($braceCount -le 0 -and $nodeStarted) 
            {
                Write-Info "节点块结束于行: $i"
                $inTargetNode = $false
                $nodeStarted = $false
            }
        } 
        else 
        {
            $newLines += $line
        }
    }

    # 检查是否找到了目标节点
    if (-not $nodeStarted) 
    {
        Write-Error "无法在 $buildGradleFile 中找到节点 '$RemoveNode' 的配置。"
        Write-Info "现有节点列表:"
        $existingNodes = [regex]::Matches($content, 'name\s+"([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
        Write-Info ($existingNodes -join ", ")
        exit 1
    }

    # 写入更新后的内容
    Write-Utf8File -Path $buildGradleFile -Content ($newLines -join "`n")
    Write-Success "已成功从 $buildGradleFile 中删除节点 '$RemoveNode'（方法2）"
    
    # 验证删除结果
    $updatedContent = Get-Content $buildGradleFile -Raw -Encoding UTF8
    $remainingNodes = [regex]::Matches($updatedContent, 'name\s+"([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
    Write-Info "删除后剩余节点: $($remainingNodes -join ', ')"
}

# 修改 setup_corda_db.sql 文件
function Modify-SetupDbSql 
{
    Write-Info "正在修改 setup_corda_db.sql 文件..."
    $sqlFile = "setup_corda_db.sql"
    if (-not (Test-Path $sqlFile)) 
    { 
        Write-Warning "找不到 $sqlFile 文件，将跳过此步骤。"
        return 
    }
    
    # 备份
    $backupFile = "setup_corda_db.sql.backup.$(Get-Date -Format 'yyyyMMddHHmmss')"
    Copy-Item $sqlFile $backupFile
    Write-Info "已备份 $sqlFile 到 $backupFile"
    
    # 创建新的SQL语句
    $newSqlBlock = @"

-- 配置为节点 '$NodeName' 自动添加
SELECT 'CREATE DATABASE $DbName' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname='$DbName')\gexec
SELECT 'CREATE USER $DbUser WITH PASSWORD ''123456''' WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname='$DbUser')\gexec
GRANT ALL PRIVILEGES ON DATABASE $DbName TO $DbUser;
\c $DbName
DO `$`$ BEGIN IF NOT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name='public' AND schema_owner='$DbUser') THEN
    GRANT CREATE ON SCHEMA public TO $DbUser; ALTER SCHEMA public OWNER TO $DbUser;
END IF; END `$`$;
"@
    
    # 在文件末尾添加
    Add-Content -Path $sqlFile -Value $newSqlBlock -Encoding UTF8
    Write-Success "已更新 $sqlFile"
}

# 从 setup_corda_db.sql 文件中删除节点相关的数据库配置 - 修复版本
function Remove-NodeFromSetupDbSql 
{
    Write-Info "正在从 setup_corda_db.sql 文件中删除节点 '$RemoveNode' 的数据库配置..."
    $sqlFile = "setup_corda_db.sql"
    if (-not (Test-Path $sqlFile)) 
    { 
        Write-Warning "找不到 $sqlFile 文件，将跳过此步骤。"
        return 
    }
    # 备份
    $backupFile = "setup_corda_db.sql.backup.$(Get-Date -Format 'yyyyMMddHHmmss')"
    Copy-Item $sqlFile $backupFile
    Write-Info "已备份 $sqlFile 到 $backupFile"
    $content = Get-Content $sqlFile -Raw -Encoding UTF8
    # 从节点名称中提取数据库名和用户名
    if ($RemoveNode -match 'O=Party([A-Z])') 
    {
        $nodeLetter = $Matches[1].ToLower()
        $dbName = "corda_party_$nodeLetter"
        $dbUser = "user_$nodeLetter"
        Write-Info "从节点名称提取的数据库配置: DB=$dbName, User=$dbUser"
    } 
    else 
    {
        Write-Warning "无法从节点名称 '$RemoveNode' 中提取数据库配置，将尝试使用备用方法。"
        $dbName = ""
        $dbUser = ""
    }
    $commentPattern = "-- 配置为节点 '$([regex]::Escape($RemoveNode))' 自动添加"
    Write-Info "搜索注释模式: $commentPattern"
    if ($content -match $commentPattern) 
    {
        Write-Info "找到匹配的注释块"
        $commentIndex = $content.IndexOf($commentPattern)
        Write-Info "注释块开始位置: $commentIndex"
        $remainingContent = $content.Substring($commentIndex)
        $nextCommentIndex = $remainingContent.IndexOf("`n-- 配置为节点", 1)
        if ($nextCommentIndex -eq -1) 
        {
            # 没有下一个注释块，删除到文件结束
            $newContent = $content.Substring(0, $commentIndex).Trim()
            Write-Info "删除到文件末尾"
        } 
        else 
        {
            # 删除到下一个注释块之前
            $newContent = $content.Substring(0, $commentIndex) + $remainingContent.Substring($nextCommentIndex)
            Write-Info "删除到下一个注释块之前"
        }  
        # 写入更新后的内容
        Write-Utf8File -Path $sqlFile -Content $newContent
        Write-Success "已从 $sqlFile 中删除节点 '$RemoveNode' 的数据库配置"
        return
    }
    if ($dbName -and $dbUser)
    {
        Write-Info "使用方法2: 通过数据库名和用户名删除"
        # 构建要删除的SQL语句模式
        $patterns = @(
            "SELECT 'CREATE DATABASE $dbName'.*?\\gexec",
            "SELECT 'CREATE USER $dbUser WITH PASSWORD ''123456'''.*?\\gexec",
            "GRANT ALL PRIVILEGES ON DATABASE $dbName TO $dbUser;",
            "\\\\c $dbName",
            "DO `$`$ BEGIN IF NOT EXISTS\(SELECT 1 FROM information_schema\.schemata WHERE schema_name='public' AND schema_owner='$dbUser'\) THEN",
            "GRANT CREATE ON SCHEMA public TO $dbUser; ALTER SCHEMA public OWNER TO $dbUser;",
            "END IF; END `$`$;"
        )
        
        $newContent = $content
        foreach ($pattern in $patterns) 
        {
            Write-Info "删除模式: $pattern"
            $newContent = $newContent -replace $pattern, ""
        }
        
        # 清理多余的空行
        $newContent = $newContent -replace "(`r`n){3,}", "`r`n`r`n"
        # 写入更新后的内容
        Write-Utf8File -Path $sqlFile -Content $newContent.Trim()
        Write-Success "已从 $sqlFile 中删除节点 '$RemoveNode' 的数据库配置（方法2）"
        return
    }
    Write-Info "使用方法3: 逐行处理"
    $lines = Get-Content $sqlFile -Encoding UTF8
    $newLines = @()
    $skipMode = $false
    $inTargetBlock = $false
    
    foreach ($line in $lines) 
    {
        # 检查是否进入目标节点块
        if ($line -match "-- 配置为节点 '$([regex]::Escape($RemoveNode))' 自动添加") 
        {
            Write-Info "找到目标节点注释行: $line"
            $inTargetBlock = $true
            $skipMode = $true
            continue
        }
        # 如果已经在目标块中，检查是否遇到下一个注释块
        if ($skipMode -and $line -match "^-- 配置为节点") 
        {
            Write-Info "遇到下一个注释块，停止跳过: $line"
            $skipMode = $false
            $inTargetBlock = $false
        }
        # 如果已经在目标块中，检查是否遇到文件末尾或空行后的新内容
        if ($skipMode -and $inTargetBlock -and $line.Trim() -eq "" -and $newLines[-1] -match "END `$`$;") 
        {
            Write-Info "目标块结束于空行前"
            $skipMode = $false
            $inTargetBlock = $false
            continue
        }
        
        if (-not $skipMode) 
        {
            $newLines += $line
        } 
        else 
        {
            Write-Info "跳过行: $line"
        }
    }
    Write-Utf8File -Path $sqlFile -Content ($newLines -join "`n")
    Write-Success "已从 $sqlFile 中删除节点 '$RemoveNode' 的数据库配置（方法3）"
}

# 更新 build.gradle 中的 copyDriversToNodes 任务
function Update-CopyDriversTask 
{
    Write-Info "正在更新 copyDriversToNodes 任务..."
    $buildGradleFile = "build.gradle"
    $content = Get-Content $buildGradleFile -Raw -Encoding UTF8

    # 从节点名称中提取目录名 (例如 'O=PartyE,L=Tokyo,C=JP' -> 'PartyE')
    if ($NodeName -match 'O=([^,]+)') 
    {
        $nodeDirName = $Matches[1]
    } 
    else 
    {
        Write-Warning "无法从节点名称 '$NodeName' 中提取目录名，跳过更新 copyDriversToNodes 任务。"
        return
    }
    $driverCopyStatement = "    into file(`"`${buildDir}/nodes/$nodeDirName/drivers`")"
    # 如果已经存在，则不执行任何操作
    if ($content.Contains($driverCopyStatement)) {
        Write-Info "CopyDrivers任务已包含 '$nodeDirName' 的配置，无需修改。"
        return
    }
    # 找到 copyDriversToNodes 任务并在其结尾插入
    if ($content -match '(?s)(task copyDriversToNodes\(type: Copy\) \{.*?)(\n\})') {
        $before = $matches[1]
        $closingBrace = $matches[2]
        $newContent = $content -replace '(?s)(task copyDriversToNodes\(type: Copy\) \{.*?)(\n\})', "`$1`n$driverCopyStatement`$2"
        Write-Utf8File -Path $buildGradleFile -Content $newContent
        Write-Success "已更新 $buildGradleFile 中的 copyDriversToNodes 任务"
    } 
    else 
    {
        Write-Warning "在 $buildGradleFile 中找不到 'task copyDriversToNodes' 块。请手动添加驱动程序复制配置。"
        Write-Host "需要手动添加的配置: $driverCopyStatement"
    }
}

# 从 copyDriversToNodes 任务中删除节点配置 - 简化修复版本
function Remove-NodeFromCopyDrivers 
{
    Write-Info "正在从 copyDriversToNodes 任务中删除节点 '$RemoveNode' 的配置..."
    $buildGradleFile = "build.gradle"
    $content = Get-Content $buildGradleFile -Raw -Encoding UTF8

    # 从节点名称中提取目录名
    if ($RemoveNode -match 'O=([^,]+)') 
    {
        $nodeDirName = $Matches[1]
        Write-Info "从节点名称提取的目录名: $nodeDirName"
    } 
    else 
    {
        Write-Warning "无法从节点名称 '$RemoveNode' 中提取目录名，跳过删除 copyDriversToNodes 任务中的配置。"
        return
    }
    
    # 使用简单的字符串匹配和逐行处理
    Write-Info "使用逐行处理方法删除驱动程序配置..."
    $lines = Get-Content $buildGradleFile -Encoding UTF8
    $newLines = @()
    $targetLineFound = $false
    # 要查找的目标字符串
    $targetString = "into file(`"`${buildDir}/nodes/$nodeDirName/drivers`")"
    Write-Info "查找目标字符串: $targetString"
    
    foreach ($line in $lines) 
    {
        # 检查是否是要删除的行
        if ($line.Trim() -eq $targetString.Trim()) 
        {
            Write-Info "找到并删除目标行: $line"
            $targetLineFound = $true
            continue  # 跳过这一行，不添加到新内容中
        }
        $newLines += $line
    }
    
    if ($targetLineFound) 
    {
        # 写入更新后的内容
        Write-Utf8File -Path $buildGradleFile -Content ($newLines -join "`n")
        Write-Success "已从 $buildGradleFile 的 copyDriversToNodes 任务中删除节点 '$RemoveNode' 的配置"
    } 
    else 
    {
        Write-Info "CopyDrivers任务中未找到 '$nodeDirName' 的配置，无需删除。"
        Write-Info "当前 copyDriversToNodes 任务内容:"
        $inTask = $false
        foreach ($line in $lines) 
        {
            if ($line -match '^task copyDriversToNodes\(') 
            {
                $inTask = $true
            }
            if ($inTask) 
            {
                Write-Info "  $line"
            }
            if ($inTask -and $line.Trim() -eq '}') 
            {
                $inTask = $false
            }
        }
    }
}

# --- 主逻辑 ---
function Main 
{
    try {
        # 记录当前目录，用于可能需要的恢复
        $originalLocation = Get-Location
        
        # 方法1: 使用 $PSScriptRoot（PowerShell Core 推荐方式）
        if ($PSScriptRoot) {
            $scriptDir = $PSScriptRoot
            Write-Info "使用 PSScriptRoot 获取脚本目录: $scriptDir"
        } 
        # 方法2: 使用 $MyInvocation.MyCommand.Path
        elseif ($MyInvocation.MyCommand.Path) {
            $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
            Write-Info "使用 MyCommand.Path 获取脚本目录: $scriptDir"
        } 
        # 方法3: 使用 .NET 方法获取当前进程的脚本路径
        else {
            try {
                $scriptDir = [System.IO.Path]::GetDirectoryName(
                    [System.Diagnostics.Process]::GetCurrentProcess().MainModule.FileName
                )
                Write-Info "使用 .NET 方法获取脚本目录: $scriptDir"
            } catch {
                Write-Warning "无法获取脚本目录，使用当前目录"
                $scriptDir = Get-Location
            }
        }
        
        # 切换到脚本所在目录
        if (Test-Path $scriptDir) {
            Set-Location $scriptDir
            Write-Info "已切换到脚本目录: $(Get-Location)"
        }
        
        # 查找项目根目录（包含 build.gradle 的目录）
        Write-Info "正在查找项目根目录（包含 build.gradle）..."
        
        $projectRootFound = $false
        $maxSearchLevels = 8  # 最多向上查找8层目录
        $searchLevel = 0
        
        while ($searchLevel -lt $maxSearchLevels) {
            $currentPath = Get-Location
            
            # 检查当前目录是否有 build.gradle
            if (Test-Path "build.gradle") {
                Write-Success "找到项目根目录: $currentPath"
                $projectRootFound = $true
                break
            }
            
            # 检查当前目录是否有 .gradle 或 settings.gradle 作为备用
            if (Test-Path ".gradle" -or Test-Path "settings.gradle" -or Test-Path "gradlew" -or Test-Path "gradlew.bat") {
                Write-Info "找到项目相关文件，但缺少 build.gradle"
            }
            
            # 如果不是根目录，尝试向上一级
            if ($currentPath -eq "/" -or $currentPath -match '^[A-Za-z]:\\$') {
                Write-Info "已达到根目录，停止查找"
                break
            }
            
            Write-Info "目录 $currentPath 没有 build.gradle，向上查找..."
            Set-Location ..
            $searchLevel++
            
            # 显示进度
            if ($searchLevel % 2 -eq 0) {
                Write-Info "已向上查找 $searchLevel 级目录，当前在: $(Get-Location)"
            }
        }
        
        if (-not $projectRootFound) {
            Write-Error "未能在上级目录中找到 build.gradle 文件"
            Write-Host ""
            Write-Host "可能的解决方案："
            Write-Host "  1. 请确保在 Corda 项目根目录下运行此脚本"
            Write-Host "  2. 或者将脚本放在项目目录中"
            Write-Host "  3. 手动指定项目路径:"
            Write-Host "     cd /path/to/corda/project"
            Write-Host "     pwsh /path/to/script/add_node.ps1 [参数]"
            Write-Host ""
            
            # 恢复原始目录
            Set-Location $originalLocation
            
            # 让用户选择是否继续
            $choice = Read-Host "是否尝试在当前目录继续？(Y/N)"
            if ($choice -notmatch '^[Yy]') {
                exit 1
            }
            
            Write-Warning "在当前目录继续，但可能找不到必要的配置文件"
            
            # 列出当前目录内容，帮助调试
            Write-Info "当前目录内容:"
            Get-ChildItem | Select-Object -First 10 | ForEach-Object {
                Write-Info "  - $($_.Name) ($($_.GetType().Name))"
            }
        }
        
        Write-Success "工作目录已准备就绪: $(Get-Location)"
        Write-Info "检查必要文件是否存在..."
        
        $requiredFiles = @("build.gradle", "settings.gradle", "gradlew")
        $missingFiles = @()
        
        foreach ($file in $requiredFiles) {
            if (Test-Path $file) {
                Write-Info "  ✓ $file 存在"
            } else {
                Write-Warning "  ✗ $file 不存在"
                $missingFiles += $file
            }
        }
        
        if ($missingFiles.Count -gt 0 -and $missingFiles -contains "build.gradle") {
            Write-Error "缺少关键文件 build.gradle"
            exit 1
        }
        
    } catch {
        Write-Error "设置工作目录时发生错误: $($_.Exception.Message)"
        Write-Error "错误详情: $($_.ScriptStackTrace)"
        exit 1
    }
    
    # 检查是否显示帮助（使用参数集方式）
    if ($Help -or $PSCmdlet.ParameterSetName -eq "Help") 
    {
        Show-Usage
        return
    }
    
    # 根据参数集决定执行添加还是删除操作
    if ($PSCmdlet.ParameterSetName -eq "RemoveNode") 
    {
        Write-Info "开始从Corda网络中删除节点 '$RemoveNode'..."
        
        try 
        {
            Validate-Input-Remove
            
            Remove-NodeFromBuildGradle
            Remove-NodeFromCopyDrivers
            Remove-NodeFromSetupDbSql
            Remove-TaskFromClientsBuildGradle

            Write-Success "节点 '$RemoveNode' 删除完成！"
            Write-Host ""
            Write-Warning "下一步操作:"
            Write-Host "  1. 检查 'build.gradle' 和 'setup_corda_db.sql' 文件的更改是否正确。"
            Write-Host "  2. 重新部署节点: ./gradlew clean deployNodes"
            Write-Host "  3. 如果需要，手动清理 PostgreSQL 中的数据库和用户"
        } 
        catch 
        {
            Write-Error "删除节点过程中发生错误: $($_.Exception.Message)"
            Write-Error "错误堆栈: $($_.Exception.StackTrace)"
            exit 1
        }
    } 
    else 
    {
        Write-Info "开始向Corda网络添加新节点 '$NodeName'..."
        
        try 
        {
            Validate-Input-Add
            
            Modify-BuildGradle
            Update-CopyDriversTask
            Modify-SetupDbSql
            Update-ClientsBuildGradle

            Write-Success "新节点 '$NodeName' 添加完成！"
            Write-Host ""
            Write-Info "节点信息:"
            Write-Host "  - 名称: $NodeName"
            Write-Host "  - P2P端口: $P2PPort"
            Write-Host "  - RPC端口: $RPCPort"
            Write-Host "  - Admin端口: $AdminPort"
            Write-Host "  - 数据库: $DbName"
            Write-Host "  - 数据库用户: $DbUser"
            Write-Host ""
            Write-Warning "下一步操作:"
            Write-Host "  1. 检查 'build.gradle' 和 'setup_corda_db.sql' 文件的更改是否正确。"
            Write-Host "  2. 如果使用 PostgreSQL, 请先运行数据库脚本以创建新用户和数据库。"
            Write-Host "  3. 在项目根目录运行: ./gradlew clean deployNodes"
            Write-Host "  4. 启动所有节点: ./build/nodes/runnodes.sh"
        } 
        catch 
        {
            Write-Error "添加节点过程中发生错误: $($_.Exception.Message)"
            Write-Error "错误堆栈: $($_.Exception.StackTrace)"
            exit 1
        }
    }
}
# 执行主函数
Main
