/*
 * ============================================================================
 * - WrapperTransformer.kt - Native 方法包装转换器
 * ----------------------------------------------------------------------------
 * hook 目标：java.lang.ClassLoader
 *
 * 修改字节码：包装 loadClass 等方法以支持 native 方法的 prefix 替换
 *
 * 工作原理：
 *   - 当原始类有 native 方法时，JVM 通过 JNI 调用
 *   - 如果设置了 native-method-prefix，会用前缀包装方法名
 *   - WrapperTransformer 修改 ClassLoader 的方法，支持 native 方法包装
 * ============================================================================
 */
package com.janetfilter.plugins.native_wrapper

import com.janetfilter.core.Environment
import com.janetfilter.core.commons.DebugInfo
import com.janetfilter.core.models.FilterRule
import com.janetfilter.core.plugin.MyTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * ClassLoader 字节码转换器（native 包装支持）
 */
class WrapperTransformer(
    private val env: Environment,
    private val rules: List<FilterRule>
) : MyTransformer {

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

            var modified = false
            for (method in classNode.methods) {
                // hook loadClass(String)
                if (method.name == "loadClass" && method.desc == "(Ljava/lang/String;)Ljava/lang/Class;") {
                    // 添加 native prefix 支持
                    wrapLoadClass(method)
                    modified = true
                }
            }

            if (!modified) return classBuffer

            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
            classNode.accept(classWriter)
            DebugInfo.info("[native] Transformed java.lang.ClassLoader")
            return classWriter.toByteArray()
        } catch (e: Exception) {
            DebugInfo.error("[native] WrapperTransformer failed", e)
            return classBuffer
        }
    }

    /**
     * 包装 loadClass 方法以支持 native prefix
     */
    private fun wrapLoadClass(method: MethodNode) {
        // 简化实现：在方法最开始插入一段代码检查 native method prefix
        val instructions = method.instructions
        if (instructions.size() == 0) return

        val insnList = InsnList()

        // 加载 prefix
        insnList.add(LdcInsnNode(env.nativePrefix))

        // 加载 className
        insnList.add(VarInsnNode(Opcodes.ALOAD, 1))

        // 调用 String.startsWith 检查
        insnList.add(MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/String",
            "startsWith",
            "(Ljava/lang/String;)Z",
            false
        ))

        // 如果匹配 prefix，移除 prefix 再加载
        val labelSkip = org.objectweb.asm.tree.LabelNode()
        val labelStrip = org.objectweb.asm.tree.LabelNode()

        insnList.add(JumpInsnNode(Opcodes.IFEQ, labelSkip))
        // 匹配：移除 prefix
        insnList.add(labelStrip)
        insnList.add(VarInsnNode(Opcodes.ALOAD, 1))
        insnList.add(LdcInsnNode(env.nativePrefix.length))
        insnList.add(MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/String",
            "substring",
            "(I)Ljava/lang/String;",
            false
        ))
        insnList.add(VarInsnNode(Opcodes.ASTORE, 1))
        insnList.add(labelSkip)

        instructions.insertBefore(instructions.first, insnList)
    }
}