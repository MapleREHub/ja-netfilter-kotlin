/*
 * ============================================================================
 * - Launcher.kt - Java Agent 主入口点
 * ----------------------------------------------------------------------------
 * 这是 ja-netfilter.jar 的入口点（Premain-Class / Agent-Class）。
 * 当 JVM 启动参数包含 -javaagent:ja-netfilter.jar[=config] 时，
 * JVM 会先调用此类的 premain() 方法。
 *
 * 同时支持 attach 模式：通过 jvm.attach API 动态加载到已运行的 JVM。
 *
 * 主流程：
 *   1. premain() 创建 Environment 上下文
 *   2. 调用 Initializer.init(env) 初始化框架
 *   3. Initializer 创建 Dispatcher 和 PluginManager
 *   4. Dispatcher 注册为 Instrumentation 的 ClassFileTransformer
 *   5. PluginManager 扫描 plugins 目录加载所有插件
 * ============================================================================
 */
package com.janetfilter.core

import com.janetfilter.core.commons.DebugInfo
import java.io.File
import java.lang.instrument.Instrumentation

/**
 * Java Agent 主启动器
 *
 * 该类是 Manifest 中指定的 Preain-Class 和 Agent-Class。
 * JVM 在 -javaagent 加载时调用 premain()，attach 加载时调用 agentmain()。
 *
 * @author neo
 */
object Launcher {
    /** agent 参数 中的 attach 关键字 */
    const val ATTACH_ARG: String = "attach"

    /** 版本号字符串 */
    const val VERSION: String = "2.2.0"

    /** 版本号数字 */
    const val VERSION_NUMBER: Int = 220

    /** 是否已加载标记（防止重复初始化） */
    @Volatile
    private var loaded: Boolean = false

    /**
     * 主入口方法（用于 java -jar 直接运行，可作为 attach 工具使用）
     */
    @JvmStatic
    fun main(args: Array<String>) {
        println("ja-netfilter $VERSION")
        println("Usage:")
        println("  As Java Agent:  -javaagent:/path/to/ja-netfilter.jar[=config_name]")
        println("  As Attach Tool: java -jar ja-netfilter.jar <PID> [config_name]")
        println("")
        printUsage()
    }

    /**
     * JVM 启动时调用（-javaagent 模式）
     *
     * @param agentArgs agent 参数（= 后面的部分）
     * @param inst Instrumentation 实例
     */
    @JvmStatic
    fun premain(agentArgs: String?, inst: Instrumentation) {
        premain(agentArgs, inst, false)
    }

    /**
     * attach 模式调用（已运行的 JVM）
     */
    @JvmStatic
    fun agentmain(agentArgs: String?, inst: Instrumentation) {
        premain(agentArgs, inst, true)
    }

    /**
     * 内部 premain 实现
     *
     * @param agentArgs agent 参数
     * @param inst Instrumentation 实例
     * @param isAttach 是否 attach 模式
     */
    @JvmStatic
    fun premain(agentArgs: String?, inst: Instrumentation, isAttach: Boolean) {
        if (loaded) {
            return
        }
        loaded = true

        try {
            // 1. 定位 agent jar 文件位置
            val agentFile = locateAgentJar()

            // 2. 创建 Environment 上下文
            val environment = Environment(inst, agentFile, agentArgs, isAttach)

            // 3. 初始化日志
            DebugInfo.useFile(environment.logsDir)

            // 4. 调用初始化器
            Initializer.init(environment)

            DebugInfo.info("ja-netfilter $VERSION loaded successfully (mode: ${if (isAttach) "attach" else "javaagent"})")
        } catch (e: Throwable) {
            DebugInfo.error("Failed to initialize ja-netfilter", e)
            throw e
        }
    }

    /**
     * 定位 agent jar 文件位置
     *
     * 通过 ProtectionDomain 获取 jar 路径。
     */
    private fun locateAgentJar(): File {
        val sourceUrl = Launcher::class.java.protectionDomain.codeSource?.location
            ?: error("Cannot locate agent jar location")
        return File(sourceUrl.toURI())
    }

    /**
     * 打印使用说明
     */
    private fun printUsage() {
        println("Configuration directory structure:")
        println("  <jar-dir>/config-jetbrains/   - Plugin configuration files")
        println("  <jar-dir>/plugins-jetbrains/  - Plugin jars (auto-loaded)")
        println("  <jar-dir>/vmoptions/          - JetBrains vmoptions templates")
        println("  <jar-dir>/logs/               - Runtime logs")
    }
}