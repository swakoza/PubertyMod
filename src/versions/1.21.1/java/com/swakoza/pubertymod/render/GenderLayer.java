package com.swakoza.pubertymod.render;

import com.swakoza.pubertymod.api.IGenderArmor;
import com.swakoza.pubertymod.compat.EntityCompat;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.SwakozaHelper;
import com.swakoza.pubertymod.main.entitydata.Breasts;
import com.swakoza.pubertymod.main.entitydata.EntityConfig;
import com.swakoza.pubertymod.physics.BreastPhysics;
import com.swakoza.pubertymod.render.SwakozaModelRenderer.BreastModelBox;
import com.swakoza.pubertymod.render.SwakozaModelRenderer.OverlayModelBox;
import com.swakoza.pubertymod.render.SwakozaModelRenderer.PositionTextureVertex;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.player.PlayerModelPart;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ConcurrentModificationException;

public class GenderLayer<T extends LivingEntity, M extends BipedEntityModel<T>> extends FeatureRenderer<T, M> {
    private BreastModelBox lBreast;
    private BreastModelBox rBreast;
    private final OverlayModelBox lBreastWear;
    private final OverlayModelBox rBreastWear;
    private float preBreastSize = 0f;
    private Breasts breasts;
    protected ItemStack armorStack;
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

    public GenderLayer(FeatureRendererContext<T, M> render) {
        super(render);
        lBreast = new BreastModelBox(64, 64, 16, 17, -4F, 0.0F, 0F, 4, 5, 4, 0.0F, false);
        rBreast = new BreastModelBox(64, 64, 20, 17, 0, 0.0F, 0F, 4, 5, 4, 0.0F, false);
        lBreastWear = new OverlayModelBox(true, 64, 64, 17, 34, -4F, 0.0F, 0F, 4, 5, 3, 0.0F, false);
        rBreastWear = new OverlayModelBox(false, 64, 64, 21, 34, 0, 0.0F, 0F, 4, 5, 3, 0.0F, false);
    }

    private @Nullable RenderLayer getRenderLayer(T entity) {
        boolean bodyVisible = !entity.isInvisible();
        boolean translucent = !bodyVisible && !entity.isInvisibleTo(MinecraftClient.getInstance().player);
        Identifier texture = getTexture(entity);
        if (translucent) return RenderLayer.getItemEntityTranslucentCull(texture);
        if (bodyVisible) return RenderLayer.getEntityTranslucent(texture);
        if (entity.isGlowing()) return RenderLayer.getOutline(texture);
        return null;
    }

    protected @Nullable EntityConfig getConfig(T entity) {
        try {
            return EntityConfig.getEntity(entity);
        } catch (ConcurrentModificationException e) {
            return null;
        }
    }

    @Override
    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int packedLightIn, @Nonnull T ent, float limbAngle,
                       float limbDistance, float partialTicks, float animationProgress, float headYaw, float headPitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        EntityConfig entityConfig = getConfig(ent);
        if (entityConfig == null) return;

        try {
            if (!setupRender(ent, entityConfig, partialTicks)) return;
            int combineTex = LivingEntityRenderer.getOverlay(ent, 0);
            BipedEntityModel<T> model = getContextModel();

            matrixStack.push();
            try {
                setupTransformations(ent, model.body, matrixStack, BreastSide.LEFT);
                renderBreast(ent, matrixStack, vertexConsumerProvider, packedLightIn, combineTex, BreastSide.LEFT);
            } finally {
                matrixStack.pop();
            }

            matrixStack.push();
            try {
                setupTransformations(ent, model.body, matrixStack, BreastSide.RIGHT);
                renderBreast(ent, matrixStack, vertexConsumerProvider, packedLightIn, combineTex, BreastSide.RIGHT);
            } finally {
                matrixStack.pop();
            }
        } catch (Exception e) {
            SwakozaPubertyMod.LOGGER.error("Failed to render breast layer", e);
        }
    }

    protected boolean setupRender(T entity, EntityConfig entityConfig, float partialTicks) {
        if (entity.isBaby()) return false;

        armorStack = entity.getEquippedStack(EquipmentSlot.CHEST);
        genderArmor = SwakozaHelper.getArmorConfig(armorStack);
        isChestplateOccupied = genderArmor.coversBreasts() && !entityConfig.getArmorPhysicsOverride();
        if (genderArmor.alwaysHidesBreasts() || !entityConfig.showBreastsInArmor() && isChestplateOccupied) return false;

        RenderLayer type = getRenderLayer(entity);
        if (type == null && !isChestplateOccupied) return false;

        breasts = entityConfig.getBreasts();
        breastOffsetX = Math.round((Math.round(breasts.getXOffset() * 100f) / 100f) * 10) / 10f;
        breastOffsetY = -Math.round((Math.round(breasts.getYOffset() * 100f) / 100f) * 10) / 10f;
        breastOffsetZ = -Math.round((Math.round(breasts.getZOffset() * 100f) / 100f) * 10) / 10f;

        BreastPhysics leftBreastPhysics = entityConfig.getLeftBreastPhysics();
        final float bSize = leftBreastPhysics.getBreastSize(partialTicks);
        outwardAngle = Math.min((Math.round(breasts.getCleavage() * 100f) / 100f) * 100f, 10);

        float reducer = -1;
        if (bSize < 0.84f) reducer++;
        if (bSize < 0.72f) reducer++;

        if (preBreastSize != bSize) {
            lBreast = new BreastModelBox(64, 64, 16, 17, -4F, 0.0F, 0F, 4, 5, (int)(4 - breastOffsetZ - reducer), 0.0F, false);
            rBreast = new BreastModelBox(64, 64, 20, 17, 0, 0.0F, 0F, 4, 5, (int)(4 - breastOffsetZ - reducer), 0.0F, false);
            preBreastSize = bSize;
        }

        lPhysPositionY = MathHelper.lerp(partialTicks, leftBreastPhysics.getPrePositionY(), leftBreastPhysics.getPositionY());
        lPhysPositionX = MathHelper.lerp(partialTicks, leftBreastPhysics.getPrePositionX(), leftBreastPhysics.getPositionX());
        lPhysBounceRotation = MathHelper.lerp(partialTicks, leftBreastPhysics.getPreBounceRotation(), leftBreastPhysics.getBounceRotation());
        if (breasts.isUniboob()) {
            rPhysPositionY = lPhysPositionY;
            rTotalX = lPhysPositionX;
            rPhysBounceRotation = lPhysBounceRotation;
        } else {
            BreastPhysics rightBreastPhysics = entityConfig.getRightBreastPhysics();
            rPhysPositionY = MathHelper.lerp(partialTicks, rightBreastPhysics.getPrePositionY(), rightBreastPhysics.getPositionY());
            rTotalX = MathHelper.lerp(partialTicks, rightBreastPhysics.getPrePositionX(), rightBreastPhysics.getPositionX());
            rPhysBounceRotation = MathHelper.lerp(partialTicks, rightBreastPhysics.getPreBounceRotation(), rightBreastPhysics.getBounceRotation());
        }

        breastSize = bSize * 1.5f;
        if (breastSize > 0.7f) breastSize = 0.7f;
        if (bSize > 0.7f) breastSize = bSize;
        if (breastSize < 0.02f) return false;

        zOffset = 0.0625f - (bSize * 0.0625f);
        breastSize = bSize + 0.5f * Math.abs(bSize - 0.7f) * 2f;

        float resistance = MathHelper.clamp(genderArmor.physicsResistance(), 0, 1);
        breathingAnimation = ((entityConfig.getArmorPhysicsOverride() || resistance <= 0.5F)
                && (!entity.isSubmergedInWater()
                || StatusEffectUtil.hasWaterBreathing(entity)
                || EntityCompat.getWorld(entity).getBlockState(new BlockPos(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ())).isOf(Blocks.BUBBLE_COLUMN)));
        bounceEnabled = entityConfig.hasBreastPhysics() && (!isChestplateOccupied || resistance < 1);
        return true;
    }

    protected void setupTransformations(T entity, ModelPart body, MatrixStack matrixStack, BreastSide side) {
        boolean left = side == BreastSide.LEFT;
        matrixStack.translate(body.pivotX * 0.0625f, body.pivotY * 0.0625f, body.pivotZ * 0.0625f);
        if (body.roll != 0.0F) matrixStack.multiply(new Quaternionf().rotationXYZ(0f, 0f, body.roll));
        if (body.yaw != 0.0F) matrixStack.multiply(new Quaternionf().rotationXYZ(0f, body.yaw, 0f));
        if (body.pitch != 0.0F) matrixStack.multiply(new Quaternionf().rotationXYZ(body.pitch, 0f, 0f));

        if (bounceEnabled) {
            matrixStack.translate((left ? lPhysPositionX : rTotalX) / 32f, 0, 0);
            matrixStack.translate(0, (left ? lPhysPositionY : rPhysPositionY) / 32f, 0);
        }

        matrixStack.translate((left ? breastOffsetX : -breastOffsetX) * 0.0625f, 0.05625f + (breastOffsetY * 0.0625f), zOffset - 0.125f + (breastOffsetZ * 0.0625f));

        if (!breasts.isUniboob()) matrixStack.translate(-0.125f * (left ? 1 : -1), 0, 0);
        if (bounceEnabled) matrixStack.multiply(new Quaternionf().rotationXYZ(0, (float)((left ? lPhysBounceRotation : rPhysBounceRotation) * (Math.PI / 180f)), 0));
        if (!breasts.isUniboob()) matrixStack.translate(0.125f * (left ? 1 : -1), 0, 0);

        float rotationMultiplier = 0;
        if (bounceEnabled) {
            matrixStack.translate(0, -0.035f * breastSize, 0);
            rotationMultiplier = -(left ? lPhysPositionY : rPhysPositionY) / 12f;
        }
        float totalRotation = bounceEnabled ? breastSize + rotationMultiplier : breastSize;
        if (totalRotation > breastSize + 0.2F) totalRotation = breastSize + 0.2F;
        totalRotation = Math.min(totalRotation, 1);

        if (isChestplateOccupied) matrixStack.translate(0, 0, 0.01f);
        matrixStack.multiply(new Quaternionf().rotationXYZ(0, (float)((left ? outwardAngle : -outwardAngle) * (Math.PI / 180f)), 0));
        matrixStack.multiply(new Quaternionf().rotationXYZ((float)(-35f * totalRotation * (Math.PI / 180f)), 0, 0));

        if (breathingAnimation) {
            float breathing = -MathHelper.cos(entity.age * 0.09F) * 0.45F + 0.45F;
            matrixStack.multiply(new Quaternionf().rotationXYZ((float)(breathing * (Math.PI / 180f)), 0, 0));
        }

        matrixStack.scale(0.9995f, 1f, 1f);
    }

    private void renderBreast(T entity, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int packedLightIn, int packedOverlayIn, BreastSide side) {
        RenderLayer breastRenderType = getRenderLayer(entity);
        if (breastRenderType == null) return;
        float alpha = entity.isInvisible() ? 0.15F : 1;
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(breastRenderType);
        renderBox(side == BreastSide.LEFT ? lBreast : rBreast, matrixStack, vertexConsumer, packedLightIn, packedOverlayIn, 1f, 1f, 1f, alpha);
        if (entity instanceof AbstractClientPlayerEntity player && player.isPartVisible(PlayerModelPart.JACKET)) {
            matrixStack.translate(0, 0, -0.015f);
            matrixStack.scale(1.05f, 1.05f, 1.05f);
            renderBox(side == BreastSide.LEFT ? lBreastWear : rBreastWear, matrixStack, vertexConsumer, packedLightIn, packedOverlayIn, 1f, 1f, 1f, alpha);
        }
    }

    protected static void renderBox(SwakozaModelRenderer.ModelBox model, MatrixStack matrixStack, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn,
                                    float red, float green, float blue, float alpha) {
        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
        Matrix3f matrix3f = matrixStack.peek().getNormalMatrix();
        for (SwakozaModelRenderer.TexturedQuad quad : model.quads) {
            Vector3f normal = new Vector3f(quad.normal.x, quad.normal.y, quad.normal.z);
            normal.mul(matrix3f);
            for (PositionTextureVertex vertex : quad.vertexPositions) {
                Vector4f position = new Vector4f(vertex.x() / 16.0F, vertex.y() / 16.0F, vertex.z() / 16.0F, 1.0F);
                position.mul(matrix4f);
                int color = ColorHelper.Argb.fromFloats(alpha, red, green, blue);
                bufferIn.vertex(position.x, position.y, position.z, color, vertex.texturePositionX(), vertex.texturePositionY(), packedOverlayIn, packedLightIn, normal.x, normal.y, normal.z);
            }
        }
    }

    protected @Nullable Identifier getTexture(T entity) {
        if (entity instanceof AbstractClientPlayerEntity player) {
            return player.getSkinTextures().texture();
        }
        if (entity instanceof net.minecraft.entity.decoration.ArmorStandEntity) {
            return net.minecraft.client.render.entity.ArmorStandEntityRenderer.TEXTURE;
        }
        return null;
    }
}
