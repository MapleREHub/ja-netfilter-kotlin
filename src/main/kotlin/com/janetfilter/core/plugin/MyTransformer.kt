/*
 * ============================================================================
 * - MyTransformer.kt - 自定义字节码转换器接口
 * ----------------------------------------------------------------------------
 * 插件需要实现该接口来 hook 目标类的字节码。
 *
 * 工作模式：
 *   - hookClassName: 目标类的内部名（点分），如 "java.net.InetAddress"
 *   - transform(): 实际修改字节码的方法
 *
 * 可选的钩子方法：
 *   - before/after: 转换前后调用
 *   - preTransform/postTransform: 在所有 transformer 之前/之后调用
 *   - attachMode/javaagentMode: 限制运行模式
 * ============================================================================
 */
package com.janetfilter.core.plugin

import java.security.ProtectionDomain

/**
 * 自定义字节码转换器接口
 *
 * 插件通过实现该接口来 hook JVM 中的特定类。
 * 每个 transformer 只 hook 一个类（通过 hookClassName 指定）。
 *
 * 默认方法都是空实现，子类只需重写需要的方法。
 */
interface MyTransformer {

    /**
     * 要 hook 的目标类名
     *
     * @return 类的完全限定名（点分格式），如 "java.net.InetAddress"
     *         返回 null 表示全局 transformer（不区分目标类）
     */
    fun hookClassName(): String?

    /**
     * 是否在 attach 模式下运行
     *
     * 默认 false：只在 javaagent 模式下运行
     */
    fun attachMode(): Boolean = false

    /**
     * 是否在 javaagent 模式下运行
     *
     * 默认 true：只在 javaagent 模式下运行
     */
    fun javaagentMode(): Boolean = true

    /**
     * 是否为管理层 transformer（attach 模式专用）
     */
    fun isManager(): Boolean = false

    /**
     * 在 transform 之前调用
     */
    fun before(loader: ClassLoader, clazz: Class<*>, domain: ProtectionDomain, className: String, classBuffer: ByteArray) {
    }

    /**
     * 在 transform 之前调用（无 ClassLoader 版本）
     */
    fun before(className: String, classBuffer: ByteArray) {
    }

    /**
     * preTransform 阶段（在所有 transformer 之前调用）
     *
     * @return 修改后的字节码，或 null 表示未修改
     */
    @Throws(Exception::class)
    fun preTransform(
        loader: ClassLoader,
        clazz: Class<*>,
        domain: ProtectionDomain,
        className: String,
        classBuffer: ByteArray,
        order: Int
    ): ByteArray? = null

    /**
     * preTransform 阶段（无 ClassLoader 版本）
     */
    @Throws(Exception::class)
    fun preTransform(className: String, classBuffer: ByteArray, order: Int): ByteArray? = null

    /**
     * 主转换方法
     *
     * 默认实现：原样返回
     * 子类应重写此方法来修改字节码
     *
     * @return 修改后的字节码，或 null 表示未修改
     */
    @Throws(Exception::class)
    fun transform(
        loader: ClassLoader,
        clazz: Class<*>,
        domain: ProtectionDomain,
        className: String,
        classBuffer: ByteArray,
        order: Int
    ): ByteArray? = null

    /**
     * 主转换方法（无 ClassLoader 版本）
     */
    @Throws(Exception::class)
    fun transform(className: String, classBuffer: ByteArray, order: Int): ByteArray? = null

    /**
     * postTransform 阶段（在所有 transformer 之后调用）
     */
    @Throws(Exception::class)
    fun postTransform(
        loader: ClassLoader,
        clazz: Class<*>,
        domain: ProtectionDomain,
        className: String,
        classBuffer: ByteArray,
        order: Int
    ): ByteArray? = null

    /**
     * postTransform 阶段（无 ClassLoader 版本）
     */
    @Throws(Exception::class)
    fun postTransform(className: String, classBuffer: ByteArray, order: Int): ByteArray? = null

    /**
     * 在 transform 之后调用
     */
    fun after(loader: ClassLoader, clazz: Class<*>, domain: ProtectionDomain, className: String, classBuffer: ByteArray) {
    }

    /**
     * 在 transform 之后调用（无 ClassLoader 版本）
     */
    fun after(className: String, classBuffer: ByteArray) {
    }
}