package com.swakoza.pubertymod.compat;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Projects gravity and animation acceleration into the current torso frame. */
public final class TorsoPoseMath {
    private static final float CHEST_Y = 2.5F / 16;
    private static final float CHEST_Z = -2.0F / 16;
    private static final float PLAYER_SCALE = 0.9375F;
    private static final float MODEL_ORIGIN_Y = -1.501F;
    private TorsoPoseMath() {}

    public static PalPhysicsCompat.AttachmentPose sample(Matrix4f modelTransform) {
        return sampleWorld(new Matrix4f().scaling(-PLAYER_SCALE, -PLAYER_SCALE, PLAYER_SCALE)
                .translate(0, MODEL_ORIGIN_Y, 0).mul(modelTransform));
    }

    public static PalPhysicsCompat.AttachmentPose sampleWorld(Matrix4f transform) {
        Matrix3f inverse = new Matrix3f(transform);
        if (Math.abs(inverse.determinant()) < 1.0E-6F) return null;
        inverse.invert();
        Vector3f gravity = inverse.transform(new Vector3f(0, -1, 0)).normalize();
        Vector3f chest = transform.transformPosition(0, CHEST_Y, CHEST_Z, new Vector3f());
        if (!chest.isFinite() || !gravity.isFinite()) return null;
        Matrix3f neutralToLocal = inverse.scale(-PLAYER_SCALE, -PLAYER_SCALE, PLAYER_SCALE);
        return new PalPhysicsCompat.AttachmentPose(-chest.x / PLAYER_SCALE,
                chest.y / PLAYER_SCALE + MODEL_ORIGIN_Y + CHEST_Y,
                chest.z / PLAYER_SCALE - CHEST_Z, gravity.x, gravity.y, gravity.z, neutralToLocal);
    }
}
