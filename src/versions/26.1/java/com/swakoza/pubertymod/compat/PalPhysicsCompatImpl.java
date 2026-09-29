package com.swakoza.pubertymod.compat;

import com.zigythebird.playeranim.accessors.IAnimatedAvatar;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

// PAL retains the deprecated bend channel for legacy EmoteCraft animations.
// Pivot-based modern animations already transform the vanilla torso model.
@SuppressWarnings("removal")
final class PalPhysicsCompatImpl {
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
        PlayerAnimBone body = manager.get3DTransform("body");
        PlayerAnimBone torso = manager.get3DTransform("torso");
        // Follow PAL's world-space body pivot, then vanilla's model flip,
        // player scale and origin before applying the torso pose.
        Matrix4f transform = new Matrix4f().translation(-body.position.x / 16,
                body.position.y / 16 + 0.75F, body.position.z / 16)
                .rotateZYX(body.rotation.z, -body.rotation.y, -body.rotation.x)
                .scale(body.scale.x, body.scale.y, body.scale.z)
                .translate(0, -0.75F, 0)
                .scale(-0.9375F, -0.9375F, 0.9375F).translate(0, -1.501F, 0)
                .translate(torso.position.x / 16, -torso.position.y / 16, torso.position.z / 16)
                .rotateZYX(torso.rotation.z, torso.rotation.y, torso.rotation.x)
                .scale(torso.scale.x, torso.scale.y, torso.scale.z);
        if (BENDABLE_LOADED && upperHalf) transform.translate(0, 0.375F, 0).rotateX(torso.bend).translate(0, -0.375F, 0);
        return TorsoPoseMath.sampleWorld(transform);
    }

    static void applyTorsoBend(LivingEntity entity, PoseStack matrices) {
        AvatarAnimManager manager = activeManager(entity);
        if (manager == null) return;
        float bend = manager.get3DTransform("torso").bend;
        if (!BENDABLE_LOADED || bend == 0) return;
        matrices.translate(0, 0.375F, 0);
        matrices.mulPose(new Quaternionf().rotationX(bend));
        matrices.translate(0, -0.375F, 0);
    }
}
