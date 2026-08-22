/*
 * ============================================================================
 * - CommandLine.kt - 命令行工具入口
 * ----------------------------------------------------------------------------
 * 提供独立运行时的命令行工具：
 *   - 验证激活码格式
 *   - 解析 power.conf
 *   - 生成新激活码（需要 ckey.run 私钥）
 * ============================================================================
 */
package com.janetfilter.core.util

import java.io.File

/**
 * 命令行工具入口
 */
object CommandLine {

    /**
     * 主入口
     */
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            printUsage()
            return
        }

        when (args[0]) {
            "verify" -> verifyCode(args.getOrNull(1))
            "parse" -> parsePowerConfig(args.getOrNull(1))
            "generate" -> generateCode(args)
            else -> printUsage()
        }
    }

    /**
     * 验证激活码格式
     */
    private fun verifyCode(code: String?) {
        if (code == null) {
            println("Usage: verify <activation-code>")
            return
        }
        val valid = ActivationCodeGenerator.validateFormat(code)
        println("Format: ${if (valid) "VALID" else "INVALID"}")

        if (valid) {
            val parsed = ActivationCodeGenerator.parse(code)
            if (parsed != null) {
                val (lid, payload, sig) = parsed
                println("License ID: $lid")
                println("Payload (base64): ${payload.take(50)}...")
                println("Signature (base64): ${sig.take(50)}...")

                try {
                    val decoded = ActivationCodeGenerator.decodePayload(payload)
                    println("Payload (decoded):")
                    println(decoded)
                } catch (e: Exception) {
                    println("Payload decode failed: ${e.message}")
                }
            }
        }
    }

    /**
     * 解析 power.conf
     */
    private fun parsePowerConfig(path: String?) {
        if (path == null) {
            println("Usage: parse <power.conf>")
            return
        }
        val file = File(path)
        if (!file.exists()) {
            println("File not found: $path")
            return
        }
        val key = PowerConfigParser.extractPublicKey(file)
        if (key == null) {
            println("Failed to extract RSA public key")
            return
        }
        println("RSA Public Key:")
        println("  Exponent (e): ${key.exponent}")
        println("  Modulus (n): ${key.modulus}")
    }

    /**
     * 生成激活码（占位实现，需要 ckey.run 私钥）
     */
    private fun generateCode(args: Array<String>) {
        println("Activation code generation requires ckey.run private key.")
        println("This tool only validates existing codes.")
        println("Use https://ckey.run to generate codes online.")
    }

    /**
     * 打印使用说明
     */
    private fun printUsage() {
        println("ja-netfilter utility tool")
        println("")
        println("Usage:")
        println("  verify <code>     - Verify activation code format")
        println("  parse <file>      - Parse power.conf and extract RSA key")
        println("  generate          - Show generation instructions")
    }
}