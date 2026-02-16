import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.UUID
import java.security.MessageDigest

plugins {
    id("build-logic.android.application")
    alias(libs.plugins.protobuf)
    alias(libs.plugins.serialization)
    alias(libs.plugins.android.application)
    id("com.google.devtools.ksp") version "2.3.4"
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// 生成方法 hash 的任务
val generateMethodHashes = tasks.register("generateMethodHashes") {
    group = "wekit"
    val sourceDir = file("src/main/java")
    val outputDir = layout.buildDirectory.dir("generated/source/methodhashes")
    val outputFile = outputDir.get().file("moe/ouom/wekit/dexkit/cache/GeneratedMethodHashes.kt").asFile

    inputs.dir(sourceDir)
    outputs.dir(outputDir)

    doLast {
        val hashMap = mutableMapOf<String, String>()
        sourceDir.walk().filter { it.isFile && it.extension == "kt" && it.readText().contains("IDexFind") }.forEach { file ->
            val content = file.readText()
            val packageName = Regex("""package\s+([\w.]+)""").find(content)?.groupValues?.get(1)
            val className = Regex("""(?:class|object)\s+(\w+)""").find(content)?.groupValues?.get(1) ?: return@forEach
            val fullClassName = if (packageName != null) "$packageName.$className" else className
            val dexFindMatch = Regex("""override\s+fun\s+dexFind\s*\(""").find(content)
            if (dexFindMatch != null) {
                val start = content.indexOf('{', dexFindMatch.range.last)
                if (start != -1) {
                    var count = 0
                    for (i in start until content.length) {
                        if (content[i] == '{') count++ else if (content[i] == '}') count--
                        if (count == 0) {
                            val body = content.substring(start, i + 1)
                            val hash = MessageDigest.getInstance("MD5").digest(body.toByteArray()).joinToString("") { "%02x".format(it) }
                            hashMap[fullClassName] = hash
                            break
                        }
                    }
                }
            }
        }
        outputFile.parentFile.mkdirs()
        outputFile.writeText("""
            package moe.ouom.wekit.dexkit.cache
            object GeneratedMethodHashes {
                private val hashes = mapOf(${hashMap.entries.sortedBy { it.key }.joinToString(",") { "\"${it.key}\" to \"${it.value}\"" }})
                fun getHash(className: String) = hashes[className] ?: ""
            }
        """.trimIndent())
    }
}


android {
    namespace = "moe.ouom.wekit"
    compileSdk = Version.compileSdkVersion

    val buildUUID = UUID.randomUUID()
    println(
        """
        __        __  _____   _  __  ___   _____ 
         \ \      / / | ____| | |/ / |_ _| |_   _|
          \ \ /\ / /  |  _|   | ' /   | |    | |  
           \ V  V /   | |___  | . \   | |    | |  
            \_/\_/    |_____| |_|\_\ |___|   |_|  
                                              
                        [WECHAT KIT] WeChat, Now with Superpowers
        """
    )

    println("buildUUID: $buildUUID")

    signingConfigs {
        create("release") {
            val storePath = project.findProperty("KEYSTORE_FILE") as String? ?: "wekit.jks"
            val resolved = file(storePath)
            if (resolved.exists()) {
                storeFile = resolved
                storePassword = project.findProperty("KEYSTORE_PASSWORD") as String? ?: ""
                keyAlias = project.findProperty("KEY_ALIAS") as String? ?: "key0"
                keyPassword = project.findProperty("KEY_PASSWORD") as String? ?: ""
            } else {
                println("🔐 Release keystore not found at '${resolved.path}'. Will fallback for PR/builds without secrets.")
            }
        }
    }

    defaultConfig {
        applicationId = "moe.ouom.wekit"
        buildConfigField("String", "BUILD_UUID", "\"${buildUUID}\"")
        buildConfigField("String", "TAG", "\"[WeKit-TAG]\"")
        buildConfigField("long", "BUILD_TIMESTAMP", "${System.currentTimeMillis()}L")
        // noinspection ChromeOsAbiSupport
        ndk { abiFilters += "arm64-v8a" }

        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                cppFlags("-I${project.file("src/main/cpp/include")}")

                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384"
                )
            }
        }
    }

    sourceSets {
        getByName("main") {
            kotlin.srcDir(generateMethodHashes)
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        val debugSigning = signingConfigs.getByName("debug")

        release {
            isMinifyEnabled = true
            isShrinkResources = true

            signingConfig = debugSigning

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget("17"))
        }
    }

    packaging {
        resources.excludes += listOf(
            "kotlin/**",
            "**.bin",
            "kotlin-tooling-metadata.json"
        )
        resources {
            merges += "META-INF/xposed/*"
            merges += "org/mozilla/javascript/**"  // 合并 Mozilla Rhino 所有资源
            excludes += "**"
        }
    }

    androidResources {
        additionalParameters += listOf(
            "--allow-reserved-package-id",
            "--package-id", "0x69"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }
}


fun isHooksDirPresent(task: Task): Boolean {
    return task.outputs.files.any { outputDir ->
        File(outputDir, "moe/ouom/wekit/hooks").exists()
    }
}

tasks.withType<KotlinCompile>().configureEach {
    if (name.contains("Release")) {
        outputs.upToDateWhen { task ->
            isHooksDirPresent(task)
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    if (name.contains("Release")) {
        outputs.upToDateWhen { task ->
            isHooksDirPresent(task)
        }
    }
}

fun String.capitalizeUS() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }

val adbProvider = androidComponents.sdkComponents.adb
fun hasConnectedDevice(): Boolean {
    val adbPath = adbProvider.orNull?.asFile?.absolutePath ?: return false
    return runCatching {
        val proc = ProcessBuilder(adbPath, "devices").redirectErrorStream(true).start()
        proc.waitFor(5, TimeUnit.SECONDS)
        proc.inputStream.bufferedReader().readLines().any { it.trim().endsWith("\tdevice") }
    }.getOrElse { false }
}

val packageName = "com.tencent.mm"
val killWeChat = tasks.register("kill-wechat") {
    group = "wekit"
    description = "Force-stop WeChat on a connected device; skips gracefully if none."
    onlyIf { hasConnectedDevice() }
    doLast {
        val adbFile = adbProvider.orNull?.asFile ?: return@doLast
        for (i in 1..10) {  // 貌似国内定制系统中的的微信一次杀不死？
            project.exec {
                commandLine(adbFile, "shell", "am", "force-stop", packageName)
                isIgnoreExitValue = true
                standardOutput = ByteArrayOutputStream(); errorOutput = ByteArrayOutputStream()
            }
        }

        logger.lifecycle("✅  kill-wechat executed.")
    }
}

androidComponents.onVariants { variant ->
    if (!variant.debuggable) return@onVariants

    val vCap = variant.name.capitalizeUS()
    val installTaskName = "install${vCap}"

    val installAndRestart = tasks.register("install${vCap}AndRestartWeChat") {
        group = "wekit"
        dependsOn(installTaskName)
        finalizedBy(killWeChat)
        onlyIf { hasConnectedDevice() }
    }

    afterEvaluate { tasks.findByName("assemble${vCap}")?.finalizedBy(installAndRestart) }
}

afterEvaluate {
    tasks.matching { it.name.startsWith("install") }.configureEach { onlyIf { hasConnectedDevice() } }
    if (!hasConnectedDevice()) logger.lifecycle("⚠️  No device detected — all install tasks skipped")
}

android.applicationVariants.all {
    val variant = this
    val buildTypeName = variant.buildType.name.uppercase()

    outputs.all {
        if (this is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
            val config = project.android.defaultConfig
            val versionName = config.versionName
            this.outputFileName = "WeKit-${buildTypeName}-${versionName}.apk"
        }
    }
}

// =========================================================================

// 配置 generateMethodHashes 任务在 Kotlin 编译之前执行
tasks.withType<KotlinCompile>().configureEach {
    dependsOn("generateMethodHashes")
}

// 配置 KSP 任务也依赖于 generateMethodHashes
tasks.matching { it.name.startsWith("ksp") && it.name.contains("Kotlin") }.configureEach {
    dependsOn("generateMethodHashes")
}

// =========================================================================

kotlin {
    sourceSets.configureEach {
        kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/$name/kotlin/"))
        // 添加生成的方法hash文件目录
        kotlin.srcDir(layout.buildDirectory.dir("generated/source/methodhashes"))
    }
}

protobuf {
    protoc { artifact = libs.google.protobuf.protoc.get().toString() }
    generateProtoTasks { all().forEach { it.builtins { create("java") { option("lite") } } } }
}

configurations.configureEach { exclude(group = "androidx.appcompat", module = "appcompat") }

dependencies {
    implementation(libs.core.ktx)

    implementation(libs.appcompat)

    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout) { exclude("androidx.appcompat", "appcompat") }

    implementation(platform(libs.compose.bom))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.kotlinx.io.jvm)
    implementation(libs.dexkit)
    implementation(libs.hiddenApiBypass)
    implementation(libs.gson)

    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.grpc.protobuf)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.mmkv)
    implementation(projects.libs.common.libxposed.service)

    compileOnly(libs.xposed)
    compileOnly(projects.libs.common.libxposed.api)

    implementation(libs.dexlib2)
    implementation(libs.google.guava)
    implementation(libs.google.protobuf.java)
    implementation(libs.kotlinx.serialization.protobuf)

    implementation(libs.sealedEnum.runtime)
    ksp(libs.sealedEnum.ksp)
    implementation(projects.libs.common.annotationScanner)
    ksp(projects.libs.common.annotationScanner)

    implementation(libs.material.preference)
    implementation(libs.dev.appcompat)

    implementation(libs.recyclerview)

    implementation(libs.material.dialogs.core)
    implementation(libs.material.dialogs.input)
    implementation(libs.preference)
    implementation(libs.fastjson2)

    implementation(libs.glide)
    implementation(libs.byte.buddy)
    implementation(libs.byte.buddy.android)
    implementation(libs.dalvik.dx)
    implementation(libs.okhttp3.okhttp)
    implementation(libs.markdown.core)
    implementation(libs.blurView)
    implementation(libs.hutool.core)
    implementation(libs.nanohttpd)

    implementation(libs.rhino.android)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
