/*
 * ============================================================================
 * - ArgsFilter.kt - 大数运算参数过滤器（运行时部分）
 * ----------------------------------------------------------------------------
 * 这是 ja-netfilter 最核心的插件之一。
 *
 * 工作原理：
 *   - JetBrains 使用 RSA 公钥验证激活码签名
 *   - 签名验证时使用 BigInteger.modPow(signature, exponent, modulus)
 *   - power.conf 中定义了 (e, n, expectedResult) 三元组
 *   - 当匹配到目标签名时，返回预定义的验签结果
 *
 * ArgsFilter.testFilter(a, b, m) 用于在调用 modPow 之前/之后修改参数：
 *   - a: base（通常是签名值）
 *   - b: exponent（公钥指数 e）
 *   - m: modulus（公钥模数 n）
 *
 * 配置示例 (power.conf)：
 *   [Args]
 *   EQUAL,<sig>,<e>,<n>-><result>
 *
 * 实现技巧：
 *   使用 L1/L2 缓存避免重复解析大数字
 * ============================================================================
 */
package com.janetfilter.plugins.power

import com.janetfilter.core.models.FilterRule
import java.math.BigInteger

/**
 * 大数运算参数过滤器
 *
 * 用于拦截 java.math.BigInteger.modPow 调用，实现激活码签名替换。
 */
object ArgsFilter {

    /** L1 缓存：EQUAL 规则按完整 (sig,e,n) 缓存 */
    private val l1Cached: MutableSet<String> = mutableSetOf()

    /** L2 缓存：按 (sig,e,n) 字符串缓存解析后的 BigInteger[] */
    private val l2Cached: MutableMap<String, BigIntegerArray> = mutableMapOf()

    /** 规则列表 */
    private var ruleList: List<FilterRule> = emptyList()

    /**
     * 设置规则列表
     *
     * @param rules 规则列表
     */
    @JvmStatic
    fun setRules(rules: List<FilterRule>) {
        ruleList = rules
        l1Cached.clear()
        l2Cached.clear()
        for (rule in rules) {
            l1Cached.add(rule.rule)
        }
        println("[power] Loaded ${rules.size} args rules")
    }

    /**
     * 测试 modPow 调用
     *
     * @param sig 签名值
     * @param e 指数
     * @param m 模数
     * @return 如果匹配则返回 [sig, expectedResult]，否则返回 null
     */
    @JvmStatic
    fun testFilter(sig: BigInteger, e: BigInteger, m: BigInteger): BigIntegerArray? {
        if (ruleList.isEmpty()) return null

        // 构造缓存键
        val key = "$sig,$e,$m"

        // 检查 L1 缓存
        if (!l1Cached.contains(key)) {
            return null
        }

        // 检查 L2 缓存
        var cached = l2Cached[key]
        if (cached == null) {
            // 解析规则：sig,e,n->result
            for (rule in ruleList) {
                if (rule.rule == key) {
                    val parts = rule.rule.split("->", limit = 2)
                    if (parts.size == 2) {
                        cached = BigIntegerArray(
                            sig,
                            BigInteger(parts[1])
                        )
                        l2Cached[key] = cached
                        break
                    }
                }
            }
        }

        return cached
    }

    /**
     * BigInteger 数组包装类
     *
     * component1() / component2() 由 data class 自动生成，
     * 与字段名 signature / expectedResult 对应。字节码注入（ArgsTransformer）
     * 通过 INVOKEVIRTUAL 调用的就是这些自动生成的方法。
     */
    data class BigIntegerArray(val signature: BigInteger, val expectedResult: BigInteger)
}