/*
 * ============================================================================
 * - PrefixRuler.kt - 前缀匹配器
 * ----------------------------------------------------------------------------
 * 用于 PREFIX 和 PREFIX_IC 两种规则类型。
 *
 * PREFIX    : String.startsWith()
 * PREFIX_IC : String.startsWith(ignoreCase = true)
 * ============================================================================
 */
package com.janetfilter.core.rulers

/**
 * 前缀匹配器
 *
 * @param ignoreCase 是否忽略大小写
 */
class PrefixRuler(private val ignoreCase: Boolean) : Ruler {
    override fun test(rule: String, input: String): Boolean {
        return input.startsWith(rule, ignoreCase)
    }
}