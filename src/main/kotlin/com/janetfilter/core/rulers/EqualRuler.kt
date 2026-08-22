/*
 * ============================================================================
 * - EqualRuler.kt - 完全匹配器
 * ----------------------------------------------------------------------------
 * 用于 EQUAL 和 EQUAL_IC 两种规则类型。
 *
 * EQUAL    : String.equals()
 * EQUAL_IC : String.equalsIgnoreCase()
 * ============================================================================
 */
package com.janetfilter.core.rulers

/**
 * 完全匹配器
 *
 * @param ignoreCase 是否忽略大小写
 */
class EqualRuler(private val ignoreCase: Boolean) : Ruler {
    override fun test(rule: String, input: String): Boolean {
        return if (ignoreCase) {
            rule.equals(input, ignoreCase = true)
        } else {
            rule == input
        }
    }
}