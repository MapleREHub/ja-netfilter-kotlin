/*
 * ============================================================================
 * - PluginClassLoaderTransformer.kt - 插件 ClassLoader 拦截器
 * ============================================================================
 */
package com.novitechie

import com.janetfilter.core.commons.DebugInfo
import com.janetfilter.core.plugin.MyTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode

/**
 * PluginClassLoader 字节码转换器
 */
class PluginClassLoaderTransformer : MyTransformer {

    override fun hookClassName(): String =
        "com.intellij.ide.plugins.cl.PluginClassLoader"

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

            DebugInfo.debug("[privacy] Found PluginClassLoader (no modification)")

            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
            classNode.accept(classWriter)
            return classWriter.toByteArray()
        } catch (e: Exception) {
            DebugInfo.warn("[privacy] PluginClassLoaderTransformer failed", e)
            return classBuffer
        }
    }
}