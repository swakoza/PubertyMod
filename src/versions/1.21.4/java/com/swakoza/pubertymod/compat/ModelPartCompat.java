package com.swakoza.pubertymod.compat;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;

public final class ModelPartCompat {
    private ModelPartCompat() {}
    public static void applyTransform(ModelPart part, MatrixStack matrices) {
        part.rotate(matrices);
    }
}
