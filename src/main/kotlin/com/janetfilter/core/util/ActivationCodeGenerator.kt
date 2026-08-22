/*
 * ============================================================================
 * - ActivationCodeGenerator.kt - 激活码生成器
 * ----------------------------------------------------------------------------
 * 用于生成 ckey.run 兼容的 JetBrains 激活码。
 *
 * 激活码格式：
 *   LICENSE_ID-BASE64_PAYLOAD-BASE64_SIGNATURE
 *
 *   其中：
 *     LICENSE_ID : 10 位 hex（licenseId）
 *     PAYLOAD    : base64(JSON)
 *     SIGNATURE  : base64(RSA 私钥签名的 SHA-256(PAYLOAD))
 *
 * JSON 结构：
 *   {
 *     "licenseId": "7FB23A91A2",
 *     "licenseeName": "ckey.run",
 *     "assigneeName": "",
 *     "products": [
 *       {"code": "PS", "fallbackDate": "2099-12-31", "paidUpTo": "2099-12-31"},
 *       ...
 *     ],
 *     "metadata": "0120230914PSAX000005"
 *   }
 *
 * 注意：实际使用时需要从 power.conf 提取 (e, n) 并反向求解 (d) 才能签名
 * 这里使用 ckey.run 自带的 RSA 私钥（如果可用）
 * ============================================================================
 */
package com.janetfilter.core.util

import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.spec.RSAPrivateKeySpec
import java.util.Base64
import java.util.UUID

/**
 * 激活码生成器
 *
 * 用于生成 ckey.run 兼容的激活码。
 */
object ActivationCodeGenerator {

    /**
     * 激活码产品信息
     */
    data class ProductInfo(
        /** 产品代码，如 "PS", "IU", "WS" */
        val code: String,
        /** 回退日期 */
        val fallbackDate: String = "2099-12-31",
        /** 付费截止日期 */
        val paidUpTo: String = "2099-12-31"
    )

    /**
     * 生成激活码
     *
     * @param products 产品列表
     * @param licenseeName 许可证持有人
     * @param metadata 元数据（可选）
     * @param licenseId 许可证 ID（可选，默认随机生成）
     * @param privateKey RSA 私钥（可选，无则生成演示码）
     * @return 激活码字符串
     */
    @JvmStatic
    fun generate(
        products: List<ProductInfo>,
        licenseeName: String = "ckey.run",
        metadata: String? = null,
        licenseId: String? = null,
        privateKey: PrivateKey? = null
    ): String {
        // 1. 生成 licenseId
        val lid = licenseId ?: randomLicenseId()

        // 2. 构造 JSON payload
        val payload = buildPayload(lid, licenseeName, products, metadata)

        // 3. base64 编码
        val payloadBase64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.toByteArray(Charsets.UTF_8))

        // 4. 签名
        val signature = if (privateKey != null) {
            signPayload(payload.toByteArray(Charsets.UTF_8), privateKey)
        } else {
            // 无私钥时，生成占位签名
            "PLACEHOLDER_SIGNATURE_WITHOUT_PRIVATE_KEY"
        }

        // 5. 拼接最终激活码
        return "$lid-$payloadBase64-$signature"
    }

    /**
     * 生成随机 licenseId
     */
    private fun randomLicenseId(): String {
        // 10 位大写 hex
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).uppercase()
    }

    /**
     * 构造 payload JSON
     */
    private fun buildPayload(
        licenseId: String,
        licenseeName: String,
        products: List<ProductInfo>,
        metadata: String?
    ): String {
        val productsJson = products.joinToString(",") { p ->
            """{"code":"${p.code}","fallbackDate":"${p.fallbackDate}","paidUpTo":"${p.paidUpTo}"}"""
        }
        val meta = metadata ?: "01${currentDate()}${products[0].code}X000000"

        return """{"licenseId":"$licenseId","licenseeName":"$licenseeName","assigneeName":"","products":[$productsJson],"metadata":"$meta"}"""
    }

    /**
     * 获取当前日期字符串 (yyyyMMdd)
     */
    private fun currentDate(): String {
        val now = java.time.LocalDateTime.now()
        return "%04d%02d%02d".format(now.year, now.monthValue, now.dayOfMonth)
    }

    /**
     * 签名 payload
     */
    private fun signPayload(payload: ByteArray, privateKey: PrivateKey): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(payload)

        val signer = java.security.Signature.getInstance("SHA256withRSA")
        signer.initSign(privateKey)
        signer.update(digest)
        val signature = signer.sign()

        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature)
    }

    /**
     * 从 RSA 公钥参数构造私钥
     *
     * 用于从 power.conf 中提取的 (e, n) 反推 (d)
     *
     * 注意：仅当 p 是素数时才能正确计算 d
     * 对于 JetBrains 使用的小 e（通常是 65537），从公钥求解私钥是计算上不可行的
     * 因此本函数主要用于测试目的
     *
     * @param e 公钥指数
     * @param n 公钥模数
     * @param d 私钥指数
     * @return RSA 私钥
     */
    @JvmStatic
    fun buildPrivateKey(e: BigInteger, n: BigInteger, d: BigInteger): PrivateKey {
        val keySpec = RSAPrivateKeySpec(n, d)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(keySpec)
    }

    /**
     * 验证激活码格式
     *
     * @param code 激活码字符串
     * @return true 表示格式有效
     */
    @JvmStatic
    fun validateFormat(code: String): Boolean {
        val parts = code.split("-")
        if (parts.size < 3) return false
        // 第一段是 licenseId
        if (parts[0].length != 10) return false
        return true
    }

    /**
     * 解析激活码
     *
     * @param code 激活码字符串
     * @return Triple<licenseId, payloadBase64, signatureBase64>
     */
    @JvmStatic
    fun parse(code: String): Triple<String, String, String>? {
        val parts = code.split("-", limit = 3)
        if (parts.size != 3) return null
        return Triple(parts[0], parts[1], parts[2])
    }

    /**
     * 解码 payload
     */
    @JvmStatic
    fun decodePayload(payloadBase64: String): String {
        val bytes = Base64.getUrlDecoder().decode(payloadBase64)
        return String(bytes, Charsets.UTF_8)
    }
}