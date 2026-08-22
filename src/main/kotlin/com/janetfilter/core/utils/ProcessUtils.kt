/*
 * ============================================================================
 * - ProcessUtils.kt - 进程工具类
 * ----------------------------------------------------------------------------
 * 提供进程管理相关工具方法。
 * ============================================================================
 */
package com.janetfilter.core.utils

import java.io.File

/**
 * 进程工具类
 */
object ProcessUtils {

    /**
     * 执行 shell 命令并返回输出
     *
     * @param command 命令及参数
     * @return 命令输出
     */
    @JvmStatic
    fun exec(command: List<String>): String {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor()
        return output
    }

    /**
     * 执行 shell 命令并返回输出（工作目录指定）
     */
    @JvmStatic
    fun exec(command: List<String>, workDir: File): String {
        val process = ProcessBuilder(command).directory(workDir).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor()
        return output
    }

    /**
     * 输出重定向任务（内部类）
     */
    class RedirectOutput
}