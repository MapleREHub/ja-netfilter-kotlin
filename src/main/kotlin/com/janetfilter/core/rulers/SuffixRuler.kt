/*
 * ============================================================================
 * - SuffixRuler.kt - 后缀匹配器
 * ----------------------------------------------------------------------------
 * 用于 SUFFIX 和 SUFFIX_IC 两种规则类型。
 *
 * SUFFIX    : String.endsWith()
 * SUFFIX_IC : String.endsWith(ignoreCase = true)
 * ============================================================================
 */
package com.janetfilter.core.rulers

/**
 * 后缀匹配器
 *
 * @param ignoreCase 是否忽略大小写
 */
class SuffixRuler(private val ignoreCase: Boolean) : Ruler {
    override fun test(rule: String, input: String): Boolean {
        return input.endsWith(rule, ignoreCase)
    }
}