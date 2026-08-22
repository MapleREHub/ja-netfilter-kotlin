/*
 * ============================================================================
 * - VmArgumentFilter.kt - VM 参数过滤器
 * ----------------------------------------------------------------------------
 * 提供过滤 VM 启动参数的功能。
 *
 * 工作原理：
 *   - sun.management.VMManagementImpl.getVmArguments() 返回 JVM 启动参数
 *   - 当应用通过 ManagementFactory 查询参数时，会返回过滤后的列表
 *   - 移除包含 ja-netfilter 关键字的参数，隐藏激活痕迹
 * ============================================================================
 */
package com.janetfilter.plugins.hideme

import com.janetfilter.core.Environment

/**
 * VM 参数过滤器
 */
object VmArgumentFilter {

    /** 环境上下文 */
    private var environment: Environment? = null

    /**
     * 设置环境
     */
    @JvmStatic
    fun setEnvironment(env: Environment) {
        environment = env
    }

    /**
     * 测试 VM 参数列表
     *
     * @param args 原始参数列表
     * @return 过滤后的列表（移除包含 ja-netfilter 的参数）
     */
    @JvmStatic
    fun testArgs(args: List<String>): List<String> {
        return args.filterNot { arg ->
            arg.contains("ja-netfilter") || arg.contains("jetbrains")
        }
    }
}