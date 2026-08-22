/*
 * ============================================================================
 * - RegExpRuler.kt - 正则匹配器
 * ----------------------------------------------------------------------------
 * 用于 REGEXP 规则类型。
 *
 * 使用 java.util.regex.Pattern 编译并匹配。
 * 注意：正则表达式会被缓存以提高性能。
 * ============================================================================
 */
package com.janetfilter.core.rulers

import java.util.concurrent.ConcurrentHashMap

/**
 * 正则匹配器
 *
 * 使用 ConcurrentHashMap 缓存编译后的 Pattern，避免重复编译。
 */
class RegExpRuler : Ruler {
    companion object {
        private val patternCache = ConcurrentHashMap<String, java.util.regex.Pattern>()
    }

    override fun test(rule: String, input: String): Boolean {
        val pattern = patternCache.computeIfAbsent(rule) {
            try {
                java.util.regex.Pattern.compile(it)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid regex pattern: $rule", e)
            }
        }
        return pattern.matcher(input).find()
    }
}