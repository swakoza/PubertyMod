package com.swakoza.pubertymod.render;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Affine stretch: the open back face is attached, the front carries the spring displacement. */
public final class BreastDeformation {
    private BreastDeformation() {}

    public record Displacement(float x, float y, float z) {
        public static final Displacement ZERO = new Displacement(0, 0, 0);
    }

    // The rear plane still trims hidden geometry. The upper bound moves the whole mesh instead.
    public record BackPlane(float x, float y, float z, float limit) {
        public static BackPlane atTorsoBack(Matrix4f bodyMatrix, Matrix3f normalMatrix) {
            Vector3f normal = normalMatrix.transform(new Vector3f(0, 0, 1)).normalize();
            Vector3f point = bodyMatrix.transformPosition(0, 0, 1.75F / 16.0F, new Vector3f());
            return new BackPlane(normal.x, normal.y, normal.z, normal.dot(point));
        }

        public void keepInside(Vector4f vertex) {
            float excess = x * vertex.x + y * vertex.y + z * vertex.z - limit;
            if (excess > 0) {
                vertex.x -= excess * x;
                vertex.y -= excess * y;
                vertex.z -= excess * z;
            }
        }
    }

    /** Clamp the shared origin, preserving the cuboid and its jacket/armor when it reaches the shoulder. */
    public static float clampOriginY(float requestedY, float pitch, float scaleY, float scaleZ,
                                     float depth, float displacementY, float displacementZ,
                                     boolean jacket, boolean armor) {
        float sine = (float) Math.sin(pitch);
        float cosine = (float) Math.cos(pitch);
        float minimum = minimumCornerY(sine, cosine, scaleY, scaleZ, depth,
                displacementY, displacementZ, 1.0F, 0, 0);
        if (jacket && !armor) {
            minimum = Math.min(minimum, minimumCornerY(sine, cosine, scaleY, scaleZ, 3,
                    displacementY, displacementZ, 1.05F, 0, -0.015F));
        }
        if (armor) {
            float shellScale = jacket ? 1.05F : 1.0F;
            float shellY = shellScale * 0.015F;
            float shellZ = (jacket ? -0.015F : 0) - shellScale * 0.015F;
            minimum = Math.min(minimum, minimumCornerY(sine, cosine, scaleY, scaleZ, 3,
                    displacementY, displacementZ, shellScale, shellY, shellZ));
            minimum = Math.min(minimum, minimumCornerY(sine, cosine, scaleY, scaleZ, 4,
                    displacementY, displacementZ, shellScale, shellY, shellZ));
        }
        return Math.max(requestedY, 0.5F / 16.0F - minimum);
    }

    private static float minimumCornerY(float sine, float cosine, float scaleY, float scaleZ,
                                        float depth, float displacementY, float displacementZ,
                                        float shellScale, float shellY, float shellZ) {
        float minimum = Float.POSITIVE_INFINITY;
        // The deformation is affine: the extrema of each cuboid are at its corners.
        for (int y = 0; y <= 5; y += 5) {
            for (int z = 0; z <= 1; z++) {
                float weight = 1 - z;
                float localY = scaleY * (shellY + shellScale * (y + displacementY * weight) / 16.0F);
                float localZ = scaleZ * (shellZ + shellScale * (z * depth + displacementZ * weight) / 16.0F);
                minimum = Math.min(minimum, cosine * localY - sine * localZ);
            }
        }
        return minimum;
    }

    public static float weight(float z, float backZ, float depth) {
        return (backZ - z) / depth;
    }

    public static void transformNormal(Vector3f normal, float depth, float x, float y, float z) {
        // Inverse transpose of the shear/stretch Jacobian, for correct lighting.
        float stretch = 1.0F - z / depth;
        normal.z = (normal.z + x / depth * normal.x + y / depth * normal.y) / stretch;
    }
}
