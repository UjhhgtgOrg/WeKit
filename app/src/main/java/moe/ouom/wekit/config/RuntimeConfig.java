package moe.ouom.wekit.config;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import java.lang.ref.WeakReference;

import lombok.Getter;

public class RuntimeConfig {

    private RuntimeConfig() {
        throw new AssertionError("No instance for you!");
    }

    private static WeakReference<Activity> launcherUIActivityRef;
    public static ClassLoader hostClassLoader;
    public static ApplicationInfo hostApplicationInfo;

    // account info //

    private static SharedPreferences mmPrefs;

    // login_weixin_username: wxid_apfe8lfoeoad13
    // last_login_nick_name: 帽子叔叔
    // login_user_name: 15068586147
    // last_login_uin: 1293948946

    // ------- //

    // WeChat app info //

    @Getter
    private static String wechatVersionName; // "8.0.65"
    @Getter
    private static long wechatVersionCode;    // 2960

    // ------- //

    public static void setLauncherUIActivity(Activity activity) {
        if (activity == null) {
            launcherUIActivityRef = null;
        } else {
            launcherUIActivityRef = new WeakReference<>(activity);
        }
    }

    public static Activity getLauncherUIActivity() {
        if (launcherUIActivityRef == null) {
            return null;
        }
        var activity = launcherUIActivityRef.get();

        if (activity != null && (activity.isFinishing() || activity.isDestroyed())) {
            launcherUIActivityRef = null;
            return null;
        }

        return activity;
    }

    public static ClassLoader getHostClassLoader() {
        return hostClassLoader;
    }

    public static void setHostClassLoader(ClassLoader classLoader) {
        assert classLoader != null;
        hostClassLoader = classLoader;
    }

    public static ApplicationInfo getHostApplicationInfo() {
        return hostApplicationInfo;
    }

    public static void setHostApplicationInfo(ApplicationInfo appInfo) {
        assert appInfo != null;
        hostApplicationInfo = appInfo;
    }

    public static void setWechatVersionName(String wechatVersionName) {
        RuntimeConfig.wechatVersionName = wechatVersionName;
    }

    public static void setWechatVersionCode(long wechatVersionCode) {
        RuntimeConfig.wechatVersionCode = wechatVersionCode;
    }

    public static void setmmPrefs(SharedPreferences sharedPreferences) {
        RuntimeConfig.mmPrefs = sharedPreferences;
    }

    public static String getLogin_weixin_username() {
        return mmPrefs.getString("login_weixin_username", "");
    }

    public static String getLast_login_nick_name() {
        return mmPrefs.getString("last_login_nick_name", "");
    }

    public static String getLogin_user_name() {
        return mmPrefs.getString("login_user_name", "");
    }

    public static String getLast_login_uin() {
        return mmPrefs.getString("last_login_uin", "0");
    }
}