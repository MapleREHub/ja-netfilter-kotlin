/*
 * ============================================================================
 * - MethodTransformer.kt - Method 拦截器
 * ----------------------------------------------------------------------------
 * hook 目标：java.lang.reflect.Method
 * ============================================================================
 */
package com.novitechie

import com.janetfilter.core.commons.DebugInfo
import com.janetfilter.core.plugin.MyTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode

/**
 * Method 字节码转换器
 */
class MethodTransformer : MyTransformer {

    override fun hookClassName(): String = "java.lang.reflect.Method"

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

            DebugInfo.debug("[privacy] Processing java.lang.reflect.Method")

            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
            classNode.accept(classWriter)
            return classWriter.toByteArray()
        } catch (e: Exception) {
            DebugInfo.warn("[privacy] MethodTransformer failed", e)
            return classBuffer
        }
    }
}