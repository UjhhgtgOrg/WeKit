package moe.ouom.wekit.hooks.sdk.protocol.intf

interface WeReqCallback {
    fun onSuccess(json: String, bytes: ByteArray?)
    fun onFail(errType: Int, errCode: Int, errMsg: String)
}