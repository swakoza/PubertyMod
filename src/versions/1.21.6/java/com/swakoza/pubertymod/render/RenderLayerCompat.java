package com.swakoza.pubertymod.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

public final class RenderLayerCompat {
    private RenderLayerCompat() {}

    public static RenderLayer itemEntityTranslucentCull(Identifier texture) {
        return RenderLayer.getItemEntityTranslucentCull(texture);
    }

    public static RenderLayer entityTranslucent(Identifier texture) {
        return RenderLayer.getEntityTranslucent(texture);
    }

    public static RenderLayer outlineNoCull(Identifier texture) {
        return RenderLayer.getOutline(texture);
    }

    public static RenderLayer armorCutoutNoCull(Identifier texture) {
        return RenderLayer.getArmorCutoutNoCull(texture);
    }

    public static RenderLayer armorEntityGlint() {
        return RenderLayer.getArmorEntityGlint();
    }
}
