/*
 * ============================================================================
 * - CollectionsTransformer.kt - 集合类拦截器
 * ----------------------------------------------------------------------------
 * hook 目标：java.util.Collections
 *
 * 用于拦截一些集合操作以隐藏激活信息。
 * ============================================================================
 */
package com.novitechie

import com.janetfilter.core.commons.DebugInfo
import com.janetfilter.core.plugin.MyTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode

/**
 * 集合类字节码转换器
 */
class CollectionsTransformer : MyTransformer {

    override fun hookClassName(): String = "java.util.Collections"

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

            DebugInfo.debug("[privacy] Processing java.util.Collections")

            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
            classNode.accept(classWriter)
            return classWriter.toByteArray()
        } catch (e: Exception) {
            DebugInfo.warn("[privacy] CollectionsTransformer failed", e)
            return classBuffer
        }
    }
}