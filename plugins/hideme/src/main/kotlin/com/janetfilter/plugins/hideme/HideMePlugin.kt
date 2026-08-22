/*
 * ============================================================================
 * - HideMePlugin.kt - 隐藏模式插件入口
 * ============================================================================
 */
package com.janetfilter.plugins.hideme

import com.janetfilter.core.Environment
import com.janetfilter.core.plugin.MyTransformer
import com.janetfilter.core.plugin.PluginConfig
import com.janetfilter.core.plugin.PluginEntry

/**
 * 隐藏模式插件入口
 */
class HideMePlugin : PluginEntry {

    private companion object {
        const val PLUGIN_NAME = "hideme"
    }

    private val myTransformers: MutableList<MyTransformer> = mutableListOf()
    private lateinit var environment: Environment

    override fun init(env: Environment, config: PluginConfig?) {
        environment = env
        myTransformers.clear()
        myTransformers.add(VMTransformer(env))
        myTransformers.add(ClassNameTransformer())
    }

    override val name: String = PLUGIN_NAME
    override val author: String = "neo"
    override val version: String = "1.0.0"
    override val description: String = "Hide ja-netfilter traces from VM/Class inspection"

    override val transformers: List<MyTransformer>
        get() = myTransformers.toList()
}