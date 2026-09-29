package com.swakoza.pubertymod.compat;

import com.zigythebird.playeranim.accessors.IAnimatedAvatar;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

final class PalPhysicsCompatImpl {
    private static final PlayerAnimBone BODY = new PlayerAnimBone("body");
    private static final PlayerAnimBone TORSO = new PlayerAnimBone("torso");
    private static final boolean BENDABLE_LOADED = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("bendable_cuboids");
    private PalPhysicsCompatImpl() {}

    private static AvatarAnimManager activeManager(LivingEntity entity) {
        if (!(entity instanceof IAnimatedAvatar animated)) return null;
        AvatarAnimManager manager = animated.playerAnimLib$getAnimManager();
        return manager != null && manager.isActive() ? manager : null;
    }

    static PalPhysicsCompat.AttachmentPose sample(LivingEntity entity, boolean upperHalf) {
        AvatarAnimManager manager = activeManager(entity);
        if (manager == null) return null;
        BODY.setToInitialPose();
        TORSO.setToInitialPose();
        PlayerAnimBone body = manager.get3DTransform(BODY);
        PlayerAnimBone torso = manager.get3DTransform(TORSO);
        // Follow PAL's world-space body pivot, then vanilla's model flip,
        // player scale and origin before applying the torso pose.
        Matrix4f transform = new Matrix4f().translation(-body.getPosX() / 16,
                body.getPosY() / 16 + 0.75F, body.getPosZ() / 16)
                .rotateZYX(body.getRotZ(), -body.getRotY(), -body.getRotX())
                .scale(body.getScaleX(), body.getScaleY(), body.getScaleZ())
                .translate(0, -0.75F, 0)
                .scale(-0.9375F, -0.9375F, 0.9375F).translate(0, -1.501F, 0)
                .translate(torso.getPosX() / 16, -torso.getPosY() / 16, torso.getPosZ() / 16)
                .rotateZYX(torso.getRotZ(), torso.getRotY(), torso.getRotX())
                .scale(torso.getScaleX(), torso.getScaleY(), torso.getScaleZ());
        if (BENDABLE_LOADED && upperHalf) transform.translate(0, 0.375F, 0).rotateX(torso.getBend()).translate(0, -0.375F, 0);
        return TorsoPoseMath.sampleWorld(transform);
    }

    static void applyTorsoBend(LivingEntity entity, MatrixStack matrices) {
        AvatarAnimManager manager = activeManager(entity);
        if (manager == null) return;
        TORSO.setToInitialPose();
        float bend = manager.get3DTransform(TORSO).getBend();
        if (!BENDABLE_LOADED || bend == 0) return;
        matrices.translate(0, 0.375F, 0);
        matrices.multiply(new Quaternionf().rotationX(bend));
        matrices.translate(0, -0.375F, 0);
    }
}
