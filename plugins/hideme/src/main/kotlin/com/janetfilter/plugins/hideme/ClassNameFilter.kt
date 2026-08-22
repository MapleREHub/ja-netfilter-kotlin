/*
 * ============================================================================
 * - ClassNameFilter.kt - 类名过滤器
 * ----------------------------------------------------------------------------
 * 过滤 ClassLoader 加载类时的某些类名，阻止应用检测到激活相关类。
 * ============================================================================
 */
package com.janetfilter.plugins.hideme

/**
 * 类名过滤器
 */
object ClassNameFilter {

    /**
     * 测试类名
     *
     * @param className 要检查的类名
     * @throws ClassNotFoundException 如果类名应该被隐藏（永远不返回）
     */
    @JvmStatic
    @Throws(ClassNotFoundException::class)
    fun testClass(className: String) {
        // 当前实现：不主动阻止，让 ClassNameTransformer 来处理
    }
}