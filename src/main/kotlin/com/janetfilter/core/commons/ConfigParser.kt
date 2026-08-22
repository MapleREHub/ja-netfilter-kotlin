/*
 * ============================================================================
 * - ConfigParser.kt - 配置文件解析器
 * ----------------------------------------------------------------------------
 * 解析 .conf 格式的配置文件：
 *
 *   # 这是注释
 *   [Section1]
 *   EQUAL,rule1
 *   PREFIX,rule2
 *
 *   [Section2]
 *   KEYWORD,rule3
 *
 * 返回 Map<sectionName, List<FilterRule>>。
 * ============================================================================
 */
package com.janetfilter.core.commons

import com.janetfilter.core.models.FilterRule
import java.io.File

/**
 * 配置文件解析器
 *
 * 单例工具类，提供 parse(File) 静态方法。
 */
object ConfigParser {

    /**
     * 解析配置文件
     *
     * @param file 配置文件
     * @return 解析结果：Map<sectionName, List<FilterRule>>
     * @throws Exception 如果文件读取失败
     */
    @JvmStatic
    @Throws(Exception::class)
    fun parse(file: File): Map<String, List<FilterRule>> {
        val result = mutableMapOf<String, MutableList<FilterRule>>()
        var currentSection: String? = null

        file.useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()

                // 跳过空行和注释
                if (trimmed.isEmpty() || trimmed.startsWith(";")) continue

                // 解析 [ section ]
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    val section = trimmed.substring(1, trimmed.length - 1).trim()
                    currentSection = section
                    if (section.isNotEmpty()) {
                        result.getOrPut(section) { mutableListOf() }
                    }
                    continue
                }

                // 解析 TYPE,rule
                val section = currentSection ?: continue

                val parts = trimmed.split(",", limit = 2)
                if (parts.size < 2) continue

                try {
                    val rule = FilterRule.of(parts[0].trim(), parts[1].trim())
                    result.getOrPut(section) { mutableListOf() }.add(rule)
                } catch (e: Exception) {
                    DebugInfo.warn("Failed to parse rule '$trimmed' in section '$section'", e)
                }
            }
        }

        return result
    }
}