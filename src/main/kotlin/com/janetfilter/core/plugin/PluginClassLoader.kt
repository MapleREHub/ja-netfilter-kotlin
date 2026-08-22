/*
 * ============================================================================
 * - PluginClassLoader.kt - 插件类加载器
 * ----------------------------------------------------------------------------
 * 专用于加载插件 jar 的 ClassLoader。
 *
 * 每个插件使用独立的 ClassLoader 实例，实现插件隔离：
 *   - 不同插件可以使用相同类名的不同版本
 *   - 插件类可以访问 JDK 类
 *   - 但不能访问宿主应用的类（隔离）
 * ============================================================================
 */
package com.janetfilter.core.plugin

import java.util.jar.JarFile

/**
 * 插件类加载器
 *
 * 用于加载插件 jar 中的类。
 * 委托给父 ClassLoader（系统 ClassLoader）加载 JDK 类。
 *
 * @param jarFile 插件 jar 文件
 */
class PluginClassLoader(
    private val jarFile: JarFile
) : ClassLoader() {

    /**
     * 查找类
     *
     * @param name 类的完全限定名
     * @return 加载的 Class 对象
     * @throws ClassNotFoundException 如果找不到
     */
    @Throws(ClassNotFoundException::class)
    fun loadPluginClass(name: String): Class<*> {
        // 先尝试父加载器
        try {
            return super.loadClass(name, false)
        } catch (_: ClassNotFoundException) {
            // 从 jar 中查找
            return loadClassFromJar(name)
        }
    }

    /**
     * 兼容 ClassLoader API（findClass 是 protected，必须重写才能被父类 loadClass 调用）
     */
    @Throws(ClassNotFoundException::class)
    override fun findClass(name: String): Class<*> {
        return loadClassFromJar(name)
    }

    /**
     * 从 jar 中加载类
     */
    private fun loadClassFromJar(name: String): Class<*> {
        val classFileName = name.replace('.', '/') + ".class"
        val entry = jarFile.getJarEntry(classFileName)
            ?: throw ClassNotFoundException("Class not found in jar: $name")

        val bytes = jarFile.getInputStream(entry).use { it.readBytes() }
        return defineClass(name, bytes, 0, bytes.size)
    }
}