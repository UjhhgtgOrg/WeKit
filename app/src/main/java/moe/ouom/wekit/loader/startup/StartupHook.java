package moe.ouom.wekit.loader.startup;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;

import moe.ouom.wekit.BuildConfig;
import moe.ouom.wekit.host.impl.HostInfo;
import moe.ouom.wekit.loader.core.NativeCoreBridge;
import moe.ouom.wekit.loader.core.WeLauncher;
import moe.ouom.wekit.utils.common.SyncUtils;
import moe.ouom.wekit.utils.log.WeLogger;

/**
 * Startup hook for QQ They should act differently according to the process they belong to.
 * <p>
 * I don't want to cope with them anymore, enjoy it as long as possible.
 * <p>
 * DO NOT MODIFY ANY CODE HERE UNLESS NECESSARY.
 *
 * @author cinit
 */
public class StartupHook {

    private static StartupHook sInstance;
    private static boolean sSecondStageInit = false;

    /**
     * Entry point for static or dynamic initialization. NOTICE: Do NOT change the method name or signature.
     *
     * @param ctx         Application context for host
     */
    public static void execStartupInit(@NonNull Context ctx) {
        if (sSecondStageInit) {
            throw new IllegalStateException("Second stage init already executed");
        }
        HybridClassLoader.setHostClassLoader(ctx.getClassLoader());
        execPostStartupInit(ctx);
        sSecondStageInit = true;
        deleteDirIfNecessaryNoThrow(ctx);
    }

    public static void execPostStartupInit(@NonNull Context ctx) {
        // init all kotlin utils here
        HostInfo.init((Application) ctx);
        // perform full initialization for native core -- including primary and secondary native libraries
        NativeCoreBridge.initNativeCore();
        WeLogger.d("execPostStartupInit -> processName: " + SyncUtils.getProcessName());
        var launcher = new WeLauncher();
        launcher.init(ctx.getClassLoader(), ctx.getApplicationInfo(), ctx.getApplicationInfo().sourceDir, ctx);
    }

    static void deleteDirIfNecessaryNoThrow(Context ctx) {
        try {
            deleteFile(new File(ctx.getDataDir(), "app_qqprotect"));
        } catch (Throwable e) {
            log_e(e);
        }
    }

    public static StartupHook getInstance() {
        if (sInstance == null) {
            sInstance = new StartupHook();
        }
        return sInstance;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void deleteFile(File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isFile()) {
            file.delete();
        } else if (file.isDirectory()) {
            var listFiles = file.listFiles();
            if (listFiles != null) {
                for (var deleteFile : listFiles) {
                    deleteFile(deleteFile);
                }
            }
            file.delete();
        }
        file.exists();
    }

    static void log_e(Throwable th) {
        if (th == null) {
            return;
        }
        var msg = Log.getStackTraceString(th);
        Log.e(BuildConfig.TAG, msg);
    }

    public void initializeAfterAppCreate(@NonNull Context ctx) {
        execStartupInit(ctx);
        deleteDirIfNecessaryNoThrow(ctx);
    }
}
