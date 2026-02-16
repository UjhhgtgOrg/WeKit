package moe.ouom.wekit.dexkit.intf

import moe.ouom.wekit.core.dsl.DexClassDelegate
import moe.ouom.wekit.core.dsl.DexMethodDelegate
import org.luckypray.dexkit.DexKitBridge

/**
 * Dex 查找接口
 * 实现此接口的 HookItem 将支持自包含的 Dex 方法查找
 */
interface IDexFind {
    /**
     * 执行 Dex 查找
     * @param dexKit DexKitBridge 实例
     * @return Map<Key, descriptor字符串>
     */
    fun dexFind(dexKit: DexKitBridge): Map<String, String>

    /**
     * 从缓存加载 descriptors
     * @param cache 缓存的 Map<Key, descriptor字符串>
     * @throws IllegalStateException 如果缓存不完整（缺少必需的委托）
     */
    fun loadFromCache(cache: Map<String, Any>) {
        // 自动收集所有 dex 开头的委托属性
        val delegates = collectDexDelegates()

        // 检查缓存完整性：所有委托都应该在缓存中
        val missingKeys = mutableListOf<String>()
        delegates.forEach { (key, delegate) ->
            val value = cache[key] as? String
            if (value != null) {
                when (delegate) {
                    is DexClassDelegate -> delegate.setDescriptor(value)
                    is DexMethodDelegate -> delegate.setDescriptorFromString(value)
                }
            } else {
                missingKeys.add(key)
            }
        }

        // 如果有缺失的键，抛出异常触发重新查找
        if (missingKeys.isNotEmpty()) {
            throw IllegalStateException(
                "Cache incomplete for ${this::class.java.simpleName}: missing keys $missingKeys. " +
                        "This will trigger a rescan."
            )
        }
    }

    /**
     * 收集所有 dex 委托属性
     */
    fun collectDexDelegates(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val clazz = this::class.java

        // 遍历所有字段
        clazz.declaredFields.forEach { field ->
            try {
                field.isAccessible = true
                when (val value = field.get(this)) {
                    is DexClassDelegate -> result[value.key] = value
                    is DexMethodDelegate -> result[value.key] = value
                }
            } catch (_: Exception) {
                // 忽略无法访问的字段
            }
        }

        return result
    }
}
