#!/usr/bin/env pwsh

[CmdletBinding()]
param(
    [string]$DataDir = "$HOME/psql/data",
    [string]$LogDir = "$HOME/psql/logs",
    [string]$LogFile = "$LogDir/postgres.log",
    [int]$Port = 5432,
    [string]$UserName = (whoami),
    [string]$Password = "890415",
    [switch]$Help
)

# --- 颜色定义 ---
$ESC = [char]27
$RED = "$ESC[91m"
$GREEN = "$ESC[92m"
$YELLOW = "$ESC[93m"
$BLUE = "$ESC[94m"
$NC = "$ESC[0m"

function Write-Info { param([string]$message) Write-Host "${BLUE}[INFO]${NC} $message" }
function Write-Success { param([string]$message) Write-Host "${GREEN}[SUCCESS]${NC} $message" }
function Write-Warning { param([string]$message) Write-Host "${YELLOW}[WARNING]${NC} $message" }
function Write-Error { param([string]$message) Write-Host "${RED}[ERROR]${NC} $message" }

function Show-Usage {
    Write-Host "PostgreSQL 启动脚本"
    Write-Host "用法: ./start_postgres.ps1 [选项]"
    Write-Host ""
    Write-Host "选项:"
    Write-Host "  -DataDir <路径>      PostgreSQL 数据目录 (默认: ~/psql/data)"
    Write-Host "  -LogDir <路径>       日志目录 (默认: ~/psql/logs)"
    Write-Host "  -LogFile <文件>      日志文件 (默认: ~/psql/logs/postgres.log)"
    Write-Host "  -Port <端口>         PostgreSQL 端口 (默认: 5432)"
    Write-Host "  -UserName <用户名>    数据库用户名 (默认: 当前用户)"
    Write-Host "  -Password <密码>      数据库密码 (默认: 890415)"
    Write-Host "  -Help               显示此帮助信息"
    Write-Host ""
    Write-Host "示例:"
    Write-Host "  # 使用默认设置启动 PostgreSQL"
    Write-Host "  ./start_postgres.ps1"
    Write-Host ""
    Write-Host "  # 指定数据目录和端口"
    Write-Host "  ./start_postgres.ps1 -DataDir /var/lib/postgresql/data -Port 5433"
    Write-Host ""
    Write-Host "  # 显示帮助信息"
    Write-Host "  ./start_postgres.ps1 -Help"
    Write-Host ""
}

# 检查是否显示帮助
if ($Help) {
    Show-Usage
    exit 0
}

# 检查 PostgreSQL 是否已安装
function Test-PostgresInstalled {
    Write-Info "检查 PostgreSQL 是否已安装..."
    
    # 检查 pg_ctl 命令
    if (Get-Command "pg_ctl" -ErrorAction SilentlyContinue) {
        Write-Info "PostgreSQL 已安装 (找到 pg_ctl 命令)"
        return $true
    }
    
    # 检查 PostgreSQL 服务
    if (Get-Command "postgres" -ErrorAction SilentlyContinue) {
        Write-Info "PostgreSQL 已安装 (找到 postgres 命令)"
        return $true
    }
    
    # 检查 PostgreSQL 版本
    try {
        $version = psql --version 2>$null
        if ($version) {
            Write-Info "PostgreSQL 已安装: $version"
            return $true
        }
    } catch {
        # 忽略错误
    }
    
    return $false
}

# 检查端口是否已占用 - Ubuntu兼容版本
function Test-PortInUse {
    param([int]$Port)
    try {
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
        
        # 使用 ss 命令作为备选检查
        $result = ss -tuln 2>$null | Select-String ":$Port\s"
        if ($result) {
            return $true
        }
        
        return $false
    } catch {
        return $false
    }
}

# 自动创建目录（若不存在）
Write-Info "检查并创建必要的目录..."
if (-not (Test-Path $DataDir)) {
    New-Item -ItemType Directory -Force -Path $DataDir | Out-Null
    Write-Success "创建数据目录: $DataDir"
} else {
    Write-Info "数据目录已存在: $DataDir"
}

if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
    Write-Success "创建日志目录: $LogDir"
} else {
    Write-Info "日志目录已存在: $LogDir"
}

# 检查端口是否已占用
Write-Info "检查端口 $Port 是否可用..."
if (Test-PortInUse -Port $Port) {
    Write-Warning "端口 $Port 已被占用，可能是 PostgreSQL 已经在运行"
    
    # 检查是否是 PostgreSQL 进程占用了端口
    $processInfo = lsof -i:$Port 2>$null | Select-String "postgres"
    if ($processInfo) {
        Write-Success "PostgreSQL 已经在端口 $Port 上运行"
        exit 0
    } else {
        Write-Error "端口 $Port 被其他进程占用，请检查或更换端口"
        exit 1
    }
}
Write-Info "端口 $Port 可用"

# 检查 PostgreSQL 是否已安装
if (-not (Test-PostgresInstalled)) {
    Write-Error "PostgreSQL 未安装或未在 PATH 中找到"
    Write-Host ""
    Write-Host "请安装 PostgreSQL:"
    Write-Host "  Ubuntu/Debian: sudo apt-get install -y postgresql postgresql-contrib"
    Write-Host "  CentOS/RHEL:   sudo yum install -y postgresql-server postgresql-contrib"
    Write-Host "  macOS:         brew install postgresql"
    Write-Host ""
    exit 1
}

# 首次初始化（如果数据目录为空）
Write-Info "检查是否需要初始化 PostgreSQL 数据目录..."
$initCheckFiles = @("postgresql.conf", "pg_hba.conf", "PG_VERSION")
$needsInit = $true

foreach ($file in $initCheckFiles) {
    if (Test-Path "$DataDir/$file") {
        $needsInit = $false
        break
    }
}

if ($needsInit) {
    Write-Info "正在初始化 PostgreSQL 数据目录..."
    
    # 使用 initdb 初始化 PostgreSQL 数据目录
    try {
        $initResult = initdb -D $DataDir -U $UserName 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Success "PostgreSQL 数据目录初始化成功"
            
            # 设置密码
            Write-Info "设置数据库用户密码..."
            $pwFile = "$DataDir/pw.tmp"
            $Password | Out-File -FilePath $pwFile -Encoding UTF8 -Force
            
            # 修改 pg_hba.conf 以允许密码认证
            $pgHbaConf = "$DataDir/pg_hba.conf"
            if (Test-Path $pgHbaConf) {
                $content = Get-Content $pgHbaConf
                # 添加或修改认证方法为 md5
                $newContent = @()
                foreach ($line in $content) {
                    if ($line -match "^#?\s*local\s+all\s+all\s+") {
                        $newContent += "local   all             all                                     md5"
                    } elseif ($line -match "^#?\s*host\s+all\s+all\s+127.0.0.1/32\s+") {
                        $newContent += "host    all             all             127.0.0.1/32            md5"
                    } elseif ($line -match "^#?\s*host\s+all\s+all\s+::1/128\s+") {
                        $newContent += "host    all             all             ::1/128                 md5"
                    } else {
                        $newContent += $line
                    }
                }
                Set-Content -Path $pgHbaConf -Value ($newContent -join "`n") -Encoding UTF8
                Write-Info "已更新 pg_hba.conf 启用密码认证"
            }
            
            # 修改 postgresql.conf 监听地址和端口
            $pgConf = "$DataDir/postgresql.conf"
            if (Test-Path $pgConf) {
                $content = Get-Content $pgConf
                $newContent = @()
                $listenSet = $false
                $portSet = $false
                
                foreach ($line in $content) {
                    if ($line -match "^#?\s*listen_addresses\s*=") {
                        $newContent += "listen_addresses = '172.18.44.66,localhost'"
                        $listenSet = $true
                    } elseif ($line -match "^#?\s*port\s*=") {
                        $newContent += "port = $Port"
                        $portSet = $true
                    } else {
                        $newContent += $line
                    }
                }
                
                # 如果未找到设置，则添加
                if (-not $listenSet) {
                    $newContent += "listen_addresses = '172.18.44.66,localhost'"
                }
                if (-not $portSet) {
                    $newContent += "port = $Port"
                }
                
                Set-Content -Path $pgConf -Value ($newContent -join "`n") -Encoding UTF8
                Write-Info "已更新 postgresql.conf 监听地址和端口"
            }
            
            Remove-Item $pwFile -Force -ErrorAction SilentlyContinue
        } else {
            Write-Error "PostgreSQL 初始化失败: $initResult"
            exit 1
        }
    } catch {
        Write-Error "初始化失败: $($_.Exception.Message)"
        exit 1
    }
} else {
    Write-Info "PostgreSQL 数据目录已存在，跳过初始化"
    
    # 检查配置中的端口和地址
    $pgConf = "$DataDir/postgresql.conf"
    if (Test-Path $pgConf) {
        $content = Get-Content $pgConf
        
        # 检查监听地址
        $listenLine = $content | Where-Object { $_ -match "^listen_addresses\s*=\s*" } | Select-Object -First 1
        if ($listenLine) {
            if (-not $listenLine.Contains("172.18.44.66")) {
                Write-Warning "postgresql.conf 中未配置监听地址 172.18.44.66"
                Write-Info "当前配置: $listenLine"
                Write-Info "建议更新配置以监听 172.18.44.66"
            }
        }
        
        # 检查端口
        $portLine = $content | Where-Object { $_ -match "^port\s*=\s*" } | Select-Object -First 1
        if ($portLine) {
            if ($portLine -notmatch "port\s*=\s*$Port") {
                Write-Warning "postgresql.conf 中的端口配置 ($portLine) 与指定端口 $Port 不匹配"
            }
        }
    }
}

# 启动 PostgreSQL
Write-Info "正在启动 PostgreSQL..."
try {
    # 使用 pg_ctl 启动 PostgreSQL
    $startArgs = @(
        "start",
        "-D", $DataDir,
        "-l", $LogFile,
        "-o", "-c listen_addresses=172.18.44.66,localhost -c port=$Port"
    )
    
    Write-Debug "执行命令: pg_ctl $startArgs"
    $startResult = pg_ctl @startArgs 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "PostgreSQL 启动命令已执行"
    } else {
        Write-Warning "pg_ctl 启动返回非零状态: $LASTEXITCODE"
        Write-Debug "输出: $startResult"
    }
    
    # 等待 PostgreSQL 启动
    Write-Info "等待 PostgreSQL 启动..."
    $maxAttempts = 30
    $attempt = 0
    $started = $false
    
    while ($attempt -lt $maxAttempts) {
        if (Test-PortInUse -Port $Port) {
            # 检查 PostgreSQL 是否真的准备好了
            try {
                $testResult = psql -h 172.18.44.66 -p $Port -U $UserName -d postgres -c "SELECT 1;" 2>$null
                if ($testResult -and $testResult -match "1 row") {
                    $started = $true
                    break
                }
            } catch {
                # 忽略错误，继续等待
            }
        }
        
        $attempt++
        Write-Info "等待 PostgreSQL 启动... ($attempt/$maxAttempts)"
        Start-Sleep -Seconds 1
    }
    
    if ($started) {
        Write-Success "PostgreSQL 启动成功!"
        Write-Host ""
        Write-Info "连接信息:"
        Write-Host "  主机: 172.18.44.66"
        Write-Host "  端口: $Port"
        Write-Host "  用户: $UserName"
        Write-Host "  密码: $Password"
        Write-Host "  数据目录: $DataDir"
        Write-Host "  日志文件: $LogFile"
        Write-Host ""
        
        # 测试连接
        Write-Info "测试数据库连接..."
        $testCmd = "PGPASSWORD='$Password' psql -h 172.18.44.66 -p $Port -U $UserName -d postgres -c '\l'"
        try {
            $databases = bash -c $testCmd 2>$null
            if ($databases) {
                Write-Success "数据库连接测试成功"
                Write-Info "可用数据库:"
                $databases | Select-String -Pattern "^\s*\w+" | ForEach-Object {
                    $dbName = $_.ToString().Trim()
                    if ($dbName -notmatch "^\s*Name\s*|^\s*----" -and $dbName -ne "") {
                        Write-Host "  - $dbName"
                    }
                }
            }
        } catch {
            Write-Warning "数据库连接测试失败: $_"
        }
    } else {
        Write-Error "PostgreSQL 启动超时或失败"
        Write-Info "请检查日志文件: $LogFile"
        Write-Info "尝试手动启动: pg_ctl start -D '$DataDir' -l '$LogFile'"
        exit 1
    }
} catch {
    Write-Error "启动 PostgreSQL 时出错: $($_.Exception.Message)"
    Write-Info "请检查 PostgreSQL 是否已正确安装"
    exit 1
}

# 添加停止 PostgreSQL 的说明
Write-Host ""
Write-Warning "停止 PostgreSQL 的命令:"
Write-Host "  pg_ctl stop -D '$DataDir'"
Write-Host ""
Write-Info "PostgreSQL 启动完成!"