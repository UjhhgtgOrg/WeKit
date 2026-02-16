package moe.ouom.wekit.core.bridge

import moe.ouom.wekit.core.bridge.api.IHookFactoryDelegate
import moe.ouom.wekit.core.model.BaseClickableFunctionHookItem
import moe.ouom.wekit.core.model.BaseHookItem
import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem

object HookFactoryBridge {
    private var delegate: IHookFactoryDelegate? = null

    fun registerDelegate(impl: IHookFactoryDelegate) {
        this.delegate = impl
    }

    fun getAllItemList(): List<BaseHookItem> {
        return delegate?.getAllItemList() ?: emptyList()
    }

    fun getAllSwitchFunctionItemList(): List<BaseSwitchFunctionHookItem> {
        return delegate?.getAllSwitchFunctionItemList() ?: emptyList()
    }

    fun getAllClickableFunctionItemList(): List<BaseClickableFunctionHookItem> {
        return delegate?.getAllClickableFunctionItemList() ?: emptyList()
    }

    fun findHookItemByPath(path: String): BaseSwitchFunctionHookItem? {
        return delegate?.findHookItemByPath(path)
    }

    fun isReady(): Boolean = delegate != null
}