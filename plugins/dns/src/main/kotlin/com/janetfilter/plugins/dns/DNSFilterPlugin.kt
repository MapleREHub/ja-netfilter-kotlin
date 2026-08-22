/*
 * ============================================================================
 * - DNSFilterPlugin.kt - DNS 过滤插件入口
 * ----------------------------------------------------------------------------
 * 该类是插件的入口点，被 PluginManager 加载。
 *
 * 工作流程：
 *   1. PluginManager 实例化 DNSFilterPlugin
 *   2. 调用 init() 加载配置
 *   3. 调用 getTransformers() 获取 transformer 列表
 *   4. PluginManager 注册 transformer 到 Dispatcher
 * ============================================================================
 */
package com.janetfilter.plugins.dns

import com.janetfilter.core.Environment
import com.janetfilter.core.plugin.MyTransformer
import com.janetfilter.core.plugin.PluginConfig
import com.janetfilter.core.plugin.PluginEntry

/**
 * DNS 过滤插件入口
 */
class DNSFilterPlugin : PluginEntry {

    private companion object {
        const val PLUGIN_NAME = "dns"
    }

    /** transformer 列表 */
    private val myTransformers: MutableList<MyTransformer> = mutableListOf()

    /** 规则列表（缓存） */
    private var rules = emptyList<com.janetfilter.core.models.FilterRule>()

    override fun init(env: Environment, config: PluginConfig?) {
        // 从配置中加载规则
        if (config != null) {
            rules = config.getBySection("DNS") + config.getBySection("Url") + config.getBySection("dns")
        }
        // 创建 transformer
        myTransformers.clear()
        myTransformers.add(InetAddressTransformer(rules))
    }

    override val name: String = PLUGIN_NAME
    override val author: String = "neo"
    override val version: String = "1.0.0"
    override val description: String = "DNS query filter for blocking jetbrains.com"

    override val transformers: List<MyTransformer>
        get() = myTransformers.toList()
}