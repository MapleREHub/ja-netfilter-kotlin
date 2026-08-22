/*
 * ============================================================================
 * - ClassTransformer.kt - 通用 Class 拦截器
 * ----------------------------------------------------------------------------
 * 拦截 java.lang.Class 加载过程中的隐私相关类
 * ============================================================================
 */
package com.novitechie

import com.janetfilter.core.commons.DebugInfo
import com.janetfilter.core.plugin.MyTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode

/**
 * 通用 Class 转换器
 */
class ClassTransformer : MyTransformer {

    override fun hookClassName(): String = "java.lang.Class"

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

            DebugInfo.debug("[privacy] Processing java.lang.Class")

            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
            classNode.accept(classWriter)
            return classWriter.toByteArray()
        } catch (e: Exception) {
            DebugInfo.warn("[privacy] ClassTransformer failed", e)
            return classBuffer
        }
    }
}