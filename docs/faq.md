# 常见问题 (FAQ)

## 一、构建和编译问题

### Q1: 编译时找不到 ASM 库？

**A**: 检查 `build.gradle.kts` 中的依赖配置：

```kotlin
dependencies {
    implementation("org.ow2.asm:asm:9.9")
    implementation("org.ow2.asm:asm-tree:9.9")
    implementation("org.ow2.asm:asm-commons:9.9")
}
```

确保 mavenCentral()  仓库已配置：

```kotlin
repositories {
    mavenCentral()
}
```

### Q2: Gradle 构建失败？

**A**: 检查以下几点：
1. JDK 版本 ≥ 17
2. 设置 `JAVA_HOME` 环境变量
3. 检查网络连接（需要访问 Maven 仓库）
4. 清理后重新构建：`gradlew clean build`

### Q3: Kotlin 编译错误？

**A**: 常见的 Kotlin 错误：
- `Type mismatch`: 检查类型转换
- `Unresolved reference`: 确认导入语句
- `Overload resolution ambiguity`: 检查函数签名

### Q4: 如何生成 fat jar？

**A**: 在 build.gradle.kts 中添加：

```kotlin
plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

tasks.shadowJar {
    archiveBaseName.set("ja-netfilter")
    archiveClassifier.set("all")
    manifest {
        attributes["Premain-Class"] = "com.janetfilter.core.Launcher"
    }
}
```

## 二、运行时问题

### Q5: 启动 IDE 时报错？

**A**: 常见错误及解决方法：

**错误 1: `java.lang.ClassNotFoundException`**
- 检查 ja-netfilter.jar 是否在正确路径
- 检查 plugins-jetbrains/ 目录是否存在

**错误 2: `java.lang.VerifyError`**
- 字节码注入问题
- 检查 plugin 是否兼容当前 IDE 版本
- 尝试更新 ja-netfilter 版本

**错误 3: `java.lang.UnsupportedClassVersionError`**
- JDK 版本不兼容
- 使用 JDK 17+ 运行

### Q6: 激活码无效？

**A**: 检查以下事项：
1. `-javaagent` 参数格式正确
2. power.conf 中的规则是最新版本
3. 完全退出 JetBrains 账户（Help → Register → Remove License）
4. 重启 IDE

### Q8: 日志在哪里？

**A**: 日志文件位于 `logs/ja-netfilter-<pid>.log`。

可以通过以下方式查看：
- 控制台输出（如果有 attach terminal）
- 文件输出（在 ja-netfilter.jar 同级目录的 logs/ 子目录）

### Q9: 性能问题？

**A**: 常见性能优化：
1. 减少插件数量（只启用需要的）
2. 减少规则数量
3. 检查是否有错误的循环注入
4. 使用 L2 缓存（power 插件已实现）

## 三、配置问题

### Q10: power.conf 中的大数怎么填？

**A**: 大数使用十进制字符串：

```
公钥指数 e = 65537
公钥模数 n = 1234567890...（约 309 位十进制）

）
```

可以从 [ckey.run](https://ckey.run) 获取最新的 power.conf。

### Q11: 如何添加自定义规则？

**A**: 在对应配置文件中添加：

```ini
[SectionName]
TYPE,rule_pattern
```

例如：
```ini
[DNS]
EQUAL,blocked-domain.com
PREFIX,*.ads.example.com
```

### Q12: 如何禁用某个插件？

**A**: 修改插件文件名，添加 `.disabled` 后缀：

```bash
mv plugins-jetbrains/dns.jar plugins-jetbrains/dns.jar.disabled
```

### Q13: power.conf 规则冲突怎么办？

**A**: 规则按顺序匹配，第一条匹配的规则生效。
建议：
- 将最具体的规则放在前面
- 使用更精确的规则类型（EQUAL 比 KEYWORD 优先）

## 四、IDE 集成问题

### Q14: IDEA 无法识别 -javaagent 参数？

**A**: 检查 vmoptions 文件的位置：

**Windows**:
```
%APPDATA%\JetBrains\<product>\<product>.vmoptions
```

**Linux**:
```
~/.config/JetBrains/<product>/<product>.vmoptions
```

**macOS**:
```
~/Library/Application Support/JetBrains/<product>/<product>.vmoptions
```

### Q15: Toolbox 安装的 IDE 怎么配置？

**A**: Toolbox 管理的 IDE 使用独立配置：
1. 打开 Toolbox
2. 右键点击 IDE → Settings → Java arguments
3. 添加 `-javaagent:/path/to/ja-netfilter.jar=jetbrains`

或者使用环境变量：
```
IDEA_VM_OPTIONS=/path/to/vmoptions/idea.vmoptions
```

### Q16: Toolbox App 不允许修改 vmoptions？

**A**: Toolbox 2.0+ 会忽略 IDE 自带的 vmoptions。
解决方案：
1. 在 Toolbox 中修改 Java arguments
2. 或者使用独立安装（非 Toolbox）

## 五、安全和合规

### Q17: 这样激活合法吗？

**A**: 本项目仅供学习 Java Agent 技术。请勿用于商业用途或违反 JetBrains
服务条款的行为。

### Q18: 会被检测到吗？

**A**: 风险点：
1. JetBrains 可能更新 IDE 内部 API 导致插件失效
2. 部分反作弊机制可能检测到签名异常
3. 使用 hideme 插件可以降低被检测的概率

### Q19: 会被远程禁用吗？

**A**: 由于是离线激活，理论上不会被远程禁用。但：
1. JetBrains 可能更新后无法激活
2. 建议保留原版的激活码以备使用

## 六、开发问题

### Q20: 如何添加新插件？

**A**: 步骤：

```bash
# 1. 在 plugins/ 目录下创建新模块
mkdir -p plugins/myplugin/src/main/kotlin/com/janetfilter/plugins/myplugin
mkdir -p plugins/myplugin/src/main/resources/META-INF

# 2. 创建 build.gradle.kts
cat > plugins/myplugin/build.gradle.kts << 'EOF'
plugins {
    kotlin("jvm")
}
dependencies {
    implementation(project(":ja-netfilter"))
    implementation("org.ow2.asm:asm:9.9")
}
EOF

# 3. 在 settings.gradle.kts 中添加
echo 'include("plugins:myplugin")' >> settings.gradle.kts

# 4. 实现插件代码
# 5. 创建 META-INF/MANIFEST.MF
```

### Q21: 如何调试插件？

**A**: 调试技巧：

```kotlin
// 在 PluginEntry.init() 中添加日志
override fun init(env: Environment, config: PluginConfig?) {
    println("[myplugin] Loading with ${config?.getBySection("My")?.size ?: 0} rules")

    // 测试 transformer
    transformers.add(MyTransformer())
}

// 在 transformer 中添加日志
override fun transform(...): ByteArray? {
    println("[myplugin] Transforming $className")
    return null
}
```

### Q22: 如何处理 ClassCircularityError？

**A**: 当插件类与目标类有循环依赖时会发生。

解决方案：
```kotlin
// 使用 lazy 初始化
private val myFilter by lazy { MyFilter() }

override fun transform(...): ByteArray? {
    val filter = myFilter  // 第一次访问时才初始化
    // ...
}
```

## 七、特定 IDE 问题

### Q23: IDEA 2025.x 激活后仍然提示？

**A**: 某些版本的 IDEA 使用了新的验证机制。
- 确认 power.conf 是最新版本
- 确认已完全退出 JB 账户
- 清除 IDEA 缓存：`~/.cache/JetBrains/<product>`

### Q24: Gateway 不能激活？

**A**: Gateway 是较新的产品：
- 使用更新的 power.conf
- 确认插件支持

### Q25: Aqua 不能激活？

**A**: 同 Gateway 问题。

### Q26: JetBrains Client 不能激活？

**A**: 注意区分 `jetbrains_client` 和 `jetbrainsclient`（两个产品名）。

## 八、技术细节

### Q27: 字节码注入的原理？

**A**: JVM 的 Instrumentation API 允许在类加载时修改字节码：

```kotlin
instrumentation.addTransformer(ClassFileTransformer { loader, className, classBeingRedefined, domain, classBuffer ->
    // 修改 classBuffer
    return newBytes
})
```

### Q28: modPow 是怎么被拦截的？

**A**: power 插件的 ResultTransformer 修改 `BigInteger.modPow` 方法的字节码：

```java
// 原始
public BigInteger modPow(BigInteger e, BigInteger m) {
    return montgomeryMultiplication(...);
}

// 注入后
public BigInteger modPow(BigInteger e, BigInteger m) {
    BigInteger result = montgomeryMultiplication(...);

    // 注入的代码：
    BigInteger filtered = ResultFilter.testFilter(this, e, m);
    if (filtered != null) {
        return filtered;
    }
    return result;
}
```

### Q29: 为什么使用 ASM 而不是 javassist？

**A**: ASM 的优势：
1. 性能更好
2. API 更底层
3. 不需要运行时类型信息
4. 兼容所有 JDK 版本

javassist 更简单，但 ASM 在生产环境中更常用。

### Q30: Power 插件的缓存是怎么工作的？

**A**: 两级缓存：

```kotlin
object ResultFilter {
    // L1: key 集合，用于快速否定
    private val l1Cached: MutableSet<String> = mutableSetOf()

    // L2: 完整的 BigInteger 替换值
    private val l2Cached: MutableMap<String, BigInteger> = mutableMapOf()
}
```

L1 用于快速判断 key 是否在缓存中（O(1) 查找）；
L2 用于存储实际的替换值（lazy 解析 BigInteger）。

## 九、其他

### Q31: 我可以自己修改 ja-netfilter 吗？

**A**: 可以，本项目就是完整的 Kotlin 源码。
欢迎贡献代码！

### Q32: 有什么推荐的学习资源？

**A**:
1. [JVM TI 官方文档](https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html)
2. [ASM 字节码库](https://asm.ow2.io/)
3. [Java Agent 入门](https://www.baeldung.com/java-instrumentation)
4. [Effective Java](https://www.oreilly.com/library/view/effective-java/9780134685991/)

### Q33: 反馈问题在哪里提？

**A**: 请在本项目的 Issue 页面提交。