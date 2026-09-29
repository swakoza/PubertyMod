package com.swakoza.pubertymod.render;

import org.joml.Vector3f;

/** Affine stretch: the open back face is attached, the front carries the spring displacement. */
public final class BreastDeformation {
    private BreastDeformation() {}

    public static float weight(float z, float backZ, float depth) {
        return (backZ - z) / depth;
    }

    public static void transformNormal(Vector3f normal, float depth, float x, float y, float z) {
        // Inverse transpose of the shear/stretch Jacobian, for correct lighting.
        float stretch = 1.0F - z / depth;
        normal.z = (normal.z + x / depth * normal.x + y / depth * normal.y) / stretch;
    }
}
