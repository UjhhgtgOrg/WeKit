package moe.ouom.wekit.loader.startup;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

import moe.ouom.wekit.BuildConfig;

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

    private StartupHook() {
    }

    /**
     * Entry point for static or dynamic initialization. NOTICE: Do NOT change the method name or signature.
     *
     * @param ctx         Application context for host
     * @param step        Step instance
     * @param lpwReserved null, not used
     * @param bReserved   false, not used
     */
    public static void execStartupInit(@NonNull Context ctx, @Nullable Object step, String lpwReserved, boolean bReserved) {
        if (sSecondStageInit) {
            throw new IllegalStateException("Second stage init already executed");
        }
        HybridClassLoader.setHostClassLoader(ctx.getClassLoader());
        StartupRoutine.execPostStartupInit(ctx, step, lpwReserved, bReserved);
        sSecondStageInit = true;
        deleteDirIfNecessaryNoThrow(ctx);

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
        try {
            StartupInfo.getLoaderService().log(th);
        } catch (NoClassDefFoundError | NullPointerException e) {
            Log.e("Xposed", msg);
            Log.e("EdXposed-Bridge", msg);
        }
    }

    public void initializeAfterAppCreate(@NonNull Context ctx) {
        execStartupInit(ctx, null, null, false);
        deleteDirIfNecessaryNoThrow(ctx);
    }

}
