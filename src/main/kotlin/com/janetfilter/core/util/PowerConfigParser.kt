/*
 * ============================================================================
 * - PowerConfigParser.kt - power.conf 解析器
 * ----------------------------------------------------------------------------
 * 解析 power.conf 文件，提取 RSA 公钥参数 (e, n)。
 *
 * 文件格式：
 *   [Result]
 *   EQUAL,<sig>,<e>,<n>-><expected_result>
 *
 * 第一条规则即为 RSA 公钥。
 * ============================================================================
 */
package com.janetfilter.core.util

import com.janetfilter.core.commons.ConfigParser
import com.janetfilter.core.models.FilterRule
import java.io.File
import java.math.BigInteger

/**
 * power.conf 解析器
 */
object PowerConfigParser {

    /**
     * RSA 公钥参数
     */
    data class RSAPublicKey(
        /** 指数 */
        val exponent: BigInteger,
        /** 模数 */
        val modulus: BigInteger
    )

    /**
     * 从 power.conf 中提取 RSA 公钥
     *
     * @param file power.conf 文件
     * @return RSA 公钥
     */
    @JvmStatic
    fun extractPublicKey(file: File): RSAPublicKey? {
        val data = ConfigParser.parse(file)
        val rules = data["Result"] ?: data["result"] ?: return null
        if (rules.isEmpty()) return null

        // 第一条规则的格式：sig,e,n->result
        val firstRule = rules[0]
        val parts = firstRule.rule.split(",", limit = 3)
        if (parts.size < 3) return null

        return try {
            RSAPublicKey(
                exponent = BigInteger(parts[1]),
                modulus = BigInteger(parts[2])
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 列出所有签名替换规则
     */
    @JvmStatic
    fun listResultRules(file: File): List<FilterRule> {
        val data = ConfigParser.parse(file)
        return data["Result"] ?: emptyList()
    }
}