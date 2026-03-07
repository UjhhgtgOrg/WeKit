package moe.ouom.wekit.ui.utils

import android.annotation.SuppressLint
import android.view.View
import moe.ouom.wekit.constants.PackageConstants

@SuppressLint("DiscouragedApi")
fun <T : View> View.findViewByIdStr(idStr: String): T {
    val id = resources.getIdentifier(idStr, "id", PackageConstants.PACKAGE_NAME_WECHAT)
    return this.findViewById<T>(id)
}