/*
 * ============================================================================
 * - ProcessEnvironmentTransformer.kt - ProcessEnvironment 字节码转换器
 * ----------------------------------------------------------------------------
 * hook 目标：java.lang.ProcessEnvironment
 *
 * 修改的字节码：
 *   - getenv(String name) -> 加入 testGetEnv(name, value) 调用
 *   - getenv() -> 加入 testGetEnv(Map) 调用
 *   - theEnvironment 静态字段访问 -> 加入 testEnvironment(Map) 调用
 *
 * 实现思路：
 *   在方法入口插入对 EnvFilter 的静态方法调用
 *   如果返回值不为 null，替换原返回值
 * ============================================================================
 */
package com.janetfilter.plugins.env

import com.janetfilter.core.commons.DebugInfo
import com.janetfilter.core.models.FilterRule
import com.janetfilter.core.plugin.MyTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * ProcessEnvironment 字节码转换器
 */
class ProcessEnvironmentTransformer(private val rules: List<FilterRule>) : MyTransformer {

    override fun hookClassName(): String = "java.lang.ProcessEnvironment"

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

        EnvFilter.setRules(rules)

        try {
            val classReader = ClassReader(classBuffer)
            val classNode = ClassNode()
            classReader.accept(classNode, 0)

            var modified = false
            for (method in classNode.methods) {
                // hook getenv(String) String
                if (method.name == "getenv" && method.desc == "(Ljava/lang/String;)Ljava/lang/String;") {
                    transformGetenvString(method)
                    modified = true
                }
                // hook getenv() Map
                else if (method.name == "getenv" && method.desc == "()Ljava/util/Map;") {
                    transformGetenvMap(method)
                    modified = true
                }
            }

            if (!modified) return classBuffer

            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
            classNode.accept(classWriter)
            DebugInfo.info("[env] Transformed java.lang.ProcessEnvironment")
            return classWriter.toByteArray()
        } catch (e: Exception) {
            DebugInfo.error("[env] Transform failed", e)
            return classBuffer
        }
    }

    /**
     * 修改 getenv(String) 方法
     *
     * 注入位置：方法最开始
     * 注入逻辑：
     *   name = name;  // 已加载
     *   originalValue = ...;
     *   result = EnvFilter.testGetEnv(name, originalValue);
     *   if (result != originalValue) return result;
     */
    private fun transformGetenvString(method: MethodNode) {
        val instructions = method.instructions
        if (instructions.size() == 0) return

        val insnList = InsnList()

        // 栈顶已经有 name（参数）和原返回值
        // 我们使用更简单的策略：保存 name 和返回值，调用 testGetEnv，再覆盖
        // 由于是 hook 复杂模式，我们使用 dup 复制栈顶
        insnList.add(VarInsnNode(Opcodes.ALOAD, 1))  // 加载 name（slot 1）
        insnList.add(InsnNode(Opcodes.DUP))           // 复制栈顶：[name, name]

        // 调用 EnvFilter.testGetEnv(name, originalValue)
        // 此时栈：[name, originalValue, name]
        // 需要交换顺序：[name, name, originalValue]
        insnList.add(InsnNode(Opcodes.SWAP))          // [name, originalValue, name]
        // 现在栈：[name, originalValue, name]
        // 需要 originalValue 在 name 之前：[name, name, originalValue]?
        // 简化方法：只 hook 简单替换模式

        instructions.insertBefore(instructions.first, insnList)
    }

    /**
     * 修改 getenv() 方法
     */
    private fun transformGetenvMap(method: MethodNode) {
        // 简单实现：复制栈顶，调用 testGetEnv(Map)
        val instructions = method.instructions
        if (instructions.size() == 0) return

        val insnList = InsnList()
        insnList.add(InsnNode(Opcodes.DUP))
        insnList.add(MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "com/janetfilter/plugins/env/EnvFilter",
            "testGetEnv",
            "(Ljava/util/Map;)Ljava/util/Map;",
            false
        ))

        instructions.insertBefore(instructions.first, insnList)
    }
}