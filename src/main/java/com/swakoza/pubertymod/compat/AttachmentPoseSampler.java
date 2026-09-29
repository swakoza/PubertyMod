package com.swakoza.pubertymod.compat;

import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/** Shared model-space sampling for the two optional player animation libraries. */
final class AttachmentPoseSampler {
    private static final float CHEST_Y = 2.5F / 16.0F;
    private static final float CHEST_FRONT_Z = -2.0F / 16.0F;
    private static final float PLAYER_MODEL_SCALE = 0.9375F;
    private static final float MODEL_ORIGIN_Y = -1.501F;

    private AttachmentPoseSampler() {
    }

    static void applyVanillaPlayerTransform(MatrixStack matrices) {
        matrices.scale(-1.0F, -1.0F, 1.0F);
        matrices.scale(PLAYER_MODEL_SCALE, PLAYER_MODEL_SCALE, PLAYER_MODEL_SCALE);
        matrices.translate(0.0F, MODEL_ORIGIN_Y, 0.0F);
    }

    static PalPhysicsCompat.AttachmentPose sample(MatrixStack matrices) {
        Vector3f chest = matrices.peek().getPositionMatrix()
                .transformPosition(0.0F, CHEST_Y, CHEST_FRONT_Z, new Vector3f());
        Matrix3f chestOrientation = new Matrix3f(matrices.peek().getPositionMatrix());
        if (Math.abs(chestOrientation.determinant()) < 1.0E-6F) return null;
        chestOrientation.invert();
        Vector3f gravity = chestOrientation.transform(new Vector3f(0.0F, -1.0F, 0.0F)).normalize();
        Matrix3f neutralToLocal = chestOrientation.scale(
                -PLAYER_MODEL_SCALE, -PLAYER_MODEL_SCALE, PLAYER_MODEL_SCALE);

        float x = -chest.x / PLAYER_MODEL_SCALE;
        float y = chest.y / PLAYER_MODEL_SCALE + MODEL_ORIGIN_Y + CHEST_Y;
        float z = chest.z / PLAYER_MODEL_SCALE - CHEST_FRONT_Z;
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)
                || !Float.isFinite(gravity.x) || !Float.isFinite(gravity.y) || !Float.isFinite(gravity.z)) return null;
        return new PalPhysicsCompat.AttachmentPose(x, y, z, gravity.x, gravity.y, gravity.z, neutralToLocal);
    }
}
