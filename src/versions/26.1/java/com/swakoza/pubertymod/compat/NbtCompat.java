package com.swakoza.pubertymod.compat;

import java.lang.reflect.Method;
import net.minecraft.nbt.CompoundTag;

public final class NbtCompat {
    private static final Method GET_COMPOUND = find("getCompound", String.class);
    private static final Method GET_FLOAT = find("getFloat", String.class);
    private static final Method GET_BOOLEAN = find("getBoolean", String.class);

    private NbtCompat() {}

    public static CompoundTag getCompound(CompoundTag nbt, String key) {
        Object value = invoke(nbt, GET_COMPOUND, key);
        if (value instanceof CompoundTag compound) {
            return compound;
        }
        if (value instanceof java.util.Optional<?> optional && optional.orElse(null) instanceof CompoundTag compound) {
            return compound;
        }
        return null;
    }

    public static float getFloat(CompoundTag nbt, String key, float fallback) {
        Object value = invoke(nbt, GET_FLOAT, key);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        if (value instanceof java.util.Optional<?> optional && optional.orElse(null) instanceof Number number) {
            return number.floatValue();
        }
        return fallback;
    }

    public static boolean getBoolean(CompoundTag nbt, String key, boolean fallback) {
        Object value = invoke(nbt, GET_BOOLEAN, key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof java.util.Optional<?> optional && optional.orElse(null) instanceof Boolean bool) {
            return bool;
        }
        return fallback;
    }

    private static Method find(String name, Class<?>... params) {
        try {
            return CompoundTag.class.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Object invoke(CompoundTag nbt, Method method, String key) {
        if (nbt == null || method == null || !nbt.contains(key)) {
            return null;
        }
        try {
            return method.invoke(nbt, key);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
