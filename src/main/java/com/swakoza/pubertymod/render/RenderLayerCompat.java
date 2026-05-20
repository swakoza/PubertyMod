package com.swakoza.pubertymod.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.Identifier;

public final class RenderLayerCompat {
    private RenderLayerCompat() {}

    public static RenderLayer itemEntityTranslucentCull(Identifier texture) {
        return RenderLayers.itemEntityTranslucentCull(texture);
    }

    public static RenderLayer entityTranslucent(Identifier texture) {
        return RenderLayers.entityTranslucent(texture);
    }

    public static RenderLayer outlineNoCull(Identifier texture) {
        return RenderLayers.outlineNoCull(texture);
    }

    public static RenderLayer armorCutoutNoCull(Identifier texture) {
        return RenderLayers.armorCutoutNoCull(texture);
    }

    public static RenderLayer armorEntityGlint() {
        return RenderLayers.armorEntityGlint();
    }
}
