package com.swakoza.pubertymod.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Method;
import net.minecraft.client.model.geom.ModelPart;

public final class ModelPartCompat {
    private static final Method TRANSFORM = find("applyTransform", "rotate");

    private ModelPartCompat() {}

    public static void applyTransform(ModelPart part, PoseStack matrices) {
        if (part == null || TRANSFORM == null) return;
        try {
            TRANSFORM.invoke(part, matrices);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Method find(String... names) {
        for (String name : names) {
            try {
                return ModelPart.class.getMethod(name, PoseStack.class);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
