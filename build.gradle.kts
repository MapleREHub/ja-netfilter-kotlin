// ============================================================================
// JetBrains 激活工具 - Gradle 多模块构建脚本 (Kotlin DSL)
// ----------------------------------------------------------------------------
// 本项目使用 Kotlin 实现 ja-netfilter Java Agent 主框架，以及 7 个激活插件：
//   - dns      : DNS 过滤插件（拦截 InetAddress 域名解析）
//   - env      : 环境变量过滤插件（拦截 ProcessEnvironment）
//   - hideme   : 隐藏模式插件（从 ClassLoader/VM 中隐藏痕迹）
//   - native   : Native 方法包装插件（包装 JNI 调用）
//   - power    : 大数运算拦截插件（Hook BigInteger.modPow 用于 RSA 签名替换）
//   - privacy  : 隐私过滤插件（拦截 JetBrains 内部类）
//   - url      : URL 过滤插件（拦截 HttpURLConnection）
// ============================================================================

group = "com.janetfilter"
version = "2.2.0"

plugins {
    kotlin("jvm") version "1.9.25" apply false
    `java-library`
    application
}

// 顶层仓库配置
allprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://maven.aliyun.com/repository/public")
    }
}

// ============================================================================
// 主项目：ja-netfilter（Java Agent 框架）
// ============================================================================
project(":ja-netfilter") {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "application")
    apply(plugin = "java")

    // 源码位于根项目的 src/main/kotlin 目录，配置 sourceSets 指向正确位置
    sourceSets {
        main {
            java.srcDirs("$rootDir/src/main/java", "src/main/java")
            resources.srcDirs("$rootDir/src/main/resources", "src/main/resources")
        }
    }

    // 通过 Kotlin 扩展配置 Kotlin 源码目录
    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        sourceSets.getByName("main").kotlin {
            srcDirs("$rootDir/src/main/kotlin", "src/main/kotlin")
        }

        jvmToolchain(17)
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }

    extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    dependencies {
        // ASM 字节码操作库（用于 ClassFileTransformer）
        implementation("org.ow2.asm:asm:9.9")
        implementation("org.ow2.asm:asm-tree:9.9")
        implementation("org.ow2.asm:asm-commons:9.9")

        // 子模块：各插件作为运行时依赖
        runtimeOnly(project(":plugins:dns"))
        runtimeOnly(project(":plugins:env"))
        runtimeOnly(project(":plugins:hideme"))
        runtimeOnly(project(":plugins:native"))
        runtimeOnly(project(":plugins:power"))
        runtimeOnly(project(":plugins:privacy"))
        runtimeOnly(project(":plugins:url"))
    }

    application {
        mainClass.set("com.janetfilter.core.Launcher")
    }

    tasks.jar {
        archiveBaseName.set("ja-netfilter")
        manifest {
            attributes["Premain-Class"] = "com.janetfilter.core.Launcher"
            attributes["Agent-Class"] = "com.janetfilter.core.Launcher"
            attributes["Main-Class"] = "com.janetfilter.core.Launcher"
            attributes["Can-Redefine-Classes"] = "true"
            attributes["Can-Retransform-Classes"] = "true"
            attributes["Can-Set-Native-Method-Prefix"] = "true"
        }
    }

    // 配置 fat jar 任务：将 Kotlin 运行时打包进 ja-netfilter.jar
    tasks.register<Jar>("fatJar") {
        archiveBaseName.set("ja-netfilter")
        archiveClassifier.set("all")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes["Premain-Class"] = "com.janetfilter.core.Launcher"
            attributes["Agent-Class"] = "com.janetfilter.core.Launcher"
            attributes["Main-Class"] = "com.janetfilter.core.Launcher"
            attributes["Can-Redefine-Classes"] = "true"
            attributes["Can-Retransform-Classes"] = "true"
            attributes["Can-Set-Native-Method-Prefix"] = "true"
        }
        from(sourceSets.main.get().output)
        dependsOn(configurations.runtimeClasspath)
        from({
            configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
        })
    }
}

// ============================================================================
// 插件子模块配置
// ============================================================================
val configurePlugin: org.gradle.api.Project.() -> Unit = {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(17)
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }

    extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    dependencies {
        implementation(project(":ja-netfilter"))
        implementation("org.ow2.asm:asm:9.9")
        implementation("org.ow2.asm:asm-tree:9.9")
        implementation("org.ow2.asm:asm-commons:9.9")
    }
}

project(":plugins:dns") { configurePlugin() }
project(":plugins:env") { configurePlugin() }
project(":plugins:hideme") { configurePlugin() }
project(":plugins:native") { configurePlugin() }
project(":plugins:power") { configurePlugin() }
project(":plugins:privacy") { configurePlugin() }
project(":plugins:url") { configurePlugin() }