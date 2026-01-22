package moe.ouom.wekit.dexkit.intf

import org.luckypray.dexkit.DexKitBridge

/**
 * Dex 查找接口
 * 实现此接口的 HookItem 将支持自包含的 Dex 方法查找
 */
interface IDexFind {
    /**
     * 执行 Dex 查找并返回所有 descriptor
     * @param dexKit DexKitBridge 实例
     * @return Map<属性名, descriptor字符串>
     */
    fun dexFind(dexKit: DexKitBridge): Map<String, String>

    /**
     * 从缓存加载 descriptors
     * @param cache 缓存的 Map<属性名, descriptor字符串>
     */
    fun loadFromCache(cache: Map<String, Any>)
}
