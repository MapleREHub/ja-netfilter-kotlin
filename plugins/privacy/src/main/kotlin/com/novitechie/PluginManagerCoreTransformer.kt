/*
 * ============================================================================
 * - PluginManagerCoreTransformer.kt - 插件管理器拦截器
 * ============================================================================
 */
package com.novitechie

import com.janetfilter.core.commons.DebugInfo
import com.janetfilter.core.plugin.MyTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode

/**
 * PluginManagerCore 字节码转换器
 */
class PluginManagerCoreTransformer : MyTransformer {

    override fun hookClassName(): String =
        "com.intellij.ide.plugins.PluginManagerCore"

    override fun transform(
        loader: ClassLoader,
        clazz: Class<*>,
        domain: java.security.ProtectionDomain,
        className: String,
        classBuffer: ByteArray,
        order: Int
    ): ByteArray? {
        try {
            val classReader = ClassReader(classBuffer)
            val classNode = ClassNode()
            classReader.accept(classNode, 0)

            DebugInfo.debug("[privacy] Found PluginManagerCore (no modification)")

            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
            classNode.accept(classWriter)
            return classWriter.toByteArray()
        } catch (e: Exception) {
            DebugInfo.warn("[privacy] PluginManagerCoreTransformer failed", e)
            return classBuffer
        }
    }
}