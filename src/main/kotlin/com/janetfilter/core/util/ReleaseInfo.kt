/*
 * ============================================================================
 * - ReleaseInfo.kt - 发布信息工具
 * ----------------------------------------------------------------------------
 * 提供从环境变量或系统属性获取发布版本信息的功能。
 */
 * ============================================================================
 */
package com.janetfilter.core.util

/**
 * 发布信息工具
 */
object ReleaseInfo {
    /**
     * 从环境变量获取版本
     */
    @JvmStatic
    fun getVersion(): String {
        return System.getenv("JA_NETFILTER_VERSION")
            ?: System.getProperty("ja-netfilter.version")
            ?: BuildInfo.VERSION
    }
}
