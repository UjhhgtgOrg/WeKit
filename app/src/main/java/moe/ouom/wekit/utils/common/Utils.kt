package moe.ouom.wekit.utils.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.size
import java.text.SimpleDateFormat
import java.util.Date
import java.util.regex.Pattern

object Utils {

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
