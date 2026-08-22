# 插件实现详解

> 详细介绍每个插件的实现细节、字节码注入位置和配置格式。

## 一、DNS 过滤插件 (dns)

### 1.1 功能

拦截 `java.net.InetAddress` 的 DNS 查询，阻止特定域名的解析。

### 1.2 实现

**核心文件**:
- `DNSFilter.kt`: 运行时过滤器
- `InetAddressTransformer.kt`: 字节码转换器
- `DNSFilterPlugin.kt`: 插件入口

### 1.3 hook 目标

`java.net.InetAddress`

### 1.4 修改的方法

- `getAllByName0(String host, InetAddress reqAddr, boolean includeCaches)`: 主要的 DNS 解析方法

### 1.5 字节码注入

```kotlin
// 在 getAllByName0 方法最开始插入：
// 加载 host 参数
aload_1

// 调用 DNSFilter.testQuery(host)
invokestatic com/janetfilter/plugins/dns/DNSFilter.testQuery(Ljava/lang/String;)Ljava/lang/String;

// 移除返回值（保持栈平衡）
pop
```

### 1.6 配置格式 (dns.conf)

```ini
[DNS]
EQUAL,jetbrains.com
EQUAL,plugin.obroom.com
```

## 二、URL 过滤插件 (url)

### 2.1 功能

拦截 HTTP 请求，阻止与 JetBrains 许可证服务器的通信。

### 2.2 实现

**核心文件**:
- `URLFilter.kt`: 运行时过滤器
- `HttpClientTransformer.kt`: 字节码转换器
- `URLFilterPlugin.kt`: 插件入口

### 2.3 hook 目标

`sun.net.www.http.HttpClient`

### 2.4 修改的方法

构造方法：
- `HttpClient(URL url, Proxy proxy, int connectTimeout)`
- `HttpClient(URL url, Proxy proxy, int connectTimeout, RedirectPermissions)`

### 2.5 字节码注入

```kotlin
// 在构造方法最开始：
// 加载第一个参数 URL
aload_1

// 调用 URLFilter.testURL(url)
invokestatic com/janetfilter/plugins/url/URLFilter.testURL(Ljava/net/URL;)Ljava/net/URL;

// 保存返回值到 slot 1
astore_1
```

### 2.6 配置格式 (url.conf)

```ini
[URL]
PREFIX,https://account.jetbrains.com/lservice/rpc/validateKey.action
PREFIX,https://account.jetbrains.com/lservice/rpc/validateLicense.action
PREFIX,https://account.jetbrains.com/lservice/rpc/obtainAgreement.action
PREFIX,https://account.jetbrains.com/lservice/rpc/obtainLicense.action
PREFIX,https://account.jetbrains.com/lservice/rpc/fetchData.action
```

## 三、ENV 环境变量过滤插件 (env)

### 3.1 功能

拦截 `System.getenv()` 调用，修改返回值。

### 3.2 实现

**核心文件**:
- `EnvFilter.kt`: 运行时过滤器
- `ProcessEnvironmentTransformer.kt`: 字节码转换器
- `EnvFilterPlugin.kt`: 插件入口

### 3.3 hook 目标

`java.lang.ProcessEnvironment`

### 3.4 修改的方法

- `getenv(String)`: 获取单个环境变量
- `getenv()`: 获取所有环境变量

### 3.5 字节码注入

```kotlin
// 在 getenv() 方法最开始：
// 复制栈顶（返回的 Map）
dup

// 调用 EnvFilter.testFilter(Map)
invokestatic com/janetfilter/plugins/env/EnvFilter.testGetEnv(Ljava/util/Map;)Ljava/util/Map;
```

### 3.6 配置格式 (env.conf)

```ini
[ENV]
EQUAL,SPECIFIC_VAR_NAME
```

## 四、Native 包装插件 (native)

### 4.1 功能

包装 JNI native 方法调用，支持 native-method-prefix。

### 4.2 实现

**核心文件**:
- `WrapperTransformer.kt`: 字节码转换器
- `NativePlugin.kt`: 插件入口

### 4.3 hook 目标

`java.lang.ClassLoader`

### 4.4 修改的方法

- `loadClass(String name)`: 类加载

### 4.5 字节码注入

```kotlin
// 在 loadClass 方法最开始：
// 加载 native prefix
ldc "wrapped_"

// 加载类名
aload_1

// 调用 String.startsWith
invokevirtual java/lang/String.startsWith(Ljava/lang/String;)Z

// 如果匹配：截取 prefix 后的部分
ifeq SKIP_LABEL
aload_1
ldc 8  // "wrapped_".length()
invokevirtual java/lang/String.substring(I)Ljava/lang/String;
astore_1
SKIP_LABEL:
```

### 4.6 配置格式 (native.conf)

```ini
[Class]
EQUAL,com.example.TargetClass
```

## 五、隐藏模式插件 (hideme)

### 5.1 功能

从 `ManagementFactory` 返回值中过滤掉 ja-netfilter 相关参数。

### 5.2 实现

**核心文件**:
- `VmArgumentFilter.kt`: 运行时过滤器
- `VMTransformer.kt`: 字节码转换器
- `ClassNameTransformer.kt`: 字节码转换器
- `HideMePlugin.kt`: 插件入口

### 5.3 hook 目标

- `sun.management.VMManagementImpl`
- `java.lang.Class`

### 5.4 修改的方法

- `getVmArguments()`: 返回 VM 启动参数

### 5.5 字节码注入

```kotlin
// 在 getVmArguments() 方法最开始：
// 复制栈顶（返回的 List）
dup

// 调用 VmArgumentFilter.testArgs(List)
invokestatic com/janetfilter/plugins/hideme/VmArgumentFilter.testArgs(Ljava/util/List;)Ljava/util/List;
```

### 5.6 工作原理

```kotlin
object VmArgumentFilter {
    fun testArgs(args: List<String>): List<String> {
        return args.filterNot { arg ->
            arg.contains("ja-netfilter") || arg.contains("jetbrains")
        }
    }
}
```

## 六、大数运算拦截插件 (power)

### 6.1 功能

这是整个框架的核心插件。通过 hook `BigInteger.modPow` 实现 RSA 签名验证绕过。

### 6.2 实现

**核心文件**:
- `ArgsFilter.kt`: 参数过滤器
- `ResultFilter.kt`: 结果过滤器
- `ArgsTransformer.kt`: 参数转换器
- `ResultTransformer.kt`: 结果转换器
- `PowerPlugin.kt`: 插件入口

### 6.3 hook 目标

`java.math.BigInteger`

### 6.4 修改的方法

- `modPow(BigInteger exponent, BigInteger modulus)`: RSA 模幂运算

### 6.5 字节码注入

#### 6.5.1 ArgsTransformer（修改前）

```kotlin
// 在 modPow 方法最开始：
// aload_0 (this = sig)
aload_0

// aload_1 (e)
aload_1

// aload_2 (m)
aload_2

// 调用 ArgsFilter.testFilter(sig, e, m)
invokestatic com/janetfilter/plugins/power/ArgsFilter.testFilter
    (Ljava/math/BigInteger;Ljava/math/BigInteger;Ljava/math/BigInteger;)
    Lcom/janetfilter/plugins/power/ArgsFilter$BigIntegerArray;
```

#### 6.5.2 ResultTransformer（修改后）

```kotlin
// 在每个 ARETURN 前：
// 复制栈顶（返回值）
dup

// aload_0 (sig)
aload_0

// aload_1 (e)
aload_1

// aload_2 (m)
aload_2

// 调用 ResultFilter.testFilter(result, e, m)
invokestatic com/janetfilter/plugins/power/ResultFilter.testFilter
    (Ljava/math/BigInteger;Ljava/math/BigInteger;Ljava/math/BigInteger;)
    Ljava/math/BigInteger;
```

### 6.6 配置格式 (power.conf)

```ini
[Args]
EQUAL,<sig>,<e>,<n>-><expected_result>

[Result]
EQUAL,<result>,<e>,<n>-><new_result>
```

**字段说明**:
- `<sig>`: BigInteger（十进制字符串）
- `<e>`: 公钥指数
- `<n>`: 公钥模数
- `<expected_result>`: 期望的 modPow 结果
- `<new_result>`: 替换后的结果

### 6.7 RSA 签名验证流程

```
┌─────────────────────────────────────────────────┐
│  JetBrains 客户端                                │
│                                                  │
│  1. 解析激活码 → (signature, payload)            │
│                                                  │
│  2. 使用公钥 (e, n) 验证签名                     │
│     result = signature.modPow(e, n)              │
│                                                  │
│  3. 解包 PKCS#1 v1.5                             │
│     hash = 解包后的 SHA-256(payload)             │
│                                                  │
│  4. 比较 result 与 hash                          │
│     匹配 → → 验证通过                             │
└─────────────────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────┐
│  power 插件拦截                                  │
│                                                  │
│  ResultFilter.testFilter(result, e, n):         │
│    if result matches rule:                       │
│      return new_result  ← 替换！                │
│                                                  │
│  这样无论原签名是什么，验证都能通过              │
└─────────────────────────────────────────────────┘
```

### 6.8 缓存机制

```kotlin
object ResultFilter {
    private val l1Cached: MutableSet<String> = mutableSetOf()  // 缓存 key
    private val l2Cached: MutableMap<String, BigInteger> = mutableMapOf()  // 缓存 替换值

    fun testFilter(result: BigInteger, e: BigInteger, m: BigInteger): BigInteger? {
        val key = "$result,$e,$m"
        if (!l1Cached.contains(key)) return null  // 快速检查
        return l2Cached.getOrPut(key) {
            // 解析规则
        }
    }
}
```

## 七、隐私过滤插件 (privacy)

### 7.1 功能

拦截 JetBrains 内部类，阻止隐私/遥测行为。

### 7.2 实现

**核心文件**:
- `LogUtil.kt`: 日志工具
- `LicensingFacadeTransformer.kt`: 许可门面
- `ClassTransformer.kt`: Class 拦截
- `CollectionsTransformer.kt`: 集合类拦截
- `MethodTransformer.kt`: Method 拦截
- `ClassLoaderTransformer.kt`: ClassLoader 拦截
- `PluginClassLoaderTransformer.kt`: 插件 ClassLoader 拦截
- `PluginManagerCoreTransformer.kt`: 插件管理器拦截
- `PrivacyPlugin.kt`: 插件入口

### 7.3 hook 目标列表

| 目标类 | 说明 |
|--------|------|
| `com.intellij.idea.LicensingFacade` | 许可门面 |
| `java.lang.Class` | Class 类 |
| `java.util.Collections` | 集合工具 |
| `java.lang.reflect.Method` | 反射 Method |
| `java.lang.ClassLoader` | 类加载器 |
| `com.intellij.ide.plugins.cl.PluginClassLoader` | IDE 插件 ClassLoader |
| `com.intellij.ide.plugins.PluginManagerCore` | IDE 插件管理器 |

### 7.4 特点

注意包名是 `com.novitechie`，与其他插件的 `com.janetfilter.plugins.*` 不同，
这可能是为了兼容早期的 ja-netfilter 版本。

### 7.5 实现策略

当前实现主要是记录调用（hook 但不修改），因为：
1. JetBrains 内部类在不同版本中变化较大
2. 直接修改可能导致 IDE 崩溃
3. 记录模式更安全

## 八、字节码注入通用模式

### 8.1 三种基本模式

#### 模式 A: 简单前置插入

```kotlin
val insnList = InsnList()
insnList.add(MethodInsnNode(
    Opcodes.INVOKESTATIC,
    "com/example/Filter",
    "intercept",
    "()V",
    false
))
method.instructions.insertBefore(method.instructions.first, insnList)
```

#### 模式 B: 修改方法参数

```kotlin
val insnList = InsnList()
insnList.add(VarInsnNode(Opcodes.ALOAD, 1))  // 加载第一个参数
insnList.add(MethodInsnNode(
    Opcodes.INVOKESTATIC,
    "com/example/Filter",
    "transform",
    "(Ljava/lang/Object;)Ljava/lang/Object;",
    false
))
insnList.add(VarInsnNode(Opcodes.ASTORE, 1))  // 保存回 slot 1
method.instructions.insertBefore(method.instructions.first, insnList)
```

#### 模式 C: 修改方法返回值

```kotlin
// 找到所有 ARETURN 指令
val returnInstructions = mutableListOf<AbstractInsnNode>()
var insn: AbstractInsnNode? = method.instructions.first
while (insn != null) {
    if (insn.opcode == Opcodes.ARETURN) {
        returnInstructions.add(insn)
    }
    insn = insn.next
}

// 在每个 ARETURN 前插入
for (aret in returnInstructions) {
    val insnList = InsnList()
    insnList.add(InsnNode(Opcodes.DUP))  // 复制栈顶
    insnList.add(MethodInsnNode(
        Opcodes.INVOKESTATIC,
        "com/example/Filter",
        "filter",
        "(Ljava/lang/Object;)Ljava/lang/Object;",
        false
    ))
    // 此时栈: [originalResult, filteredResult]
    // 需要按规则保留一个
    method.instructions.insertBefore(aret, insnList)
}
```

### 8.2 类型系统注意事项

#### 8.2.1 基本类型

| 类型 | 操作码 | 描述符 |
|------|--------|--------|
| byte | BALOAD/BASTORE | B |
| short | SALOAD/SASTORE | S |
| int | IALOAD/IASTORE | I |
| long | LALOAD/LASTORE | J |
| float | FALOAD/FASTORE | F |
| double | DALOAD/DASTORE | D |
| boolean | IALOAD/IASTORE | Z |
| char | CALOAD/CASTORE | C |
| Object | AALOAD/AASTORE | L...; |

#### 8.2.2 数组类型

```java
int[] -> [I
Object[] -> [Ljava/lang/Object;
String[][] -> [[Ljava/lang/String;
```

### 8.3 ClassWriter 标志

```kotlin
ClassWriter(ClassWriter.COMPUTE_MAXS)  // 自动计算 max stack/locals
ClassWriter(ClassWriter.COMPUTE_FRAMES)  // 还会计算 stack map frames (JDK 7+)
```

**注意**: `COMPUTE_FRAMES` 计算成本高但兼容性更好。

## 九、调试技巧

### 9.1 字节码查看

```bash
# 使用 javap 查看修改后的字节码
javap -c -p <class_file>

# 使用 ASMifier 打印 ASM API
java -cp asm-util-9.9.jar org.objectweb.asm.util.ASMifierClassVisitor <class_file>
```

### 9.2 运行时调试

启用 DebugInfo 输出：

```kotlin
// 在 DebugInfo.kt 中修改 LOG_LEVEL
LOG_LEVEL = Level.DEBUG
```

### 9.3 常见错误

#### 错误 1: StackMapTable 不匹配

JDK 7+ 验证器要求正确的 stack map frames。
解决方案：
```kotlin
ClassWriter(ClassWriter.COMPUTE_FRAMES)
```

#### 错误 2: 方法签名不匹配

注入时使用的方法签名必须与运行时调用完全匹配。
使用 `javap -s` 查看签名。

#### 错误 3: ClassCircularityError

类之间的循环依赖。
解决方案：使用 lazy loading 或 thread-safe 初始化。