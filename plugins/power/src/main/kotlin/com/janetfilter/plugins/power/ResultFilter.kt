/*
 * ============================================================================
 * - ResultFilter.kt - 大数运算结果过滤器（运行时部分）
 * ----------------------------------------------------------------------------
 * 用于在 modPow 调用后修改返回结果。
 *
 * 工作原理：
 *   - modPow(sig, e, n) 返回签名验证结果
 *   - testFilter(result, e, n) 检查是否是 RSA PKCS#1 v1.5 解包后的结果
 *   - 如果匹配，则返回预定义的结果（通常是 licenseData 的 hash）
 *
 * 配置示例 (power.conf)：
 *   [Result]
 *   EQUAL,<expectedRawResult>,<e>,<n>-><desiredResult>
 * ============================================================================
 */
package com.janetfilter.plugins.power

import com.janetfilter.core.models.FilterRule
import java.math.BigInteger

/**
 * 大数运算结果过滤器
 */
object ResultFilter {

    /** L1 缓存 */
    private val l1Cached: MutableSet<String> = mutableSetOf()

    /** L2 缓存 */
    private val l2Cached: MutableMap<String, BigInteger> = mutableMapOf()

    /** 规则列表 */
    private var ruleList: List<FilterRule> = emptyList()

    /**
     * 设置规则列表
     */
    @JvmStatic
    fun setRules(rules: List<FilterRule>) {
        ruleList = rules
        l1Cached.clear()
        l2Cached.clear()
        for (rule in rules) {
            l1Cached.add(rule.rule)
        }
        println("[power] Loaded ${rules.size} result rules")
    }

    /**
     * 测试 modPow 结果
     *
     * @param result modPow 返回值
     * @param e 指数
     * @param m 模数
     * @return 如果匹配则返回替换结果，否则返回 null
     */
    @JvmStatic
    fun testFilter(result: BigInteger, e: BigInteger, m: BigInteger): BigInteger? {
        if (ruleList.isEmpty()) return null

        val key = "$result,$e,$m"

        if (!l1Cached.contains(key)) {
            return null
        }

        // 查找替换结果
        var cached = l2Cached[key]
        if (cached == null) {
            for (rule in ruleList) {
                if (rule.rule == key) {
                    val parts = rule.rule.split("->", limit = 2)
                    if (parts.size == 2) {
                        cached = BigInteger(parts[1])
                        l2Cached[key] = cached
                        break
                    }
                }
            }
        }

        return cached
    }
}