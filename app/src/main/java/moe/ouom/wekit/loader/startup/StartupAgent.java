package moe.ouom.wekit.loader.startup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.io.File;
import java.lang.reflect.Field;

import moe.ouom.wekit.BuildConfig;
import moe.ouom.wekit.utils.log.WeLogger;


@Keep
public class StartupAgent {

    private static final String TAG = "StartupAgent";
    private static boolean sInitialized = false;

    @Keep
    public static void startup(
        @NonNull String modulePath,
        @NonNull String hostDataDir,
        @NonNull ClassLoader hostClassLoader
    ) {
        if (sInitialized) {
            WeLogger.w(TAG, "already initialized");
            return;
        }
        sInitialized = true;
        if ("true".equals(System.getProperty(StartupAgent.class.getName()))) {
            WeLogger.e(TAG, "WeKit reloaded??");
            return;
        }

        System.setProperty(StartupAgent.class.getName(), "true");
        StartupInfo.setModulePath(modulePath);
        StartupInfo.setInHostProcess(true);

        // bypass hidden api
        ensureHiddenApiAccess();
        checkWriteXorExecuteForModulePath(modulePath);
        // we want context
        var ctx = getBaseApplication(hostClassLoader);

        StartupHook.getInstance().initializeAfterAppCreate(ctx);
    }

    private static void checkWriteXorExecuteForModulePath(@NonNull String modulePath) {
        var moduleFile = new File(modulePath);
        if (moduleFile.canWrite()) {
            android.util.Log.w(BuildConfig.TAG, "Module path is writable: " + modulePath);
            android.util.Log.w(BuildConfig.TAG, "This may cause issues on Android 15+, please check your Xposed framework");
        }
    }

    public static Context getBaseApplication(@NonNull ClassLoader classLoader) {
        try {
            var tinkerAppClz = classLoader.loadClass("com.tencent.tinker.loader.app.TinkerApplication");
            var getInstanceMethod = tinkerAppClz.getMethod("getInstance");
            var app = (Context) getInstanceMethod.invoke(null);

            if (app != null) {
                return app;
            }
        } catch (Throwable e) {
            Log.w(BuildConfig.TAG, "Failed to call TinkerApplication.getInstance()", e);
        }

        // 只有在 TinkerApplication 还没初始化或者反射失败时，才使用 ActivityThread 作为最后的保底手段
        try {
            @SuppressLint("PrivateApi") var activityThreadClz = classLoader.loadClass("android.app.ActivityThread");
            @SuppressLint("DiscouragedPrivateApi") var currentAppMethod = activityThreadClz.getDeclaredMethod("currentApplication");
            currentAppMethod.setAccessible(true);
            var app = (Context) currentAppMethod.invoke(null);

            if (app != null) {
                return app;
            }
        } catch (Exception e) {
            WeLogger.e("getBaseApplication: ActivityThread fallback failed", e);
        }

        throw new UnsupportedOperationException("Failed to retrieve Application instance.");
    }

    @SuppressLint("ObsoleteSdkInt")
    private static void ensureHiddenApiAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !isHiddenApiAccessible()) {
            android.util.Log.w(BuildConfig.TAG, "Hidden API access not accessible, SDK_INT is " + Build.VERSION.SDK_INT);
            HiddenApiBypass.setHiddenApiExemptions("L");
        }
    }

    @SuppressLint({"BlockedPrivateApi", "PrivateApi"})
    public static boolean isHiddenApiAccessible() {
        Class<?> kContextImpl;
        try {
            kContextImpl = Class.forName("android.app.ContextImpl");
        } catch (ClassNotFoundException e) {
            return false;
        }
        Field mActivityToken = null;
        Field mToken = null;
        try {
            mActivityToken = kContextImpl.getDeclaredField("mActivityToken");
        } catch (NoSuchFieldException ignored) {
        }
        try {
            mToken = kContextImpl.getDeclaredField("mToken");
        } catch (NoSuchFieldException ignored) {
        }
        return mActivityToken != null || mToken != null;
    }

}
