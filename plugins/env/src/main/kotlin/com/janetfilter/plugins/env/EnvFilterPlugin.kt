/*
 * ============================================================================
 * - EnvFilterPlugin.kt - 环境变量过滤插件入口
 * ============================================================================
 */
package com.janetfilter.plugins.env

import com.janetfilter.core.Environment
import com.janetfilter.core.plugin.MyTransformer
import com.janetfilter.core.plugin.PluginConfig
import com.janetfilter.core.plugin.PluginEntry

/**
 * 环境变量过滤插件入口
 */
class EnvFilterPlugin : PluginEntry {

    private companion object {
        const val PLUGIN_NAME = "env"
    }

    private val myTransformers: MutableList<MyTransformer> = mutableListOf()
    private var rules = emptyList<com.janetfilter.core.models.FilterRule>()

    override fun init(env: Environment, config: PluginConfig?) {
        if (config != null) {
            rules = config.getBySection("ENV") + config.getBySection("env")
        }
        myTransformers.clear()
        myTransformers.add(ProcessEnvironmentTransformer(rules))
    }

    override val name: String = PLUGIN_NAME
    override val author: String = "neo"
    override val version: String = "1.0.0"
    override val description: String = "Environment variable filter"

    override val transformers: List<MyTransformer>
        get() = myTransformers.toList()
}