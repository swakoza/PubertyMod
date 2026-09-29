package com.swakoza.pubertymod.compat;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Optional PlayerAnimator 2.x integration. */
final class PlayerAnimatorCompatImpl {
    private static final boolean BENDY_LOADED = FabricLoader.getInstance().isModLoaded("bendy-lib");
    private static final float BODY_BEND_PIVOT_Y = 0.375F;
    private static final float BODY_ROTATION_PIVOT_Y = 0.7F;

    private PlayerAnimatorCompatImpl() {
    }

    static boolean applyTorsoBend(LivingEntity entity, MatrixStack matrices, float tickDelta, boolean upperHalf) {
        AnimationStack stack = activeStack(entity);
        if (stack == null) return false;
        if (BENDY_LOADED && upperHalf) {
            // playerAnimator already puts the body bend around all upper-body
            // feature layers. Only the torso bend is missing after body.rotate.
            applyBend(matrices, transform(stack, "torso", TransformType.BEND, tickDelta, Vec3f.ZERO));
        }
        return true;
    }

    static PalPhysicsCompat.AttachmentPose sample(LivingEntity entity, boolean upperHalf) {
        AnimationStack stack = activeStack(entity);
        if (stack == null) return null;
        stack.setupAnim(0.0F);

        MatrixStack matrices = new MatrixStack();
        Vec3f bodyScale = transform(stack, "body", TransformType.SCALE, 0.0F, new Vec3f(1, 1, 1));
        Vec3f bodyPosition = transform(stack, "body", TransformType.POSITION, 0.0F, Vec3f.ZERO);
        Vec3f bodyRotation = transform(stack, "body", TransformType.ROTATION, 0.0F, Vec3f.ZERO);
        // Match playerAnimator's PlayerRendererMixin before vanilla model setup.
        matrices.scale(bodyScale.getX(), bodyScale.getY(), bodyScale.getZ());
        matrices.translate(bodyPosition.getX(), bodyPosition.getY() + BODY_ROTATION_PIVOT_Y, bodyPosition.getZ());
        matrices.multiply(RotationAxis.POSITIVE_Z.rotation(bodyRotation.getZ()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(bodyRotation.getY()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotation(bodyRotation.getX()));
        matrices.translate(0.0F, -BODY_ROTATION_PIVOT_Y, 0.0F);
        AttachmentPoseSampler.applyVanillaPlayerTransform(matrices);

        if (BENDY_LOADED) {
            // Upper-body feature layers already inherit this body bend.
            applyBend(matrices, transform(stack, "body", TransformType.BEND, 0.0F, Vec3f.ZERO));
        }
        Vec3f torsoPosition = transform(stack, "torso", TransformType.POSITION, 0.0F, Vec3f.ZERO);
        Vec3f torsoRotation = transform(stack, "torso", TransformType.ROTATION, 0.0F, Vec3f.ZERO);
        Vec3f torsoScale = transform(stack, "torso", TransformType.SCALE, 0.0F, new Vec3f(1, 1, 1));
        matrices.translate(torsoPosition.getX() / 16.0F, torsoPosition.getY() / 16.0F, torsoPosition.getZ() / 16.0F);
        matrices.multiply(new Quaternionf().rotationZYX(
                torsoRotation.getZ(), torsoRotation.getY(), torsoRotation.getX()));
        matrices.scale(torsoScale.getX(), torsoScale.getY(), torsoScale.getZ());
        if (BENDY_LOADED && upperHalf) {
            applyBend(matrices, transform(stack, "torso", TransformType.BEND, 0.0F, Vec3f.ZERO));
        }
        return AttachmentPoseSampler.sample(matrices);
    }

    private static AnimationStack activeStack(LivingEntity entity) {
        if (!(entity instanceof AbstractClientPlayerEntity player)) return null;
        AnimationStack stack = PlayerAnimationAccess.getPlayerAnimLayer(player);
        return stack.isActive() ? stack : null;
    }

    private static Vec3f transform(AnimationStack stack, String part, TransformType type, float tickDelta, Vec3f initial) {
        return stack.get3DTransform(part, type, tickDelta, initial);
    }

    private static void applyBend(MatrixStack matrices, Vec3f bend) {
        if (Math.abs(bend.getY()) < 1.0E-4F) return;
        // playerAnimator IBendHelper.rotateMatrixStack: waist pivot and angled
        // bend axis, applied after the relevant ModelPart transform.
        float axis = -bend.getX();
        matrices.translate(0.0F, BODY_BEND_PIVOT_Y, 0.0F);
        matrices.multiply(new Quaternionf().rotateAxis(bend.getY(),
                new Vector3f((float) Math.cos(axis), 0.0F, (float) Math.sin(axis))));
        matrices.translate(0.0F, -BODY_BEND_PIVOT_Y, 0.0F);
    }
}
