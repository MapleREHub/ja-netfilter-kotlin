/*
 * ============================================================================
 * - ClassNameTransformer.kt - Class 字节码转换器
 * ----------------------------------------------------------------------------
 * hook 目标：java.lang.Class
 *
 * 修改字节码：将 getName() 等方法修改，使其不暴露 ja-netfilter 相关信息。
 *
 * 在实际逆向中，原始 jar 的这个 transformer 是为占位实现。
 * 主要的隐藏逻辑在 VMTransformer 和 ClassLoaderTransformer 中。
 * ============================================================================
 */
package com.janetfilter.plugins.hideme

import com.janetfilter.core.commons.DebugInfo
import com.janetfilter.core.plugin.MyTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode

/**
 * Class 字节码转换器
 */
class ClassNameTransformer : MyTransformer {

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

            // 当前为简化实现，不修改 Class 的方法
            // 实际隐藏逻辑在 VMTransformer 和 ClassLoaderTransformer

            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
            classNode.accept(classWriter)
            DebugInfo.debug("[hideme] Processed java.lang.Class (no actual transformation)")
            return classWriter.toByteArray()
        } catch (e: Exception) {
            DebugInfo.error("[hideme] ClassNameTransformer failed", e)
            return classBuffer
        }
    }
}