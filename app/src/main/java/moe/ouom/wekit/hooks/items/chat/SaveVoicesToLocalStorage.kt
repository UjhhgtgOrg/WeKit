package moe.ouom.wekit.hooks.items.chat

import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.toClass
import moe.ouom.wekit.core.dsl.dexClass
import moe.ouom.wekit.core.dsl.dexMethod
import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem
import moe.ouom.wekit.dexkit.intf.IDexFind
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.hooks.sdk.base.WeServiceApi
import moe.ouom.wekit.hooks.sdk.base.model.MessageType
import moe.ouom.wekit.hooks.sdk.ui.WeChatMessageContextMenuApi
import moe.ouom.wekit.host.HostInfo
import moe.ouom.wekit.utils.common.ModuleRes
import moe.ouom.wekit.utils.common.ToastUtils
import moe.ouom.wekit.utils.log.WeLogger
import org.luckypray.dexkit.DexKitBridge
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.inputStream

@HookItem(path = "聊天/语音保存到本地", desc = "在语音消息菜单添加保存按钮, 允许将语音文件保存到本地")
object SaveVoicesToLocalStorage : BaseSwitchFunctionHookItem(), IDexFind,
    WeChatMessageContextMenuApi.IMenuItemsProvider {

    private const val TAG = "SaveVoicesToLocalStorage"

    private val classVoiceLogic by dexClass()
    private val methodGetAmrFullPath by dexMethod()

    private lateinit var methodStreamSilkDecInit: Method
    private lateinit var methodStreamSilkDecUnInit: Method
    private lateinit var methodStreamSilkDoDec: Method

    override fun entry(classLoader: ClassLoader) {
        val clazz = "com.tencent.mm.modelvoice.MediaRecorder".toClass(classLoader)
        methodStreamSilkDecInit = clazz.asResolver()
            .firstMethod { name = "StreamSilkDecInit" }
            .self
        methodStreamSilkDecUnInit = clazz.asResolver()
            .firstMethod { name = "StreamSilkDecUnInit" }
            .self
        methodStreamSilkDoDec = clazz.asResolver()
            .firstMethod { name = "StreamSilkDoDec" }
            .self

        WeChatMessageContextMenuApi.addProvider(this)
    }

    override fun unload(classLoader: ClassLoader) {
        WeChatMessageContextMenuApi.removeProvider(this)
        super.unload(classLoader)
    }

    override fun dexFind(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        classVoiceLogic.find(dexKit, descriptors) {
            matcher {
                usingEqStrings("MicroMsg.VoiceLogic", "startRecord insert voicestg success")
            }
        }

        methodGetAmrFullPath.find(dexKit, descriptors) {
            matcher {
                usingEqStrings("getAmrFullPath cost: ")
            }
        }

        return descriptors
    }

    override fun getMenuItems(): List<WeChatMessageContextMenuApi.MenuItem> {
        return listOf(
            @Suppress("UNCHECKED_CAST")
            WeChatMessageContextMenuApi.MenuItem(
                777003,
                "存本地",
                lazy { ModuleRes.getDrawable("download_24px") },
                { msgInfo -> msgInfo.isType(MessageType.VOICE) }
            ) { _, _, msgInfo ->
                val encPath = msgInfo.imagePath

                var service: Any? = null
                if (!Modifier.isStatic(methodGetAmrFullPath.method.modifiers)) {
                    service = WeServiceApi.getServiceByClass(methodGetAmrFullPath.method.declaringClass)
                }
                val srcPath = Path(methodGetAmrFullPath.method.invoke(service, null, encPath, true) as String)

                saveAudio(srcPath)

                val dstPath = srcPath.resolveSibling(srcPath.fileName.toString() + ".pcm")

                decodeWeChatSilkToPcm(srcPath, dstPath)

                saveAudio(Path("${srcPath}.pcm"))
            }
        )
    }

    private fun decodeWeChatSilkToPcm(srcPath: Path, dstPath: Path) {
        if (!srcPath.exists()) return

        val fis = FileInputStream(srcPath.toString())
        val fos = FileOutputStream(dstPath.toString())

        try {
            // 1. Handle the WeChat Header
            // WeChat files usually start with 0x02 followed by "#!SILK_V3"
            val firstByte = fis.read()
            if (firstByte == 0x02) {
                // It's a standard WeChat Silk file, skip this byte and continue
            } else {
                // If it doesn't start with 0x02, it might be a raw Silk file.
                // We reset to start or handle accordingly.
                fis.channel.position(0)
            }

            // Skip the Silk Magic Header ("#!SILK_V3" is 9 bytes)
            val magicHeader = ByteArray(9)
            fis.read(magicHeader)

            // 2. Initialize the Decoder
            // 24000 is the standard sample rate for WeChat voice.
            // 0L is the default initial state/flag.
            val handle = methodStreamSilkDecInit.invoke(null, 24000, 0L) as Long

            if (handle == 0L) {
                WeLogger.e(TAG, "Failed to initialize Silk decoder handle")
                return
            }

            // 3. Prepare Buffers
            // Silk frames are prefixed with a 2-byte (Short) length in Little-Endian
            val sizeBuffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
            val outLenArray = IntArray(1)
            val pcmBuffer = ByteArray(2048) // 2048 bytes is more than enough for one Silk frame

            // 4. Decoding Loop
            while (fis.read(sizeBuffer.array()) != -1) {
                val frameSize = sizeBuffer.short.toInt()
                sizeBuffer.clear()

                if (frameSize <= 0) break

                val silkFrame = ByteArray(frameSize)
                val readCount = fis.read(silkFrame)

                if (readCount == frameSize) {
                    // native int StreamSilkDoDec(byte[] in, int inLen, byte[] out, int[] outLen, boolean z, long handle)
                    val result = methodStreamSilkDoDec.invoke(
                        null,
                        silkFrame,
                        frameSize,
                        pcmBuffer,
                        outLenArray,
                        false, // z16: False (standard decoding)
                        handle
                    ) as Int

                    if (result == 0) {
                        val decodedBytes = outLenArray[0]
                        if (decodedBytes > 0) {
                            fos.write(pcmBuffer, 0, decodedBytes)
                        }
                    }
                }
            }

            // 5. Cleanup
            methodStreamSilkDecUnInit.invoke(null, handle)

        } catch (e: Exception) {
            WeLogger.e(TAG, "Critical error during Silk decoding", e)
        } finally {
            fis.close()
            fos.flush()
            fos.close()
        }
    }

    private fun saveAudio(sourceFile: Path) {
        val extension = sourceFile.extension
        val resolver = HostInfo.getApplication().contentResolver
        val fileName = "voice_${System.currentTimeMillis()}.$extension"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/$extension")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_MUSIC + "/WeKit"
            )
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        val audioUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

        audioUri?.let { uri ->
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                ToastUtils.showToast("已将语音保存到 /sdcard/Music/WeKit/$fileName")
            } catch (e: Exception) {
                WeLogger.e(TAG, "failed to save voice message", e)
                resolver.delete(uri, null, null)
            }
        }
    }
}