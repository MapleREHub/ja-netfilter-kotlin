/*
 * ============================================================================
 * - HttpClientTransformer.kt - HttpClient 字节码转换器
 * ----------------------------------------------------------------------------
 * hook 目标：sun.net.www.http.HttpClient
 *
 * 修改的字节码：
 *   - 在 HttpClient 构造方法中插入 URLFilter.testURL() 调用
 *   - 如果 URL 被拦截，则替换为无效 URL
 *
 * 实现思路：
 *   HttpClient 的构造方法签名：
 *     HttpClient(URL url, Proxy proxy, int connectTimeout)
 *
 *   注入位置：构造方法最开始
 *   注入代码：
 *     url = URLFilter.testURL(url);
 * ============================================================================
 */
package com.janetfilter.plugins.url

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
 * HttpClient 字节码转换器
 */
class HttpClientTransformer(private val rules: List<FilterRule>) : MyTransformer {

    override fun hookClassName(): String = "sun.net.www.http.HttpClient"

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

        URLFilter.setRules(rules)

        try {
            val classReader = ClassReader(classBuffer)
            val classNode = ClassNode()
            classReader.accept(classNode, 0)

            var modified = false
            for (method in classNode.methods) {
                // hook 构造方法
                if (method.name == "<init>" &&
                    (method.desc == "(Ljava/net/URL;Ljava/net/Proxy;I)V" ||
                     method.desc == "(Ljava/net/URL;Ljava/net/Proxy;ILsun/net/www/HttpClientBase\$RedirectPermissions;)V")) {
                    transformConstructor(method)
                    modified = true
                }
            }

            if (!modified) return classBuffer

            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
            classNode.accept(classWriter)
            DebugInfo.info("[url] Transformed sun.net.www.http.HttpClient")
            return classWriter.toByteArray()
        } catch (e: Exception) {
            DebugInfo.error("[url] Transform failed", e)
            return classBuffer
        }
    }

    /**
     * 修改构造方法
     *
     * 注入位置：构造方法最开始
     * 注入逻辑：
     *   dup
     *   invokestatic URLFilter.testURL(Ljava/net/URL;)Ljava/net/URL;
     */
    private fun transformConstructor(method: MethodNode) {
        val instructions = method.instructions
        if (instructions.size() == 0) return

        val insnList = InsnList()
        // 第一个参数是 URL（在 slot 1，因为 slot 0 是 this）
        insnList.add(VarInsnNode(Opcodes.ALOAD, 1))
        insnList.add(MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "com/janetfilter/plugins/url/URLFilter",
            "testURL",
            "(Ljava/net/URL;)Ljava/net/URL;",
            false
        ))
        insnList.add(VarInsnNode(Opcodes.ASTORE, 1))

        instructions.insertBefore(instructions.first, insnList)
    }
}