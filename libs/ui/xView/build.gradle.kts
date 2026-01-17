@file:Suppress("UnstableApiUsage")

plugins {
    id("build-logic.android.library")
}

android {
    namespace = "com.lxj.xpopup"
    sourceSets {
        val main by getting
        main.apply {
            manifest.srcFile("AndroidManifest.xml")
            java.setSrcDirs(listOf("XPopup/library/src/main/java", "EasyAdapter/easy-adapter/src/main/java"))
            res.setSrcDirs(listOf("EasyAdapter/easy-adapter/src/main/res", "XPopup/library/src/main/res"))
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.glide)
    implementation(libs.davemorrissey.subsampling.scale.image.view)
}
