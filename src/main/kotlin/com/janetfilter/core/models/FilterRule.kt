/*
 * ============================================================================
 * - FilterRule.kt - 过滤规则模型
 * ----------------------------------------------------------------------------
 * 表示配置文件中的一行规则：
 *
 *   TYPE,rule_pattern
 *
 * 例如：
 *   EQUAL,jetbrains.com
 *   PREFIX,https://account.jetbrains.com/
 *
 * 使用 FilterRule.of() 静态方法从字符串创建。
 * ============================================================================
 */
package com.janetfilter.core.models

import com.janetfilter.core.enums.RuleType

/**
 * 过滤规则模型
 *
 * 表示一条匹配规则，由类型和模式组成。
 * 提供 test() 方法判断输入是否符合规则。
 */
class FilterRule(
    /** 规则类型 */
    var type: RuleType,

    /** 规则字符串 */
    var rule: String
) {
    /**
     * 测试输入是否符合规则
     *
     * @param input 输入字符串
     * @return true 表示匹配
     */
    fun test(input: String): Boolean {
        return type.getRuler().test(rule, input)
    }

    override fun toString(): String {
        return "$type,$rule"
    }

    companion object {
        /** 类型字符串到 RuleType 的映射 */
        private val SUPPORTED_TYPE_MAP = mapOf(
            "PREFIX" to RuleType.PREFIX,
            "PREFIX_IC" to RuleType.PREFIX_IC,
            "SUFFIX" to RuleType.SUFFIX,
            "SUFFIX_IC" to RuleType.SUFFIX_IC,
            "KEYWORD" to RuleType.KEYWORD,
            "KEYWORD_IC" to RuleType.KEYWORD_IC,
            "EQUAL" to RuleType.EQUAL,
            "EQUAL_IC" to RuleType.EQUAL_IC,
            "REGEXP" to RuleType.REGEXP
        )

        /**
         * 从字符串创建 FilterRule
         *
         * @param typeStr 类型字符串
         * @param ruleStr 规则字符串
         * @return FilterRule 实例
         * @throws IllegalArgumentException 如果类型不支持
         */
        @JvmStatic
        fun of(typeStr: String, ruleStr: String): FilterRule {
            val type = SUPPORTED_TYPE_MAP[typeStr.uppercase()]
                ?: throw IllegalArgumentException("Unsupported rule type: $typeStr")
            return FilterRule(type, ruleStr)
        }
    }
}