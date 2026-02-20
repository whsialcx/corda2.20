# start_postgres.ps1
param(
    [string]$DataDir = "$env:USERPROFILE\psql\data",
    [string]$LogDir  = "$env:USERPROFILE\psql\logs",
    [string]$LogFile = "$LogDir\postgres.log",
    [int]$Port = 5432,
    [string]$UserName = (whoami),
    [string]$Password = "890415"
)

# 自动创建目录（若不存在）
if (-not (Test-Path $DataDir)) {
    New-Item -ItemType Directory -Force -Path $DataDir | Out-Null
    Write-Host "[CordApp] Created data directory: $DataDir"
}
if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
    Write-Host "[CordApp] Created log directory: $LogDir"
}

# 检查端口是否已占用
function Test-Port([int]$port) {
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $tcp.Connect("127.0.0.1", $port)
        return $true
    } catch {
        return $false
    }
}
if (Test-Port $Port) {
    Write-Host "[CordApp] PostgreSQL already running on port $Port"
    exit 0
}

# 首次初始化
if (-not (Test-Path "$DataDir\postgresql.conf")) {
    Write-Host "[CordApp] Initializing PostgreSQL..."
    & pg_ctl initdb -D $DataDir --username=$UserName --pwfile=`<(echo $Password)
}

# 启动 PostgreSQL
Write-Host "[CordApp] Starting PostgreSQL..."
Start-Process -FilePath "pg_ctl" -ArgumentList "start", "-D", "`"$DataDir`"", "-l", "`"$LogFile`"" -NoNewWindow
Start-Sleep 5  

# 等待端口可用
$attempts = 0
while (-not (Test-Port $Port)) {
    $attempts++
    if ($attempts -gt 30) {
        Write-Host "[CordApp] PostgreSQL failed to start within 30 seconds"
        exit 1
    }
    Start-Sleep -Seconds 1
}
Write-Host "[CordApp] PostgreSQL started successfully"

