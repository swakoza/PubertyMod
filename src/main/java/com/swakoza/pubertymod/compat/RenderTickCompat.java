package com.swakoza.pubertymod.compat;

import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Method;

public final class RenderTickCompat {
    private RenderTickCompat() {}

    public static float getTickProgress(MinecraftClient client, boolean ignoreFreeze) {
        Object counter = client.getRenderTickCounter();
        Float progress = invoke(counter, "getTickProgress", ignoreFreeze);
        if (progress != null) return progress;
        progress = invoke(counter, "getTickDelta", ignoreFreeze);
        return progress == null ? 0f : progress;
    }

    private static Float invoke(Object target, String methodName, boolean argument) {
        try {
            Method method = target.getClass().getMethod(methodName, boolean.class);
            Object value = method.invoke(target, argument);
            return value instanceof Float floatValue ? floatValue : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
