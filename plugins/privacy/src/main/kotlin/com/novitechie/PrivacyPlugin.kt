/*
 * ============================================================================
 * - PrivacyPlugin.kt - 隐私过滤插件入口
 * ============================================================================
 */
package com.novitechie

import com.janetfilter.core.Environment
import com.janetfilter.core.plugin.MyTransformer
import com.janetfilter.core.plugin.PluginConfig
import com.janetfilter.core.plugin.PluginEntry

/**
 * 隐私过滤插件入口
 *
 * 该插件通过拦截 JetBrains 内部类来阻止隐私/遥测行为。
 * 包括：
 *   - 数据上报
 *   - 用户行为跟踪
 *   - 异常信息收集
 */
class PrivacyPlugin : PluginEntry {

    private companion object {
        const val PLUGIN_NAME = "privacy"
    }

    private val myTransformers: MutableList<MyTransformer> = mutableListOf()

    override fun init(env: Environment, config: PluginConfig?) {
        myTransformers.clear()
        // 注册所有 transformer
        myTransformers.add(LicensingFacadeTransformer())
        myTransformers.add(ClassTransformer())
        myTransformers.add(CollectionsTransformer())
        myTransformers.add(MethodTransformer())
        myTransformers.add(ClassLoaderTransformer())
        myTransformers.add(PluginClassLoaderTransformer())
        myTransformers.add(PluginManagerCoreTransformer())
    }

    override val name: String = PLUGIN_NAME
    override val author: String = "neo"
    override val version: String = "1.0.0"
    override val description: String = "Block JetBrains privacy/telemetry features"

    override val transformers: List<MyTransformer>
        get() = myTransformers.toList()
}