package com.swakoza.pubertymod.render;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public final class RenderLayerCompat {
    private RenderLayerCompat() {}

    public static RenderType itemEntityTranslucentCull(Identifier texture) {
        return RenderTypes.entityTranslucentCullItemTarget(texture);
    }

    public static RenderType entityTranslucent(Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    public static RenderType outlineNoCull(Identifier texture) {
        return RenderTypes.outline(texture);
    }

    public static RenderType armorCutoutNoCull(Identifier texture) {
        return RenderTypes.armorCutoutNoCull(texture);
    }

    public static RenderType armorEntityGlint() {
        return RenderTypes.armorEntityGlint();
    }
}
