package com.swakoza.pubertymod.compat;

import net.minecraft.client.MinecraftClient;

public final class RenderTickCompat {
    private RenderTickCompat() {}
    public static float getTickProgress(MinecraftClient client, boolean ignoreFreeze) {
        return client.getRenderTickCounter().getTickDelta(ignoreFreeze);
    }
}
