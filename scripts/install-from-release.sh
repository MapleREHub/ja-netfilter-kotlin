#!/bin/sh
# =============================================================================
# install-from-release.sh - 从 GitHub Release 一键安装最新版本
# ----------------------------------------------------------------------------
# 该脚本自动从 GitHub Release 下载最新版本的 ja-netfilter 并部署到当前目录。
#
# 使用方法：
#   ./install-from-release.sh
#
# 可选参数：
#   --version=v2.2.0   指定版本（默认 latest）
#   --target=/path/to  指定安装目录（默认当前目录）
#   --java-only         只下载 fat jar
# =============================================================================

set -e

# 默认配置
VERSION="${VERSION:-latest}"
TARGET_DIR="${TARGET_DIR:-$(pwd)}"
JAVA_ONLY=false

# 解析参数
while [ $# -gt 0 ]; do
  case "$1" in
    --version=*)
      VERSION="${1#*=}"
      shift
      ;;
    --target=*)
      TARGET_DIR="${1#*=}"
      shift
      ;;
    --java-only)
      JAVA_ONLY=true
      shift
      ;;
    --help|-h)
      echo "Usage: $0 [--version=vX.Y.YZ|latest] [--target=/path] [--java-only]"
      echo ""
      echo "Options:"
      echo "  --version    Specify version (default: latest)"
      echo "  --target     Specify install directory (default: current dir)"
      echo "  --java-only  Only download fat jar"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

# GitHub 配置
REPO="MapleREHub/ja-netfilter-kotlin"

echo "==================================================================="
echo " ja-netfilter 一键安装脚本"
echo "==================================================================="
echo " Repository: $REPO"
echo " Version:    $VERSION"
echo " Target:     $TARGET_DIR"
echo "==================================================================="
echo ""

# 检查依赖
if ! command -v curl >/dev/null 2>&1 && ! command -v wget >/dev/null 2>&1; then
    echo "ERROR: 需要 curl 或 wget"
    exit 1
fi

# 检查 unzip
if ! command -v unzip >/dev/null 2>&1; then
    echo "ERROR: 需要 unzip"
    exit 1
fi

# 创建临时目录
TMP_DIR=$(mktemp -d)
cd "$TMP_DIR"

# 确定下载 URL
if [ "$VERSION" = "latest" ]; then
    # 获取最新 release 的 tag
    if command -v curl >/dev/null 2>&1; then
        VERSION=$(curl -s "https://api.github.com/repos/$REPO/releases/latest" | grep '"tag_name"' | head -1 | sed -E 's/.*"v?([^"]+)".*/\1/')
    else
        VERSION=$(wget -qO- "https://api.github.com/repos/$REPO/releases/latest" | grep '"tag_name"' | head -1 | sed -E 's/.*"v?([^"]+)".*/\1/')
    fi

    if [ -z "$VERSION" ]; then
        echo "ERROR: 无法获取最新版本号"
        exit 1
    fi
fi

VERSION_TAG="v$VERSION"
echo "[1/5] 版本: $VERSION_TAG"

# 下载源码包（包含所有文件）
ARCHIVE_URL="https://github.com/$REPO/archive/refs/tags/$VERSION_TAG.tar.gz"
echo "[2/5] 下载源码: $ARCHIVE_URL"

if command -v curl >/dev/null 2>&1; then
    curl -L -o "$VERSION_TAG.tar.gz" "$ARCHIVE_URL" 2>&1 | tail -3
else
    wget -O "$VERSION_TAG.tar.gz" "$ARCHIVE_URL" 2>&1 | tail -3
fi

if [ ! -f "$VERSION_TAG.tar.gz" ]; then
    echo "ERROR: 下载失败"
    exit 1
fi

# 解压
echo "[3/5] 解压文件"
tar -xzf "$VERSION_TAG.tar.gz"

SOURCE_DIR="ja-netfilter-kotlin-$VERSION"
if [ ! -d "$SOURCE_DIR" ]; then
    # 尝试其他目录名格式
    SOURCE_DIR=$(ls -d ja-netfilter-kotlin-* 2>/dev/null | head -1)
    if [ -z "$SOURCE_DIR" ]; then
        echo "ERROR: 解压后未找到源码目录"
        exit 1
    fi
fi

cd "$SOURCE_DIR"

# 创建目标目录
mkdir -p "$TARGET_DIR/config-jetbrains"
mkdir -p "$TARGET_DIR/plugins-jetbrains"
mkdir -p "$TARGET_DIR/vmoptions"
mkdir -p "$TARGET_DIR/scripts"

# 复制文件
echo "[4/5] 复制文件到: $TARGET_DIR"
cp config-jetbrains/*.conf "$TARGET_DIR/config-jetbrains/"
cp scripts/* "$TARGET_DIR/scripts/" 2>/dev/null || true
cp vmoptions/*.vmoptions "$TARGET_DIR/vmoptions/"
cp README.md "$TARGET_DIR/"
cp code.txt "$TARGET_DIR/" 2>/dev/null || echo "# See https://ckey.run for activation codes" > "$TARGET_DIR/code.txt"

# 检查是否有预编译的 jar
echo "[5/5] 获取 ja-netfilter.jar"

# 优先下载 release 中的预编译 jar
JAR_URL="https://github.com/$REPO/releases/download/$VERSION_TAG/ja-netfilter.jar"
echo "  尝试下载预编译 jar: $JAR_URL"

JAR_DOWNLOADED=false
if command -v curl >/dev/null 2>&1; then
    if curl -fsL -o "$TARGET_DIR/ja-netfilter.jar" "$JAR_URL" 2>/dev/null; then
        if [ -s "$TARGET_DIR/ja-netfilter.jar" ]; then
            JAR_DOWNLOADED=true
            echo "  ✓ 预编译 jar 下载成功"
        fi
    fi
else
    if wget -q --tries=3 -O "$TARGET_DIR/ja-netfilter.jar" "$JAR_URL" 2>/dev/null; then
        if [ -s "$TARGET_DIR/ja-netfilter.jar" ]; then
            JAR_DOWNLOADED=true
            echo "  ✓ 预编译 jar 下载成功"
        fi
    fi
fi

if [ "$JAR_DOWNLOADED" = false ]; then
    echo "  ⚠ 预编译 jar 不存在，尝试本地构建..."

    # 检查 Java 和 Gradle
    if ! command -v java >/dev/null 2>&1; then
        echo "ERROR: 本地构建需要 Java"
        exit 1
    fi

    if command -v gradle >/dev/null 2>&1; then
        echo "  使用 gradle 构建..."
        gradle :ja-netfilter:fatJar --no-daemon 2>&1 | tail -10
    else
        echo "  未找到 gradle，尝试使用 Gradle Wrapper..."
        if [ -f "./gradlew" ]; then
            chmod +x ./gradlew
            ./gradlew :ja-netfilter:fatJar --no-daemon 2>&1 | tail -10
        else
            echo "ERROR: 找不到 gradle 或 gradlew"
            exit 1
        fi
    fi

    if [ -f "ja-netfilter/build/libs/ja-netfilter-${VERSION}-all.jar" ]; then
        cp "ja-netfilter/build/libs/ja-netfilter-${VERSION}-all.jar" "$TARGET_DIR/ja-netfilter.jar"
        JAR_DOWNLOADED=true
        echo "  ✓ 本地构建成功"
    fi
fi

# 复制插件 jar（如果有预编译的）
for plugin in dns env hideme native power privacy url; do
    PLUGIN_URL="https://github.com/$REPO/releases/download/$VERSION_TAG/${plugin}.jar"
    if command -v curl >/dev/null 2>&1; then
        curl -fsL -o "$TARGET_DIR/plugins-jetbrains/${plugin}.jar" "$PLUGIN_URL" 2>/dev/null || true
    else
        wget -q --tries=3 -O "$TARGET_DIR/plugins-jetbrains/${plugin}.jar" "$PLUGIN_URL" 2>/dev/null || true
    fi
done

# 清理临时目录
cd /
rm -rf "$TMP_DIR"

echo ""
echo "==================================================================="
echo " ✓ 安装完成！"
echo "==================================================================="
echo ""
echo "下一步："
echo "  1. 编辑 $TARGET_DIR/config-jetbrains/ 中的配置文件"
echo "  2. 运行安装脚本："
echo "     Linux/macOS:  $TARGET_DIR/scripts/install.sh"
echo "     Windows:       $TARGET_DIR/scripts/install-current-user.vbs"
echo ""
echo "  3. 激活码："
echo "     - $TARGET_DIR/code.txt"
echo "     - https://ckey.run"
echo ""
echo "或手动配置："
echo "  在 IDE 的 vmoptions 文件中添加："
echo "  -javaagent:$TARGET_DIR/ja-netfilter.jar=jetbrains"
echo ""
echo "==================================================================="