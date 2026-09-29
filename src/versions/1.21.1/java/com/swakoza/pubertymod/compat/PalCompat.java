package com.swakoza.pubertymod.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;

/** Resolves optional PAL and playerAnimator classes only after their mods are present. */
public final class PalCompat {
    private static final boolean PAL_LOADED = FabricLoader.getInstance().isModLoaded("player_animation_library");
    private static final boolean PLAYER_ANIMATOR_LOADED = FabricLoader.getInstance().isModLoaded("playeranimator");

    private PalCompat() {
    }

    public static void applyTorsoBend(LivingEntity entity, MatrixStack matrices, float tickDelta, boolean upperHalf) {
        if (PLAYER_ANIMATOR_LOADED && PlayerAnimatorCompatImpl.applyTorsoBend(entity, matrices, tickDelta, upperHalf)) return;
        if (PAL_LOADED && upperHalf) {
            PalCompatImpl.applyTorsoBend(entity, matrices);
        }
    }
}
