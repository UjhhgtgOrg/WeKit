package moe.ouom.wekit.util.io

import android.content.Context
import android.os.Environment
import moe.ouom.wekit.host.impl.hostInfo
import java.io.File

object PathTool {
    fun getDataSavePath(context: Context, dirName: String?): String {
        // getExternalFilesDir()：SDCard/Android/data/你的应用的包名/files/dirName
        return context.getExternalFilesDir(dirName)!!.absolutePath
    }

    val storageDirectory: String
        get() = Environment.getExternalStorageDirectory().absolutePath

    val moduleDataPath: String
        get() {
            val directory: String =
                storageDirectory + "/Android/data/" + hostInfo.packageName + "/WeKit"
            val file = File(directory)
            if (!file.exists()) {
                file.mkdirs()
            }
            return directory
        }

    fun getModuleCachePath(dirName: String?): String {
        val cache = File("$moduleDataPath/cache/$dirName")
        if (!cache.exists()) {
            cache.mkdirs()
        }
        return cache.absolutePath
    }
}
