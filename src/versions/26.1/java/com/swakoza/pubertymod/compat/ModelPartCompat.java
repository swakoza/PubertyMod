package com.swakoza.pubertymod.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;

public final class ModelPartCompat {
    private ModelPartCompat() {}
    public static void applyTransform(ModelPart part, PoseStack matrices) {
        part.translateAndRotate(matrices);
    }
}
