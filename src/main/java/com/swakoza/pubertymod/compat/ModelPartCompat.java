package com.swakoza.pubertymod.compat;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;

import java.lang.reflect.Method;

public final class ModelPartCompat {
    private static final Method TRANSFORM = find("applyTransform", "rotate");

    private ModelPartCompat() {}

    public static void applyTransform(ModelPart part, MatrixStack matrices) {
        if (part == null || TRANSFORM == null) return;
        try {
            TRANSFORM.invoke(part, matrices);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Method find(String... names) {
        for (String name : names) {
            try {
                return ModelPart.class.getMethod(name, MatrixStack.class);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
