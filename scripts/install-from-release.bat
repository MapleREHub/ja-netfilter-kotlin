@echo off
REM =============================================================================
REM install-from-release.bat - 从 GitHub Release 一键安装最新版本 (Windows Batch)
REM ----------------------------------------------------------------------------
REM 使用方法：
REM   install-from-release.bat
REM   install-from-release.bat v2.2.0
REM   install-from-release.bat v2.2.0 "C:\Program Files\JetBrains"
REM =============================================================================

setlocal enabledelayedexpansion

set "REPO=MapleREHub/ja-netfilter-kotlin"
set "VERSION=%~1"
if "%VERSION%"=="" set "VERSION=latest"
set "TARGET_DIR=%~2"
if "%TARGET_DIR%"=="" set "TARGET_DIR=%CD%"

echo ============================================================
echo  ja-netfilter 一键安装脚本
echo ============================================================
echo  Repository: %REPO%
echo  Version:    %VERSION%
echo  Target:     %TARGET_DIR%
echo ============================================================
echo.

REM 创建临时目录
set "TMP_DIR=%TEMP%\jnf-%RANDOM%"
mkdir "%TMP_DIR%" 2>nul
cd /d "%TMP_DIR%"

REM 获取最新版本
if /i "%VERSION%"=="latest" (
    echo [1/5] 获取最新版本号...
    powershell -Command "try { (Invoke-RestMethod -Uri 'https://api.github.com/repos/%REPO%/releases/latest').tag_name -replace '^v','' } catch { '' }" > version.txt
    set /p VERSION=<version.txt
    del version.txt
    if "%VERSION%"=="" (
        echo ERROR: 无法获取最新版本号
        exit /b 1
    )
)

set "VERSION_TAG=v%VERSION%"
echo [1/5] 版本: %VERSION_TAG%

REM 下载源码包
echo [2/5] 下载源码...
powershell -Command "Invoke-WebRequest -Uri 'https://github.com/%REPO%/archive/refs/tags/%VERSION_TAG%.zip' -OutFile '%VERSION_TAG%.zip' -UseBasicParsing"

if not exist "%VERSION_TAG%.zip" (
    echo ERROR: 下载失败
    exit /b 1
)

REM 解压
echo [3/5] 解压文件...
powershell -Command "Expand-Archive -Path '%VERSION_TAG%.zip' -DestinationPath '.' -Force"

REM 找到解压目录
for /d %%d in (ja-netfilter-kotlin-*) do set "SOURCE_DIR=%%d"

if "%SOURCE_DIR%"=="" (
    echo ERROR: 解压后未找到源码目录
    exit /b 1
)

cd /d "%SOURCE_DIR%"

REM 创建目标目录
mkdir "%TARGET_DIR%\config-jetbrains" 2>nul
mkdir "%TARGET_DIR%\plugins-jetbrains" 2>nul
mkdir "%TARGET_DIR%\vmoptions" 2>nul
mkdir "%TARGET_DIR%\scripts" 2>nul

REM 复制文件
echo [4/5] 复制文件到: %TARGET_DIR%
copy /Y "config-jetbrains\*.conf" "%TARGET_DIR%\config-jetbrains\" >nul
copy /Y "vmoptions\*.vmoptions" "%TARGET_DIR%\vmoptions\" >nul
copy /Y "scripts\*" "%TARGET_DIR%\scripts\" >nul 2>&1
copy /Y "README.md" "%TARGET_DIR%\" >nul 2>&1
copy /Y "code.txt" "%TARGET_DIR%\" >nul 2>&1

if not exist "%TARGET_DIR%\code.txt" (
    echo # See https://ckey.run for activation codes > "%TARGET_DIR%\code.txt"
)

REM 获取 ja-netfilter.jar
echo [5/5] 获取 ja-netfilter.jar...

set "JAR_URL=https://github.com/%REPO%/releases/download/%VERSION_TAG%/ja-netfilter.jar"
echo   尝试下载预编译 jar: %JAR_URL%

powershell -Command "try { Invoke-WebRequest -Uri '%JAR_URL%' -OutFile '%TARGET_DIR%\ja-netfilter.jar' -UseBasicParsing; exit 0 } catch { exit 1 }"
if exist "%TARGET_DIR%\ja-netfilter.jar" (
    if not "%~z1"=="0" (
        echo   ^√^√ 预编译 jar 下载成功
        goto :jar_ready
    )
)

echo   ⚠ 预编译 jar 不存在，尝试本地构建...
where gradle >nul 2>&1
if %ERRORLEVEL% equ 0 (
    gradle :ja-netfilter:fatJar --no-daemon
) else (
    if exist "gradlew.bat" (
        call gradlew.bat :ja-netfilter:fatJar --no-daemon
    ) else (
        echo ERROR: 找不到 gradle
        exit /b 1
    )
)

if exist "ja-netfilter\build\libs\ja-netfilter-%VERSION%-all.jar" (
    copy /Y "ja-netfilter\build\libs\ja-netfilter-%VERSION%-all.jar" "%TARGET_DIR%\ja-netfilter.jar" >nul
    echo   ^√^√ 本地构建成功
)

:jar_ready

REM 下载插件 jar
for %%p in (dns env hideme native power privacy url) do (
    powershell -Command "try { Invoke-WebRequest -Uri 'https://github.com/%REPO%/releases/download/%VERSION_TAG%/%%p.jar' -OutFile '%TARGET_DIR%\plugins-jetbrains\%%p.jar' -UseBasicParsing } catch { }" >nul 2>&1
)

REM 清理
cd /d "%TEMP%"
rmdir /s /q "%TMP_DIR%" 2>nul

echo.
echo ============================================================
echo  ✓ 安装完成！
echo ============================================================
echo.
echo 下一步：
echo   1. 编辑 %TARGET_DIR%\config-jetbrains\ 中的配置文件
echo   2. 运行安装脚本：
echo      Windows:  %TARGET_DIR%\scripts\install-current-user.vbs
echo.
echo   3. 激活码：
echo      - %TARGET_DIR%\code.txt
echo      - https://ckey.run
echo.
echo 或手动配置：
echo   在 IDE 的 vmoptions 文件中添加：
echo   -javaagent:%TARGET_DIR%\ja-netfilter.jar=jetbrains
echo.
echo ============================================================

endlocal