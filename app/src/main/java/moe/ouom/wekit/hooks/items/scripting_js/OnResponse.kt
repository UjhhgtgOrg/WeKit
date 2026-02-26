package moe.ouom.wekit.hooks.items.scripting_js

import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem
import moe.ouom.wekit.hooks.core.annotation.HookItem

@HookItem(path = "脚本/触发器：收到响应", desc = "收到响应时是否执行 onResponse()")
object OnResponse : BaseSwitchFunctionHookItem()