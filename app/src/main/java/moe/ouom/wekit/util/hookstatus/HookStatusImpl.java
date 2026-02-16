package moe.ouom.wekit.util.hookstatus;

/**
 * Hook status detection, NO KOTLIN, NO ANDROIDX!
 */
public class HookStatusImpl {

    private HookStatusImpl() {
    }

    /**
     * To be changed by the hook
     */
    static volatile boolean sZygoteHookMode = false;

    /**
     * To be changed by the hook
     */
    static volatile String sZygoteHookProvider = null;

    /**
     * To be changed by the hook
     */
    static volatile boolean sIsLsposedDexObfsEnabled = false;

}
