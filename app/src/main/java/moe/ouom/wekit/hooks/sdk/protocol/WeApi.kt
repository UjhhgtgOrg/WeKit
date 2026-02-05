package moe.ouom.wekit.hooks.sdk.protocol

import de.robv.android.xposed.XposedHelpers

object WeApi {
    private var classLoader: ClassLoader? = null

    fun init(loader: ClassLoader) { this.classLoader = loader }

    /**
     * 预览下一个可用的 LocalMsgId
     */
    fun previewNextId(): Long {
        val loader = classLoader ?: return System.currentTimeMillis() / 1000
        val clsKernelService = XposedHelpers.findClass("ga3.x3", loader)
        val clsMMKernel = XposedHelpers.findClass("hi0.j1", loader)
        val kernelService = XposedHelpers.callStaticMethod(clsMMKernel, "s", clsKernelService)
        val storage = XposedHelpers.callMethod(kernelService, "gh")

        val c0Class = XposedHelpers.findClass("ha3.c0", loader)
        val c0Field = storage.javaClass.declaredFields.firstOrNull { it.type == c0Class }
        if (c0Field != null) {
            c0Field.isAccessible = true
            val c0Obj = c0Field.get(storage)
            val currentId = XposedHelpers.getLongField(c0Obj, "a")
            return currentId + 1
        } else {
            throw IllegalStateException("获取 MsgInfoStorage的 c0 字段失败：未找到匹配类型的字段")
        }
    }
}