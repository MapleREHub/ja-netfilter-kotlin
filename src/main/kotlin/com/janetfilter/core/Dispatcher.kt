/*
 * ============================================================================
 * - Dispatcher.kt - 字节码分派器
 * ----------------------------------------------------------------------------
 * 实现 java.lang.instrument.ClassFileTransformer 接口，是整个框架的核心。
 *
 * 工作原理：
 *   1. 维护一份 "类名 -> MyTransformer列表" 的映射
 *   2. 维护一份 "全局 transformer" 列表（不区分类名）
 *   3. JVM 加载每个类时都会调用本类的 transform() 方法
 *   4. transform() 根据类名分派到对应的 transformer
 *
 * 这是整个字节码 hook 系统的调度中心。
 * ============================================================================
 */
package com.janetfilter.core

import com.janetfilter.core.commons.DebugInfo
import com.janetfilter.core.plugin.MyTransformer
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

/**
 * 字节码分派器
 *
 * 实现了 ClassFileTransformer，作为 Instrumentation.addTransformer 的回调。
 * 维护所有 transformer 并按类名分派。
 *
 * @param environment 环境上下文
 */
class Dispatcher(private val environment: Environment) : ClassFileTransformer {

    /** 已注册的所有类名集合（用于快速查找） */
    private val classSet: MutableSet<String> = mutableSetOf()

    /** 类名到 transformer 列表的映射 */
    private val transformerMap: MutableMap<String, MutableList<MyTransformer>> = mutableMapOf()

    /** 全局 transformer 列表（无论什么类都执行） */
    private val globalTransformers: MutableList<MyTransformer> = mutableListOf()

    /** 管理层 transformer 列表（attach 模式专用） */
    private val manageTransformers: MutableList<MyTransformer> = mutableListOf()

    /**
     * 添加单个 transformer
     *
     * @param transformer 要添加的 transformer
     */
    fun addTransformer(transformer: MyTransformer) {
        val hookClassName = transformer.hookClassName()
        if (hookClassName == null) {
            // 空类名表示全局 transformer
            globalTransformers.add(transformer)
            DebugInfo.debug("Added global transformer: ${transformer.javaClass.name}")
            return
        }

        val className = hookClassName.replace('.', '/')
        transformerMap.getOrPut(className) { mutableListOf() }.add(transformer)
        classSet.add(className)

        DebugInfo.debug("Added transformer for '$className': ${transformer.javaClass.name}")
    }

    /**
     * 添加 transformer 列表
     */
    fun addTransformers(transformers: List<MyTransformer>) {
        for (t in transformers) {
            addTransformer(t)
        }
    }

    /**
     * 添加 transformer 数组
     */
    fun addTransformers(transformers: Array<MyTransformer>) {
        for (t in transformers) {
            addTransformer(t)
        }
    }

    /**
     * 获取所有 hook 的类名
     */
    fun getHookClassNames(): Set<String> = classSet.toSet()

    /**
     * ClassFileTransformer 主回调方法
     *
     * JVM 加载每个类时都会调用此方法。
     */
    override fun transform(
        loader: ClassLoader?,
        className: String?,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray?
    ): ByteArray? {
        if (className == null || classfileBuffer == null) {
            return null
        }

        // 查找该类的 transformer 列表
        val transformers = transformerMap[className]
        if (transformers == null || transformers.isEmpty()) {
            return runGlobalTransformers(loader, classBeingRedefined, protectionDomain, className, classfileBuffer)
        }

        try {
            var buffer: ByteArray = classfileBuffer

            // 1. 执行全局 transformer 的 preTransform 阶段
            for (t in globalTransformers) {
                try {
                    val result = if (loader != null) {
                        t.preTransform(loader, classBeingRedefined ?: Any::class.java, protectionDomain ?: DEFAULT_PROTECTION_DOMAIN, className, buffer, 0)
                    } else {
                        t.preTransform(className, buffer, 0)
                    }
                    buffer = result ?: buffer
                } catch (e: Exception) {
                    DebugInfo.warn("Global preTransform error in ${t.javaClass.simpleName}", e)
                }
            }

            // 2. 执行类特定的 transformer 链
            for (t in transformers) {
                try {
                    val result = if (loader != null) {
                        t.transform(loader, classBeingRedefined ?: Any::class.java, protectionDomain ?: DEFAULT_PROTECTION_DOMAIN, className, buffer, 0)
                    } else {
                        t.transform(className, buffer, 0)
                    }
                    buffer = result ?: buffer
                } catch (e: Exception) {
                    DebugInfo.warn("Transform error in ${t.javaClass.simpleName} for $className", e)
                }
            }

            // 3. 执行全局 transformer 的 postTransform 阶段
            for (t in globalTransformers) {
                try {
                    val result = if (loader != null) {
                        t.postTransform(loader, classBeingRedefined ?: Any::class.java, protectionDomain ?: DEFAULT_PROTECTION_DOMAIN, className, buffer, 0)
                    } else {
                        t.postTransform(className, buffer, 0)
                    }
                    buffer = result ?: buffer
                } catch (e: Exception) {
                    DebugInfo.warn("Global postTransform error in ${t.javaClass.simpleName}", e)
                }
            }

            return buffer
        } catch (e: Throwable) {
            DebugInfo.error("Failed to transform $className", e)
            return classfileBuffer
        }
    }

    /**
     * 执行全局 transformer
     */
    private fun runGlobalTransformers(
        loader: ClassLoader?,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        className: String,
        classfileBuffer: ByteArray
    ): ByteArray? {
        if (globalTransformers.isEmpty()) {
            return null
        }

        var buffer: ByteArray = classfileBuffer
        for (t in globalTransformers) {
            try {
                val result = if (loader != null) {
                    t.transform(loader, classBeingRedefined ?: Any::class.java, protectionDomain ?: DEFAULT_PROTECTION_DOMAIN, className, buffer, 0)
                } else {
                    t.transform(className, buffer, 0)
                }
                buffer = result ?: buffer
            } catch (e: Exception) {
                DebugInfo.warn("Global transform error in ${t.javaClass.simpleName}", e)
            }
        }
        return buffer
    }

    companion object {
        /** 用于 ClassFileTransformer 中可能为 null 的 ProtectionDomain 字段的默认值 */
        private val DEFAULT_PROTECTION_DOMAIN: ProtectionDomain = ProtectionDomain(null, null)
    }
}