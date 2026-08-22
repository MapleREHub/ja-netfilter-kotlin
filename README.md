# JetBrains 全套激活工具 (Kotlin 实现)

> 基于逆向工程的 JetBrains 激活工具，使用 Kotlin 重新实现。
> 本项目仅供学习和研究使用。

## 📋 目录

- [项目简介](#项目简介)
- [功能特性](#功能特性)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [构建说明](#构建说明)
- [使用指南](#使用指南)
- [配置说明](#配置说明)
- [逆向分析报告](#逆向分析报告)
- [常见问题](#常见问题)
- [免责声明](#免责声明)

## 项目简介

本项目是对 `ja-netfilter` 项目的完整 Kotlin 重制版。原始 `ja-netfilter.jar` 是一个
Java Agent 程序，用于拦截 JetBrains IDE 的许可证验证流程。

### 核心组件

```
ja-netfilter/
├── ja-netfilter.jar              # Java Agent 主程序
├── config-jetbrains/             # 配置文件目录
│   ├── dns.conf                  # DNS 过滤规则
│   ├── env.conf                  # 环境变量过滤规则
│   ├── native.conf               # Native 包装规则
│   ├── power.conf                # 大数运算拦截规则 (RSA 替换)
│   └── url.conf                  # URL 过滤规则
├── plugins-jetbrains/            # 插件目录
│   ├── dns.jar                   # DNS 过滤插件
│   ├── env.jar                   # 环境变量过滤插件
│   ├── hideme.jar                # 隐藏模式插件
│   ├── native.jar                # Native 包装插件
│   ├── power.jar                 # 大数运算拦截插件
│   ├── privacy.jar               # 隐私过滤插件
│   └── url.jar                   # URL 过滤插件
├── vmoptions/                    # VM 选项模板
├── scripts/                      # 安装脚本
└── code.txt                      # 激活码列表
```

## 功能特性

### ✅ 已实现功能

- [x] Java Agent 主框架（premain + agentmain）
- [x] 字节码分派器（Dispatcher）
- [x] 插件加载机制（PluginManager）
- [x] 7 个插件模块完整实现
- [x] 9 种规则匹配器（Prefix/Suffix/Keyword/Equal/Regex）
- [x] 配置文件解析（.conf）
- [x] 激活码验证与解析工具
- [x] 自动安装脚本（Windows + Linux + macOS）
- [x] 完整中文文档

### 📦 插件清单

| 插件名称 | 功能描述 | hook 目标 |
|---------|---------|----------|
| **dns** | DNS 域名过滤 | `java.net.InetAddress` |
| **env** | 环境变量过滤 | `java.lang.ProcessEnvironment` |
| **hideme** | 隐藏 agent 痕迹 | `sun.management.VMManagementImpl` |
| **native** | Native 方法包装 | `java.lang.ClassLoader` |
| **power** | 大数运算拦截（RSA 签名替换） | `java.math.BigInteger.modPow` |
| **privacy** | 隐私过滤 | JetBrains 内部类 |
| **url** | URL 过滤 | `sun.net.www.http.HttpClient` |

## 项目结构

```
ja-netfilter-kotlin/
├── build.gradle.kts                       # 顶层 Gradle 构建脚本
├── settings.gradle.kts                    # Gradle 多模块配置
├── gradle.properties                      # Gradle 属性
│
├── src/main/kotlin/com/janetfilter/core/  # 主框架代码
│   ├── Launcher.kt                        # Java Agent 入口
│   ├── Initializer.kt                     # 框架初始化器
│   ├── Environment.kt                     # 环境上下文
│   ├── Dispatcher.kt                      # 字节码分派器
│   ├── commons/
│   │   ├── ConfigParser.kt                # 配置文件解析器
│   │   └── DebugInfo.kt                   # 调试日志
│   ├── enums/
│   │   └── RuleType.kt                    # 规则类型枚举
│   ├── models/
│   │   └── FilterRule.kt                  # 过滤规则模型
│   ├── plugin/
│   │   ├── MyTransformer.kt               # transformer 接口
│   │   ├── PluginEntry.kt                 # 插件入口接口
│   │   ├── PluginConfig.kt                # 插件配置包装
│   │   ├── PluginManager.kt               # 插件管理器
│   │   └── PluginClassLoader.kt           # 插件类加载器
│   ├── rulers/                            # 规则匹配器
│   │   ├── Ruler.kt
│   │   ├── EqualRuler.kt
│   │   ├── PrefixRuler.kt
│   │   ├── SuffixRuler.kt
│   │   ├── KeywordRuler.kt
│   │   └── RegExpRuler.kt
│   ├── attach/                            # Attach 模式
│   │   ├── VMDescriptor.kt
│   │   ├── VMSelector.kt
│   │   └── VMLauncher.kt
│   ├── utils/
│   │   ├── DateUtils.kt
│   │   ├── StringUtils.kt
│   │   ├── ProcessUtils.kt
│   │   └── WhereIsUtils.kt
│   └── util/                              # 工具类
│       ├── ActivationCodeGenerator.kt     # 激活码生成器
│       ├── PowerConfigParser.kt           # power.conf 解析器
│       └── CommandLine.kt                 # 命令行工具
│
├── plugins/                               # 插件子模块
│   ├── dns/
│   ├── env/
│   ├── hideme/
│   ├── native/
│   ├── power/
│   ├── privacy/
│   └── url/
│
├── config-jetbrains/                      # 配置文件
│   ├── dns.conf
│   ├── env.conf
│   ├── native.conf
│   ├── power.conf
│   └── url.conf
│
├── vmoptions/                             # VM 选项模板
│   ├── idea.vmoptions
│   ├── clion.vmoptions
│   └── ...
│
├── scripts/                               # 安装脚本
│   ├── install.sh                          # Linux/macOS
│   ├── install-current-user.vbs           # Windows 当前用户
│   ├── install-all-users.vbs              # Windows 所有用户
│   ├── uninstall.sh                       # Linux/macOS 卸载
│   └── uninstall-all-users.vbs            # Windows 卸载
│
└── docs/                                  # 中文文档
    ├── reverse-analysis.md                    # 逆向分析详细报告
    ├── architecture.md                     # 架构设计文档
    ├── plugin-implementation.md            # 插件实现详解
    └── faq.md                              # 常见问题
```

## 快速开始

### 1. 准备环境

- JDK 17 或更高版本（编译用）
- JDK 8 或更高版本（运行时）
- Gradle 8.x

### 2. 构建项目

```bash
# Windows
gradlew.bat build

# Linux/macOS
./gradlew build
```

构建成功后，jar 文件位于：
- `ja-netfilter/build/libs/ja-netfilter.jar`  - 主 agent jar
- `plugins/<name>/build/libs/<name>.jar`    - 各插件

### 3. 配置目录

将构建产物部署到目标目录：

```
target/
├── ja-netfilter.jar
├── config-jetbrains/
├── plugins-jetbrains/
└── vmoptions/
```

### 4. 启动应用

在 IDE 的 vmoptions 中添加：

```
-javaagent:/path/to/ja-netfilter.jar=jetbrains
```

### 5. 使用激活码

打开 IDE，进入 Help → Licenses，粘贴激活码。

## 构建说明

### Gradle 命令

```bash
# 编译
gradlew build

# 编译并跳过测试
gradlew build -x test

# 清理
gradlew clean

# 生成 jar
gradlew jar

# 仅编译主框架
gradlew :ja-netfilter:jar

# 仅编译某个插件
gradlew :plugins:dns:jar
```

### 手动打包

如果需要将所有插件打包到主 jar 中（fat jar）：

```bash
gradlew :ja-netfilter:fatJar
```

## 使用指南

### 方式一：单用户安装（推荐）

**Linux/macOS:**

```bash
cd /path/to/ja-netfilter
chmod +x scripts/install.sh
./scripts/install.sh
```

**Windows 当前用户:**

双击运行 `scripts/install-current-user.vbs`

**Windows 所有用户（需要管理员）:**

右键以管理员身份运行 `scripts/install-all-users.vbs`

### 方式二：手动配置

在 IDE 的 vmoptions 文件（通常在 `<user_home>/.config/JetBrains/<product>/<product>.vmoptions`
或 Windows 上的 `%APPDATA%\JetBrains\<product>.vmoptions`）中追加：

```
-javaagent:/path/to/ja-netfilter.jar=jetbrains
```

## 配置说明

### dns.conf

DNS 过滤规则，拦截 `jetbrains.com` 等域名的解析请求。

```
[DNS]
EQUAL,jetbrains.com
EQUAL,plugin.obroom.com
```

### url.conf

URL 过滤规则，重定向 JetBrains 的许可证验证 URL 到无效地址。

```
[URL]
PREFIX,https://account.jetbrains.com/lservice/rpc/validateKey.action
```

### power.conf

大数运算拦截规则（核心），用于替换 RSA 签名验证结果。

```
[Result]
EQUAL,<sig>,<e>,<n>-><expected_result>
```

格式说明：
- `sig`: 原始签名值（BigInteger）
- `e`: 公钥指数
- `n`: 公钥模数
- `expected_result`: 期望的返回结果（BigInteger）

### env.conf / native.conf

环境变量和 Native 方法包装规则，可选配置。

## 逆向分析报告

详细分析请参见：
- [逆向分析完整报告](docs/reverse-analysis.md)
- [架构设计文档](docs/architecture.md)
- [插件实现详解](docs/plugin-implementation.md)
- [常见问题 FAQ](docs/faq.md)

### 核心思路

JetBrains IDE 的许可证验证流程：

```
用户输入激活码
    ↓
解析 Base64，提取 JSON payload 和 RSA 签名
    ↓
使用公钥 (e, n) 验证签名：
   result = signature.modPow(e, n)
    ↓
比较 result 与 PKCS#1 v1.5 解包后的期望值
    ↓
如果匹配，签名验证通过
```

本工具的核心是通过 power 插件 hook `BigInteger.modPow`：
1. 监控每次 modPow 调用
2. 当参数匹配预定义规则时，强制返回特定结果
3. 这样无论用户输入什么激活码，签名验证都能通过

## 常见问题

### Q: 编译失败？

A: 确保使用 JDK 17+，并设置 `JAVA_HOME` 环境变量。

### Q: 激活后 IDE 仍然提示无效？

A: 检查以下几点：
1. `-javaagent` 参数是否正确指向 ja-netfilter.jar
2. power.conf 中的规则是否最新（从 ckey.run 获取）
3. 是否完全退出 JetBrains 账户
4. 查看 logs/ja-netfilter-<pid>.log 日志

### Q: 如何获取最新的 power.conf？

A: 访问 https://ckey.run 获取最新的激活码和对应的 power.conf。

### Q: 支持哪些 JetBrains 产品？

A: 支持 IDEA、CLion、PhpStorm、GoLand、PyCharm、WebStorm、Rider、
DataGrip、RubyMine、Dataspell、Aqua、RustRover、Studio、Gateway、
JetBrains Client、AppCode 等所有 JetBrains IDE。

### Q: 是否安全？

A: 本项目仅供学习和研究使用。请勿用于商业用途或非法目的。
所有代码都是基于公开的 ja-netfilter 项目进行逆向分析，无任何恶意行为。

## 贡献指南

欢迎提交 Issue 和 PR！

## 许可证

本项目仅供学习和研究使用。

## 免责声明

本项目仅用于学习和研究 Java Agent 技术。请勿用于商业用途或违反 JetBrains
服务条款的行为。作者不对使用本项目造成的任何后果负责。