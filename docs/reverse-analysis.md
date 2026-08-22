# 逆向分析完整报告

> 本文档详细记录了对 ja-netfilter 项目的逆向分析过程和发现。

## 一、目标概述

`ja-netfilter.jar` 是一个 Java Agent 程序，由社区开发，用于拦截 JetBrains IDE 的
许可证验证流程。本项目对该 jar 及其附带的 7 个插件 jar 进行了完整的逆向分析。

### 1.1 待分析文件清单

| 文件 | 大小 | 说明 |
|------|------|------|
| `ja-netfilter.jar` | 299,920 bytes | Java Agent 主框架 |
| `dns.jar` | 4,847 bytes | DNS 过滤插件 |
| `env.jar` | 5,187 bytes | 环境变量过滤插件 |
| `hideme.jar` | 7,177 bytes | 隐藏模式插件 |
| `native.jar` | 4,960 bytes | Native 包装插件 |
| `power.jar` | 9,193 bytes | 大数运算拦截插件 |
| `privacy.jar` | 32,671 bytes | 隐私过滤插件 |
| `url.jar` | 4,512 bytes | URL 过滤插件 |

## 二、工具准备

### 2.1 反编译工具

```bash
# jadx 命令行反编译工具
jadx-gui-1.5.6-all.jar

# 使用方法
java -jar jadx-gui-1.5.6-all.jar --no-res -d <output_dir> <input.jar>
```

### 2.2 类结构查看

```bash
# 使用 javap 查看类签名
javap -p <class_file>
```

## 三、ja-netfilter.jar 主框架分析

### 3.1 Manifest 分析

通过 `unzip -p ja-netfilter.jar META-INF/MANIFEST.MF` 提取 Manifest：

```
Manifest-Version: 1.0
Premain-Class: com.janetfilter.core.Launcher
Agent-Class: com.janetfilter.core.Launcher
Can-Redefine-Classes: true
Can-Retransform-Classes: true
Class-Path: asm-9.9.jar jar2tree-9.9.jar asm-commons-9.9.jar
Can-Set-Native-Method-Prefix: true
Created-By: Apache Maven 3.9.9
Build-Jdk: 1.8.0_452
Main-Class: com.janetfilter.core.Launcher
```

**关键发现**：
- `Premain-Class`: JVM 启动时调用的入口
- `Agent-Class`: attach 模式下调用
- `Can-Redefine-Classes`: 支持类重定义
- `Can-Set-Native-Method-Prefix`: 支持 native 方法前缀
- 内置 ASM 9.9 库（org.objectweb.asm）

### 3.2 包结构

```
com.janetfilter.core/
├── Launcher                    # 主入口
├── Initializer                 # 初始化器
├── Environment                 # 环境上下文
├── Dispatcher                  # 字节码分派器
├── commons/
│   ├── ConfigParser            # 配置文件解析
│   └── DebugInfo               # 调试日志
├── enums/
│   └── RuleType                # 规则类型枚举
├── models/
│   └── FilterRule              # 过滤规则模型
├── plugin/
│   ├── MyTransformer           # transformer 接口
│   ├── PluginEntry             # 插件入口接口
│   ├── PluginConfig            # 插件配置包装
│   ├── PluginManager           # 插件管理器
│   └── PluginClassLoader       # 插件类加载器
├── attach/
│   ├── VMDescriptor            # VM 描述
│   ├── VMSelector              # VM 选择
│   └── VMLauncher              # VM 启动
├── rulers/
│   ├── Ruler                   # 匹配器接口
│   ├── EqualRuler              # 完全匹配
│   ├── EqualICRuler             # 完全匹配（忽略大小写）
│   ├── PrefixRuler             # 前缀匹配
│   ├── PrefixICRuler            # 前缀匹配（忽略大小写）
│   ├── SuffixRuler             # 后缀匹配
│   ├── SuffixICRuler            # 后缀匹配（忽略大小写）
│   ├── KeywordRuler            # 关键字匹配
│   └── KeywordICRuler           # 关键字匹配（忽略大小写）
└── utils/
    ├── DateUtils
    ├── StringUtils
    ├── ProcessUtils
    └── WhereIsUtils
```

### 3.3 Launcher（主入口）

```java
public class com.janetfilter.core.Launcher {
  public static final String ATTACH_ARG = "attach";
  public static final String VERSION;
  public static final int VERSION_NUMBER;
  private static boolean loaded;
  
  public static void main(String[]);
  public static void premain(String, Instrumentation);
  public static void agentmain(String, Instrumentation);
  private static void premain(String, Instrumentation, boolean);
}
```

**工作流程**：
1. `premain()` 或 `agentmain()` 被调用
2. 创建 `Environment` 对象
3. 调用 `Initializer.init(env)`
4. 整个框架启动完成

### 3.4 Environment（环境上下文）

```java
public final class Environment {
  private final String pid;
  private final String version;
  private final int versionNumber;
  private final String appName;
  private final File baseDir;
  private final File agentFile;
  private final File configDir;       // config-jetbrains/
  private final File pluginsDir;      // plugins-jetbrains/
  private final File logsDir;         // logs/
  private final String nativePrefix;  // "wrapped_"
  private final String disabledPluginSuffix;  // ".disabled"
  private final boolean attachMode;
  private final Instrumentation instrumentation;
  // ...
}
```

**关键字段**：
- `configDir`: `config-jetbrains/` 目录
- `pluginsDir`: `plugins-jetbrains/` 目录
- `logsDir`: `logs/` 目录
- `nativePrefix`: 用于 native 包装的方法前缀

### 3.5 Dispatcher（字节码分派器）

```java
public final class Dispatcher implements ClassFileTransformer {
  private final Environment environment;
  private final Set<String> classSet;
  private final Map<String, List<MyTransformer>> transformerMap;
  private final List<MyTransformer> globalTransformers;
  private final List<MyTransformer> manageTransformers;
  
  public void addTransformer(MyTransformer);
  public void addTransformers(List<MyTransformer>);
  public byte[] transform(ClassLoader, String, Class<?>, ProtectionDomain, byte[]);
}
```

**核心逻辑**：
1. `transformerMap`: 类名到 transformer 列表的映射
2. `transform()` 方法：当 JVM 加载每个类时被调用
3. 根据类名找到对应的 transformer 列表，依次执行转换

### 3.6 PluginManager（插件管理器）

```java
public final class PluginManager {
  public PluginManager(Dispatcher dispatcher, Environment environment);
  public void loadPlugins();
}
```

**加载流程**：
1. 扫描 `pluginsDir` 中所有 `.jar` 文件
2. 对每个 jar 使用 `PluginClassLoader` 加载
3. 从 Manifest 读取 `Plugin-Class`（默认 "Plugin"）
4. 实例化 `PluginEntry`
5. 调用 `init(env, config)` 初始化
6. 获取 `transformers` 列表并注册

### 3.7 PluginClassLoader（插件类加载器）

```java
public final class PluginClassLoader extends ClassLoader {
  private final JarFile jarFile;
  public Class<?> findClass(String) throws ClassNotFoundException;
}
```

**特点**：
- 每个插件独立的 ClassLoader
- 实现插件隔离
- 委托给父 ClassLoader 加载 JDK 类

### 3.8 配置文件格式

```ini
[Section]
EQUAL,rule1
PREFIX,rule2
```

**示例 - power.conf**:
```ini
[Result]
EQUAL,<sig>,<e>,<n>-><expected_result>
```

**规则类型**:
- `PREFIX` / `PREFIX_IC`: 前缀匹配（_IC 表示忽略大小写）
- `SUFFIX` / `SUFFIX_IC`: 后缀匹配
- `KEYWORD` / `KEYWORD_IC`: 关键字匹配
- `EQUAL` / `EQUAL_IC`: 完全匹配
- `REGEXP`: 正则匹配

## 四、7 个插件分析

### 4.1 dns 插件（DNS 过滤）

**包名**: `com.janetfilter.plugins.dns`

**类清单**:
- `DNSFilterPlugin`: 插件入口
- `DNSFilter`: 运行时过滤器
- `InetAddressTransformer`: 字节码转换器

**hook 目标**: `java.net.InetAddress`

**工作原理**:
1. 修改 `InetAddress.getAllByName0(String, InetAddress, boolean)` 方法
2. 在方法开头插入 `DNSFilter.testQuery(host)` 调用
3. `testQuery` 检查域名是否匹配 `dns.conf` 中的规则
4. 匹配则抛出 `UnknownHostException`，实现域名屏蔽

### 4.2 env 插件（环境变量过滤）

**包名**: `com.janetfilter.plugins.env`

**类清单**:
- `EnvFilterPlugin`: 插件入口
- `EnvFilter`: 运行时过滤器
- `ProcessEnvironmentTransformer`: 字节码转换器

**hook 目标**: `java.lang.ProcessEnvironment`

**工作原理**:
1. 修改 `ProcessEnvironment.getenv(String)` 和 `getenv()` 方法
2. 在方法开头插入 `EnvFilter.testGetEnv()` 调用
3. 根据 `env.conf` 中的规则修改返回值

### 4.3 hideme 插件（隐藏模式）

**包名**: `com.janetfilter.plugins.hideme`

**类清单**:
- `HideMePlugin`: 插件入口
- `VmArgumentFilter`: VM 参数过滤器
- `ClassNameFilter`: 类名过滤器
- `ClassNameTransformer`: 字节码转换器
- `VmArgumentTransformer`（原名 `VMTransformer`）: VM 字节码转换器

**hook 目标**: `sun.management.VMManagementImpl`, `java.lang.Class`

**工作原理**:
1. 修改 `VMManagementImpl.getVmArguments()` 方法
2. 在返回前过滤掉包含 `ja-netfilter` 或 `jetbrains` 的参数
3. 隐藏 agent 加载痕迹

### 4.4 native 插件（Native 包装）

**包名**: `com.janetfilter.plugins.native_wrapper`

**类清单**:
- `NativePlugin`: 插件入口
- `WrapperTransformer`: ClassLoader 字节码转换器

**hook 目标**: `java.lang.ClassLoader`

**工作原理**:
1. 修改 `ClassLoader.loadClass(String)` 方法
2. 如果类名以 `nativePrefix`（默认 `wrapped_`）开头
3. 移除前缀后再加载类
4. 支持 native 方法的前缀包装

### 4.5 power 插件（大数运算拦截）

**包名**: `com.janetfilter.plugins.power`

**类清单**:
- `PowerPlugin`: 插件入口
- `ArgsFilter`: 参数过滤器（修改 modPow 输入）
- `ResultFilter`: 结果过滤器（修改 modPow 输出）
- `ArgsTransformer`: BigInteger 参数字节码转换器
- `ResultTransformer`: BigInteger 结果字节码转换器

**hook 目标**: `java.math.BigInteger`

**修改方法**: `BigInteger.modPow(BigInteger exponent, BigInteger modulus)`

**核心算法**:

```java
// 签名验证流程
BigInteger result = signature.modPow(exponent, modulus);
// 验证 result 是否与 PKCS#1 v1.5 解包后匹配
if (result.equals(expectedHash)) {
    // 验证通过
}
```

**拦截策略**:
1. 在 modPow 调用前/后插入 ArgsFilter/ResultFilter 调用
2. ArgsFilter 检查参数 (sig, e, n) 是否匹配 power.conf 中的规则
3. ResultFilter 检查结果 (result, e, n) 是否匹配 power.conf 中的规则
4. 匹配则返回预定义的替换值

### 4.6 privacy 插件（隐私过滤）

**包名**: `com.novitechie`（注意：不是 com.janetfilter！）

**类清单**:
- `PrivacyPlugin`: 插件入口
- `LogUtil`: 日志工具
- `ClassLoaderTransformer`
- `ClassTransformer`
- `CollectionsTransformer`
- `LicensingFacadeTransformer`
- `MethodTransformer`
- `PluginClassLoaderTransformer`
- `PluginManagerCoreTransformer`

**hook 目标**: JetBrains 内部类

**工作原理**:
该插件主要拦截 JetBrains 内部类，阻止隐私相关行为：
- `com.intellij.idea.LicensingFacade`
- `com.intellij.ide.plugins.cl.PluginClassLoader`
- `com.intellij.ide.plugins.PluginManagerCore`
- `java.util.Collections`
- `java.lang.reflect.Method`

**特点**: 注意包名是 `com.novitechie` 而非 `com.janetfilter.plugins.*`，
这可能是为了兼容早期的 ja-netfilter 版本。

### 4.7 url 插件（URL 过滤）

**包名**: `com.janetfilter.plugins.url`

**类清单**:
- `URLFilterPlugin`: 插件入口
- `URLFilter`: URL 过滤器
- `HttpClientTransformer`: 字节码转换器

**hook 目标**: `sun.net.www.http.HttpClient`

**工作原理**:
1. 修改 `HttpClient` 构造方法
2. 在构造时插入 `URLFilter.testURL(url)` 调用
3. 匹配 `url.conf` 中的规则则重定向到 `127.0.0.1/blocked`

## 五、激活码格式分析

### 5.1 格式

```
LICENSE_ID-BASE64_PAYLOAD-BASE64_SIGNATURE
```

例如：
```
7FB23A91A2-eyJsaWNlbnNlSWQ...-MIIEsTCCApmgAw...
```

### 5.2 三段结构

#### 第一段：LICENSE_ID
- 10 个十六进制字符
- 例如：`7FB23A91A2`

#### 第二段：PAYLOAD（Base64 编码的 JSON）

```json
{
  "licenseId": "7FB23A91A2",
  "licenseeName": "ckey.run",
  "assigneeName": "",
  "products": [
    {
      "code": "PS",
      "fallbackDate": "2099-12-31",
      "paidUpTo": "2099-12-31"
    }
  ],
  "metadata": "0120230914PSAX000000"
}
```

#### 第三段：SIGNATURE（Base64 编码的 RSA 签名）

签名是使用 ckey.run 私钥对 `SHA-256(PAYLOAD)` 的 RSA PKCS#1 v1.5 签名。

### 5.3 RSA 签名验证

JetBrains 客户端代码大致逻辑：

```java
// 客户端验证流程
byte[] payload = Base64.getUrlDecoder().decode(payloadBase64);
byte[] signature = Base64.getUrlDecoder().decode(signatureBase64);

// 使用公钥验证签名
SignatureSignature = Signature.getInstance("SHA256withRSA");
signature.initVerify(cpublicKey);
signature.update(payload);
boolean valid = signature.verify(signature);
```

由于 RSA 签名验证的内部使用 `BigInteger.modPow`，
power 插件通过替换 modPow 的结果来实现激活。

## 六、配置文件详解

### 6.1 dns.conf

```ini
[DNS]
EQUAL,jetbrains.com
EQUAL,plugin.obroom.com
```

### 6.2 url.conf

```ini
[URL]
PREFIX,https://account.jetbrains.com/lservice/rpc/validateKey.action
PREFIX,https://account.jetbrains.com/lservice/rpc/validateLicense.action
PREFIX,https://account.jetbrains.com/lservice/rpc/obtainAgreement.action
PREFIX,https://account.jetbrains.com/lservice/rpc/obtainLicense.action
PREFIX,https://account.jetbrains.com/lservice/rpc/fetchData.action
```

### 6.3 power.conf（最关键）

```ini
[Result]
EQUAL,<signature>,<exponent>,<modulus>-><expected_result>
```

**字段说明**：
- `<signature>`: 签名的 BigInteger 值（十进制字符串）
- `<exponent>`: 公钥指数 e（通常为 65537）
- `<modulus>`: 公钥模数 n
- `<expected_result>`: 期望的 modPow 结果

**典型值**（截至 2025-10）：
- exponent = 65537
- modulus 是 1024 位的十进制数
- signature 是签名对应的 BigInteger
- expected_result 是 licenseData 的 SHA-256 经过 PKCS#1 v1.5 编码后的结果

## 七、关键字节码注入技术

### 7.1 ASM 字节码操作

整个项目使用 ASM 9.9 进行字节码操作：

```kotlin
import org.objectweb.asm.*
import org.objectweb.asm.tree.*

val classReader = ClassReader(byteArray)
val classNode = ClassNode()
classReader.accept(classNode, 0)

// 修改方法
for (method in classNode.methods) {
    if (method.name == "targetMethod") {
        // 在方法开头插入新指令
        val insnList = InsnList()
        insnList.add(MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "com/example/Filter",
            "intercept",
            "()V",
            false
        ))
        method.instructions.insertBefore(method.instructions.first, insnList)
    }
}

// 生成新字节码
val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
classNode.accept(classWriter)
val newBytes = classWriter.toByteArray()
```

### 7.2 典型注入模式

#### 模式 A：调用前拦截

```java
// 原始方法
public Object targetMethod(String input) {
    return process(input);
}

// 注入后（伪代码）
public Object targetMethod(String input) {
    // 插入的代码
    Object result = Filter.intercept(input);
    if (result != null) return result;
    return process(input);
}
```

#### 模式 B：调用后替换

```java
// 原始方法
public BigInteger modPow(BigInteger e, BigInteger m) {
    return computeModPow(e, m);
}

// 注入后
public BigInteger modPow(BigInteger e, BigInteger m) {
    BigInteger result = computeModPow(e, m);
    // 插入的代码
    BigInteger filtered = ResultFilter.testFilter(result, e, m);
    return filtered != null ? filtered : result;
}
```

## 八、攻击链分析

### 8.1 完整激活流程

```
1. 用户在 IDE 中粘贴激活码
   ↓
2. IDE 解析激活码为 (licenseId, signature)
   ↓
3. IDE 从服务端获取公钥 (e, n)
   ↓
4. IDE 计算 result = signature.modPow(e, n)
   ↓
5. power 插件拦截 modPow，返回预定义 result
   ↓
6. IDE 使用 result 验证签名
   ↓
7. 验证通过，激活成功
```

### 8.2 power 插件核心逻辑

```kotlin
// ArgsFilter（修改 modPow 输入）
fun testFilter(sig, e, m): BigInteger[]? {
    val key = "$sig,$e,$m"
    if (ruleMatches(key)) {
        // 命中规则，返回替换的签名
        return BigIntegerArray(newSig, expectedResult)
    }
    return null
}

// ResultFilter（修改 modPow 输出）
fun testFilter(result, e, m): BigInteger? {
    val key = "$result,$e,$m"
    if (ruleMatches(key)) {
        // 命中规则，返回替换的结果
        return expectedResult
    }
    return null
}
```

## 九、安全分析

### 9.1 风险点

1. **代码注入风险**: Java Agent 可以在任何类加载时修改字节码
2. **数据泄露风险**: privacy 插件阻止了部分 JetBrains 遥测
3. **兼容性问题**: 不同 JetBrains 版本可能使用不同的内部 API
4. **JDK 兼容性**: 项目基于 JDK 1.8 构建，但需要 JDK 17 运行

### 9.2 安全实践

- 仅从可信源获取 ja-netfilter.jar
- 使用 sandbox 环境测试
- 定期检查 power.conf 更新
- 不在生产环境中使用

## 十、复现评估

### 10.1 复现难度

| 组件 | 难度 | 备注 |
|------|------|------|
| 主框架 | ⭐⭐ | 标准 Java Agent 模式 |
| 字节码注入 | ⭐⭐⭐⭐ | 需要 ASM 知识 |
| RSA 签名替换 | ⭐⭐⭐ | 需要密码学知识 |
| 7 个插件 | ⭐⭐⭐ | 每个都是独立的字节码 hook |
| 配置文件 | ⭐ | 简单 INI 格式 |

### 10.2 关键点

1. **RSA 公钥参数**: 从 power.conf 中提取 (e, n)
2. **签名格式**: SHA-256 + RSA PKCS#1 v1.5
3. **激活码格式**: `LICENSE_ID-BASE64_PAYLOAD-BASE64_SIGNATURE`
4. **plugin 加载**: 通过 Manifest 的 `Plugin-Class` 字段

## 十二、附录

### 12.1 工具链版本

- JDK: 1.8.0_452 (编译) / 17 (运行)
- ASM: 9.9
- Kotlin: 1.9.25
- Gradle: 8.x

### 12.2 参考资料

- [JVM TI 官方文档](https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html)
- [ASM 字节码库](https://asm.ow2.io/)
- [Java Agent 规范](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html)
- [RSA 签名标准](https://datatracker.ietf.org/doc/html/rfc3447)

### 12.3 关键时间戳

- 逆向完成: 2025-10-31（原始 jar 构建日期）
- Kotlin 重制完成: 2026-08-22