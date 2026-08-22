/*
 * ============================================================================
 * - PluginConfig.kt - 插件配置包装类
 * ----------------------------------------------------------------------------
 * 包装一个 .conf 配置文件，格式：
 *
 *   [SectionName]
 *   TYPE,rule_pattern
 *   ...
 *
 * 每个 section 对应插件内部的一个配置分组。
 * 例如 power.conf 的 [Result] 和 [Args]。
 * ============================================================================
 */
package com.janetfilter.core.plugin

import com.janetfilter.core.models.FilterRule
import java.io.File

/**
 * 插件配置包装类
 *
 * 保存一个 .conf 配置文件解析后的所有 section 及其规则列表。
 *
 * @param file 配置文件
 * @param data 解析后的数据：section 名 -> 规则列表
 */
class PluginConfig(
    /** 配置文件对象 */
    val file: File,

    /** 解析后的数据 */
    val data: Map<String, List<FilterRule>>
) {
    /**
     * 获取指定 section 的规则列表
     *
     * @param section section 名（如 "Result"、"Args"、"DNS"）
     * @return 规则列表（不包含则返回空列表）
     */
    fun getBySection(section: String): List<FilterRule> {
        return data[section] ?: emptyList()
    }
}