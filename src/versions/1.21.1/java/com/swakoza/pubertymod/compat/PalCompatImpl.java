package com.swakoza.pubertymod.compat;

import com.zigythebird.playeranim.animation.PlayerAnimManager;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;

final class PalCompatImpl {
    // PlayerAnimationController moves top bones around world height 18 while the
    // torso bone starts at height 24. In body ModelPart coordinates (Y down),
    // that makes the bend pivot 6 pixels below the body's top pivot.
    private static final float TORSO_BEND_PIVOT_Y = (24.0F - 18.0F) / 16.0F;
    // Feature renderers run on the client render thread. PAL mutates the supplied
    // bone, so reset it before each evaluation instead of allocating per breast.
    private static final PlayerAnimBone TORSO_BONE = new PlayerAnimBone("torso");

    private PalCompatImpl() {
    }

    static void applyTorsoBend(LivingEntity entity, MatrixStack matrices) {
        if (!(entity instanceof AbstractClientPlayerEntity player)) return;

        PlayerAnimManager manager = PlayerAnimationAccess.getPlayerAnimManager(player);
        if (manager == null || !manager.isActive()) return;

        // PAL has already copied torso position, rotation and scale into the
        // vanilla body ModelPart. Bend is the only missing transformation.
        TORSO_BONE.setToInitialPose();
        float bend = manager.get3DTransform(TORSO_BONE).getBend();
        if (bend == 0.0F) return;

        // Matches PlayerAnimationController's top-bone displacement:
        // a point above the waist moves down by d*(1-cos(bend)) and along Z
        // by -d*sin(bend). RotationAxis.POSITIVE_X gives the same sign.
        applyBend(bend, matrices);
    }

    static void applyBend(float bend, MatrixStack matrices) {
        matrices.translate(0.0F, TORSO_BEND_PIVOT_Y, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotation(bend));
        matrices.translate(0.0F, -TORSO_BEND_PIVOT_Y, 0.0F);
    }
}
