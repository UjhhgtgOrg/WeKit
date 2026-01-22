package moe.ouom.wekit.core.dsl

import moe.ouom.wekit.loader.startup.HybridClassLoader

/**
 * DSL 扩展函数
 * 为 DexMethodDescriptorWrapper 提供便捷的 Hook 语法
 */

/**
 * 扩展函数：使用全局优先级 Hook 方法
 * 自动使用 HybridClassLoader 和全局优先级配置
 */
fun DexMethodDescriptorWrapper.toDexMethod(
    block: DexMethodHookBuilder.HookConfigBuilder.() -> Unit
) {
    this.toDexMethod(
        HybridClassLoader.getHostClassLoader()
    ) {
        hook(block)
    }
}

/**
 * 扩展函数：使用自定义优先级 Hook 方法
 * @param priority 自定义优先级值
 */
fun DexMethodDescriptorWrapper.toDexMethod(
    priority: Int,
    block: DexMethodHookBuilder.HookConfigBuilder.() -> Unit
) {
    this.toDexMethod(
        HybridClassLoader.getHostClassLoader(),
        priority
    ) {
        hook {
            block()
        }
    }
}
