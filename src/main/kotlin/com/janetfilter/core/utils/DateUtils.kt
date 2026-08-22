/*
 * ============================================================================
 * - DateUtils.kt - 日期工具类
 * ----------------------------------------------------------------------------
 * 提供日期格式化、解析等常用方法。
 * ============================================================================
 */
package com.janetfilter.core.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 日期工具类
 */
object DateUtils {
    /** 标准日期格式：yyyy-MM-dd */
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * 格式化 LocalDate 为 yyyy-MM-dd
     */
    @JvmStatic
    fun format(date: LocalDate): String {
        return date.format(DATE_FORMATTER)
    }

    /**
     * 解析 yyyy-MM-dd 为 LocalDate
     */
    @JvmStatic
    fun parse(text: String): LocalDate {
        return LocalDate.parse(text, DATE_FORMATTER)
    }
}