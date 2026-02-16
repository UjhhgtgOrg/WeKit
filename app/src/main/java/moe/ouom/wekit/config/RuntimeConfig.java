package moe.ouom.wekit.config;

import android.app.Activity;
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

    // 注意时效性，这里保存的登录信息是刚启动应用时的登录信息，而不是实时的登录信息
    // TODO: 需要一个机制来更新这些信息


    // login_weixin_username: wxid_apfe8lfoeoad13
    // last_login_nick_name: 帽子叔叔
    // login_user_name: 15068586147
    // last_login_uin: 1293948946
    public static String login_weixin_username;
    public static String last_login_nick_name;
    public static String login_user_name;
    public static String last_login_uin;

    // ------- //


    // wechat app info //

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

    public static void setLogin_weixin_username(String login_weixin_username) {
        RuntimeConfig.login_weixin_username = login_weixin_username;
    }

    public static void setLast_login_nick_name(String last_login_nick_name) {
        RuntimeConfig.last_login_nick_name = last_login_nick_name;
    }

    public static void setLogin_user_name(String login_user_name) {
        RuntimeConfig.login_user_name = login_user_name;
    }

    public static void setLast_login_uin(String last_login_uin) {
        RuntimeConfig.last_login_uin = last_login_uin;
    }

    public static void setWechatVersionName(String wechatVersionName) {
        RuntimeConfig.wechatVersionName = wechatVersionName;
    }

    public static void setWechatVersionCode(long wechatVersionCode) {
        RuntimeConfig.wechatVersionCode = wechatVersionCode;
    }

    public static String getLogin_weixin_username() {
        return login_weixin_username;
    }

    public static String getLast_login_nick_name() {
        return last_login_nick_name;
    }

    public static String getLogin_user_name() {
        return login_user_name;
    }

    public static String getLast_login_uin() {
        return last_login_uin;
    }
}