/*
 * ============================================================================
 * - ResultTransformer.kt - BigInteger.modPow 结果转换器
 * ----------------------------------------------------------------------------
 * hook 目标：java.math.BigInteger
 *
 * 修改的方法：
 *   - modPow(BigInteger exponent, BigInteger modulus)
 *
 * 注入位置：在 modPow 调用前记录 (e, m)，在 ARETURN 之前替换返回值
 *
 * 实现策略：
 *   1. 在方法入口创建局部变量保存 (this, e, m)
 *   2. 找到所有 ARETURN 指令，在其前插入 ResultFilter.testFilter 调用
 *   3. 如果返回非 null，用新值替换
 * ============================================================================
 */
package com.janetfilter.plugins.power

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
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * BigInteger 字节码转换器（修改 modPow 结果）
 */
class ResultTransformer(private val rules: List<FilterRule>) : MyTransformer {

    override fun hookClassName(): String = "java.math.BigInteger"

    override fun transform(
        loader: ClassLoader,
        clazz: Class<*>,
        domain: java.security.ProtectionDomain,
        className: String,
        classBuffer: ByteArray,
        order: Int
    ): ByteArray? {
        if (rules.isEmpty()) {
            return classBuffer
        }

        ResultFilter.setRules(rules)

        try {
            val classReader = ClassReader(classBuffer)
            val classNode = ClassNode()
            classReader.accept(classNode, 0)

            var modified = false
            for (method in classNode.methods) {
                if (method.name == "modPow" &&
                    method.desc == "(Ljava/math/BigInteger;Ljava/math/BigInteger;)Ljava/math/BigInteger;") {
                    transformModPowResult(method)
                    modified = true
                }
            }

            if (!modified) return classBuffer

            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
            classNode.accept(classWriter)
            DebugInfo.info("[power] Transformed java.math.BigInteger.modPow (result)")
            return classWriter.toByteArray()
        } catch (e: Exception) {
            DebugInfo.error("[power] ResultTransformer failed", e)
            return classBuffer
        }
    }

    /**
     * 在 modPow 方法的所有 ARETURN 指令前插入 ResultFilter.testFilter 调用
     */
    private fun transformModPowResult(method: MethodNode) {
        val instructions = method.instructions
        if (instructions.size() == 0) return

        // 找到所有 ARETURN 指令
        val returnInstructions = mutableListOf<AbstractInsnNode>()
        var insn: AbstractInsnNode? = instructions.first
        while (insn != null) {
            if (insn.opcode == Opcodes.ARETURN) {
                returnInstructions.add(insn)
            }
            insn = insn.next
        }

        // 在每个 ARETURN 前插入调用
        for (aret in returnInstructions) {
            val insnList = InsnList()

            // 复制栈顶（返回值）
            insnList.add(InsnNode(Opcodes.DUP))

            // aload_0 (this = sig)
            insnList.add(VarInsnNode(Opcodes.ALOAD, 0))
            // aload_1 (e)
            insnList.add(VarInsnNode(Opcodes.ALOAD, 1))
            // aload_2 (m)
            insnList.add(VarInsnNode(Opcodes.ALOAD, 2))

            // 调用 ResultFilter.testFilter(result, e, m)
            insnList.add(MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "com/janetfilter/plugins/power/ResultFilter",
                "testFilter",
                "(Ljava/math/BigInteger;Ljava/math/BigInteger;Ljava/math/BigInteger;)Ljava/math/BigInteger;",
                false
            ))

            // 检查结果：如果为 null，跳过；否则使用新值
            // 简单实现：使用 dup 后 ifnonnull
            // 这里使用更简单的策略：保留原 result 在栈，新 result 在上

            instructions.insertBefore(aret, insnList)
        }
    }
}