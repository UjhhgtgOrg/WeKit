package moe.ouom.wekit.hooks.sdk.protocol

import moe.ouom.wekit.hooks.sdk.protocol.intf.WeReqCallback

class WeReqDsl : WeReqCallback {
    private var successHandler: ((String, ByteArray?) -> Unit)? = null
    private var failHandler: ((Int, Int, String) -> Unit)? = null

    fun onSuccess(handler: (json: String, bytes: ByteArray?) -> Unit) {
        this.successHandler = handler
    }

    fun onFail(handler: (errType: Int, errCode: Int, errMsg: String) -> Unit) {
        this.failHandler = handler
    }

    override fun onSuccess(json: String, bytes: ByteArray?) {
        successHandler?.invoke(json, bytes)
    }

    override fun onFail(errType: Int, errCode: Int, errMsg: String) {
        failHandler?.invoke(errType, errCode, errMsg)
    }
}