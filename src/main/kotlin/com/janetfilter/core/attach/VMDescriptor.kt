/*
 * ============================================================================
 * - VMDescriptor.kt - 虚拟机描述符
 * ----------------------------------------------------------------------------
 * 描述一个 JVM 实例的信息，用于 attach 模式。
 * ============================================================================
 */
package com.janetfilter.core.attach

/**
 * 虚拟机描述符
 *
 * @param id JVM 的 PID
 * @param className 进程主类名
 * @param args 进程参数
 */
data class VMDescriptor(
    var id: String,
    var className: String,
    var args: String
) {
    /** 是否为旧版 VM */
    var old: Boolean = false

    override fun toString(): String {
        return "VMDescriptor(id=$id, className=$className, args='${args.take(50)}...', old=$old)"
    }
}