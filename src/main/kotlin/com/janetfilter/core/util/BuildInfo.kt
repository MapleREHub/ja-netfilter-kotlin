/*
 * ============================================================================
 * - BuildInfo.kt - 构建信息工具
 * ----------------------------------------------------------------------------
 * 提供构建版本信息和调试输出功能。
 * ============================================================================
 */
package com.janetfilter.core.util

/**
 * 构建信息
 *
 * 提供版本号、构建时间等运行时元数据。
 */
object BuildInfo {
    /** 版本号 */
    const val VERSION: String = "2.5.0"

    /** 构建时间（ISO 8601） */
    const val BUILD_TIME: String = "2026-08-22"

    /** Git commit SHA */
    const val GIT_COMMIT: String = "main"

    /** 完整版本字符串 */
    val FULL_VERSION: String = "$VERSION ($GIT_COMMIT)"

    /**
     * 获取构建信息字符串
     */
    @JvmStatic
    fun getInfo(): String {
        return """
            ja-netfilter $VERSION
            Built: $BUILD_TIME
            Commit: $GIT_COMMIT
        """.trimIndent()
    }
}
