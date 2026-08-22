/*
 * ============================================================================
 * - DebugInfo.kt - 调试日志
 * ----------------------------------------------------------------------------
 * 框架统一的日志输出类，支持控制台和文件两种输出方式。
 *
 * 日志级别（Level）：DEBUG / INFO / WARN / ERROR
 * 输出位置（OUTPUT）：CONSOLE / FILE / BOTH
 *
 * 默认输出到控制台，可通过 useFile(dir) 切换到文件。
 * 文件名格式：ja-netfilter-<PID>.log
 * ============================================================================
 */
package com.janetfilter.core.commons

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

/**
 * 调试日志
 *
 * 单例对象，提供静态日志方法。
 * 异步写入文件以避免阻塞主流程。
 */
object DebugInfo {

    /** 控制台输出标志 */
    const val OUTPUT_CONSOLE: Long = 1L shl 0

    /** 文件输出标志 */
    const val OUTPUT_FILE: Long = 1L shl 1

    /** 输出包含 PID */
    const val OUTPUT_WITH_PID: Long = 1L shl 2

    /** 类名常量 */
    private const val CLASS_NAME = "ja-netfilter"

    /** 日志模板：时间 [LEVEL] (类名/PID) - 消息 */
    private const val LOG_TEMPLATE = "%s [%s] (%s/%s) - %s"

    /** 时间格式 */
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    /** 当前 PID */
    private val PID: String = ProcessHandle.current().pid().toString()

    /** 当前日志级别 */
    @Volatile
    private var LOG_LEVEL: Level = Level.INFO

    /** 当前输出方式 */
    @Volatile
    private var LOG_OUTPUT: Long = OUTPUT_CONSOLE

    /** 日志目录 */
    @Volatile
    private var logDir: File? = null

    /** 控制台输出执行器 */
    private val CONSOLE_EXECUTOR = Executors.newSingleThreadExecutor { r ->
        Thread(r, "jnf-log-console").apply { isDaemon = true }
    }

    /** 文件输出执行器 */
    private val FILE_EXECUTOR = Executors.newSingleThreadExecutor { r ->
        Thread(r, "jnf-log-file").apply { isDaemon = true }
    }

    /**
     * 设置日志输出到文件
     */
    @JvmStatic
    fun useFile(dir: File) {
        logDir = dir
        LOG_OUTPUT = LOG_OUTPUT or OUTPUT_FILE
    }

    /**
     * 获取当前日志级别
     */
    @JvmStatic
    fun getLogLevel(): Level = LOG_LEVEL

    /**
     * 获取当前输出方式
     */
    @JvmStatic
    fun getLogOutput(): Long = LOG_OUTPUT

    /**
     * DEBUG 级别日志
     */
    @JvmStatic
    fun debug(message: String) {
        output(Level.DEBUG, message, null)
    }

    @JvmStatic
    fun debug(message: String, throwable: Throwable) {
        output(Level.DEBUG, message, throwable)
    }

    /**
     * INFO 级别日志
     */
    @JvmStatic
    fun info(message: String) {
        output(Level.INFO, message, null)
    }

    @JvmStatic
    fun info(message: String, throwable: Throwable) {
        output(Level.INFO, message, throwable)
    }

    /**
     * WARN 级别日志
     */
    @JvmStatic
    fun warn(message: String) {
        output(Level.WARN, message, null)
    }

    @JvmStatic
    fun warn(message: String, throwable: Throwable) {
        output(Level.WARN, message, throwable)
    }

    /**
     * ERROR 级别日志
     */
    @JvmStatic
    fun error(message: String) {
        output(Level.ERROR, message, null)
    }

    @JvmStatic
    fun error(message: String, throwable: Throwable) {
        output(Level.ERROR, message, throwable)
    }

    /**
     * 输出日志
     */
    @JvmStatic
    fun output(message: String) {
        output(Level.INFO, message, null)
    }

    @JvmStatic
    fun output(message: String, throwable: Throwable) {
        output(Level.INFO, message, throwable)
    }

    /**
     * 核心输出方法
     */
    @JvmStatic
    fun output(level: Level, message: String, throwable: Throwable?) {
        // 级别过滤
        if (level.ordinal < LOG_LEVEL.ordinal) return

        val timestamp = LocalDateTime.now().format(TIME_FORMATTER)
        val line = String.format(LOG_TEMPLATE, timestamp, level.name, CLASS_NAME, PID, message)
        val fullLine = if (throwable != null) {
            val sw = java.io.StringWriter()
            throwable.printStackTrace(java.io.PrintWriter(sw))
            "$line\n${sw.toString()}"
        } else {
            line
        }

        // 控制台输出
        if (LOG_OUTPUT and OUTPUT_CONSOLE != 0L) {
            CONSOLE_EXECUTOR.execute {
                println(fullLine)
            }
        }

        // 文件输出
        if (LOG_OUTPUT and OUTPUT_FILE != 0L) {
            val dir = logDir
            if (dir != null) {
                FILE_EXECUTOR.execute {
                    try {
                        if (!dir.exists()) dir.mkdirs()
                        val logFile = File(dir, "ja-netfilter-$PID.log")
                        logFile.appendText(fullLine + System.lineSeparator())
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
    }

    /**
     * 日志级别
     */
    enum class Level {
        DEBUG, INFO, WARN, ERROR
    }
}