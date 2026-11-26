/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.render;

import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.Nonnull;

/**
 * Draws the shaped torso and, with wider hips, the shaped thighs (skin and overlay layers).
 * The vanilla parts are hidden for those players in {@code PlayerModelMixin}.
 *
 * <p>Each part is the vanilla cuboid with curved sides ({@link ProfiledBox}): front and back are
 * strips that follow the side curves, the sides are slanted strips between the same samples, and
 * their normals are averaged between neighbouring strips so the lighting is smooth as well. The skin
 * follows the curve (each texture column keeps its place across the width), so belts, seams and
 * outlines bend with the body instead of being cut off.</p>
 */
public class BodyShapeLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final float OVERLAY_DILATION = 0.25F;

    public BodyShapeLayer(RenderLayerParent<AvatarRenderState, PlayerModel> context) {
        super(context);
    }

    @Override
    public void submit(PoseStack matrices, SubmitNodeCollector queue, int light, @Nonnull AvatarRenderState state,
                       float limbAngle, float limbDistance) {
        BodyShape shape = BodyShape.of(state);
        if (shape == null) return;
        RenderType layer = renderLayer(state);
        if (layer == null) return;

        try {
            int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
            int color = ARGB.colorFromFloat(state.isInvisible ? 0.15F : 1.0F, 1.0F, 1.0F, 1.0F);
            PlayerModel model = getParentModel();

            ProfiledBox torso = torso(shape);
            submit(matrices, queue, layer, model.body, (entry, consumer) -> {
                torso.emit(16, 16, 0, entry, consumer, light, overlay, color);
                if (state.showJacket) torso.emit(16, 32, OVERLAY_DILATION, entry, consumer, light, overlay, color);
            });

            if (shape.shapesLegs()) {
                ProfiledBox left = leg(shape, true);
                ProfiledBox right = leg(shape, false);
                submit(matrices, queue, layer, model.leftLeg, (entry, consumer) -> {
                    left.emit(16, 48, 0, entry, consumer, light, overlay, color);
                    if (state.showLeftPants) left.emit(0, 48, OVERLAY_DILATION, entry, consumer, light, overlay, color);
                });
                submit(matrices, queue, layer, model.rightLeg, (entry, consumer) -> {
                    right.emit(0, 16, 0, entry, consumer, light, overlay, color);
                    if (state.showRightPants) right.emit(0, 32, OVERLAY_DILATION, entry, consumer, light, overlay, color);
                });
            }
        } catch (RuntimeException e) {
            SwakozaPubertyMod.LOGGER.error("Failed to render shaped body", e);
        }
    }

    private static void submit(PoseStack matrices, SubmitNodeCollector queue, RenderType layer, ModelPart part,
                               SubmitNodeCollector.CustomGeometryRenderer custom) {
        if (!part.visible) return;
        matrices.pushPose();
        try {
            part.translateAndRotate(matrices);
            queue.submitCustomGeometry(matrices, layer, custom);
        } finally {
            matrices.popPose();
        }
    }

    private static ProfiledBox torso(BodyShape shape) {
        int samples = BodyShape.samples();
        float[] left = new float[samples], right = new float[samples];
        for (int i = 0; i < samples; i++) {
            float hw = shape.halfWidth(BodyShape.sampleY(i));
            left[i] = -hw;
            right[i] = hw;
        }
        return new ProfiledBox(left, right, 8, 2);
    }

    /** Leg cuboid x -2..2 around its pivot; only the outer side (+X for the left leg) curves. */
    private static ProfiledBox leg(BodyShape shape, boolean leftLeg) {
        int samples = BodyShape.samples();
        float[] left = new float[samples], right = new float[samples];
        for (int i = 0; i < samples; i++) {
            float extra = shape.thighExtra(BodyShape.sampleY(i));
            left[i] = leftLeg ? -2 : -2 - extra;
            right[i] = leftLeg ? 2 + extra : 2;
        }
        return new ProfiledBox(left, right, 4, 2);
    }

    private static RenderType renderLayer(AvatarRenderState state) {
        Identifier texture = state.skin.body().texturePath();
        boolean bodyVisible = !state.isInvisible;
        if (!bodyVisible && !state.isInvisibleToPlayer) {
            return RenderLayerCompat.itemEntityTranslucentCull(texture);
        }
        if (bodyVisible) {
            return RenderLayerCompat.entityTranslucent(texture);
        }
        return null;
    }

    /**
     * A 12 px tall cuboid whose left (-X) and right (+X) sides are curves sampled every
     * {@link BodyShape#STEP}. Texture layout is the vanilla cuboid layout for a box of
     * {@code textureWidth} × 12 × 4 at (u0, v0). Coordinates without dilation decide the texture;
     * positions are pushed outward by the dilation, like vanilla {@code Dilation}.
     */
    private record ProfiledBox(float[] left, float[] right, int textureWidth, float halfDepth) {
        private static final float TEXTURE_SIZE = 64.0F;
        private static final int DEPTH = 4;

        void emit(int u0, int v0, float dilation, PoseStack.Pose entry, VertexConsumer consumer,
                  int light, int overlay, int color) {
            new Emitter(this, u0, v0, dilation, entry, consumer, light, overlay, color).emit();
        }

        private record Emitter(ProfiledBox box, int u0, int v0, float dilation, PoseStack.Pose entry,
                               VertexConsumer consumer, int light, int overlay, int color) {

            void emit() {
                float[] left = this.box.left, right = this.box.right;
                float d = this.box.halfDepth;
                int last = left.length - 1;
                float[][] rightNormals = sideNormals(right, 1);
                float[][] leftNormals = sideNormals(left, -1);

                for (int i = 0; i < last; i++) {
                    float y1 = BodyShape.sampleY(i), y2 = BodyShape.sampleY(i + 1);
                    float l1 = left[i], l2 = left[i + 1], r1 = right[i], r2 = right[i + 1];

                    // Front (z = -d): top-right, top-left, bottom-left, bottom-right.
                    vertex(r1, y1, -d, frontU(r1, l1, r1), faceV(y1), 0, 0, -1);
                    vertex(l1, y1, -d, frontU(l1, l1, r1), faceV(y1), 0, 0, -1);
                    vertex(l2, y2, -d, frontU(l2, l2, r2), faceV(y2), 0, 0, -1);
                    vertex(r2, y2, -d, frontU(r2, l2, r2), faceV(y2), 0, 0, -1);

                    // Back (z = +d).
                    vertex(l1, y1, d, backU(l1, l1, r1), faceV(y1), 0, 0, 1);
                    vertex(r1, y1, d, backU(r1, l1, r1), faceV(y1), 0, 0, 1);
                    vertex(r2, y2, d, backU(r2, l2, r2), faceV(y2), 0, 0, 1);
                    vertex(l2, y2, d, backU(l2, l2, r2), faceV(y2), 0, 0, 1);

                    // +X side.
                    vertex(r1, y1, d, eastU(d), faceV(y1), rightNormals[i][0], rightNormals[i][1], 0);
                    vertex(r1, y1, -d, eastU(-d), faceV(y1), rightNormals[i][0], rightNormals[i][1], 0);
                    vertex(r2, y2, -d, eastU(-d), faceV(y2), rightNormals[i + 1][0], rightNormals[i + 1][1], 0);
                    vertex(r2, y2, d, eastU(d), faceV(y2), rightNormals[i + 1][0], rightNormals[i + 1][1], 0);

                    // -X side.
                    vertex(l1, y1, -d, westU(-d), faceV(y1), leftNormals[i][0], leftNormals[i][1], 0);
                    vertex(l1, y1, d, westU(d), faceV(y1), leftNormals[i][0], leftNormals[i][1], 0);
                    vertex(l2, y2, d, westU(d), faceV(y2), leftNormals[i + 1][0], leftNormals[i + 1][1], 0);
                    vertex(l2, y2, -d, westU(-d), faceV(y2), leftNormals[i + 1][0], leftNormals[i + 1][1], 0);
                }

                // Top and bottom caps, mapped like the vanilla cuboid.
                float lt = left[0], rt = right[0], yt = BodyShape.TOP;
                vertex(rt, yt, d, capU(rt, lt, rt, false), capV(d), 0, -1, 0);
                vertex(lt, yt, d, capU(lt, lt, rt, false), capV(d), 0, -1, 0);
                vertex(lt, yt, -d, capU(lt, lt, rt, false), capV(-d), 0, -1, 0);
                vertex(rt, yt, -d, capU(rt, lt, rt, false), capV(-d), 0, -1, 0);

                float lb = left[last], rb = right[last], yb = BodyShape.BOTTOM;
                vertex(rb, yb, -d, capU(rb, lb, rb, true), capV(-d), 0, 1, 0);
                vertex(lb, yb, -d, capU(lb, lb, rb, true), capV(-d), 0, 1, 0);
                vertex(lb, yb, d, capU(lb, lb, rb, true), capV(d), 0, 1, 0);
                vertex(rb, yb, d, capU(rb, lb, rb, true), capV(d), 0, 1, 0);
            }

            /** Outward normals (x, y) of one side at every sample, averaged over the two strips meeting there. */
            private static float[][] sideNormals(float[] side, int outward) {
                int last = side.length - 1;
                float[][] result = new float[side.length][2];
                for (int i = 0; i <= last; i++) {
                    float sx = 0, sy = 0;
                    if (i > 0) {
                        float dx = (side[i] - side[i - 1]) * outward;
                        float length = (float) Math.sqrt(dx * dx + BodyShape.STEP * BodyShape.STEP);
                        sx += BodyShape.STEP / length;
                        sy += -dx / length;
                    }
                    if (i < last) {
                        float dx = (side[i + 1] - side[i]) * outward;
                        float length = (float) Math.sqrt(dx * dx + BodyShape.STEP * BodyShape.STEP);
                        sx += BodyShape.STEP / length;
                        sy += -dx / length;
                    }
                    float length = (float) Math.sqrt(sx * sx + sy * sy);
                    result[i][0] = outward * sx / length;
                    result[i][1] = sy / length;
                }
                return result;
            }

            private void vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
                float centre = 0;
                float px = x + Math.signum(x - centre) * this.dilation;
                float py = y + (y <= BodyShape.TOP ? -this.dilation : y >= BodyShape.BOTTOM ? this.dilation : 0);
                float pz = z + Math.signum(z) * this.dilation;
                Matrix4f position = this.entry.pose();
                Matrix3f normals = this.entry.normal();
                Vector4f transformed = new Vector4f(px / 16.0F, py / 16.0F, pz / 16.0F, 1.0F).mul(position);
                Vector3f normal = normals.transform(new Vector3f(nx, ny, nz)).normalize();
                this.consumer.addVertex(transformed.x, transformed.y, transformed.z, this.color,
                        u / TEXTURE_SIZE, v / TEXTURE_SIZE, this.overlay, this.light, normal.x, normal.y, normal.z);
            }

            /** Texture column 0..width of point x between the two sides at this height. */
            private float column(float x, float left, float right) {
                return (x - left) / Math.max(1.0E-4F, right - left) * this.box.textureWidth;
            }

            private float frontU(float x, float left, float right) {
                return this.u0 + DEPTH + column(x, left, right);
            }

            private float backU(float x, float left, float right) {
                return this.u0 + 2 * DEPTH + 2 * this.box.textureWidth - column(x, left, right);
            }

            private float eastU(float z) {
                return this.u0 + DEPTH + this.box.textureWidth + (z + this.box.halfDepth);
            }

            private float westU(float z) {
                return this.u0 + DEPTH - (z + this.box.halfDepth);
            }

            private float faceV(float y) {
                return this.v0 + DEPTH + y;
            }

            private float capU(float x, float left, float right, boolean bottom) {
                return this.u0 + DEPTH + (bottom ? this.box.textureWidth : 0) + column(x, left, right);
            }

            private float capV(float z) {
                return this.v0 + DEPTH - (z + this.box.halfDepth);
            }
        }
    }
}
