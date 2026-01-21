package moe.ouom.wekit.hooks.core.factory;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import moe.ouom.wekit.core.bridge.api.IHookFactoryDelegate;
import moe.ouom.wekit.core.model.BaseClickableFunctionHookItem;
import moe.ouom.wekit.core.model.BaseHookItem;
import moe.ouom.wekit.core.model.BaseSwitchFunctionHookItem;
// 如果下面这一行报错，请先 clean project
import moe.ouom.wekit.hooks.gen.HookItemEntryList;

public class HookItemFactory implements IHookFactoryDelegate {
    public static final HookItemFactory INSTANCE = new HookItemFactory();

    private static final Map<Class<? extends BaseHookItem>, BaseHookItem> ITEM_MAP = new HashMap<>();

    static {
        List<BaseHookItem> items = HookItemEntryList.getAllHookItems();
        for (BaseHookItem item : items) {
            ITEM_MAP.put(item.getClass(), item);
        }
    }

    @Override
    public BaseSwitchFunctionHookItem findHookItemByPath(String path) {
        return findHookItemByPathStatic(path);
    }

    @NonNull
    @Override
    public List<BaseSwitchFunctionHookItem> getAllSwitchFunctionItemList() {
        return getAllSwitchFunctionItemListStatic();
    }

    @NonNull
    @Override
    public List<BaseClickableFunctionHookItem> getAllClickableFunctionItemList() {
        return getAllClickableFunctionItemListStatic();
    }

    @NonNull
    @Override
    public List<BaseHookItem> getAllItemList() {
        return getAllItemListStatic();
    }


    public static BaseSwitchFunctionHookItem findHookItemByPathStatic(String path) {
        for (BaseHookItem item : ITEM_MAP.values()) {
            if (item.getPath().equals(path)) {
                return (BaseSwitchFunctionHookItem) item;
            }
        }
        return null;
    }

    public static List<BaseSwitchFunctionHookItem> getAllSwitchFunctionItemListStatic() {
        ArrayList<BaseSwitchFunctionHookItem> result = new ArrayList<>();
        for (BaseHookItem item : ITEM_MAP.values()) {
            if (item instanceof BaseSwitchFunctionHookItem) {
                result.add((BaseSwitchFunctionHookItem) item);
            }
        }
        result.sort(Comparator.comparing(BaseHookItem::getSimpleName));
        return result;
    }

    public static List<BaseClickableFunctionHookItem> getAllClickableFunctionItemListStatic() {
        ArrayList<BaseClickableFunctionHookItem> result = new ArrayList<>();
        for (BaseHookItem item : ITEM_MAP.values()) {
            if (item instanceof BaseClickableFunctionHookItem) {
                result.add((BaseClickableFunctionHookItem) item);
            }
        }
        result.sort(Comparator.comparing(BaseHookItem::getSimpleName));
        return result;
    }

    public static List<BaseHookItem> getAllItemListStatic() {
        return new ArrayList<>(ITEM_MAP.values());
    }

    public static <T extends BaseHookItem> T getItem(Class<T> clazz) {
        BaseHookItem item = ITEM_MAP.get(clazz);
        return clazz.cast(item);
    }
}