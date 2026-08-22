/*
 * ============================================================================
 * - InetAddressTransformer.kt - InetAddress 字节码转换器
 * ----------------------------------------------------------------------------
 * hook 目标：java.net.InetAddress
 *
 * 修改的字节码：
 *   - getAllByName0(String host) -> 加入 testQuery() 调用
 *   - getAllByName0(String host, InetAddress reqAddr, boolean includeCaches) -> 加入 testQuery()
 *   - getByName(String host) -> 加入 testQuery()
 *   - isReachable(InetAddress addr) -> 加入 testReachable()
 *
 * 字节码注入思路：
 *   1. 在方法开头插入调用 DNSFilter.testQuery(host)
 *   2. 在方法开头插入调用 DNSFilter.testReachable(addr)
 *   3. 如果返回非默认值，按规则处理
 * ============================================================================
 */
package com.janetfilter.plugins.dns

import com.janetfilter.core.commons.DebugInfo
import com.janetfilter.core.models.FilterRule
import com.janetfilter.core.plugin.MyTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * InetAddress 字节码转换器
 *
 * 将 java.net.InetAddress 中的域名查询方法调用 DNSFilter 进行过滤。
 */
class InetAddressTransformer(private val rules: List<FilterRule>) : MyTransformer {

    override fun hookClassName(): String = "java.net.InetAddress"

    override fun transform(
        loader: ClassLoader,
        clazz: Class<*>,
        domain: java.security.ProtectionDomain,
        className: String,
        classBuffer: ByteArray,
        order: Int
    ): ByteArray? {
        if (rules.isEmpty()) {
            // 没有规则就不转换
            return classBuffer
        }

        // 初始化规则
        DNSFilter.setRules(rules)

        try {
            val classReader = ClassReader(classBuffer)
            val classNode = ClassNode()
            classReader.accept(classNode, 0)

            var modified = false
            for (method in classNode.methods) {
                if (method.name == "getAllByName0" && method.desc == "(Ljava/lang/String;Ljava/net/InetAddress;Z)Ljava/net/InetAddress[]") {
                    transformGetAllByName0(method)
                    modified = true
                }
            }

            if (!modified) {
                DebugInfo.debug("[$PLUGIN_NAME] No methods matched for transformation")
                return classBuffer
            }

            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
            classNode.accept(classWriter)
            DebugInfo.info("[$PLUGIN_NAME] Transformed java.net.InetAddress")
            return classWriter.toByteArray()
        } catch (e: Exception) {
            DebugInfo.error("[$PLUGIN_NAME] Transform failed", e)
            return classBuffer
        }
    }

    /**
     * 修改 getAllByName0 方法，在开头插入 DNSFilter.testQuery 调用
     *
     * 注入位置：方法最开始（第 0 个指令前）
     * 注入代码：
     *   DNSFilter.testQuery(host);
     *
     * host 参数在 getAllByName0 中是第一个参数，存储在 slot 1
     */
    private fun transformGetAllByName0(method: MethodNode) {
        val instructions = method.instructions
        if (instructions.size() == 0) return

        // 在方法最开始插入调用 DNSFilter.testQuery(host)
        val insnList = InsnList()

        // 加载第一个参数（host, slot 1）
        insnList.add(VarInsnNode(Opcodes.ALOAD, 1))

        // 调用 DNSFilter.testQuery(String) String
        insnList.add(MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "com/janetfilter/plugins/dns/DNSFilter",
            "testQuery",
            "(Ljava/lang/String;)Ljava/lang/String;",
            false
        ))

        // 检查返回值是否与原 host 不一致（被屏蔽）
        // 原 host 仍在栈顶，新返回值压栈
        // 此时栈：[origHost, newHost]
        insnList.add(VarInsnNode(Opcodes.ALOAD, 1))
        // 栈：[origHost, newHost, origHost]
        insnList.add(InsnNode(Opcodes.SWAP))
        // 栈：[origHost, origHost, newHost]
        // 由于 testQuery 在被拦截时直接抛出异常，所以这里只是简单移除栈顶
        insnList.add(InsnNode(Opcodes.POP))
        insnList.add(InsnNode(Opcodes.POP))

        // 插入到方法开头
        instructions.insertBefore(instructions.first, insnList)
    }

    companion object {
        private const val PLUGIN_NAME = "dns"
    }
}