package moe.ouom.wekit.config;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import moe.ouom.wekit.constants.Constants;

public abstract class WeConfig implements SharedPreferences, SharedPreferences.Editor {

    private static WeConfig sDefConfig;
    private static WeConfig sCache;
    private static final ConcurrentHashMap<String, WeConfig> sUinConfig =
            new ConcurrentHashMap<>(4);

    protected WeConfig() {
    }

    @NonNull
    public static synchronized WeConfig getDefaultConfig() {
        if (sDefConfig == null) {
            sDefConfig = new MmkvConfigManagerImpl("global_config");
        }
        return sDefConfig;
    }

    /**
     * Get isolated config for a specified account
     *
     * @param wxid wechat id
     * @return config for raed/write
     */
    @NonNull
    public static synchronized WeConfig forAccount(String wxid) {
        var cfg = sUinConfig.get(wxid);
        if (cfg != null) {
            return cfg;
        }
        cfg = new MmkvConfigManagerImpl("cfg_" + wxid);
        sUinConfig.put(wxid, cfg);
        // save uin to config
        if (cfg.getLongOrDefault("wx", 0) == 0) {
            cfg.putString("uin", wxid);
        }
        return cfg;
    }

    /**
     * Get isolated config for current account logged in. See {@link #forAccount(String)}
     *
     * @return if no account is logged in, {@code null} will be returned.
     */
    @Nullable
    public static WeConfig getExFriendCfg() {
        // TODO: get current account
        return null;
    }

    @NonNull
    public static synchronized WeConfig getCache() {
        if (sCache == null) {
            sCache = new MmkvConfigManagerImpl("global_cache");
        }
        return sCache;
    }

    @Nullable
    public abstract File getFile();

    @Nullable
    public Object getOrDefault(@NonNull String key, @Nullable Object def) {
        if (!containsKey(key)) {
            return def;
        }
        return getObject(key);
    }

    public static void dPutBoolean(@NonNull String key, Boolean b) {
        getDefaultConfig().edit().putBoolean(key, b).apply();
    }

    public static void dPutString(@NonNull String key, String s) {
        getDefaultConfig().edit().putString(key, s).apply();
    }

    public static void dPutInt(@NonNull String key, int i) {
        getDefaultConfig().edit().putInt(key, i).apply();
    }

    public static boolean dGetBoolean(@NonNull String key) {
        return getDefaultConfig().getBooleanOrFalse(key);
    }

    public static boolean dGetBooleanDefTrue(@NonNull String key) {
        return getDefaultConfig().getBooleanOrDefault(key, true);
    }

    public static String dGetString(@NonNull String key, String d) {
        return getDefaultConfig().getStringOrDefault(key, d);
    }

    public static int dGetInt(@NonNull String key, int d) {
        return getDefaultConfig().getIntOrDefault(key, d);
    }


    public static void cPutBoolean(@NonNull String key, Boolean b) {
        getCache().edit().putBoolean(key, b).apply();
    }

    public static void cPutString(@NonNull String key, String s) {
        getCache().edit().putString(key, s).apply();
    }

    public static void cPutInt(@NonNull String key, int i) {
        getCache().edit().putInt(key, i).apply();
    }

    public static boolean cGetBoolean(@NonNull String key) {
        return getCache().getBooleanOrFalse(key);
    }

    public static boolean cGetBoolean(@NonNull String key, boolean d) {
        return getCache().getBooleanOrDefault(key, d);
    }

    public static String cGetString(@NonNull String key, String d) {
        return getCache().getStringOrDefault(key, d);
    }

    public static int cGetInt(@NonNull String key, int d) {
        return getCache().getIntOrDefault(key, d);
    }

    public boolean getBooleanOrFalse(@NonNull String key) {
        return getBooleanOrDefault(key, false);
    }

    public boolean getBoolPrek(@NonNull String key) {
        return getBooleanOrDefault(Constants.PrekXXX + key, false);
    }

    public String getStringPrek(@NonNull String key, String def) {
        return getString(Constants.PrekXXX + key, def);
    }

    public int getIntPrek(@NonNull String key, int def) {
        return getInt(Constants.PrekXXX + key, def);
    }

    public long getLongPrek(@NonNull String key, long def) {
        return getLong(Constants.PrekXXX + key, def);
    }

    public boolean getBooleanOrDefault(@NonNull String key, boolean def) {
        return getBoolean(key, def);
    }

    public int getIntOrDefault(@NonNull String key, int def) {
        return getInt(key, def);
    }


    @Nullable
    public abstract String getString(@NonNull String key);

    @NonNull
    public String getStringOrDefault(@NonNull String key, @NonNull String defVal) {
        return getString(key, defVal);
    }

    @NonNull
    public Set<String> getStringSetOrDefault(@NonNull String key, @NonNull Set<String> defVal) {
        return getStringSet(key, defVal);
    }

    @Nullable
    public abstract Object getObject(@NonNull String key);

    public <T> T cGetObject(String key, TypeReference<T> type) {
        var data = getCache().getString(key);
        if (data == null || data.isEmpty()) {
            return null;
        }
        return JSON.parseObject(data, type);
    }

    @Nullable
    public byte[] getBytes(@NonNull String key) {
        return getBytes(key, null);
    }

    @Nullable
    public abstract byte[] getBytes(@NonNull String key, @Nullable byte[] defValue);

    @NonNull
    public abstract byte[] getBytesOrDefault(@NonNull String key, @NonNull byte[] defValue);

    @NonNull
    public abstract WeConfig putBytes(@NonNull String key, @NonNull byte[] value);

    /**
     * @return READ-ONLY all config
     * @deprecated Avoid use getAll(), MMKV only have limited support for this.
     */
    @Override
    @Deprecated
    @NonNull
    public abstract Map<String, ?> getAll();

    public abstract void save();

    public long getLongOrDefault(@Nullable String key, long i) {
        return getLong(key, i);
    }

    @NonNull
    public abstract WeConfig putObject(@NonNull String key, @NonNull Object v);

    public boolean containsKey(@NonNull String k) {
        return contains(k);
    }

    @NonNull
    @Override
    public Editor edit() {
        return this;
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(
            @NonNull OnSharedPreferenceChangeListener listener) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
            @NonNull OnSharedPreferenceChangeListener listener) {
        throw new UnsupportedOperationException("not implemented");
    }

    public abstract boolean isReadOnly();

    public abstract boolean isPersistent();
}
