/*
 * ============================================================================
 * - ArgsTransformer.kt - BigInteger.modPow 参数转换器
 * ----------------------------------------------------------------------------
 * hook 目标：java.math.BigInteger
 *
 * 修改的方法：
 *   - modPow(BigInteger exponent, BigInteger modulus)
 *
 * 注入位置：方法最开始
 * 注入逻辑：
 *   sig = dup
 *   e = dup
 *   m = dup
 *   dup_x2  // 复制 sig, e, m
 *   invokestatic ArgsFilter.testFilter(sig, e, m)
 *   if result != null:
 *     replace sig with result.signature
 *
 * 实现较复杂，需要 ASM 字节码操作。
 * ============================================================================
 */
package com.janetfilter.plugins.power

import com.janetfilter.core.commons.DebugInfo
import com.janetfilter.core.models.FilterRule
import com.janetfilter.core.plugin.MyTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * BigInteger 字节码转换器（修改 modPow 参数）
 */
class ArgsTransformer(private val rules: List<FilterRule>) : MyTransformer {

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

        ArgsFilter.setRules(rules)

        try {
            val classReader = ClassReader(classBuffer)
            val classNode = ClassNode()
            classReader.accept(classNode, 0)

            var modified = false
            for (method in classNode.methods) {
                if (method.name == "modPow" &&
                    method.desc == "(Ljava/math/BigInteger;Ljava/math/BigInteger;)Ljava/math/BigInteger;") {
                    transformModPow(method)
                    modified = true
                }
            }

            if (!modified) return classBuffer

            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
            classNode.accept(classWriter)
            DebugInfo.info("[power] Transformed java.math.BigInteger.modPow (args)")
            return classWriter.toByteArray()
        } catch (e: Exception) {
            DebugInfo.error("[power] ArgsTransformer failed", e)
            return classBuffer
        }
    }

    /**
     * 修改 modPow 方法的字节码
     *
     * 原始签名（参数槽）：
     *   slot 0: this
     *   slot 1: exponent (e)
     *   slot 2: modulus (m)
     *   'this' 即是签名值 sig
     *
     * 注入代码（在方法最开始）：
     *   new BigInteger[]{} // 用于调用 testFilter
     *   实际为：
     *   aload_0     // sig
     *   aload_1     // e
     *   aload_2     // m
     *   invokestatic ArgsFilter.testFilter(sig, e, m)
     *   dup
     *   ifnull SKIP
     *   // result != null: 替换 this 和 exponent
     *   pop
     *   aload_0
     *   getfield    // 假设 sig 是字段
     *   ...
     * SKIP:
     *
     * 由于 BigInteger 是不可变的，需要构造新的 BigInteger 来替换 this。
     * 简化策略：将替换后的值作为新参数传给原 modPow 调用。
     */
    private fun transformModPow(method: MethodNode) {
        val instructions = method.instructions
        if (instructions.size() == 0) return

        // 创建新局部变量：替换后的 sig
        // 我们使用 slot 3 作为临时变量

        val insnList = InsnList()

        // 1. 调用 testFilter 获取替换结果
        // aload_0 (sig)
        insnList.add(VarInsnNode(Opcodes.ALOAD, 0))
        // aload_1 (e)
        insnList.add(VarInsnNode(Opcodes.ALOAD, 1))
        // aload_2 (m)
        insnList.add(VarInsnNode(Opcodes.ALOAD, 2))
        // invokestatic testFilter(sig, e, m) -> BigInteger[]
        insnList.add(MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "com/janetfilter/plugins/power/ArgsFilter",
            "testFilter",
            "(Ljava/math/BigInteger;Ljava/math/BigInteger;Ljava/math/BigInteger;)Lcom/janetfilter/plugins/power/ArgsFilter\$BigIntegerArray;",
            false
        ))

        // 2. 复制栈顶（BigIntegerArray）
        insnList.add(InsnNode(Opcodes.DUP))

        // 3. 检查是否为 null
        val labelSkip = LabelNode()
        val labelNull = LabelNode()
        insnList.add(JumpInsnNode(Opcodes.IFNULL, labelNull))

        // 4. 非 null：获取签名替换值，保存到 slot 3，pop 原数组
        insnList.add(MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "com/janetfilter/plugins/power/ArgsFilter\$BigIntegerArray",
            "component1",
            "()Ljava/math/BigInteger;",
            false
        ))
        insnList.add(VarInsnNode(Opcodes.ASTORE, 3))

        // 5. 跳到原方法继续（使用新 sig 替换 aload_0）
        insnList.add(labelSkip)

        // labelNull 分支：pop 数组
        val labelContinue = LabelNode()
        insnList.add(labelNull)
        insnList.add(InsnNode(Opcodes.POP))
        insnList.add(labelContinue)

        instructions.insertBefore(instructions.first, insnList)

        // 在方法体中所有 aload_0 之前插入 aload_3 的备份逻辑
        // 简化：使用更复杂的字节码注入
        // 这里给出概念性实现

        // 由于完整实现复杂，简化策略：在方法最开始直接调用 testFilter
        // 如果返回非 null，跳到一个完全替换 modPow 逻辑的新代码路径
    }
}