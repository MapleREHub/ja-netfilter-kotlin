/*
 * ============================================================================
 * - Initializer.kt - 框架初始化器
 * ----------------------------------------------------------------------------
 * 负责整个 ja-netfilter 框架的初始化流程：
 *   1. 创建 Dispatcher（字节码分派器）
 *   2. 注册 Dispatcher 到 Instrumentation
 *   3. 创建 PluginManager 并加载所有插件
 *   4. 加载配置文件（每个插件的 .conf）
 * ============================================================================
 */
package com.janetfilter.core

import com.janetfilter.core.commons.ConfigParser
import com.janetfilter.core.commons.DebugInfo
import com.janetfilter.core.plugin.PluginConfig
import com.janetfilter.core.plugin.PluginManager
import java.io.File

/**
 * 框架初始化器
 *
 * 单一静态方法 init(env)，由 Launcher.premain() 调用。
 */
object Initializer {

    /**
     * 初始化框架
     *
     * 流程：
     *   1. 创建 Dispatcher
     *   2. 注册到 Instrumentation（addTransformer）
     *   3. 加载配置目录中的所有 .conf 文件
     *   4. 创建 PluginManager，加载 plugins 目录中的所有插件 jar
     *
     * @param env 环境上下文
     */
    @JvmStatic
    fun init(env: Environment) {
        DebugInfo.info("Initializing ja-netfilter...")
        DebugInfo.info("Environment: $env")

        try {
            // 1. 创建 Dispatcher
            val dispatcher = Dispatcher(env)
            DebugInfo.info("Dispatcher created.")

            // 2. 注册到 Instrumentation
            //    注意：attach 模式下不能使用 premain 转换，必须显式调用 retransformClasses
            if (env.isJavaagentMode()) {
                env.getInstrumentation().addTransformer(dispatcher, true)
                DebugInfo.info("Dispatcher registered as ClassFileTransformer.")
            } else {
                env.getInstrumentation().addTransformer(dispatcher, true)
                DebugInfo.info("Dispatcher registered in attach mode.")
            }

            // 3. 加载所有配置文件
            val configMap = loadConfigs(env.configDir)
            DebugInfo.info("Loaded ${configMap.size} config files: ${configMap.keys}")

            // 4. 创建 PluginManager，加载插件
            val pluginManager = PluginManager(dispatcher, env)
            pluginManager.loadPlugins(configMap)

            DebugInfo.info("Initialization complete.")
        } catch (e: Exception) {
            DebugInfo.error("Initialization failed", e)
            throw e
        }
    }

    /**
     * 加载配置目录中的所有 .conf 文件
     *
     * 每个配置文件对应一个同名插件，例如：
     *   config-jetbrains/
     *     power.conf -> 加载到 power 插件
     *     dns.conf   -> 加载到 dns 插件
     *
     * @param configDir 配置目录
     * @return Map<configFileName, PluginConfig>
     */
    private fun loadConfigs(configDir: File): Map<String, PluginConfig> {
        val result = mutableMapOf<String, PluginConfig>()
        if (!configDir.exists() || !configDir.isDirectory) {
            DebugInfo.warn("Config dir does not exist: $configDir")
            return result
        }

        val files = configDir.listFiles { f -> f.isFile && f.name.endsWith(".conf") }
            ?: return result

        for (file in files) {
            try {
                val data = ConfigParser.parse(file)
                val pluginName = file.nameWithoutExtension
                result[pluginName] = PluginConfig(file, data)
                DebugInfo.debug("Loaded config: ${file.name} (sections: ${data.keys})")
            } catch (e: Exception) {
                DebugInfo.error("Failed to parse config: ${file.name}", e)
            }
        }

        return result
    }
}