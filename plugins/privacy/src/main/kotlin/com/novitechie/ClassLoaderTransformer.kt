/*
 * ============================================================================
 * - ClassLoaderTransformer.kt - ClassLoader 拦截器
 * ============================================================================
 */
package com.novitechie

import com.janetfilter.core.commons.DebugInfo
import com.janetfilter.core.plugin.MyTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode

/**
 * ClassLoader 字节码转换器
 */
class ClassLoaderTransformer : MyTransformer {

    override fun hookClassName(): String = "java.lang.ClassLoader"

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

            DebugInfo.debug("[privacy] Processing java.lang.ClassLoader")

            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
            classNode.accept(classWriter)
            return classWriter.toByteArray()
        } catch (e: Exception) {
            DebugInfo.warn("[privacy] ClassLoaderTransformer failed", e)
            return classBuffer
        }
    }
}