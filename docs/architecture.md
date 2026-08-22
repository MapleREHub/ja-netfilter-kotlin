# 架构设计文档

> 详细介绍 ja-netfilter Kotlin 版的架构设计、模块划分和数据流。

## 一、整体架构

### 1.1 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                  JetBrains IDE 进程                          │
│  ┌───────────────────────────────────────────────────────┐ │
│  │          ja-netfilter.jar (Java Agent)               │ │
│  │                                                       │ │
│  │  ┌───────────────────────────────────────────────┐  │ │
│  │  │   Launcher (premain/agentmain)                │  │ │
│  │  └────────────────┬──────────────────────────────┘  │ │
│  │                   ↓                                  │ │
│  │  ┌───────────────────────────────────────────────┐  │ │
│  │  │   Initializer                                  │  │ │
│  │  │   • 创建 Environment                          │  │ │
│  │  │   • 创建 Dispatcher                           │  │ │
│  │  │   • 加载配置                                  │  │ │
│  │  │   • 加载插件                                  │  │ │
│  │  └────────────────┬──────────────────────────────┘  │ │
│  │                   ↓                                  │ │
│  │  ┌──────────────┐  ┌─────────────────────────────┐  │ │
│  │  │  Dispatcher  │←─│  PluginManager              │  │ │
│  │  │  (分派器)    │  │  • 扫描 plugins 目录        │  │ │
│  │  │              │  │  • 加载 jar 文件            │  │ │
│  │  └──────┬───────┘  │  • 注册 transformer        │  │ │
│  │         │          └─────────────────────────────┘  │ │
│  │         ↓                                            │ │
│  │  ┌──────────────────────────────────────────────┐  │ │
│  │  │   transformers (来自各插件)                  │  │ │
│  │  └──────────────────────────────────────────────┘  │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐ │
│  │   IDE 代码（被修改字节码）                            │ │
│  │   java.math.BigInteger.modPow                        │ │
│  │   java.net.InetAddress.*                            │ │
│  │   sun.net.www.http.HttpClient                       │ │
│  │   ...                                                │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 关键组件关系

```
Launcher
  ↓ creates
Environment
  ↓ passed to
Initializer
  ↓ creates
Dispatcher ─────→ Instrumentation (JVM)
  ↓ registered in
PluginManager
  ↓ loads
Plugins (DNS, ENV, HIDEME, Native, Privacy, Power, URL)
  ↓ provides
MyTransformer list → Dispatcher
```

## 二、核心数据流

### 2.1 启动流程

```
1. JVM 启动 -javaagent 参数
   ↓
2. Launcher.premain() 被调用
   ↓
3. 创建 Environment 对象（读取 agentArgs, 定位 jar）
   ↓
4. Initializer.init(env) 调用
   ↓
5. 创建 Dispatcher，注册到 Instrumentation
   ↓
6. PluginManager 扫描 plugins 目录
   ↓
7. 对每个 jar:
   - PluginClassLoader 加载
   - 实例化 PluginEntry
   - 调用 init(env, config)
   - 获取 transformers
   - 注册到 Dispatcher
   ↓
8. 框架初始化完成，等待 JVM 加载类
```

### 2.2 字节码转换流程

```
JVM 加载类 (e.g. java.math.BigInteger)
   ↓
Instrumentation 调用 Dispatcher.transform()
   ↓
Dispatcher 查找 transformerMap["java/math/BigInteger"]
   ↓
对每个 transformer:
   - 调用 preTransform() 钩子
   - 调用 transform() 主方法
   - 调用 postTransform() 钩子
   ↓
返回修改后的字节码
   ↓
JVM 使用新字节码定义类
```

### 2.3 配置文件加载

```
Initializer.init(env)
   ↓
扫描 config-jetbrains/ 目录
   ↓
对每个 .conf 文件:
   - ConfigParser.parse(file)
   - 解析 [Section] 和 TYPE,rule 行
   - 返回 Map<section, List<FilterRule>>
   ↓
传递给 PluginManager
   ↓
根据 jar 文件名查找对应的 config
   ↓
传给 PluginEntry.init(env, config)
```

## 三、模块设计

### 3.1 核心模块

#### 3.1.1 Launcher

**职责**: Java Agent 入口点

**关键方法**:
- `premain(String, Instrumentation)`: JVM 启动时调用
- `agentmain(String, Instrumentation)`: attach 模式调用
- `main(String[])`: 命令行运行入口

**实现要点**:
- 静态初始化只执行一次
- 通过 `loaded` 标志防止重复加载
- 捕获所有异常并打印到 DebugInfo

#### 3.1.2 Environment

**职责**: 持有框架运行所需的所有环境信息

**关键字段**:
- `pid`: 当前进程 ID
- `version`: 来自 agentArgs 的版本字符串
- `appName`: 应用名（同 version）
- `baseDir`: agent jar 所在目录
- `configDir`: `config-jetbrains/` 目录
- `pluginsDir`: `plugins-jetbrains/` 目录
- `logsDir`: `logs/` 目录
- `nativePrefix`: native 方法前缀包装
- `disabledPluginSuffix`: 禁用插件后缀

**不可变性**: 所有字段在构造后不可修改

#### 3.1.3 Initializer

**职责**: 协调整个框架的初始化流程

**关键方法**:
- `init(Environment)`: 主入口方法

**初始化步骤**:
1. 创建 Dispatcher
2. 注册到 Instrumentation
3. 扫描 config-J 目录加载 .conf 文件
4. 创建 PluginManager
5. 加载所有插件 jar

#### 3.1.4 Dispatcher

**职责**: 字节码转换的调度中心

**关键数据结构**:
```kotlin
class Dispatcher {
    private val classSet: MutableSet<String>           // 已注册类名集合
    private val transformerMap: MutableMap<String, MutableList<MyTransformer>>  // 类名 -> transformer
    private val globalTransformers: MutableList<MyTransformer>  // 全局 transformer
}
```

**关键方法**:
- `addTransformer(MyTransformer)`: 注册 transformer
- `transform(...)`: ClassFileTransformer 主回调

#### 3.1.5 PluginManager

**职责**: 扫描并加载所有插件

**关键方法**:
- `loadPlugins(configMap)`: 加载所有插件

**加载步骤**:
1. 扫描 plugins 目录
2. 对每个 jar 创建 PluginClassLoader
3. 读取 Manifest 的 `Plugin-Class`
4. 加载并实例化
6. 调用 init(env, config)
7. 注册 transformers

### 3.2 插件接口设计

#### 3.2.1 PluginEntry 接口

```kotlin
interface PluginEntry {
    fun init(env: Environment, config: PluginConfig?)
    val name: String
    val author: String
    val version: String
    val description: String
    val transformers: List<MyTransformer>
}
```

**生命周期**:
1. PluginManager 调用无参构造
2. 调用 init(env，config) 传递环境
3. 通过 transformers 属性获取所有 transformer
4. PluginManager 注册 transformer

#### 3.2.2 MyTransformer 接口

```kotlin
interface MyTransformer {
    fun hookClassName(): String?
    fun attachMode(): Boolean
    fun javaagentMode(): Boolean
    fun isManager(): Boolean

    // 生命周期钩子
    fun before(...) { }    // 转换前
    fun preTransform(...): ByteArray? = null  // 所有 transformer 之前
    fun transform(...): ByteArray? = null      // 主转换
    fun postTransform(...): ByteArray? = null // 所有 transformer 之后
    fun after(...) { }     // 转换后
}
```

**方法分类**:
- **生命周期**: `before`, `after`
- **阶段**: `preTransform`, `postTransform`
- **核心**: `transform`

### 3.3 规则匹配器

#### 3.3.1 Ruler 接口

```kotlin
fun interface Ruler {
    fun test(rule: String, input: String): Boolean
}
```

#### 3.3.2 9 种实现

| 实现 | 匹配方式 | 大小写敏感 |
|------|---------|-----------|
| EqualRuler | equals | true / false |
| PrefixRuler | startsWith | true / false |
| SuffixRuler | endsWith | true / false |
| KeywordRuler | contains | true / false |
| RegExpRuler | Pattern.matcher | (正则内部控制) |

**策略模式**: RuleType 枚举持有 Ruler 实例

## 四、字节码注入设计

### 4.1 ASM Tree API

本项目使用 ASM 的 Tree API 进行字节码操作：

```kotlin
val classReader = ClassReader(byteArray)
val classNode = ClassNode()
classReader.accept(classNode, 0)

// 修改 classNode
for (method in classNode.methods) {
    // 修改方法
}

val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
classNode.accept(classWriter)
val newBytes = classWriter.toByteArray()
```

### 4.2 注入位置

**方法开头** (最常用):
```kotlin
val insnList = InsnList()
insnList.add(...)  // 加载参数
insnList.add(MethodInsnNode(INVOKESTATIC, ...))  // 调用过滤方法
instructions.insertBefore(instructions.first, insnList)
```

**方法返回前** (用于结果替换):
```kotlin
for (insn in instructions) {
    if (insn is InsnNode && insn.opcode == ARETURN) {
        // 在 ARETURN 前插入
        instructions.insertBefore(insn, replaceLogic)
    }
}
```

### 4.3 注入模式

#### 模式 A: 替换方法返回值

```java
// 原始
public BigInteger modPow(BigInteger e, BigInteger m) {
    return compute(...);
}

// 注入后
public BigInteger modPow(BigInteger e, BigInteger m) {
    BigInteger originalResult = compute(...);
    // 注入：
    BigInteger filtered = ResultFilter.testFilter(originalResult, e, m);
    return filtered != null ? filtered : originalResult;
}
```

#### 模式 B: 修改方法参数

```java
// 原始
public Object target(String arg) {
    return process(arg);
}

// 注入后
public Object target(String arg) {
    // 注入：
    Object intercepted = Filter.testQuery(arg);
    if (intercepted != null) arg = (String) intercepted;
    return process(arg);
}
```

## 五、线程模型

### 5.1 单线程模型

整个框架运行在 JVM 主线程中：
- `premain()` 在主线程中调用
- `Dispatcher.transform()` 在类加载线程中调用
- `transform()` 方法应该是线程安全的

### 5.2 异步日志

`DebugInfo` 使用单独的线程池异步写文件：
- 控制台输出：单线程执行器
- 文件输出：单线程执行器
- 避免阻塞主线程

### 5.3 线程安全考虑

- `transformerMap`: 使用 `ConcurrentHashMap`
- `globalTransformers`: 使用 `CopyOnWriteArrayList`
- `ruleList` (各 Filter): 单线程初始化，多线程读访问

## 六、配置系统

### 6.1 文件组织

```
config-jetbrains/
├── dns.conf      # DNS 过滤
├── env.conf      # 环境变量过滤
├── native.conf   # Native 包装
├── power.conf    # 大数运算拦截
└── url.conf      # URL 过滤
```

### 6.2 解析流程

```kotlin
fun ConfigParser.parse(file: File): Map<String, List<FilterRule>> {
    val result = mutableMapOf<String, MutableList<FilterRule>>()
    var currentSection: String? = null

    file.useLines { lines ->
        for (line in lines) {
            val trimmed = line.trim()
            // 跳过空行和注释
            if (trimmed.isEmpty() || trimmed.startsWith(";")) continue

            // [Section] 标记
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                currentSection = trimmed.substring(1, length-1)
                result.getOrPut(currentSection) { mutableListOf() }
                continue
            }

            // TYPE,rule 行
            val parts = trimmed.split(",", limit = 2)
            if (parts.size == 2) {
                val rule = FilterRule.of(parts[0], parts[1])
                result.getOrPut(currentSection!!) { mutableListOf() }.add(rule)
            }
        }
    }

    return result
}
```

### 6.3 规则加载

```kotlin
class PluginConfig(file: File, data: Map<String, List<FilterRule>>) {
    fun getBySection(section: String): List<FilterRule> {
        return data[section] ?: emptyList()
    }
}
```

## 七、错误处理

### 7.1 转换失败

如果 transformer 在转换过程中抛出异常：
- 记录到 DebugInfo
- 跳过该 transformer，继续下一个
- 最终返回原始字节码（不修改）

### 7.2 插件加载失败

如果插件 jar 加载失败：
- 记录到 DebugInfo
- 跳过该插件，继续加载其他插件
- 不影响框架整体运行

### 7.3 配置文件解析失败

如果 .conf 文件解析失败：
- 记录到 DebugInfo
- 该插件将没有配置，但仍然加载
- 插件 transformer 接收空规则列表

## 八、性能考虑

### 8.1 类转换开销

- 字节码转换发生在类加载时（一次性）
- 转换完成后，新字节码被缓存
- 后续使用不影响性能

### 8.2 运行时代价

- DNS/URL 过滤：每次域名解析/URL 创建时执行规则检查
- power 过滤：每次 modPow 调用时执行
- 使用 L1/L2 缓存减少字符串操作

### 8.3 内存占用

- 一个 PluginClassLoader 每个插件约 100KB
- FilterRule 列表约 100 bytes/rule
- 全局 transformer 列表约 1KB
- 总内存占用小于 1MB

## 九、扩展性

### 9.1 添加新插件

1. 创建插件子模块
2. 实现 `PluginEntry` 接口
3. 实现 transformer
4. 创建 Manifest 的 `Plugin-Class`
5. 添加到 plugins 目录

### 9.2 添加新规则类型

```kotlin
enum class RuleType(private val ruler: Ruler) {
    // 添加新规则
    MY_NEW_RULE(MyNewRuler()),
    // ...
}

object MyNewRuler : Ruler {
    override fun test(rule: String, input: String): Boolean {
        // 自定义匹配逻辑
    }
}
```

### 9.3 添加新 hook 目标

```kotlin
class MyNewTransformer : MyTransformer {
    override fun hookClassName(): String = "com.example.Target"

    override fun transform(...): ByteArray? {
        // 使用 ASM 修改字节码
    }
}
```

## 十、测试策略

### 10.1 单元测试

- 规则匹配器测试
- 配置文件解析测试
- 激活码生成/解析测试

### 10.2 集成测试

- 框架初始化测试
- 插件加载测试
- 字节码转换正确性测试

### 10.3 运行时测试

- 在沙箱环境中启动 IDE
- 验证激活码被接受
- 检查日志输出