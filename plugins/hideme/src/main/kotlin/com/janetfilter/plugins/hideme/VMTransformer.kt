/*
 * ============================================================================
 * - VMTransformer.kt - VM 字节码转换器
 * ----------------------------------------------------------------------------
 * hook 目标：sun.management.VMManagementImpl
 *
 * 修改字节码：
 *   - getVmArguments() -> 加入 VmArgumentFilter.testArgs() 调用
 *
 * 作用：
 *   隐藏应用启动时的 javaagent 参数，使应用无法检测到 ja-netfilter。
 * ============================================================================
 */
package com.janetfilter.plugins.hideme

import com.janetfilter.core.Environment
import com.janetfilter.core.commons.DebugInfo
import com.janetfilter.core.plugin.MyTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

/**
 * VM 字节码转换器
 */
class VMTransformer(private val environment: Environment) : MyTransformer {

    override fun hookClassName(): String = "sun.management.VMManagementImpl"

    override fun transform(
        loader: ClassLoader,
        clazz: Class<*>,
        domain: java.security.ProtectionDomain,
        className: String,
        classBuffer: ByteArray,
        order: Int
    ): ByteArray? {
        VmArgumentFilter.setEnvironment(environment)

        try {
            val classReader = ClassReader(classBuffer)
            val classNode = ClassNode()
            classReader.accept(classNode, 0)

            var modified = false
            for (method in classNode.methods) {
                // hook getVmArguments()
                if (method.name == "getVmArguments" && method.desc == "()Ljava/util/List;") {
                    transformGetVmArguments(method)
                    modified = true
                }
            }

            if (!modified) return classBuffer

            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
            classNode.accept(classWriter)
            DebugInfo.info("[hideme] Transformed sun.management.VMManagementImpl")
            return classWriter.toByteArray()
        } catch (e: Exception) {
            DebugInfo.error("[hideme] VMTransformer failed", e)
            return classBuffer
        }
    }

    /**
     * 修改 getVmArguments 方法
     *
     * 注入逻辑：
     *   dup
     *   invokestatic VmArgumentFilter.testArgs(List) List
     */
    private fun transformGetVmArguments(method: MethodNode) {
        val instructions = method.instructions
        if (instructions.size() == 0) return

        val insnList = InsnList()
        insnList.add(InsnNode(Opcodes.DUP))
        insnList.add(MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "com/janetfilter/plugins/hideme/VmArgumentFilter",
            "testArgs",
            "(Ljava/util/List;)Ljava/util/List;",
            false
        ))

        instructions.insertBefore(instructions.first, insnList)
    }
}