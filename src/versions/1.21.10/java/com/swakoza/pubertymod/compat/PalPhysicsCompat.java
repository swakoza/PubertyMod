/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public final class PalPhysicsCompat {
    private static final boolean PAL_LOADED = compatiblePalLoaded();

    private static boolean compatiblePalLoaded() {
        var container = FabricLoader.getInstance().getModContainer("player_animation_library");
        if (container.isEmpty()) return false;
        var version = container.get().getMetadata().getVersion();
        if (version instanceof net.fabricmc.loader.api.SemanticVersion semantic
                && semantic.getVersionComponent(0) == 1
                && semantic.getVersionComponent(1) == 1
                && semantic.getVersionComponent(2) >= 3) return true;
        com.swakoza.pubertymod.main.SwakozaPubertyMod.LOGGER.warn(
                "PAL {} does not expose the supported 1.1.3 API; using the rendered torso pose",
                version.getFriendlyString());
        return false;
    }
    private PalPhysicsCompat() {}

    public static AttachmentPose sample(LivingEntity entity, boolean upperHalf) {
        if (PAL_LOADED) {
            AttachmentPose pose = PalPhysicsCompatImpl.sample(entity, upperHalf);
            if (pose != null) return pose;
        }
        return TorsoPhysicsCompat.sample(entity);
    }

    public static void applyTorsoBend(LivingEntity entity, MatrixStack matrices, float tickDelta, boolean upperHalf) {
        if (PAL_LOADED && upperHalf) PalPhysicsCompatImpl.applyTorsoBend(entity, matrices);
    }

    public record AttachmentPose(float x, float y, float z, float gravityX, float gravityY, float gravityZ,
                                 Matrix3f neutralToLocal, Matrix3f localToNeutral) {
        public AttachmentPose(float x, float y, float z, float gravityX, float gravityY, float gravityZ,
                              Matrix3f neutralToLocal) {
            this(x, y, z, gravityX, gravityY, gravityZ, neutralToLocal,
                    new Matrix3f(neutralToLocal).invert());
        }

        /** A point on each breast in the moving torso frame, including bone rotation. */
        public Vector3f breastProbe(boolean left) {
            Vector3f point = new Vector3f(left ? -0.125F : 0.125F, 0, -0.20F);
            localToNeutral.transform(point);
            return point.add(x, y, z);
        }
    }
}
