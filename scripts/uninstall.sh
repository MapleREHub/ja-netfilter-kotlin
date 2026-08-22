#!/bin/sh
# ============================================================================
# - uninstall.sh - Linux/macOS 卸载脚本
# ============================================================================

set -e

OS_NAME=$(uname -s)
JB_PRODUCTS="idea clion phpstorm goland pycharm webstorm webide rider datagrip rubymine dataspell aqua rustrover gateway jetbrains_client jetbrainsclient studio devecostudio"

PROFILE_PATH="${HOME}/.profile"
ZSH_PROFILE_PATH="${HOME}/.zshrc"

if [ "$OS_NAME" = "Darwin" ]; then
  BASH_PROFILE_PATH="${HOME}/.bash_profile"
else
  BASH_PROFILE_PATH="${HOME}/.bashrc"
fi

# 移除 .jetbrains.vmoptions.sh 相关行
for file in "${PROFILE_PATH}" "${BASH_PROFILE_PATH}" "${ZSH_PROFILE_PATH}"; do
  if [ -f "$file" ]; then
    sed -i '/___MY_VMOPTIONS_SHELL_FILE="${HOME}\/\.jetbrains\.vmoptions\.sh"/d' "$file" 2>/dev/null || true
  fi
done

# 删除 .jetbrains.vmoptions.sh 文件
rm -f "${HOME}/.jetbrains.vmoptions.sh"

# 移除 KDE 环境链接
if [ -d "${HOME}/.config/plasma-workspace/env" ]; then
  rm -f "${HOME}/.config/plasma-workspace/env/jetbrains.vmoptions.sh"
fi

# 移除 macOS plist
if [ "$OS_NAME" = "Darwin" ] && [ -d "${HOME}/Library/LaunchAgents" ]; then
  rm -f "${HOME}/Library/LaunchAgents/jetbrains.vmoptions.plist"
fi

echo "Uninstallation completed. Please remove -javaagent lines from your vmoptions files manually."