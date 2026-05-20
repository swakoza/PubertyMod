package com.swakoza.pubertymod.render;

import com.swakoza.pubertymod.api.IGenderArmor;
import com.swakoza.pubertymod.compat.EntityCompat;
import com.swakoza.pubertymod.compat.ModelPartCompat;
import com.swakoza.pubertymod.compat.RenderTickCompat;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.SwakozaHelper;
import com.swakoza.pubertymod.main.entitydata.Breasts;
import com.swakoza.pubertymod.main.entitydata.EntityConfig;
import com.swakoza.pubertymod.physics.BreastPhysics;
import com.swakoza.pubertymod.render.SwakozaModelRenderer.BreastModelBox;
import com.swakoza.pubertymod.render.SwakozaModelRenderer.OverlayModelBox;
import com.swakoza.pubertymod.render.SwakozaModelRenderer.PositionTextureVertex;
import java.util.ConcurrentModificationException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ArmorStandEntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class GenderLayer<S extends BipedEntityRenderState, M extends BipedEntityModel<S>> extends FeatureRenderer<S, M> {
    private BreastModelBox lBreast;
    private BreastModelBox rBreast;
    private final OverlayModelBox lBreastWear;
    private final OverlayModelBox rBreastWear;
    private float preBreastSize = 0f;
    private Breasts breasts;
    protected ItemStack armorStack = ItemStack.EMPTY;
    protected IGenderArmor genderArmor;
    protected boolean isChestplateOccupied;
    protected boolean bounceEnabled;
    protected boolean breathingAnimation;
    protected float breastOffsetX;
    protected float breastOffsetY;
    protected float breastOffsetZ;
    protected float lPhysPositionY;
    protected float lPhysPositionX;
    protected float rPhysPositionY;
    protected float rTotalX;
    protected float lPhysBounceRotation;
    protected float rPhysBounceRotation;
    protected float breastSize;
    protected float zOffset;
    protected float outwardAngle;
    protected EntityConfig entityConfig;

    public GenderLayer(FeatureRendererContext<S, M> render) {
        super(render);
        this.lBreast = new BreastModelBox(64, 64, 16, 17, -4F, 0.0F, 0F, 4, 5, 4, 0.0F, false);
        this.rBreast = new BreastModelBox(64, 64, 20, 17, 0, 0.0F, 0F, 4, 5, 4, 0.0F, false);
        this.lBreastWear = new OverlayModelBox(true, 64, 64, 17, 34, -4F, 0.0F, 0F, 4, 5, 3, 0.0F, false);
        this.rBreastWear = new OverlayModelBox(false, 64, 64, 21, 34, 0, 0.0F, 0F, 4, 5, 3, 0.0F, false);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, @Nonnull S state, float limbAngle, float limbDistance) {
        LivingEntity entity = getEntity(state);
        if (entity == null) return;

        this.entityConfig = getConfig(entity);
        if (this.entityConfig == null) return;

            float tickProgress = RenderTickCompat.getTickProgress(MinecraftClient.getInstance(), true);
        try {
            if (!setupRender(entity, state, tickProgress)) return;

            int overlay = LivingEntityRenderer.getOverlay(state, 0.0F);
            ModelPart body = getContextModel().body;

            matrices.push();
            try {
                setupTransformations(entity, state, body, matrices, BreastSide.LEFT);
                renderBreast(state, matrices, vertexConsumers, light, overlay, BreastSide.LEFT);
            } finally {
                matrices.pop();
            }

            matrices.push();
            try {
                setupTransformations(entity, state, body, matrices, BreastSide.RIGHT);
                renderBreast(state, matrices, vertexConsumers, light, overlay, BreastSide.RIGHT);
            } finally {
                matrices.pop();
            }
        } catch (Exception e) {
            SwakozaPubertyMod.LOGGER.error("Failed to render breast layer", e);
        }
    }

    protected @Nullable LivingEntity getEntity(S state) {
        if (state instanceof SwakozaEntityRenderState accessor) {
            return accessor.swakozapuberty$getEntity();
        }
        return null;
    }

    protected @Nullable Identifier getTexture(S state) {
        if (state instanceof PlayerEntityRenderState playerState) {
            return playerState.skinTextures.texture();
        }
        if (state instanceof ArmorStandEntityRenderState) {
            return ArmorStandEntityRenderer.TEXTURE;
        }
        return null;
    }

    protected @Nullable RenderLayer getRenderLayer(S state) {
        Identifier texture = getTexture(state);
        if (texture == null) return null;
        boolean bodyVisible = !state.invisible;
        boolean translucent = !bodyVisible && !state.invisibleToPlayer;
        if (translucent) return RenderLayerCompat.itemEntityTranslucentCull(texture);
        if (bodyVisible) return RenderLayerCompat.entityTranslucent(texture);
        return state.hasOutline ? RenderLayerCompat.outlineNoCull(texture) : null;
    }

    protected @Nullable EntityConfig getConfig(LivingEntity entity) {
        try {
            return EntityConfig.getEntity(entity);
        } catch (ConcurrentModificationException e) {
            return null;
        }
    }

    protected boolean setupRender(LivingEntity entity, S state, float tickProgress) {
        if (entity.isBaby()) return false;

        this.armorStack = state.equippedChestStack;
        this.genderArmor = SwakozaHelper.getArmorConfig(this.armorStack);
        this.isChestplateOccupied = this.genderArmor.coversBreasts() && !this.entityConfig.getArmorPhysicsOverride();
        if (this.genderArmor.alwaysHidesBreasts() || !this.entityConfig.showBreastsInArmor() && this.isChestplateOccupied) return false;

        RenderLayer renderLayer = getRenderLayer(state);
        if (renderLayer == null && !this.isChestplateOccupied) return false;

        this.breasts = this.entityConfig.getBreasts();
        this.breastOffsetX = Math.round((Math.round(this.breasts.getXOffset() * 100f) / 100f) * 10) / 10f;
        this.breastOffsetY = -Math.round((Math.round(this.breasts.getYOffset() * 100f) / 100f) * 10) / 10f;
        this.breastOffsetZ = -Math.round((Math.round(this.breasts.getZOffset() * 100f) / 100f) * 10) / 10f;

        BreastPhysics leftBreastPhysics = this.entityConfig.getLeftBreastPhysics();
        final float bSize = leftBreastPhysics.getBreastSize(tickProgress);
        this.outwardAngle = Math.min((Math.round(this.breasts.getCleavage() * 100f) / 100f) * 100f, 10);

        float reducer = -1;
        if (bSize < 0.84f) reducer++;
        if (bSize < 0.72f) reducer++;

        if (this.preBreastSize != bSize) {
            this.lBreast = new BreastModelBox(64, 64, 16, 17, -4F, 0.0F, 0F, 4, 5, (int)(4 - this.breastOffsetZ - reducer), 0.0F, false);
            this.rBreast = new BreastModelBox(64, 64, 20, 17, 0, 0.0F, 0F, 4, 5, (int)(4 - this.breastOffsetZ - reducer), 0.0F, false);
            this.preBreastSize = bSize;
        }

        this.lPhysPositionY = MathHelper.lerp(tickProgress, leftBreastPhysics.getPrePositionY(), leftBreastPhysics.getPositionY());
        this.lPhysPositionX = MathHelper.lerp(tickProgress, leftBreastPhysics.getPrePositionX(), leftBreastPhysics.getPositionX());
        this.lPhysBounceRotation = MathHelper.lerp(tickProgress, leftBreastPhysics.getPreBounceRotation(), leftBreastPhysics.getBounceRotation());
        if (this.breasts.isUniboob()) {
            this.rPhysPositionY = this.lPhysPositionY;
            this.rTotalX = this.lPhysPositionX;
            this.rPhysBounceRotation = this.lPhysBounceRotation;
        } else {
            BreastPhysics rightBreastPhysics = this.entityConfig.getRightBreastPhysics();
            this.rPhysPositionY = MathHelper.lerp(tickProgress, rightBreastPhysics.getPrePositionY(), rightBreastPhysics.getPositionY());
            this.rTotalX = MathHelper.lerp(tickProgress, rightBreastPhysics.getPrePositionX(), rightBreastPhysics.getPositionX());
            this.rPhysBounceRotation = MathHelper.lerp(tickProgress, rightBreastPhysics.getPreBounceRotation(), rightBreastPhysics.getBounceRotation());
        }

        this.breastSize = bSize * 1.5f;
        if (this.breastSize > 0.7f) this.breastSize = 0.7f;
        if (bSize > 0.7f) this.breastSize = bSize;
        if (this.breastSize < 0.02f) return false;

        this.zOffset = 0.0625f - (bSize * 0.0625f);
        this.breastSize = bSize + 0.5f * Math.abs(bSize - 0.7f) * 2f;

        float resistance = MathHelper.clamp(this.genderArmor.physicsResistance(), 0, 1);
        this.breathingAnimation = ((this.entityConfig.getArmorPhysicsOverride() || resistance <= 0.5F)
                && (!entity.isSubmergedInWater()
                || StatusEffectUtil.hasWaterBreathing(entity)
                || EntityCompat.getWorld(entity).getBlockState(new BlockPos(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ())).isOf(Blocks.BUBBLE_COLUMN)));
        this.bounceEnabled = this.entityConfig.hasBreastPhysics() && (!this.isChestplateOccupied || resistance < 1);
        return true;
    }

    protected void setupTransformations(LivingEntity entity, S state, ModelPart body, MatrixStack matrices, BreastSide side) {
        boolean left = side == BreastSide.LEFT;
        ModelPartCompat.applyTransform(body, matrices);

        if (this.bounceEnabled) {
            matrices.translate((left ? this.lPhysPositionX : this.rTotalX) / 32f, 0, 0);
            matrices.translate(0, (left ? this.lPhysPositionY : this.rPhysPositionY) / 32f, 0);
        }

        matrices.translate((left ? this.breastOffsetX : -this.breastOffsetX) * 0.0625f, 0.05625f + (this.breastOffsetY * 0.0625f), this.zOffset - 0.125f + (this.breastOffsetZ * 0.0625f));
        if (!this.breasts.isUniboob()) matrices.translate(-0.125f * (left ? 1 : -1), 0, 0);
        if (this.bounceEnabled) matrices.multiply(new Quaternionf().rotationXYZ(0, (float)((left ? this.lPhysBounceRotation : this.rPhysBounceRotation) * (Math.PI / 180f)), 0));
        if (!this.breasts.isUniboob()) matrices.translate(0.125f * (left ? 1 : -1), 0, 0);

        float rotationMultiplier = 0;
        if (this.bounceEnabled) {
            matrices.translate(0, -0.035f * this.breastSize, 0);
            rotationMultiplier = -(left ? this.lPhysPositionY : this.rPhysPositionY) / 12f;
        }
        float totalRotation = this.bounceEnabled ? this.breastSize + rotationMultiplier : this.breastSize;
        if (totalRotation > this.breastSize + 0.2F) totalRotation = this.breastSize + 0.2F;
        totalRotation = Math.min(totalRotation, 1);

        if (this.isChestplateOccupied) matrices.translate(0, 0, 0.01f);

        matrices.multiply(new Quaternionf().rotationXYZ(0, (float)((left ? this.outwardAngle : -this.outwardAngle) * (Math.PI / 180f)), 0));
        matrices.multiply(new Quaternionf().rotationXYZ((float)(-35f * totalRotation * (Math.PI / 180f)), 0, 0));

        if (this.breathingAnimation) {
            float breathing = -MathHelper.cos(entity.age * 0.09F) * 0.45F + 0.45F;
            matrices.multiply(new Quaternionf().rotationXYZ((float)(breathing * (Math.PI / 180f)), 0, 0));
        }

        matrices.scale(0.9995f, 1f, 1f);
    }

    protected boolean hasJacketLayer(S state, LivingEntity entity) {
        if (state instanceof PlayerEntityRenderState playerState) {
            return playerState.jacketVisible;
        }
        return entity instanceof ArmorStandEntity && this.entityConfig.hasJacketLayer();
    }

    protected void renderBreast(S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, BreastSide side) {
        LivingEntity entity = getEntity(state);
        RenderLayer renderLayer = getRenderLayer(state);
        if (entity == null || renderLayer == null) return;

        float alpha = state.invisible ? 0.15F : 1f;
        SwakozaModelRenderer.ModelBox box = side == BreastSide.LEFT ? this.lBreast : this.rBreast;
        renderBox(box, matrices.peek(), vertexConsumers.getBuffer(renderLayer), light, overlay, 1f, 1f, 1f, alpha);

        if (hasJacketLayer(state, entity)) {
            matrices.push();
            try {
                matrices.translate(0, 0, -0.015f);
                matrices.scale(1.05f, 1.05f, 1.05f);
                SwakozaModelRenderer.ModelBox wearBox = side == BreastSide.LEFT ? this.lBreastWear : this.rBreastWear;
                renderBox(wearBox, matrices.peek(), vertexConsumers.getBuffer(renderLayer), light, overlay, 1f, 1f, 1f, alpha);
            } finally {
                matrices.pop();
            }
        }
    }

    protected static void renderBox(SwakozaModelRenderer.ModelBox model, MatrixStack.Entry matrixEntry, VertexConsumer consumer, int light, int overlay,
            float red, float green, float blue, float alpha) {
        Matrix4f positionMatrix = matrixEntry.getPositionMatrix();
        Matrix3f normalMatrix = matrixEntry.getNormalMatrix();
        int color = ColorHelper.fromFloats(alpha, red, green, blue);
        for (SwakozaModelRenderer.TexturedQuad quad : model.quads) {
            Vector3f normal = new Vector3f(quad.normal.x, quad.normal.y, quad.normal.z);
            normal.mul(normalMatrix);
            for (PositionTextureVertex vertex : quad.vertexPositions) {
                Vector4f transformed = new Vector4f(vertex.x() / 16.0F, vertex.y() / 16.0F, vertex.z() / 16.0F, 1.0F);
                transformed.mul(positionMatrix);
                consumer.vertex(transformed.x, transformed.y, transformed.z, color, vertex.texturePositionX(), vertex.texturePositionY(), overlay, light, normal.x, normal.y, normal.z);
            }
        }
    }
}
