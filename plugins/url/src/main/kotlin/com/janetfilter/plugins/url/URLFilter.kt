/*
 * ============================================================================
 * - URLFilter.kt - URL 过滤器（运行时部分）
 * ----------------------------------------------------------------------------
 * 提供 URL 拦截功能，由 HttpClientTransformer 在字节码层面
 * 把 sun.net.www.http.HttpClient 的相关方法替换为调用本类。
 *
 * 工作原理：
 *   - HttpClient 是 JDK 内部用于 HTTP 通信的类
 *   - testURL() 在 URL 创建或访问时拦截
 *   - 命中规则则修改 URL 或返回错误
 *
 * 配置示例 (url.conf)：
 *   [URL]
 *   PREFIX,https://account.jetbrains.com/lservice/rpc/validateKey.action
 *   PREFIX,https://account.jetbrains.com/lservice/rpc/validateLicense.action
 * ============================================================================
 */
package com.janetfilter.plugins.url

import com.janetfilter.core.models.FilterRule
import java.net.URL

/**
 * URL 过滤器运行时类
 */
object URLFilter {

    /** 规则列表 */
    private var ruleList: List<FilterRule> = emptyList()

    /**
     * 设置规则列表
     */
    @JvmStatic
    fun setRules(rules: List<FilterRule>) {
        ruleList = rules
        println("[url] Loaded ${rules.size} rules")
    }

    /**
     * 测试 URL
     *
     * @param url 要检查的 URL
     * @return 替换后的 URL（如果未命中则返回原 url）
     */
    @JvmStatic
    @Throws(java.io.IOException::class)
    fun testURL(url: URL): URL {
        if (ruleList.isEmpty()) return url

        val urlString = url.toString()
        for (rule in ruleList) {
            if (rule.test(urlString)) {
                // URL 被规则命中，重定向到无效地址
                return URL("http://127.0.0.1/blocked")
            }
        }

        return url
    }
}