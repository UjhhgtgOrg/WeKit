package moe.ouom.wekit.hooks.items.scripting_kts

import moe.ouom.wekit.constants.PackageConstants
import moe.ouom.wekit.host.HostInfo
import moe.ouom.wekit.utils.log.WeLogger
import java.io.File
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.outputStream
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath

@KotlinScript(
    fileExtension = "wekit.kts",
    compilationConfiguration = WeKitScriptConfig::class
)
abstract class WeKitScript : WeKitPlugin()

object WeKitScriptConfig : ScriptCompilationConfiguration({
    baseClass(WeKitPlugin::class)
    jvm {
        updateClasspath(getAndroidClasspath())
    }
})

fun getAndroidClasspath(): List<File> {
    val paths = mutableListOf<File>()

    // The module's own APK (contains stdlib + your classes)
    val moduleApkPath = HostInfo.getApplication().packageManager.getPackageInfo(PackageConstants.PACKAGE_NAME_SELF, 0).applicationInfo!!.sourceDir
    val wechatApkPath = HostInfo.getApplication().applicationInfo.sourceDir
//    paths.addAll(extractDexFiles(apkPath, (PathUtils.moduleDataPath!!/"module_dex").apply { createDirectories() }))
    paths.add(File(moduleApkPath))
    paths.add(File(wechatApkPath))

    // ART-specific: add the odex/oat paths if present
    val bootClassPath = System.getProperty("java.boot.class.path") ?: ""
    bootClassPath.split(File.pathSeparator)
        .map { File(it) }
        .filter { it.exists() }
        .forEach { paths.add(it) }

    WeLogger.d("PluginLoader", "classPaths: ${paths.joinToString { file -> file.path }}")
    return paths
}

@OptIn(ExperimentalPathApi::class)
fun extractDexFiles(apkPath: String, outputDir: Path): List<File> {
    val currentMd5 = File(apkPath).inputStream().use {
        MessageDigest.getInstance("MD5").digest(it.readBytes())
            .joinToString("") { b -> "%02x".format(b) }
    }

    val md5File = outputDir / "apk_md5.txt"
    val cachedMd5 = if (md5File.exists()) md5File.readText().trim() else null

    if (cachedMd5 != currentMd5) {
        if (outputDir.exists()) outputDir.deleteRecursively()
        outputDir.createDirectories()

        ZipFile(apkPath).use { zip ->
            zip.entries().asSequence()
                .filter { it.name.matches(Regex("classes\\d*\\.dex")) }
                .forEach { entry ->
                    val outFile = outputDir / entry.name
                    zip.getInputStream(entry).use { input ->
                        outFile.outputStream().use { input.copyTo(it) }
                    }
                }
        }

        md5File.writeText(currentMd5)
    }

    return outputDir.listDirectoryEntries("classes*.dex").map { it.toFile() }
}