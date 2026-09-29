package com.swakoza.pubertymod.render;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Run by verifyBreastDeformation on every release target. */
public final class BreastDeformationTest {
    public static void main(String[] args) {
        int cases = 0;
        for (float depth : new float[]{1, 2, 3, 4, 5}) {
            for (float dx : new float[]{-1.25F, 0, 1.25F}) {
                for (float dy : new float[]{-1, 0, 1.5F}) {
                    for (float dz : new float[]{-0.75F, 0, 0.75F}) {
                        float back = depth;
                        float anchorWeight = BreastDeformation.weight(back, back, depth);
                        check(anchorWeight == 0, "Attachment moved");
                        check(BreastDeformation.weight(0, back, depth) == 1, "Front lost full displacement");
                        // All layers use this affine map. Its depth stays positive,
                        // including the shallowest allowed model and largest impulse.
                        check(depth - dz > 0, "Model inverted");
                        Vector3f tangentX = new Vector3f(1, 0, 0);
                        Vector3f tangentZ = new Vector3f(-dx / depth, -dy / depth, 1 - dz / depth);
                        Vector3f expectedNormal = tangentZ.cross(tangentX, new Vector3f()).normalize();
                        Vector3f normal = new Vector3f(0, 1, 0);
                        BreastDeformation.transformNormal(normal, depth, dx, dy, dz);
                        normal.normalize();
                        check(normal.distance(expectedNormal) < 1.0E-5F, "Lighting differs from deformed surface");
                        check(Float.isFinite(normal.z), "Non-finite normal");
                        cases++;
                    }
                }
            }
        }
        // A large rotated cuboid may project behind the vanilla torso; every
        // rendered vertex, including armor/trim, must stop inside its back face.
        for (float yaw : new float[]{0, 0.6F, -1.2F}) {
            Matrix4f body = new Matrix4f().translation(0.4F, -0.2F, 0.3F)
                    .rotate(new Quaternionf().rotationY(yaw));
            Matrix3f normal = new Matrix3f(body).invert().transpose();
            BreastDeformation.BackPlane plane = BreastDeformation.BackPlane.atTorsoBack(body, normal);
            Vector4f rear = body.transform(new Vector4f(0, 0, 0.5F, 1));
            plane.keepInside(rear);
            check(plane.x() * rear.x + plane.y() * rear.y + plane.z() * rear.z <= plane.limit() + 1.0E-5F,
                    "Rear surface escaped torso");
            Vector4f front = body.transform(new Vector4f(0, 0.5F, -0.5F, 1));
            Vector4f originalFront = new Vector4f(front);
            plane.keepInside(front);
            check(front.equals(originalFront), "Front surface changed");
        }
        // Moving the origin instead of each vertex preserves the entire breast shape.
        for (float pitch : new float[]{-0.61F, -0.2F, 0}) {
            for (float requested : new float[]{-0.08F, 0.03F, 0.1F}) {
                for (float dy : new float[]{-1, 0, 1.5F}) {
                    float origin = BreastDeformation.clampOriginY(requested, pitch, 1.3F, 1.8F,
                            5, dy, -0.75F, true, true);
                    check(origin >= requested, "Upper boundary moved the breast upward");
                    for (int vertexY : new int[]{0, 5}) {
                        for (int vertexZ : new int[]{0, 5}) {
                            float weight = 1 - vertexZ / 5.0F;
                            float localY = 1.3F * 1.05F * (0.015F + (vertexY + dy * weight) / 16.0F);
                            float localZ = 1.8F * (-0.015F + 1.05F * (-0.015F
                                    + (vertexZ - 0.75F * weight) / 16.0F));
                            float worldY = origin + (float)Math.cos(pitch) * localY
                                    - (float)Math.sin(pitch) * localZ;
                            check(worldY >= 0.5F / 16.0F - 1.0E-5F,
                                    "Upper surface escaped torso");
                        }
                    }
                }
            }
        }
        SwakozaModelRenderer.BreastModelBox mesh = new SwakozaModelRenderer.BreastModelBox(
                64, 64, 16, 17, -4, 0, 0, 4, 5, 4, 0, false);
        int upperFaces = 0;
        int lowerFaces = 0;
        for (SwakozaModelRenderer.TexturedQuad quad : mesh.quads) {
            if (quad.normal.y < -0.5F) {
                upperFaces++;
                for (SwakozaModelRenderer.PositionTextureVertex vertex : quad.vertexPositions) {
                    check(vertex.y() == 0, "Upper cap moved from model top");
                }
            }
            if (quad.normal.y > 0.5F) {
                lowerFaces++;
                for (SwakozaModelRenderer.PositionTextureVertex vertex : quad.vertexPositions) {
                    check(vertex.y() == 5, "Lower cap moved from model bottom");
                }
            }
        }
        check(upperFaces == 1 && lowerFaces == 1, "Breast caps changed");
        check(BreastTorsoAttachment.followsUpperHalf(0, 0.6F, 4), "Default breast left the upper torso");
        check(!BreastTorsoAttachment.followsUpperHalf(2, 0.6F, 4), "Lowered breast did not follow lower torso");
        check(!BreastTorsoAttachment.followsUpperHalf(0, 2.0F, 5), "Large breast did not follow lower torso");
        check(BreastTorsoAttachment.modelDepth(0.6F, 0) == 3, "Breast depth no longer matches renderer");
        System.out.println("Breast attachment and deformation: " + cases + " cases passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
