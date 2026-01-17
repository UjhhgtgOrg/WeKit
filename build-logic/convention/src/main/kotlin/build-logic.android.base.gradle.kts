@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.BaseExtension

plugins {
    id("com.android.base")
    kotlin("android")
}

extensions.findByType(BaseExtension::class)?.run {
    compileSdkVersion(Version.compileSdkVersion)
    buildToolsVersion = Version.buildToolsVersion
    ndkVersion = Version.getNdkVersion()

    defaultConfig {
        minSdk = Version.minSdk
        targetSdk = Version.targetSdk
        versionCode = Common.getBuildVersionCode()
        versionName = Common.getBuildVersionName()
        resourceConfigurations += listOf("zh", "en")
    }

    compileOptions {
        sourceCompatibility = Version.java
        targetCompatibility = Version.java
    }

    packagingOptions.jniLibs.useLegacyPackaging = false
}

kotlin {
    jvmToolchain(Version.java.toString().toInt())
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
        }
    }
}
