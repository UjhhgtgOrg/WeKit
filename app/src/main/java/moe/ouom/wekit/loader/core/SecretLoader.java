package moe.ouom.wekit.loader.core;

import moe.ouom.wekit.core.bridge.HookFactoryBridge;
import moe.ouom.wekit.hooks.core.HookItemLoader;
import moe.ouom.wekit.hooks.core.factory.HookItemFactory;
import moe.ouom.wekit.util.log.WeLogger;

public class SecretLoader {
    private static final String TAG = "SecretLoader";

    public static void load(int processType) {
        WeLogger.i(TAG, "Loading hooks...");
        WeLogger.i(TAG, "Entering direct load routine...");
        var hookItemLoader = new HookItemLoader();
        hookItemLoader.loadHookItem(processType);
        var factory = HookItemFactory.INSTANCE;
        HookFactoryBridge.INSTANCE.registerDelegate(factory);
        WeLogger.i(TAG, "[Direct] HookFactoryBridge registered successfully!");
        WeLogger.i(TAG, "[Direct] load success");
    }
}