import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.UUID

plugins {
    id("build-logic.android.application")
    alias(libs.plugins.protobuf)
    alias(libs.plugins.serialization)
    alias(libs.plugins.android.application)
    id("com.google.devtools.ksp") version "2.3.4"
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}


android {
    namespace = "moe.ouom.wekit"
    compileSdk = 36

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
        ndk { abiFilters += "arm64-v8a" }
    }

    buildTypes {
        val releaseSigning = signingConfigs.getByName("release")
        val debugSigning = signingConfigs.getByName("debug")

        release {
            isMinifyEnabled = true
            isShrinkResources = true

            signingConfig = if (releaseSigning.storeFile?.exists() == true) {
                releaseSigning
            } else {
                println("✅ No release keystore detected; using DEBUG signing for release variant (PR-friendly).")
                debugSigning
            }

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
        project.exec {
            commandLine(adbFile, "shell", "am", "force-stop", packageName)
            isIgnoreExitValue = true
            standardOutput = ByteArrayOutputStream(); errorOutput = ByteArrayOutputStream()
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
    outputs.all {
        if (this is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
            val config = project.android.defaultConfig
            val versionName = config.versionName
            this.outputFileName = "WeKit-RELEASE-${versionName}.apk"
        }
    }
}

kotlin {
    sourceSets.configureEach { kotlin.srcDir("$buildDir/generated/ksp/$name/kotlin/") }
    sourceSets.main { kotlin.srcDir(File(rootDir, "libs/util/ezxhelper/src/main/java")) }
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

    val composeBom = platform("androidx.compose:compose-bom:2026.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)

    implementation(libs.kotlinx.io.jvm)
    implementation(libs.dexkit)
    implementation(libs.hiddenapibypass)
    implementation(libs.gson)

    implementation(ktor("serialization", "kotlinx-json"))
    implementation(grpc("protobuf", "1.62.2"))

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

    implementation(libs.material.preference)
    implementation(libs.dev.appcompat)

    implementation(libs.recyclerview)

    implementation(libs.material.dialogs.core)
    implementation(libs.material.dialogs.input)
    implementation(libs.preference)
    implementation(libs.fastjson2)
    implementation(projects.libs.ui.xView)

    implementation(libs.glide)
    implementation(libs.byte.buddy)
    implementation(libs.dalvik.dx)
    implementation(libs.okhttp3.okhttp)
    implementation(libs.markdown.core)
    implementation(libs.blurview)

    implementation(libs.hutool.core)
    implementation(libs.nanohttpd)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
