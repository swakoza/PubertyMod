package com.swakoza.pubertymod.compat;

import net.minecraft.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/** Uses the actual model pose when no optional animation library is active. */
public final class PalPhysicsCompat {
    private static final boolean PLAYER_ANIMATOR_LOADED = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("playeranimator");

    private PalPhysicsCompat() {
    }

    public static AttachmentPose sample(LivingEntity entity, boolean upperHalf) {
        if (PLAYER_ANIMATOR_LOADED) {
            AttachmentPose pose = PlayerAnimatorCompatImpl.sample(entity, upperHalf);
            if (pose != null) return pose;
        }
        return TorsoPhysicsCompat.sample(entity);
    }

    public static void applyTorsoBend(LivingEntity entity, net.minecraft.client.util.math.MatrixStack matrices, float tickDelta, boolean upperHalf) {
        if (PLAYER_ANIMATOR_LOADED) PlayerAnimatorCompatImpl.applyTorsoBend(entity, matrices, tickDelta, upperHalf);
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