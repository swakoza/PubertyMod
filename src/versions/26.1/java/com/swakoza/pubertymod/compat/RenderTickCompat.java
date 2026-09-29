package com.swakoza.pubertymod.compat;

import net.minecraft.client.Minecraft;

public final class RenderTickCompat {
    private RenderTickCompat() {}
    public static float getTickProgress(Minecraft client, boolean ignoreFreeze) {
        return client.getDeltaTracker().getGameTimeDeltaPartialTick(ignoreFreeze);
    }
}
