/*
 * ============================================================================
 * - PluginManager.kt - 插件管理器
 * ----------------------------------------------------------------------------
 * 负责扫描 plugins 目录，加载所有 .jar 插件。
 *
 * 加载流程：
 *   1. 扫描 plugins 目录，过滤出 .jar 文件
 *   2. 对每个 .jar 文件，用 PluginClassLoader 加载
 *   3. 从 MANIFEST 中读取 Plugin-Class 或扫描 META-INF/services
 *   4. 实例化 PluginEntry，传入环境和配置
 *   5. 获取 transformer 列表，注册到 Dispatcher
 * ============================================================================
 */
package com.janetfilter.core.plugin

import com.janetfilter.core.Dispatcher
import com.janetfilter.core.Environment
import com.janetfilter.core.commons.DebugInfo
import java.io.File
import java.util.jar.JarFile

/**
 * 插件管理器
 *
 * @param dispatcher 字节码分派器
 * @param environment 环境上下文
 */
class PluginManager(
    private val dispatcher: Dispatcher,
    private val environment: Environment
) {

    /**
     * 加载所有插件
     *
     * @param configMap 配置文件映射（key 为 config 文件名，不含后缀）
     */
    fun loadPlugins(configMap: Map<String, PluginConfig>) {
        val pluginsDir = environment.pluginsDir
        if (!pluginsDir.exists() || !pluginsDir.isDirectory) {
            DebugInfo.warn("Plugins directory does not exist: $pluginsDir")
            return
        }

        val jarFiles = pluginsDir.listFiles { f ->
            f.isFile && f.name.endsWith(".jar")
        } ?: emptyArray()

        DebugInfo.info("Found ${jarFiles.size} plugin jars in $pluginsDir")

        for (jarFile in jarFiles) {
            try {
                loadPlugin(jarFile, configMap)
            } catch (e: Exception) {
                DebugInfo.error("Failed to load plugin: ${jarFile.name}", e)
            }
        }
    }

    /**
     * 加载单个插件
     */
    private fun loadPlugin(jarFile: File, configMap: Map<String, PluginConfig>) {
        val jarFileObj = JarFile(jarFile)
        try {
            // 1. 创建插件类加载器
            val classLoader = PluginClassLoader(jarFileObj)

            // 2. 从 manifest 读取插件类名
            val manifest = jarFileObj.manifest
            val pluginClassName = manifest.mainAttributes.getValue("Plugin-Class")
                ?: ENTRY_NAME // fallback to default

            // 3. 加载插件类
            val pluginClass = classLoader.loadPluginClass(pluginClassName)

            // 4. 实例化插件
            val pluginEntry = pluginClass.getDeclaredConstructor().newInstance() as PluginEntry

            // 5. 查找配置（config 文件名应与 jar 文件名匹配，如 power.jar -> power.conf）
            val configName = jarFile.nameWithoutExtension
            val config = configMap[configName]

            // 6. 初始化插件
            pluginEntry.init(environment, config)

            // 7. 注册 transformer
            val transformers = pluginEntry.transformers
            dispatcher.addTransformers(transformers)

            DebugInfo.info("Plugin loaded: ${pluginEntry.name} v${pluginEntry.version} by ${pluginEntry.author} (${transformers.size} transformers)")
        } finally {
            jarFileObj.close()
        }
    }

    companion object {
        /** 默认插件类名（fallback） */
        private const val ENTRY_NAME = "Plugin"
    }
}