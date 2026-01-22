package moe.ouom.wekit.hooks.item.example

import android.util.Log
import de.robv.android.xposed.XposedHelpers
import moe.ouom.wekit.core.dsl.lazyDexMethod
import moe.ouom.wekit.core.dsl.resultNull
import moe.ouom.wekit.core.dsl.toDexMethod
import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem
import moe.ouom.wekit.dexkit.intf.IDexFind
import moe.ouom.wekit.util.log.WeLogger
import org.json.JSONObject
import org.luckypray.dexkit.DexKitBridge

/**
 * HookItem 的写法示例
 */

// 下面这一行在写功能的时候必须保留，否则 ksp 将无法标记此类，这里为了防止被扫描所以注释掉了
//@HookItem(path = "example/示例写法", desc = "展示新架构的简化写法")
class SimplifiedExample : BaseSwitchFunctionHookItem() /* 这里也可以继承 BaseClickableFunctionHookItem */, IDexFind {

    // DSL: 懒加载 Dex 方法（不需要 path、searchVersion、priorityKey）
    private val MethodTarget by lazyDexMethod("MethodTarget")

    // Dex 查找逻辑
    // 他会自动检测逻辑变化，当寻找逻辑发生改变时，会自动要求宿主适配新的逻辑
    override fun dexFind(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        MethodTarget.findDexClassMethod(dexKit) {
            matcher {
                name = "targetMethod"
                paramCount = 2
                usingStrings("some_string_constant")
            }
        }

        MethodTarget.getDescriptorString()?.let { descriptors["MethodTarget"] = it }

        return descriptors
    }

    override fun loadFromCache(cache: Map<String, Any>) {
        (cache["MethodTarget"] as? String)?.let { MethodTarget.setDescriptorFromCache(it) }
    }

    // Hook 入口
    override fun entry(classLoader: ClassLoader) {
        // 日志输出请务必使用 `WeLogger`，他会自动添加 TAG，并且适配多种输出需求，如：
        WeLogger.i("SimplifiedExample: 日志输出请务必使用 `WeLogger`，他会自动添加 TAG，并且适配多种输出需求，如：")
        WeLogger.i("SimplifiedExample: 错误", Throwable())
        WeLogger.e("SimplifiedExample", "xxxxx")
        WeLogger.w("SimplifiedExample", "xxxxx")
        WeLogger.v("SimplifiedExample", "xxxxx")
        WeLogger.e("SimplifiedExample", 1230000000000L)
        WeLogger.w("SimplifiedExample", WeLogger.getStackTraceString(Throwable()))
        WeLogger.printStackTrace() // DEBUG 级别
        WeLogger.printStackTrace(Log.ERROR, "SimplifiedExample", "异常堆栈：")
        WeLogger.printStackTraceErr("SimplifiedExample", Throwable())


        // 方式 1: 使用全局优先级（推荐）
        MethodTarget.toDexMethod {
            beforeIfEnabled { param ->
                // ....
                param.resultNull()
            }
        }

        // 方式 2: 使用自定义优先级
        MethodTarget.toDexMethod(priority = 100) {
             afterIfEnabled { param ->
                 // ...
             }
        }


        // 方式 3: 这里拿 Hook A 作为例子 （使用全局 HOOK 优先级）
        val clsReceiveLuckyMoney: Class<*> = XposedHelpers.findClass("com.example.LuckyMoneyReceive", classLoader)
        val mOnGYNetEnd = XposedHelpers.findMethodExact(
            clsReceiveLuckyMoney,
            "A",
            Int::class.javaPrimitiveType,
            String::class.java,
            JSONObject::class.java
        )

        val h1 =hookAfter(mOnGYNetEnd) { param ->
            // ....
        }

        // 可选：如需取消Hook，调用 h2.unhook()
        h1.unhook()


        // 方式 4: 这里拿 Hook B 作为例子 （使用自定义 HOOK 优先级）
        val clsReceiveLuckyMoney2: Class<*> = XposedHelpers.findClass("com.example.LuckyMoneyReceive", classLoader)
        val mOnGYNetEnd2 = XposedHelpers.findMethodExact(
            clsReceiveLuckyMoney2,
            "B",
            Int::class.javaPrimitiveType,
            String::class.java,
            JSONObject::class.java
        )

        hookAfter(mOnGYNetEnd2, priority = 50) { param ->
            // ....
        }

        // 方式 5: 带执行优先级的 hook 构造方法执行后
        val targetClass = XposedHelpers.findClass("com.example.TestClass", classLoader)
        val h2 = hookAfter(
            clazz = targetClass,
            priority = 50,
            action = {

            }
            // 无参构造方法，无需传parameterTypesAndCallback参数
        )

        // 可选：如需取消Hook，调用 h2.unhook()
        h2.unhook()

        // 此处不再举例....
    }

    override fun unload(classLoader: ClassLoader) {
        // 在这里清理资源
    }

    // 若继承 BaseClickableFunctionHookItem，可以重写此方法来定义点击事件
//    override fun onClick(context: Context?) {
//        WeLogger.i("onClick")
//        super.onClick(context)
//    }
}
