/*
 * ============================================================================
 * - PowerPlugin.kt - 大数运算插件入口
 * ============================================================================
 */
package com.janetfilter.plugins.power

import com.janetfilter.core.Environment
import com.janetfilter.core.plugin.MyTransformer
import com.janetfilter.core.plugin.PluginConfig
import com.janetfilter.core.plugin.PluginEntry

/**
 * 大数运算过滤插件入口
 */
class PowerPlugin : PluginEntry {

    private companion object {
        const val PLUGIN_NAME = "power"
    }

    private val myTransformers: MutableList<MyTransformer> = mutableListOf()

    override fun init(env: Environment, config: PluginConfig?) {
        myTransformers.clear()
        if (config != null) {
            // [Result] section: 用于修改 modPow 结果
            val resultRules = config.getBySection("Result")
            // [Args] section: 用于修改 modPow 参数
            val argsRules = config.getBySection("Args")
            myTransformers.add(ResultTransformer(resultRules))
            myTransformers.add(ArgsTransformer(argsRules))
        } else {
            myTransformers.add(ResultTransformer(emptyList()))
            myTransformers.add(ArgsTransformer(emptyList()))
        }
    }

    override val name: String = PLUGIN_NAME
    override val author: String = "neo"
    override val version: String = "1.0.0"
    override val description: String = "BigInteger.modPow RSA signature substitution"

    override val transformers: List<MyTransformer>
        get() = myTransformers.toList()
}