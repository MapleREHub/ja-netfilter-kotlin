/*
 * ============================================================================
 * - DNSFilter.kt - DNS 过滤器（运行时部分）
 * ----------------------------------------------------------------------------
 * 提供域名查询拦截功能，由 InetAddressTransformer 在字节码层面
 * 把 java.net.InetAddress 的相关方法调用替换为调用本类。
 *
 * 工作原理：
 *   - 当应用调用 InetAddress.getByName("blocked-domain.com") 时，
 *     实际会调用本类的 testQuery()
 *   - testQuery() 检查域名是否匹配 dns.conf 中的规则
 *   - 匹配则返回预定义 IP（如 127.0.0.1），实现屏蔽
 *
 * 配置示例 (dns.conf)：
 *   [DNS]
 *   EQUAL,jetbrains.com
 *   EQUAL,plugin.obroom.com
 * ============================================================================
 */
package com.janetfilter.plugins.dns

import com.janetfilter.core.models.FilterRule
import java.net.InetAddress

/**
 * DNS 过滤器运行时类
 *
 * 该类的方法被 transformer 注入到 java.net.InetAddress 中，
 * 用于在域名解析时进行拦截。
 */
object DNSFilter {

    /** 规则列表（从 dns.conf 加载） */
    private var ruleList: List<FilterRule> = emptyList()

    /**
     * 设置规则列表
     */
    @JvmStatic
    fun setRules(rules: List<FilterRule>) {
        ruleList = rules
        println("[dns] Loaded ${rules.size} rules")
    }

    /**
     * 测试域名查询
     *
     * 由 transformer 替换 InetAddress.getByName 调用
     *
     * @param host 要解析的域名
     * @return 域名（未拦截）/ 拦截后返回 null
     */
    @JvmStatic
    @Throws(java.io.IOException::class)
    fun testQuery(host: String): String {
        if (host.isEmpty()) return host

        // 检查是否匹配规则
        for (rule in ruleList) {
            if (rule.test(host)) {
                // 域名被屏蔽，返回空让上层返回失败
                throw java.net.UnknownHostException("Blocked by ja-netfilter: $host")
            }
        }

        return host
    }

    /**
     * 测试 InetAddress 可达性
     *
     * 由 transformer 替换 InetAddress.isReachable 等方法调用
     *
     * @param addr InetAddress 实例
     * @return 拦截后返回 false（不可达），否则返回 null
     */
    @JvmStatic
    @Throws(java.io.IOException::class)
    fun testReachable(addr: InetAddress): Any? {
        val host = addr.hostName ?: return null

        for (rule in ruleList) {
            if (rule.test(host)) {
                return false // 强制返回不可达
            }
        }

        return null
    }
}