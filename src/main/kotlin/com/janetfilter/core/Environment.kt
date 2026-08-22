/*
 * ============================================================================
 * - Environment.kt - 环境上下文
 * ----------------------------------------------------------------------------
 * 保存 ja-netfilter 框架运行所需的所有环境信息：
 *   - PID（用于 attach 模式区分）
 *   - 版本信息
 *   - 应用名（从 agentArgs 解析）
 *   - 各种目录路径（baseDir, configDir, pluginsDir, logsDir）
 *   - nativePrefix（用于 native 包装器的前缀替换）
 *   - disabledPluginSuffix（用于 hideme 插件的禁用后缀）
 *   - 模式（attach / javaagent）
 *   - Instrumentation 实例
 * ============================================================================
 */
package com.janetfilter.core

import java.io.File
import java.lang.instrument.Instrumentation

/**
 * 环境上下文
 *
 * 该对象在 premain 阶段创建，并贯穿整个框架生命周期。
 * 所有插件和 transformer 都通过该对象访问框架资源。
 *
 * 不可变对象，所有字段在构造后不可修改。
 */
class Environment(
    /** JVM Instrumentation 实例 */
    private val instrumentation: Instrumentation,

    /** agent jar 文件 */
    private val agentFile: File,

    /** agent 参数 */
    private val agentArgs: String?,

    /** 是否 attach 模式 */
    private val attachMode: Boolean
) {
    /** 当前进程 PID */
    val pid: String = ProcessHandle.current().pid().toString()

    /** 版本号（来自 agent 参数，默认为 jetbrains） */
    val version: String = parseVersion(agentArgs)

    /** 版本号数字 */
    val versionNumber: Int = parseVersionNumber(agentArgs)

    /** 应用名（与 version 一致） */
    val appName: String = version

    /** agent jar 所在目录 */
    val baseDir: File = agentFile.parentFile ?: File(".")

    /** 配置文件目录 */
    val configDir: File = File(baseDir, "config-jetbrains").apply { mkdirs() }

    /** 插件目录 */
    val pluginsDir: File = File(baseDir, "plugins-jetbrains").apply { mkdirs() }

    /** 日志目录 */
    val logsDir: File = File(baseDir, "logs").apply { mkdirs() }

    /** native 方法前缀（用于 native 包装器） */
    val nativePrefix: String = "wrapped_"

    /** 禁用插件后缀（用于 hideme 插件过滤） */
    val disabledPluginSuffix: String = ".disabled"

    /** agent 文件路径 */
    val agentFilePath: String = agentFile.absolutePath

    fun isAttachMode(): Boolean = attachMode
    fun isJavaagentMode(): Boolean = !attachMode
    fun getInstrumentation(): Instrumentation = instrumentation
    fun getAgentFile(): File = agentFile

    /**
     * 解析版本/应用名
     * 优先使用 agent 参数（= 号后面的部分），默认为 "jetbrains"
     */
    private fun parseVersion(args: String?): String {
        if (args.isNullOrBlank() || args == "jetbrains") return "jetbrains"
        // 跳过 attach 模式的关键字
        if (args.startsWith("attach")) return "jetbrains"
        return args.trim()
    }

    /**
     * 解析版本数字（从 version 字段或参数中提取数字）
     */
    private fun parseVersionNumber(args: String?): Int {
        val v = parseVersion(args)
        // 尝试从版本字符串中提取数字
        val matcher = Regex("\\d+").find(v)
        return matcher?.value?.toIntOrNull() ?: 0
    }

    override fun toString(): String {
        return "Environment{" +
                "pid='$pid'" +
                ", version='$version'" +
                ", appName='$appName'" +
                ", baseDir='$baseDir'" +
                ", attachMode=$attachMode" +
                '}'
    }
}