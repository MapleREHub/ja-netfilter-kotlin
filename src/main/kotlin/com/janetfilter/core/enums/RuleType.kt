/*
 * ============================================================================
 * - RuleType.kt - 规则类型枚举
 * ----------------------------------------------------------------------------
 * 定义 9 种规则匹配模式，支持大小写敏感（_IC 后缀表示 Ignore Case）：
 *
 *   PREFIX      : 前缀匹配
 *   PREFIX_IC   : 前缀匹配（忽略大小写）
 *   SUFFIX      : 后缀匹配
 *   SUFFIX_IC   : 后缀匹配（忽略大小写）
 *   KEYWORD     : 包含匹配
 *   KEYWORD_IC  : 包含匹配（忽略大小写）
 *   EQUAL       : 完全匹配
 *   EQUAL_IC    : 完全匹配（忽略大小写）
 *   REGEXP      : 正则匹配
 *
 * 每个枚举值绑定一个 Ruler 实例（策略模式）。
 * ============================================================================
 */
package com.janetfilter.core.enums

import com.janetfilter.core.rulers.EqualRuler
import com.janetfilter.core.rulers.KeywordRuler
import com.janetfilter.core.rulers.PrefixRuler
import com.janetfilter.core.rulers.RegExpRuler
import com.janetfilter.core.rulers.Ruler
import com.janetfilter.core.rulers.SuffixRuler

/**
 * 规则类型枚举
 *
 * 每个枚举值关联一个 Ruler（匹配器），用于判断输入字符串是否符合规则。
 */
enum class RuleType(private val ruler: Ruler) {
    /** 前缀匹配（大小写敏感） */
    PREFIX(PrefixRuler(false)),

    /** 前缀匹配（忽略大小写） */
    PREFIX_IC(PrefixRuler(true)),

    /** 后缀匹配（大小写敏感） */
    SUFFIX(SuffixRuler(false)),

    /** 后缀匹配（忽略大小写） */
    SUFFIX_IC(SuffixRuler(true)),

    /** 包含匹配（大小写敏感） */
    KEYWORD(KeywordRuler(false)),

    /** 包含匹配（忽略大小写） */
    KEYWORD_IC(KeywordRuler(true)),

    /** 完全匹配（大小写敏感） */
    EQUAL(EqualRuler(false)),

    /** 完全匹配（忽略大小写） */
    EQUAL_IC(EqualRuler(true)),

    /** 正则匹配 */
    REGEXP(RegExpRuler());

    /**
     * 获取关联的 Ruler
     */
    fun getRuler(): Ruler = ruler
}