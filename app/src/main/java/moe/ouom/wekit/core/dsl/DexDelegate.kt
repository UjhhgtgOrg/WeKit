package moe.ouom.wekit.core.dsl

import de.robv.android.xposed.XposedHelpers
import moe.ouom.wekit.config.RuntimeConfig
import moe.ouom.wekit.dexkit.DexMethodDescriptor
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindMethod
import java.lang.reflect.Method
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Dex 类委托
 * 自动生成 Key，自动反射获取 Class
 */
class DexClassDelegate internal constructor(
    val key: String
) : ReadOnlyProperty<Any?, DexClassDelegate> {

    private var descriptorString: String? = null
    private var cachedClass: Class<*>? = null

    /**
     * 获取 Class 实例
     */
    val clazz: Class<*>
        get() {
            if (cachedClass == null && descriptorString != null) {
                cachedClass = XposedHelpers.findClass(
                    descriptorString,
                    RuntimeConfig.getHostClassLoader()
                )
            }
            return cachedClass ?: throw IllegalStateException("Class not found for key: $key")
        }

    /**
     * 设置描述符
     */
    fun setDescriptor(className: String) {
        this.descriptorString = className
        this.cachedClass = null // 清除缓存，下次访问时重新反射
    }

    /**
     * 获取描述符字符串
     */
    fun getDescriptorString(): String? = descriptorString

    /**
     * 查找 Dex 类
     */
    fun find(
        dexKit: DexKitBridge,
        allowMultiple: Boolean = false,
        descriptors: MutableMap<String, String>? = null,
        block: FindClass.() -> Unit
    ): Boolean {
        val results = dexKit.findClass(block).toList()

        if (results.isEmpty()) {
            return false
        }

        if (results.size > 1 && !allowMultiple) {
            throw RuntimeException("DexKit: Multiple classes found for key: $key, count: ${results.size}")
        }

        setDescriptor(results[0].name)
        descriptors?.let { it[key] = results[0].name }
        return true
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): DexClassDelegate = this
}

/**
 * Dex 方法委托
 * 自动生成 Key，自动反射获取 Method
 */
class DexMethodDelegate internal constructor(
    val key: String
) : ReadOnlyProperty<Any?, DexMethodDelegate> {

    private var descriptor: DexMethodDescriptor? = null
    private var cachedMethod: Method? = null

    /**
     * 获取 Method 实例（自动反射）
     */
    val method: Method
        get() {
            if (cachedMethod == null && descriptor != null) {
                cachedMethod = descriptor!!.getMethodInstance(RuntimeConfig.getHostClassLoader())
            }
            return cachedMethod ?: throw IllegalStateException("Method not found for key: $key")
        }

    /**
     * 设置描述符
     */
    fun setDescriptor(desc: DexMethodDescriptor) {
        this.descriptor = desc
        this.cachedMethod = null
    }

    /**
     * 从字符串设置描述符
     */
    fun setDescriptorFromString(descriptorString: String) {
        this.descriptor = DexMethodDescriptor(descriptorString)
        this.cachedMethod = null
    }

    /**
     * 获取描述符字符串
     */
    fun getDescriptorString(): String? = descriptor?.descriptor

    /**
     * 查找 Dex 方法
     */
    fun find(
        dexKit: DexKitBridge,
        allowMultiple: Boolean = false,
        descriptors: MutableMap<String, String>? = null,
        block: FindMethod.() -> Unit
    ): Boolean {
        val results = dexKit.findMethod(block).toList()

        if (results.isEmpty()) {
            return false
        }

        if (results.size > 1 && !allowMultiple) {
            throw RuntimeException("DexKit: Multiple methods found for key: $key, count: ${results.size}")
        }

        val methodData = results[0]
        val desc = DexMethodDescriptor(
            methodData.className,
            methodData.methodName,
            methodData.methodSign
        )
        setDescriptor(desc)
        descriptors?.let { it[key] = desc.descriptor }
        return true
    }

    /**
     * DSL: 转换为可 Hook 的方法
     */
    fun toDexMethod(block: DexMethodHookBuilder.() -> Unit) {
        toDexMethod(null, block)
    }

    fun toDexMethod(priority: Int?, block: DexMethodHookBuilder.() -> Unit) {
        val builder = DexMethodHookBuilder(method, priority)
        builder.block()
        builder.execute()
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): DexMethodDelegate = this
}

/**
 * 创建 dexClass 委托
 * 自动生成 Key 为 "类名:变量名"
 */
fun dexClass(): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, DexClassDelegate>> {
    return PropertyDelegateProvider { thisRef, property ->
        val className = thisRef!!::class.java.simpleName
        val key = "$className:${property.name}"
        DexClassDelegate(key)
    }
}

/**
 * 创建 dexMethod 委托
 * 自动生成 Key 为 "类名:变量名"
 */
fun dexMethod(): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, DexMethodDelegate>> {
    return PropertyDelegateProvider { thisRef, property ->
        val className = thisRef!!::class.java.simpleName
        val key = "$className:${property.name}"
        DexMethodDelegate(key)
    }
}
