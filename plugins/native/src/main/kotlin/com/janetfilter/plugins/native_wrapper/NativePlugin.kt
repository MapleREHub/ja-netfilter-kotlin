/*
 * ============================================================================
 * - NativePlugin.kt - Native 方法包装插件入口
 * ============================================================================
 */
package com.janetfilter.plugins.native_wrapper

import com.janetfilter.core.Environment
import com.janetfilter.core.plugin.MyTransformer
import com.janetfilter.core.plugin.PluginConfig
import com.janetfilter.core.plugin.PluginEntry

/**
 * Native 方法包装插件入口
 */
class NativePlugin : PluginEntry {

    private companion object {
        const val PLUGIN_NAME = "native"
    }

    private val myTransformers: MutableList<MyTransformer> = mutableListOf()

    override fun init(env: Environment, config: PluginConfig?) {
        myTransformers.clear()
        myTransformers.add(WrapperTransformer(env, config?.getBySection("Class") ?: emptyList()))
    }

    override val name: String = PLUGIN_NAME
    override val author: String = "neo"
    override val version: String = "1.0.0"
    override val description: String = "JNI native method wrapper for class redefinition"

    override val transformers: List<MyTransformer>
        get() = myTransformers.toList()
}