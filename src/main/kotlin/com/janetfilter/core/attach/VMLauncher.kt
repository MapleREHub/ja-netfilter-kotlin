/*
 * ============================================================================
 * - VMLauncher.kt - 虚拟机启动器（attach 模式）
 * ----------------------------------------------------------------------------
 * 提供 attach API 用于动态加载 agent 到已运行的 JVM。
 * ============================================================================
 */
package com.janetfilter.core.attach

import com.janetfilter.core.commons.DebugInfo
import java.io.File

/**
 * 虚拟机启动器
 *
 * 提供 attach 模式的核心功能。
 */
object VMLauncher {

    /**
     * attach 到指定 JVM
     *
     * @param pid 目标 JVM 的 PID
     * @param agentJar agent jar 路径
     * @param agentArgs agent 参数（可选）
     */
    @JvmStatic
    fun attachVM(pid: String, agentJar: String, agentArgs: String?) {
        try {
            // 加载 tools.jar 中的 Attach API
            val vmClass = Class.forName("com.sun.tools.attach.VirtualMachine")
            val attachMethod = vmClass.getMethod("attach", String::class.java)
            val vm = attachMethod.invoke(null, pid)

            try {
                val loadAgentMethod = vmClass.getMethod("loadAgent", String::class.java, String::class.java)
                loadAgentMethod.invoke(vm, agentJar, agentArgs ?: "")
                DebugInfo.info("Agent loaded to JVM $pid successfully")
            } finally {
                val detachMethod = vmClass.getMethod("detach")
                detachMethod.invoke(vm)
            }
        } catch (e: Exception) {
            DebugInfo.error("Failed to attach to JVM $pid", e)
            throw e
        }
    }

    /**
     * 启动新的 JVM 并加载 agent
     */
    @Throws(Exception::class)
    fun launch(jarFile: File, descriptor: VMDescriptor, agentArgs: String) {
        val processBuilder = buildProcess(jarFile, jarFile.parentFile, descriptor.id, agentArgs)
        val process = processBuilder.start()
        DebugInfo.info("Launched VM ${descriptor.id}, waiting...")
        process.waitFor()
    }

    /**
     * 构建 ProcessBuilder
     */
    private fun buildProcess(jarFile: File, workDir: File, vmId: String, agentArgs: String): ProcessBuilder {
        val javaHome = System.getProperty("java.home")
        val javaBin = File(javaHome, "bin/java").absolutePath + if (System.getProperty("os.name").lowercase().contains("win")) ".exe" else ""

        val command = mutableListOf(
            javaBin,
            "-jar",
            jarFile.absolutePath,
            vmId,
            agentArgs
        )

        return ProcessBuilder(command).directory(workDir)
    }
}