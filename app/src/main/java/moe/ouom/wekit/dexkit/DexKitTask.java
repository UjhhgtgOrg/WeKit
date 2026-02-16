package moe.ouom.wekit.dexkit;

import org.luckypray.dexkit.DexKitBridge;

@FunctionalInterface
public interface DexKitTask {
    void execute(DexKitBridge bridge, ClassLoader classLoader);
}
