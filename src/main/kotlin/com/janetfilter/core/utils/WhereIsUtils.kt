/*
 * ============================================================================
 * - WhereIsUtils.kt - 路径定位工具
 * ----------------------------------------------------------------------------
 * 提供跨平台的文件路径查找方法。
 * ============================================================================
 */
package com.janetfilter.core.utils

import java.io.File

/**
 * 路径定位工具
 *
 * 用于查找系统中的关键路径：
 *   - 用户主目录
 *   - JetBrains 配置目录
 *   - 当前工作目录
 */
object WhereIsUtils {

    /**
     * 获取用户主目录
     */
    @JvmStatic
    fun userHome(): String {
        return System.getProperty("user.home")
    }

    /**
     * 获取 JetBrains 配置目录
     *
     *   Windows: %APPDATA%\JetBrains
     *   macOS:   ~/Library/Application Support/JetBrains
     *   Linux:   ~/.config/JetBrains
     */
    @JvmStatic
    fun jetbrainsConfigDir(): String {
        val os = System.getProperty("os.name").lowercase()
        val home = userHome()
        return when {
            os.contains("win") -> "${System.getenv("APPDATA")}\\JetBrains"
            os.contains("mac") -> "$home/Library/Application Support/JetBrains"
            else -> "$home/.config/JetBrains"
        }
    }

    /**
     * 获取当前工作目录
     */
    @JvmStatic
    fun currentDir(): String {
        return System.getProperty("user.dir")
    }

    /**
     * 获取平台名称
     */
    @JvmStatic
    fun platformName(): String {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> "windows"
            os.contains("mac") -> "macos"
            os.contains("nix") || os.contains("nux") -> "linux"
            else -> "unknown"
        }
    }
}