package moe.ouom.wekit.loader.core;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import com.tencent.mmkv.MMKV;
import java.io.File;

import moe.ouom.wekit.host.HostInfo;
import moe.ouom.wekit.util.log.WeLogger;
import moe.ouom.wekit.util.script.JsExecutor;


public class NativeCoreBridge {
    static {
        System.loadLibrary("dexkit");
        System.loadLibrary("wekit");
    }


    private NativeCoreBridge() {
        throw new AssertionError("No instances for you!");
    }


    private static boolean sPrimaryNativeLibraryInitialized = false;

    public static void initNativeCore() {
        Context context = HostInfo.getApplication();
        // init mmkv
        initializeMmkvForPrimaryNativeLibrary(context);
        // init JsExecutor
        JsExecutor.getInstance().initialize(context);
    }

    /**
     * 检查本地核心库是否已初始化
     * @return true 如果已成功初始化，false 如果未初始化
     */
    public static boolean isNativeCoreInitialized() {
        return sPrimaryNativeLibraryInitialized;
    }

    /**
     * 设置本地核心库初始化状态
     * @param initialized true表示已初始化，false表示未初始化
     */
    public static void setNativeCoreInitialized(boolean initialized) {
        sPrimaryNativeLibraryInitialized = initialized;
        if (initialized) {
            WeLogger.i("Native core initialization status set to: initialized");
        } else {
            WeLogger.w("Native core initialization status set to: not initialized");
        }
    }

    /**
     * 加载本地库并初始化MMKV
     *
     * @param ctx 应用上下文
     */
    @SuppressLint("SdCardPath")
    public static void initializeMmkvForPrimaryNativeLibrary(@NonNull Context ctx) {
        if (isNativeCoreInitialized()) {
            return;
        }

        // 获取微信的files目录
        File appFilesDir = ctx.getFilesDir();
        String packageName = ctx.getPackageName();

        WeLogger.i("Initializing NativeCoreBridge for package: " + packageName);

        File mmkvDir = new File(appFilesDir, "mmkv");
        // 不存在就创建mmkv目录
        if (!mmkvDir.exists()) {
            boolean created = mmkvDir.mkdirs();
            WeLogger.i("Created mmkv directory: " + created);
        }

        // 初始化 MMKV
        String mmkvRootPath = mmkvDir.getAbsolutePath();
        String initializedPath = MMKV.initialize(ctx, mmkvRootPath);

        WeLogger.i("MMKV initialized at: " + initializedPath);

        // 创建必要的 MMKV 实例
        MMKV.mmkvWithID("global_config", MMKV.MULTI_PROCESS_MODE);
        MMKV.mmkvWithID("global_cache", MMKV.MULTI_PROCESS_MODE);

        setNativeCoreInitialized(true);
        WeLogger.i("NativeCoreBridge initialization complete");
    }
}
