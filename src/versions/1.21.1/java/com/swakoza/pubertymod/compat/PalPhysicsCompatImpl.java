package com.swakoza.pubertymod.compat;

import com.zigythebird.playeranim.animation.PlayerAnimManager;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.joml.Quaternionf;

final class PalPhysicsCompatImpl {
    // PlayerRendererMixin applies the PAL body bone around this height.
    private static final float BODY_ROTATION_PIVOT_Y = 0.75F;
    private static final PlayerAnimBone BODY_BONE = new PlayerAnimBone("body");
    private static final PlayerAnimBone TORSO_BONE = new PlayerAnimBone("torso");

    private PalPhysicsCompatImpl() {
    }

    static PalPhysicsCompat.AttachmentPose sample(LivingEntity entity, boolean upperHalf) {
        if (!(entity instanceof AbstractClientPlayerEntity player)) return null;

        PlayerAnimManager manager = PlayerAnimationAccess.getPlayerAnimManager(player);
        if (manager == null || !manager.isActive()) return null;

        BODY_BONE.setToInitialPose();
        TORSO_BONE.setToInitialPose();
        PlayerAnimBone body = manager.get3DTransform(BODY_BONE);
        PlayerAnimBone torso = manager.get3DTransform(TORSO_BONE);

        MatrixStack matrices = new MatrixStack();
        // Same order as PAL's PlayerRendererMixin, followed by vanilla's
        // model flip, player scale and model origin translation.
        matrices.scale(body.getScaleX(), body.getScaleY(), body.getScaleZ());
        matrices.translate(-body.getPosX() / 16.0F,
                body.getPosY() / 16.0F + BODY_ROTATION_PIVOT_Y,
                body.getPosZ() / 16.0F);
        matrices.multiply(new Quaternionf().rotationZYX(
                body.getRotZ(), -body.getRotY(), -body.getRotX()));
        matrices.translate(0.0F, -BODY_ROTATION_PIVOT_Y, 0.0F);
        AttachmentPoseSampler.applyVanillaPlayerTransform(matrices);

        matrices.translate(torso.getPosX() / 16.0F, -torso.getPosY() / 16.0F, torso.getPosZ() / 16.0F);
        matrices.multiply(new Quaternionf().rotationZYX(torso.getRotZ(), torso.getRotY(), torso.getRotX()));
        matrices.scale(torso.getScaleX(), torso.getScaleY(), torso.getScaleZ());
        if (upperHalf) PalCompatImpl.applyBend(torso.getBend(), matrices);

        return AttachmentPoseSampler.sample(matrices);
    }
}
