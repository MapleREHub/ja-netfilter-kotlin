/*
 * ============================================================================
 * - StringUtils.kt - 字符串工具类
 * ----------------------------------------------------------------------------
 * 提供字符串处理常用方法。
 * ============================================================================
 */
package com.janetfilter.core.utils

/**
 * 字符串工具类
 */
object StringUtils {

    /**
     * 判断字符串是否为空或 null
     */
    @JvmStatic
    fun isEmpty(str: String?): Boolean {
        return str.isNullOrEmpty()
    }

    /**
     * 判断字符串是否非空
     */
    @JvmStatic
    fun isNotEmpty(str: String?): Boolean {
        return !str.isNullOrEmpty()
    }

    /**
     * 安全相等比较（处理 null）
     */
    @JvmStatic
    fun equals(a: String?, b: String?): Boolean {
        return a == b
    }
}