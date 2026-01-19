package moe.ouom.wekit.util.log;

import android.util.Log;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XposedBridge;
import moe.ouom.wekit.BuildConfig;

public class Logger {

    private Logger() {}

    private static final String TAG = BuildConfig.TAG;

    public static void e(@NonNull String msg) {
        android.util.Log.e(TAG, msg);
        try {
            LogUtils.addError("common", msg);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }
    }

    public static void e(String tag, @NonNull String msg) {
        android.util.Log.e(TAG, tag + ": "+ msg);
        try {
            LogUtils.addError("common", tag + ": "+ msg);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }
    }

    public static void w(@NonNull String msg) {
        android.util.Log.w(TAG, msg);
        try {
            LogUtils.addRunLog("common", msg);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }
    }
    public static void w(String tag, @NonNull String msg) {
        android.util.Log.w(TAG, tag + ": "+ msg);
        try {
            LogUtils.addRunLog("common", msg);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }
    }

    public static void i(@NonNull String msg) {
        android.util.Log.i(TAG, msg);
        try {
            LogUtils.addRunLog("common", msg);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }
    }
    public static void i(String tag, @NonNull String msg) {
        android.util.Log.i(TAG, tag + ": "+ msg);
        try {
            LogUtils.addRunLog("common", msg);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }
    }


    public static void d(@NonNull String msg) {
        android.util.Log.d(TAG, msg);
        try {
            LogUtils.addRunLog("common", msg);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }

    }
    public static void d(String tag, @NonNull String msg) {
        android.util.Log.d(TAG, tag + ": "+ msg);
        try {
            LogUtils.addRunLog("common", msg);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }
    }

    public static void v(@NonNull String msg) {
        android.util.Log.v(TAG, msg);
        try {
            LogUtils.addRunLog("common", msg);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }
    }
    public static void v(String tag, @NonNull String msg) {
        android.util.Log.v(TAG, tag + ": "+ msg);
        try {
            LogUtils.addRunLog("common", msg);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }
    }

    public static void e(@NonNull Throwable e) {
        android.util.Log.e(TAG, e.toString(), e);
        try {
            LogUtils.addError("common", e);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }
    }

    public static void w(@NonNull Throwable e) {
        android.util.Log.w(TAG, e.toString(), e);
        try {
            LogUtils.addError("common", e);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }
    }

    public static void i(@NonNull Throwable e) {
        android.util.Log.i(TAG, e.toString(), e);
        try {
            LogUtils.addError("common", e);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }
    }

    public static void i(@NonNull Throwable e, boolean output) {
        android.util.Log.i(TAG, e.toString(), e);
        try {
            LogUtils.addError("common", e);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }
        if (output){
            XposedBridge.log(e);
        }
    }

    public static void d(@NonNull Throwable e) {
        android.util.Log.d(TAG, e.toString(), e);
        try {
            LogUtils.addError("common", e);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }
    }

    public static void e(@NonNull String msg, @NonNull Throwable e) {
        android.util.Log.e(TAG, msg, e);
        try {
            LogUtils.addError("common", e);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }
    }

    public static void w(@NonNull String msg, @NonNull Throwable e) {
        android.util.Log.w(TAG, msg, e);
        try {
            LogUtils.addError("common", e);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }
    }

    public static void i(@NonNull String msg, @NonNull Throwable e) {
        android.util.Log.i(TAG, msg, e);
        try {
            LogUtils.addError("common", e);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }
    }

    public static void d(@NonNull String msg, @NonNull Throwable e) {
        android.util.Log.d(TAG, msg, e);
        try {
            LogUtils.addError("common", e);
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
            Log.e(BuildConfig.TAG, "common", error);
        }
    }

    @NonNull
    public static String getStackTraceString(@NonNull Throwable th) {
        return android.util.Log.getStackTraceString(th);
    }
}
