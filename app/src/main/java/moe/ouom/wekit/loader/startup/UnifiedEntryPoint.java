package moe.ouom.wekit.loader.startup;

import static moe.ouom.wekit.constants.Constants.CLAZZ_BASE_APPLICATION;
import static moe.ouom.wekit.constants.Constants.CLAZZ_MM_APPLICATION_LIKE;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import moe.ouom.wekit.BuildConfig;
import moe.ouom.wekit.loader.hookapi.IHookBridge;
import moe.ouom.wekit.loader.hookapi.ILoaderService;
import moe.ouom.wekit.util.Initiator;
import moe.ouom.wekit.util.log.WeLogger;

@Keep
@SuppressWarnings("unused")
public class UnifiedEntryPoint {

    private static boolean sInitialized = false;

    private UnifiedEntryPoint() {}

    @Keep
    public static void entry(
            @NonNull String modulePath,
            @NonNull String hostDataDir,
            @NonNull ILoaderService loaderService,
            @NonNull ClassLoader hostClassLoader,
            @Nullable IHookBridge hookBridge
    ) {
        if (sInitialized) {
            throw new IllegalStateException("UnifiedEntryPoint already initialized");
        }
        sInitialized = true;
        // fix up the class loader
        HybridClassLoader loader = HybridClassLoader.INSTANCE;
        ClassLoader self = UnifiedEntryPoint.class.getClassLoader();
        assert self != null;
        ClassLoader parent = self.getParent();
        HybridClassLoader.setLoaderParentClassLoader(parent);
        injectClassLoader(self, loader);
        callNextStep(modulePath, hostDataDir, loaderService, hostClassLoader, hookBridge);
    }

    private static void callNextStep(
            @NonNull String modulePath,
            @NonNull String hostDataDir,
            @NonNull ILoaderService loaderService,
            @NonNull ClassLoader initialClassLoader,
            @Nullable IHookBridge hookBridge
    ) {
        try {
            // Hook 壳 Application
            XposedHelpers.findAndHookMethod(
                    CLAZZ_BASE_APPLICATION,
                    initialClassLoader,
                    "attachBaseContext",
                    Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            WeLogger.i("UnifiedEntryPoint", "Shell attached (Application.attachBaseContext done).");

                            Context context = (Context) param.thisObject;
                            ClassLoader currentClassLoader = context.getClassLoader();

                            // Hook Instrumentation.callApplicationOnCreate 以处理 Tinker 热更新场景
                            try {
                                hookInstrumentationForTinker(currentClassLoader, modulePath, hostDataDir, loaderService, initialClassLoader, hookBridge);
                            } catch (Throwable t) {
                                Log.e(BuildConfig.TAG, "Failed to hook Instrumentation.callApplicationOnCreate", t);
                            }
                        }
                    }
            );
            Log.i(BuildConfig.TAG, "Hook applied: waiting for Application.attachBaseContext");
        } catch (Throwable t) {
            Log.e(BuildConfig.TAG, "Failed to hook Shell Application", t);
        }
    }

    /**
     * Hook Instrumentation.callApplicationOnCreate 以确保在 Tinker 热更新完成后再进行延迟初始化
     * 这可以解决某些模块在热更新环境下找不到入口的问题
     */
    private static void hookInstrumentationForTinker(@NonNull ClassLoader hostClassLoader,
                                                     @NonNull String modulePath,
                                                     @NonNull String hostDataDir,
                                                     @NonNull ILoaderService loaderService,
                                                     @NonNull ClassLoader initialClassLoader,
                                                     @Nullable IHookBridge hookBridge) {
        try {
            Class<?> instrumentationClass = Class.forName("android.app.Instrumentation", false, hostClassLoader);
            XposedHelpers.findAndHookMethod(
                    instrumentationClass,
                    "callApplicationOnCreate",
                    Application.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Application application = (Application) param.args[0];
                            Application hostApp = (Application) param.args[0];
                            StartupInfo.setHostApp(hostApp);

                            WeLogger.i("UnifiedEntryPoint", "Instrumentation.callApplicationOnCreate captured!");
                            WeLogger.i("UnifiedEntryPoint", "Application: " + application.getClass().getName());

                            ClassLoader realClassLoader = application.getBaseContext().getClassLoader();
                            WeLogger.i("UnifiedEntryPoint", "Real ClassLoader: " + realClassLoader.getClass().getName());
                            Initiator.init(realClassLoader);

                            WeLogger.i("UnifiedEntryPoint", "Invoking StartupAgent immediately...");
                            try {
                                Class<?> kStartupAgent = Class.forName("moe.ouom.wekit.loader.startup.StartupAgent", false, UnifiedEntryPoint.class.getClassLoader());
                                kStartupAgent.getMethod("startup", String.class, String.class, ILoaderService.class, ClassLoader.class, IHookBridge.class)
                                        .invoke(null, modulePath, hostDataDir, loaderService, realClassLoader, hookBridge);
                                WeLogger.i("UnifiedEntryPoint", "StartupAgent invoked successfully.");
                            } catch (Throwable e) {
                                Log.e(BuildConfig.TAG, "StartupAgent.startup failed", e);
                            }
                        }
                    }
            );
            WeLogger.i("UnifiedEntryPoint", "Instrumentation.callApplicationOnCreate hook installed successfully.");
        } catch (Throwable e) {
            Log.e(BuildConfig.TAG, "Failed to hook Instrumentation.callApplicationOnCreate", e);
        }
    }


    @SuppressWarnings("JavaReflectionMemberAccess")
    @SuppressLint("DiscouragedPrivateApi")
    private static void injectClassLoader(ClassLoader self, ClassLoader newParent) {
        try {
            Field fParent = ClassLoader.class.getDeclaredField("parent");
            fParent.setAccessible(true);
            fParent.set(self, newParent);
        } catch (Exception e) {
            WeLogger.e("injectClassLoader: failed", e);
        }
    }

    @NonNull
    private static Throwable getInvocationTargetExceptionCause(@NonNull Throwable e) {
        while (e instanceof InvocationTargetException) {
            Throwable cause = ((InvocationTargetException) e).getTargetException();
            if (cause != null) {
                e = cause;
            } else {
                break;
            }
        }
        return e;
    }


    @SuppressWarnings("unchecked")
    @NonNull
    private static <T extends Throwable> AssertionError unsafeThrow(@NonNull Throwable e) throws T {
        throw (T) e;
    }

}
