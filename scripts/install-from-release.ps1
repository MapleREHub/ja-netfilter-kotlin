# =============================================================================
# install-from-release.ps1 - 从 GitHub Release 一键安装最新版本 (PowerShell)
# ----------------------------------------------------------------------------
# 该脚本自动从 GitHub Release 下载最新版本的 ja-netfilter 并部署到当前目录。
#
# 使用方法：
#   .\install-from-release.ps1
#
# 可选参数：
#   -Version "v2.2.0"    指定版本（默认 latest）
#   -Target "C:\path"    指定安装目录（默认当前目录）
#   -JavaOnly           只下载 fat jar
# =============================================================================

[CmdletBinding()]
param(
    [string]$Version = "latest",
    [string]$Target = "",
    [switch]$JavaOnly = $false
)

$ErrorActionPreference = "Stop"

# 默认目标
if ([string]::IsNullOrEmpty($Target)) {
    $Target = (Get-Location).Path
}

$Repo = "MapleREHub/ja-netfilter-kotlin"

Write-Host "==================================================================="
Write-Host " ja-netfilter 一键安装脚本 (PowerShell)"
Write-Host "==================================================================="
Write-Host " Repository: $Repo"
Write-Host " Version:    $Version"
Write-Host " Target:     $Target"
Write-Host "==================================================================="
Write-Host ""

# 创建临时目录
$TmpDir = Join-Path $env:TEMP ("jnf-" + [guid]::NewGuid().ToString("N").Substring(0, 8))
New-Item -ItemType Directory -Path $TmpDir -Force | Out-Null

try {
    # 获取最新版本
    if ($Version -eq "latest") {
        Write-Host "[1/5] 获取最新版本号..."
        $apiUrl = "https://api.github.com/repos/$Repo/releases/latest"
        $release = Invoke-RestMethod -Uri $apiUrl
        # 直接使用远端 tag（如 ja-netfilter-v2.6.0），不要再拼 "v" 前缀
        $VersionTag = $release.tag_name
        if ([string]::IsNullOrEmpty($VersionTag)) {
            throw "无法获取最新版本号"
        }
    } else {
        # 允许传入 "2.6.0" / "v2.6.0" / "ja-netfilter-v2.6.0" 三种形式
        $VersionTag = $Version
        if ($VersionTag -notmatch '^ja-netfilter-') {
            $VersionTag = "ja-netfilter-v$($VersionTag -replace '^v', '')"
        }
    }

    Write-Host "[1/5] 版本: $VersionTag"

    # 下载源码包
    Write-Host "[2/5] 下载源码..."
    $archiveUrl = "https://github.com/$Repo/archive/refs/tags/$VersionTag.zip"
    $archivePath = Join-Path $TmpDir "$VersionTag.zip"
    Invoke-WebRequest -Uri $archiveUrl -OutFile $archivePath -UseBasicParsing
    Write-Host "  下载完成: $archivePath"

    # 解压
    Write-Host "[3/5] 解压文件..."
    $extractPath = Join-Path $TmpDir "extract"
    Expand-Archive -Path $archivePath -DestinationPath $extractPath -Force

    $sourceDir = Get-ChildItem -Path $extractPath -Directory | Select-Object -First 1
    if (-not $sourceDir) {
        throw "解压后未找到源码目录"
    }
    Set-Location $sourceDir.FullName

    # 创建目标目录
    Write-Host "[4/5] 复制文件到: $Target"
    @("config-jetbrains", "plugins-jetbrains", "vmoptions", "scripts") | ForEach-Object {
        $dir = Join-Path $Target $_
        if (-not (Test-Path $dir)) {
            New-Item -ItemType Directory -Path $dir -Force | Out-Null
        }
    }

    # 复制配置和脚本
    Copy-Item -Path "config-jetbrains\*.conf" -Destination "$Target\config-jetbrains\" -Force
    Copy-Item -Path "vmoptions\*.vmoptions" -Destination "$Target\vmoptions\" -Force
    if (Test-Path "scripts") {
        Copy-Item -Path "scripts\*" -Destination "$Target\scripts\" -Force
    }
    Copy-Item -Path "README.md" -Destination "$Target\" -Force -ErrorAction SilentlyContinue
    Copy-Item -Path "code.txt" -Destination "$Target\" -Force -ErrorAction SilentlyContinue

    if (-not (Test-Path "$Target\code.txt")) {
        "# See https://ckey.run for activation codes" | Out-File "$Target\code.txt" -Encoding utf8
    }

    # 获取 ja-netfilter.jar
    Write-Host "[5/5] 获取 ja-netfilter.jar..."
    $jarDownloaded = $false

    # 优先下载预编译
    $jarUrl = "https://github.com/$Repo/releases/download/$VersionTag/ja-netfilter.jar"
    try {
        Write-Host "  尝试下载预编译 jar: $jarUrl"
        Invoke-WebRequest -Uri $jarUrl -OutFile "$Target\ja-netfilter.jar" -UseBasicParsing -ErrorAction Stop
        if ((Get-Item "$Target\ja-netfilter.jar").Length -gt 0) {
            $jarDownloaded = $true
            Write-Host "  ✓ 预编译 jar 下载成功" -ForegroundColor Green
        }
    } catch {
        Write-Host "  ⚠ 预编译 jar 不存在或下载失败" -ForegroundColor Yellow
    }

    if (-not $jarDownloaded) {
        Write-Host "  尝试本地构建..."
        $hasGradle = $null -ne (Get-Command gradle -ErrorAction SilentlyContinue)
        $hasGradlew = Test-Path ".\gradlew.bat"

        if ($hasGradle -or $hasGradlew) {
            if ($hasGradlew) {
                & ".\gradlew.bat" ":ja-netfilter:fatJar" --no-daemon 2>&1 | Select-Object -Last 10
            } else {
                & "gradle" ":ja-netfilter:fatJar" --no-daemon 2>&1 | Select-Object -Last 10
            }

            $builtJar = Get-ChildItem -Path "ja-netfilter\build\libs\*-all.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($builtJar) {
                Copy-Item -Path $builtJar.FullName -Destination "$Target\ja-netfilter.jar" -Force
                $jarDownloaded = $true
                Write-Host "  ✓ 本地构建成功" -ForegroundColor Green
            }
        } else {
            Write-Host "  ERROR: 未找到 gradle 或 gradlew，请安装 Gradle 或使用 JDK 自带的 Java 直接下载 jar" -ForegroundColor Red
        }
    }

    # 下载插件 jar
    @("dns", "env", "hideme", "native", "power", "privacy", "url") | ForEach-Object {
        $plugin = $_
        $pluginUrl = "https://github.com/$Repo/releases/download/$VersionTag/$plugin.jar"
        try {
            Invoke-WebRequest -Uri $pluginUrl -OutFile "$Target\plugins-jetbrains\$plugin.jar" -UseBasicParsing -ErrorAction Stop
        } catch {
            # 忽略失败
        }
    }

    Write-Host ""
    Write-Host "==================================================================="
    Write-Host " ✓ 安装完成！" -ForegroundColor Green
    Write-Host "==================================================================="
    Write-Host ""
    Write-Host "下一步："
    Write-Host "  1. 编辑 $Target\config-jetbrains\ 中的配置文件"
    Write-Host "  2. 运行安装脚本："
    Write-Host "     Windows:  $Target\scripts\install-current-user.vbs"
    Write-Host ""
    Write-Host "  3. 激活码："
    Write-Host "     - $Target\code.txt"
    Write-Host "     - https://ckey.run"
    Write-Host ""
    Write-Host "或手动配置："
    Write-Host "  在 IDE 的 vmoptions 文件中添加："
    Write-Host "  -javaagent:$Target\ja-netfilter.jar=jetbrains"
    Write-Host ""
    Write-Host "==================================================================="
}
finally {
    Set-Location $PSScriptRoot
    if (Test-Path $TmpDir) {
        Remove-Item -Path $TmpDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}