/*
 * ============================================================================
 * - PluginEntry.kt - 插件入口接口
 * ----------------------------------------------------------------------------
 * 所有插件的主类必须实现该接口。
 *
 * 插件生命周期：
 *   1. PluginManager 实例化插件（无参构造）
 *   2. 调用 init(env, config) 传递环境和配置
 *   3. 调用 getTransformers() 获取所有 transformer
 *   4. PluginManager 将 transformer 注册到 Dispatcher
 * ============================================================================
 */
package com.janetfilter.core.plugin

import com.janetfilter.core.Environment

/**
 * 插件入口接口
 *
 * 任何 ja-netfilter 插件的 jar 必须包含一个实现该接口的类，
 * 该类的完全限定名通过 Manifest 的 Main-Class 或通过反射扫描得到。
 *
 * 命名约定：<PluginName>Plugin（如 DNSFilterPlugin、PowerPlugin）
 */
interface PluginEntry {

    /**
     * 初始化插件
     *
     * 在此方法中读取 config 中的规则，初始化内部状态。
     *
     * @param env 环境上下文
     * @param config 插件配置（如果插件不需要配置，可能为 null）
     */
    fun init(env: Environment, config: PluginConfig?) {}

    /**
     * 插件名称
     *
     * @return 简短名称，如 "dns"
     */
    val name: String

    /**
     * 插件作者
     */
    val author: String

    /**
     * 插件版本
     */
    val version: String get() = "1.0.0"

    /**
     * 插件描述
     */
    val description: String get() = ""

    /**
     * 获取插件包含的所有 transformer
     *
     * @return transformer 列表
     */
    val transformers: List<MyTransformer>
}