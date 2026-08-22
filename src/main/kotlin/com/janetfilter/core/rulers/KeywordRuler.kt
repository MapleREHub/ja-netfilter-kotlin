/*
 * ============================================================================
 * - KeywordRuler.kt - 关键字匹配器
 * ----------------------------------------------------------------------------
 * 用于 KEYWORD 和 KEYWORD_IC 两种规则类型。
 *
 * KEYWORD    : String.contains()
 * KEYWORD_IC : String.contains(ignoreCase = true)
 * ============================================================================
 */
package com.janetfilter.core.rulers

/**
 * 关键字匹配器（包含匹配）
 *
 * @param ignoreCase 是否忽略大小写
 */
class KeywordRuler(private val ignoreCase: Boolean) : Ruler {
    override fun test(rule: String, input: String): Boolean {
        return if (ignoreCase) {
            input.contains(rule, ignoreCase = true)
        } else {
            input.contains(rule)
        }
    }
}