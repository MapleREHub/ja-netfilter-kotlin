/*
 * ============================================================================
 * - URLFilterPlugin.kt - URL 过滤插件入口
 * ============================================================================
 */
package com.janetfilter.plugins.url

import com.janetfilter.core.Environment
import com.janetfilter.core.plugin.MyTransformer
import com.janetfilter.core.plugin.PluginConfig
import com.janetfilter.core.plugin.PluginEntry

/**
 * URL 过滤插件入口
 */
class URLFilterPlugin : PluginEntry {

    private companion object {
        const val PLUGIN_NAME = "url"
    }

    private val myTransformers: MutableList<MyTransformer> = mutableListOf()
    private var rules = emptyList<com.janetfilter.core.models.FilterRule>()

    override fun init(env: Environment, config: PluginConfig?) {
        if (config != null) {
            rules = config.getBySection("URL") + config.getBySection("url")
        }
        myTransformers.clear()
        myTransformers.add(HttpClientTransformer(rules))
    }

    override val name: String = PLUGIN_NAME
    override val author: String = "neo"
    override val version: String = "1.0.0"
    override val description: String = "HTTP request URL filter"

    override val transformers: List<MyTransformer>
        get() = myTransformers.toList()
}