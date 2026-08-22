/*
 * ============================================================================
 * - VMSelector.kt - 虚拟机选择器
 * ----------------------------------------------------------------------------
 * 在 attach 模式下，让用户从列表中选择要 attach 的 JVM。
 * ============================================================================
 */
package com.janetfilter.core.attach

import com.janetfilter.core.commons.DebugInfo
import java.io.File

/**
 * 虚拟机选择器
 *
 * @param thisJar 当前 ja-netfilter.jar 文件
 */
class VMSelector(private val thisJar: File) {

    /** 已发现的虚拟机列表 */
    private var descriptors: List<VMDescriptor> = emptyList()

    /**
     * 启动选择流程
     */
    @Throws(Exception::class)
    fun select() {
        descriptors = getVMList()
        if (descriptors.isEmpty()) {
            println("No JVM found.")
            return
        }

        // 打印列表
        println("Found ${descriptors.size} JVM(s):")
        descriptors.forEachIndexed { index, desc ->
            println("  [${index + 1}] PID=${desc.id}, ${desc.className}")
        }

        // 读取用户输入
        val input = getInput()
        val index = input.toIntOrNull()
        if (index == null || index < 1 || index > descriptors.size) {
            invalidInput(input)
            return
        }

        val selected = descriptors[index - 1]
        println("Selected: $selected")

        // 启动 attach
        VMLauncher.attachVM(selected.id, thisJar.absolutePath, null)
    }

    /**
     * 获取 JVM 列表
     */
    @Throws(Exception::class)
    private fun getVMList(): List<VMDescriptor> {
        // 通过 jps 命令或读取临时目录获取
        // 这里简化实现：使用 jps 命令
        val process = ProcessBuilder("jps", "-l")
            .redirectErrorStream(true)
            .start()

        val descriptors = mutableListOf<VMDescriptor>()
        process.inputStream.bufferedReader().useLines { lines ->
            for (line in lines) {
                val parts = line.trim().split(Regex("\\s+"), limit = 2)
                if (parts.size >= 2) {
                    val desc = VMDescriptor(parts[0], parts[1].takeIf { it.contains(".") } ?: "Unknown", "")
                    descriptors.add(desc)
                }
            }
        }

        process.waitFor()
        return descriptors
    }

    /**
     * 读取用户输入
     */
    @Throws(java.io.IOException::class)
    private fun getInput(): String {
        print("Select JVM (1-${descriptors.size}): ")
        return readLine() ?: ""
    }

    /**
     * 处理无效输入
     */
    @Throws(Exception::class)
    private fun invalidInput(input: String) {
        println("Invalid input: $input")
    }
}