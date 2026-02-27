package moe.ouom.wekit.utils.common

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.size
import de.robv.android.xposed.XposedBridge
import moe.ouom.wekit.utils.log.WeLogger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects
import java.util.regex.Pattern

object Utils {

    fun getAllViews(act: Activity): MutableList<View> {
        return getAllChildViews(act.window.decorView)
    }

    private fun getAllChildViews(view: View?): MutableList<View> {
        val allChildren: MutableList<View> = ArrayList<View>()
        if (view is ViewGroup) {
            for (i in 0..<view.size) {
                val viewChild = view.getChildAt(i)
                allChildren.add(viewChild!!)
                allChildren.addAll(getAllChildViews(viewChild))
            }
        }
        return allChildren
    }

    @Throws(InterruptedException::class)
    fun getViewByDesc(act: Activity, desc: String?, limit: Int): View? {
        for (x in 0..<limit) {
            for (view in getAllViews(act)) {
                try {
                    if (view.contentDescription == desc) {
                        return view
                    }
                } catch (e: Exception) {
                    XposedBridge.log(e)
                }
            }
            Thread.sleep(200)
        }

        return null
    }

    fun getViewByDesc(act: Activity, desc: String?): View? {
        try {
            for (view in getAllViews(act)) {
                try {
                    if (view.contentDescription == desc) {
                        return view
                    }
                } catch (e: Exception) {
                    XposedBridge.log(e)
                }
            }
        } catch (e: Exception) {
            WeLogger.e(e)
        }

        return null
    }

    fun printStackTrace() {
        val stackTrace = Thread.currentThread().stackTrace
        WeLogger.e("---------------------- [Stack Trace] ----------------------")
        for (element in stackTrace) {
            WeLogger.d("    at $element")
        }
        WeLogger.e("^---------------------- over ----------------------^")
    }


    fun printIntentExtras(tag: String?, intent: Intent?) {
        if (intent == null) {
            WeLogger.e("Intent is null or has no extras.")
            return
        }

        WeLogger.i("*-------------------- $tag --------------------*")
        val extras = intent.extras
        if (extras != null) {
            for (key in extras.keySet()) {
                val value = extras.get(key)
                WeLogger.d(key + " = " + Objects.requireNonNull<Any?>(value) + "(" + value!!.javaClass + ")")
            }
        } else {
            WeLogger.w("No extras found in the Intent.")
        }

        WeLogger.i("^-------------------- " + "OVER~" + " --------------------^")
    }

    fun getActivityFromView(view: View): Activity? {
        var context = view.context
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }


    fun getActivityFromContext(context: Context?): Activity? {
        var context = context
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

    fun jumpUrl(context: Context, webUrl: String?) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = webUrl?.toUri()
        context.startActivity(intent)
    }

    fun convertTimestampToDate(timestamp: Long): String {
        val date = Date(timestamp)
        @SuppressLint("SimpleDateFormat") val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return sdf.format(date)
    }

    fun bytesToHex(bytes: ByteArray): String {
        val hexString = StringBuilder()
        for (b in bytes) {
            val hex = Integer.toHexString(0xFF and b.toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }

    /**
     * 从 XML 提取属性值 (e.g. appid="xxx")
     */
    fun extractXmlAttr(xml: String, attrName: String): String {
        try {
            val pattern = Pattern.compile("$attrName=\"([^\"]*)\"")
            val matcher = pattern.matcher(xml)
            if (matcher.find()) {
                return matcher.group(1) ?: ""
            }
        } catch (e: Exception) {
            // ignore
        }
        return ""
    }

    /**
     * 从 XML 提取标签内容 (e.g. <title>xxx</title>)
     */
    fun extractXmlTag(xml: String, tagName: String): String {
        try {
            val pattern = Pattern.compile("<$tagName><!\\[CDATA\\[(.*?)]]></$tagName>")
            val matcher = pattern.matcher(xml)
            if (matcher.find()) {
                return matcher.group(1) ?: ""
            }
            // Fallback for non-CDATA
            val patternSimple = Pattern.compile("<$tagName>(.*?)</$tagName>")
            val matcherSimple = patternSimple.matcher(xml)
            if (matcherSimple.find()) {
                return matcherSimple.group(1) ?: ""
            }
        } catch (_: Exception) {
            // ignore
        }
        return ""
    }
}
