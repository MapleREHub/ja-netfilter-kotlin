/*
 * ============================================================================
 * - LicensingFacadeTransformer.kt - 许可门面拦截器
 * ----------------------------------------------------------------------------
 * hook 目标：JetBrains 内部的 LicensingFacade 类
 *
 * 修改的字节码：
 *   - 各种与许可验证相关的方法
 *
 * 实现：
 *   由于该类不是公共 API，通过 reflection 拦截
 *   简化实现：记录调用但不修改
 * ============================================================================
 */
package com.novitechie

import com.janetfilter.core.commons.DebugInfo
import com.janetfilter.core.plugin.MyTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode

/**
 * LicensingFacade 字节码转换器
 */
class LicensingFacadeTransformer : MyTransformer {

    override fun hookClassName(): String =
        "com.intellij.idea.LicensingFacade"

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

            // 简化实现：仅记录，不修改字节码
            DebugInfo.debug("[privacy] Found LicensingFacade (no modification)")

            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
            classNode.accept(classWriter)
            return classWriter.toByteArray()
        } catch (e: Exception) {
            DebugInfo.warn("[privacy] LicensingFacadeTransformer failed", e)
            return classBuffer
        }
    }
}