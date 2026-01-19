package moe.ouom.wekit.hooks._base;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findConstructorExact;
import static moe.ouom.wekit.constants.Constants.PrekCfgXXX;

import androidx.annotation.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import moe.ouom.wekit.config.ConfigManager;
import moe.ouom.wekit.hooks._core.factory.ExceptionFactory;
import moe.ouom.wekit.loader.startup.HybridClassLoader;
import moe.ouom.wekit.util.log.Logger;

/**
 * 所有 hook 功能的基础类, 都应该要继承这个类
 */
public abstract class BaseHookItem {

    /**
     * 功能名称
     */
    private String path;
    /**
     * 功能描述
     */
    private String desc;

    /**
     * 是否加载
     */
    private boolean isLoad = false;

    public String getSimpleName() {
        return this.getClass().getSimpleName();
    }

    public String getPath() {
        return path;
    }


    public void setPath(String path) {
        this.path = path;
    }

    public String getDesc() {
        return desc;
    }


    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getItemName() {
        int index = path.lastIndexOf("/");
        if (index == -1) {
            return path;
        }
        return path.substring(index + 1);
    }

    public boolean isLoad() {
        return isLoad;
    }

    public final void startLoad() {
        if (isLoad) {
            return;
        }
        try {
            isLoad = true;
            initOnce();
            if (initOnce()) {
                entry(HybridClassLoader.getHostClassLoader());
            }
        } catch (Throwable e) {
            XposedBridge.log(e);
            Logger.e("BaseHookItem Load Failed", e);
            ExceptionFactory.add(this, e);
        }
    }

    /**
     * 在 loadHook 前执行一次 返回true表示继续执行loadHook
     * 如果返回 false 表示由 initOnce 自行处理 loadHook 事件
     */
    public boolean initOnce() {
        return true;
    }

    public abstract void entry(@NonNull ClassLoader classLoader) throws Throwable;

    public abstract void unload(@NonNull ClassLoader classLoader) throws Throwable;

    /**
     * 标准hook方法执行前
     */
    protected XC_MethodHook.Unhook hookBefore(Member method, HookAction action) {
        return XposedBridge.hookMethod(method, new XC_MethodHook(ConfigManager.dGetInt(PrekCfgXXX+"qq_inside_hook_priority",50)) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                tryExecute(param, action);
            }
        });
    }

    protected XC_MethodHook.Unhook hookBefore(Class<?> clazz, HookAction action, Object... parameterTypesAndCallback) {
        Constructor<?> m = findConstructorExact(clazz, getParameterClasses(clazz.getClassLoader(), parameterTypesAndCallback));

        return XposedBridge.hookMethod(m, new XC_MethodHook(ConfigManager.dGetInt(PrekCfgXXX+"qq_inside_hook_priority",50)) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                tryExecute(param, action);
            }
        });
    }


    /**
     * 标准hook方法执行后
     */
    protected XC_MethodHook.Unhook hookAfter(Member method, HookAction action) {
        return XposedBridge.hookMethod(method, new XC_MethodHook(ConfigManager.dGetInt(PrekCfgXXX+"qq_inside_hook_priority",50)) {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                tryExecute(param, action);
            }
        });
    }

    protected XC_MethodHook.Unhook hookAfter(Class<?> clazz, HookAction action, Object... parameterTypesAndCallback) {
        Constructor<?> m = findConstructorExact(clazz, getParameterClasses(clazz.getClassLoader(), parameterTypesAndCallback));

        return XposedBridge.hookMethod(m, new XC_MethodHook(ConfigManager.dGetInt(PrekCfgXXX+"qq_inside_hook_priority",50)) {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                tryExecute(param, action);
            }
        });
    }

    /**
     * 带执行优先级的 hook
     *
     * @param priority 越高 执行优先级越高 默认50
     */
    protected XC_MethodHook.Unhook hookAfter(Member method, HookAction action, int priority) {
        return XposedBridge.hookMethod(method, new XC_MethodHook(priority) {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                tryExecute(param, action);
            }
        });
    }

    /**
     * 跟上面那个一样
     */
    protected XC_MethodHook.Unhook hookBefore(Member method, HookAction action, int priority) {
        return XposedBridge.hookMethod(method, new XC_MethodHook(priority) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                tryExecute(param, action);
            }
        });
    }

    /**
     * 真正执行接口方法的地方 ，这么写可以很便捷的捕获异常和子类重写
     */
    protected void tryExecute(XC_MethodHook.MethodHookParam param, HookAction hookAction) {
        try {
            hookAction.call(param);
        } catch (Throwable throwable) {
            ExceptionFactory.add(this, throwable);
        }
    }

    private static Class<?>[] getParameterClasses(ClassLoader classLoader, Object[] parameterTypesAndCallback) {
        Class<?>[] parameterClasses = null;
        for (int i = parameterTypesAndCallback.length - 1; i >= 0; i--) {
            Object type = parameterTypesAndCallback[i];
            if (type == null)
                throw new NullPointerException("parameter type must not be null");

            // ignore trailing callback
            if (type instanceof XC_MethodHook)
                continue;

            if (parameterClasses == null)
                parameterClasses = new Class<?>[i+1];

            if (type instanceof Class)
                parameterClasses[i] = (Class<?>) type;
            else if (type instanceof String)
                parameterClasses[i] = findClass((String) type, classLoader);
            else
                throw new IllegalArgumentException("parameter type must either be specified as Class or String", null);
        }

        // if there are no arguments for the method
        if (parameterClasses == null)
            parameterClasses = new Class<?>[0];

        return parameterClasses;
    }


    /**
     * hook 动作 指代
     * new XC_MethodHook() {
     * protected void beforeHookedMethod(MethodHookParam param) {
     * action.call(param);
     * }
     * }
     */
    protected interface HookAction {
        void call(XC_MethodHook.MethodHookParam param) throws Throwable;
    }

}
