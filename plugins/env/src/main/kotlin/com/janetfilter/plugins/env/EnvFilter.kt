/*
 * ============================================================================
 * - EnvFilter.kt - 环境变量过滤器（运行时部分）
 * ----------------------------------------------------------------------------
 * 提供环境变量拦截功能，由 ProcessEnvironmentTransformer 在字节码层面
 * 把 java.lang.ProcessEnvironment 的相关方法替换为调用本类。
 *
 * 工作原理：
 *   - 当应用调用 System.getenv("name") 时，会经过 ProcessEnvironment.getenv()
 *   - testGetenv() 检查 name/value 是否匹配 env.conf 中的规则
 *   - 匹配则按规则替换值
 *
 * 配置示例 (env.conf)：
 *   [ENV]
 *   EQUAL,JAVA_VERSION,2.0
 * ============================================================================
 */
package com.janetfilter.plugins.env

import com.janetfilter.core.models.FilterRule

/**
 * 环境变量过滤器运行时类
 *
 * 提供静态方法供 transformer 注入到 java.lang.ProcessEnvironment 中。
 */
object EnvFilter {

    /** 变量名到替换值的映射 */
    private var myEnvironment: Map<String, String> = emptyMap()

    /** 原始不可修改环境变量映射 */
    private var theUnmodifiableEnvironment: Map<String, String> = emptyMap()

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
        println("[env] Loaded ${rules.size} rules")
    }

    /**
     * 测试 getenv(String) 调用
     *
     * @param name 变量名
     * @param originalValue 原始值
     * @return 替换后的值（或 originalValue）
     */
    @JvmStatic
    fun testGetEnv(name: String, originalValue: String?): String? {
        for (rule in ruleList) {
            if (rule.test(name)) {
                // 从规则中提取替换值（如果有）
                return rule.rule
            }
        }
        return originalValue
    }

    /**
     * 测试 getenv() 返回整个 Map
     *
     * @param original 原始 Map
     * @return 修改后的 Map
     */
    @JvmStatic
    fun testGetEnv(original: Map<String, String>): Map<String, String> {
        if (ruleList.isEmpty()) return original
        val result = HashMap(original)
        for (rule in ruleList) {
            result[rule.rule] = rule.rule
        }
        return result
    }

    /**
     * 测试 ProcessEnvironment.theEnvironment 字段
     *
     * @param original 原始 Map
     * @return 修改后的 Map
     */
    @JvmStatic
    fun testEnvironment(original: Map<String, String>): Map<String, String> {
        return testGetEnv(original)
    }
}