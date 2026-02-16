import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Common {
    fun getBuildVersionCode(): Int {
        val appVerCode: Int by lazy {
            val versionCode = SimpleDateFormat("yyMMddHH", Locale.ENGLISH).format(Date())
            versionCode.toInt()
        }
        return appVerCode
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("MMddHHmm", Locale.getDefault())
        return sdf.format(Date())
    }
    
    private fun getShortGitRevision(): String {
        val command = "git rev-parse --short HEAD"
        val processBuilder = ProcessBuilder(*command.split(" ").toTypedArray())
        val process = processBuilder.start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()

        return if (exitCode == 0) {
            output.trim()
        } else {
            "no_commit"
        }
    }

    fun getBuildVersionName(): String {
        return "${getShortGitRevision()}.${getCurrentDate()}"
    }
}

