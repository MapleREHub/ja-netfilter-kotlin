/*
 * ============================================================================
 * - LogUtil.kt - 日志工具
 * ----------------------------------------------------------------------------
 * privacy 插件的日志输出工具。
 * ============================================================================
 */
package com.novitechie

/**
 * 日志工具
 */
object LogUtil {

    /**
     * 打印当前调用栈
     */
    @JvmStatic
    fun printStackTrace() {
        val stackTrace = Thread.currentThread().stackTrace
        for (i in stackTrace.indices) {
            println("[privacy] $i: ${stackTrace[i]}")
        }
    }

    /**
     * 输出调试信息
     */
    @JvmStatic
    fun log(message: String) {
        println("[privacy] $message")
    }
}