package moe.ouom.wekit.loader.startup;

import android.app.Application;

import androidx.annotation.NonNull;

import java.util.Objects;


public class StartupInfo {

    private StartupInfo() {
        throw new AssertionError("No instance for you!");
    }

    private static String modulePath;

    private static Boolean isInitMethod;
    public static Application hostApp;

    private static Boolean inHostProcess = null;


    public static String getModulePath() {
        if (modulePath == null) {
            throw new IllegalStateException("Module path is null");
        }
        return modulePath;
    }


    public static Application getHostApp() {
        return hostApp;
    }

    public static void setHostApp(Application hostApp) {
        Objects.requireNonNull(hostApp);
        StartupInfo.hostApp = hostApp;
    }


    public static void setModulePath(@NonNull String modulePath) {
        Objects.requireNonNull(modulePath);
        StartupInfo.modulePath = modulePath;
    }


    public static Boolean isInitMethod() {
        if (isInitMethod == null) {
            return false;
        }
        return isInitMethod;
    }


    public static void setIsInitMethod(Boolean b) {
        Objects.requireNonNull(b);
        StartupInfo.isInitMethod = b;
    }

    public static boolean isInHostProcess() {
        if (inHostProcess == null) {
            throw new IllegalStateException("Host process status is not initialized");
        }
        return inHostProcess;
    }

    public static void setInHostProcess(boolean inHostProcess) {
        if (StartupInfo.inHostProcess != null) {
            throw new IllegalStateException("Host process status is already initialized");
        }
        StartupInfo.inHostProcess = inHostProcess;
    }
}
