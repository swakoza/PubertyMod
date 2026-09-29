package com.swakoza.pubertymod.render;

import com.swakoza.pubertymod.api.IGenderArmor;
import com.swakoza.pubertymod.compat.EntityCompat;
import com.swakoza.pubertymod.gui.SwakozaPreviewPlayerEntity;
import com.swakoza.pubertymod.compat.PalCompat;
import com.swakoza.pubertymod.compat.TorsoPhysicsCompat;
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
    private float deformationX, deformationY, deformationZ;
    protected BreastDeformation.BackPlane backPlane;
    private BreastModelBox lBreast;
    private BreastModelBox rBreast;
    private final OverlayModelBox lBreastWear;
    private final OverlayModelBox rBreastWear;
    private int previousDepth = -1;
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
    protected float lPhysPositionZ;
    protected float rPhysPositionY;
    protected float rTotalX;
    protected float rPhysPositionZ;
    protected float lPhysBounceRotation;
    protected float rPhysBounceRotation;
    protected float breastSize;
    protected float rawBustSize;
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
            TorsoPhysicsCompat.capture(ent, entityConfig, model.body);

            matrixStack.push();
            try {
                setupTransformations(ent, model.body, matrixStack, BreastSide.LEFT, partialTicks);
                renderBreast(ent, matrixStack, vertexConsumerProvider, packedLightIn, combineTex, BreastSide.LEFT);
            } finally {
                matrixStack.pop();
            }

            matrixStack.push();
            try {
                setupTransformations(ent, model.body, matrixStack, BreastSide.RIGHT, partialTicks);
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
        boolean staticPreview = entity instanceof SwakozaPreviewPlayerEntity;
        // An unloaded player's physics never ticks. Use the synced appearance directly.
        final float bSize = staticPreview
                ? (entityConfig.getGender().canHaveBreasts()
                    ? entityConfig.getBustSize() * (1.0F - 0.15F * (entityConfig.getArmorPhysicsOverride() ? 0 : MathHelper.clamp(genderArmor.tightness(), 0, 1))) : 0)
                : leftBreastPhysics.getBreastSize(partialTicks);
        rawBustSize = bSize;
        outwardAngle = Math.min((Math.round(breasts.getCleavage() * 100f) / 100f) * 100f, 30);

        float reducer = -1;
        if (bSize < 0.84f) reducer++;
        if (bSize < 0.72f) reducer++;

        int depth = Math.max(1, (int)(4 - breastOffsetZ - reducer));
        if (previousDepth != depth) {
            lBreast = new BreastModelBox(64, 64, 16, 17, -4F, 0.0F, 0F, 4, 5, depth, 0.0F, false);
            rBreast = new BreastModelBox(64, 64, 20, 17, 0, 0.0F, 0F, 4, 5, depth, 0.0F, false);
            previousDepth = depth;
        }

        lPhysPositionY = MathHelper.lerp(partialTicks, leftBreastPhysics.getPrePositionY(), leftBreastPhysics.getPositionY());
        lPhysPositionX = MathHelper.lerp(partialTicks, leftBreastPhysics.getPrePositionX(), leftBreastPhysics.getPositionX());
        lPhysPositionZ = MathHelper.lerp(partialTicks, leftBreastPhysics.getPrePositionZ(), leftBreastPhysics.getPositionZ());
        lPhysBounceRotation = MathHelper.lerp(partialTicks, leftBreastPhysics.getPreBounceRotation(), leftBreastPhysics.getBounceRotation());
        if (breasts.isUniboob()) {
            rPhysPositionY = lPhysPositionY;
            rTotalX = lPhysPositionX;
            rPhysPositionZ = lPhysPositionZ;
            rPhysBounceRotation = lPhysBounceRotation;
        } else {
            BreastPhysics rightBreastPhysics = entityConfig.getRightBreastPhysics();
            rPhysPositionY = MathHelper.lerp(partialTicks, rightBreastPhysics.getPrePositionY(), rightBreastPhysics.getPositionY());
            rTotalX = MathHelper.lerp(partialTicks, rightBreastPhysics.getPrePositionX(), rightBreastPhysics.getPositionX());
            rPhysPositionZ = MathHelper.lerp(partialTicks, rightBreastPhysics.getPrePositionZ(), rightBreastPhysics.getPositionZ());
            rPhysBounceRotation = MathHelper.lerp(partialTicks, rightBreastPhysics.getPreBounceRotation(), rightBreastPhysics.getBounceRotation());
        }

        breastSize = bSize * 1.5f;
        if (breastSize > 0.7f) breastSize = 0.7f;
        if (bSize > 0.7f) breastSize = bSize;
        if (breastSize < 0.02f) return false;

        zOffset = 0.0625f - (bSize * 0.0625f);
        breastSize = bSize + 0.5f * Math.abs(bSize - 0.7f) * 2f;

        float resistance = MathHelper.clamp(genderArmor.physicsResistance(), 0, 1);
        breathingAnimation = !staticPreview && ((entityConfig.getArmorPhysicsOverride() || resistance <= 0.5F)
                && (!entity.isSubmergedInWater()
                || StatusEffectUtil.hasWaterBreathing(entity)
                || EntityCompat.getWorld(entity).getBlockState(new BlockPos(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ())).isOf(Blocks.BUBBLE_COLUMN)));
        bounceEnabled = !staticPreview && entityConfig.hasBreastPhysics() && (!isChestplateOccupied || resistance < 1);
        return true;
    }

    protected void setupTransformations(T entity, ModelPart body, MatrixStack matrixStack, BreastSide side, float partialTicks) {
        boolean left = side == BreastSide.LEFT;
        body.rotate(matrixStack);
        PalCompat.applyTorsoBend(entity, matrixStack, partialTicks,
                BreastTorsoAttachment.followsUpperHalf(breastOffsetY, rawBustSize,
                        lBreast.posZ2 - lBreast.posZ1));
        this.backPlane = BreastDeformation.BackPlane.atTorsoBack(matrixStack.peek().getPositionMatrix(), matrixStack.peek().getNormalMatrix());

        // Keep the attachment face fixed. Inertia deforms only the projecting
        // portion of the model, including jacket/armor layers in renderBox.
        deformationX = bounceEnabled ? MathHelper.clamp((left ? lPhysPositionX : rTotalX) * 0.4F
                + (left ? lPhysBounceRotation : rPhysBounceRotation) * 0.08F, -1.25F, 1.25F) : 0.0F;
        deformationY = bounceEnabled ? MathHelper.clamp((left ? lPhysPositionY : rPhysPositionY) * 0.5F,
                -1.0F, 1.5F) : 0.0F;
        deformationZ = bounceEnabled ? MathHelper.clamp((left ? lPhysPositionZ : rPhysPositionZ) * 0.25F,
                -0.75F, 0.75F) : 0.0F;

        float totalRotation = Math.min(breastSize, 1.0F);
        float pitch = (float) Math.toRadians(-35f * totalRotation);
        if (breathingAnimation) {
            float breathing = -MathHelper.cos((entity.age + partialTicks) * 0.09F) * 0.45F + 0.45F;
            pitch += (float) Math.toRadians(breathing);
        }
        float extraSize = Math.max(0.0f, Math.min(1.2f, rawBustSize - 0.8f));
        float scaleY = 1.0f + extraSize * 0.25f;
        float scaleZ = 1.0f + extraSize * 0.7f;
        EntityConfig armorStandConfig = entity instanceof net.minecraft.entity.decoration.ArmorStandEntity
                ? getConfig(entity) : null;
        boolean jacket = (entity instanceof AbstractClientPlayerEntity player && player.isPartVisible(PlayerModelPart.JACKET))
                || (armorStandConfig != null && armorStandConfig.hasJacketLayer());
        float originY = BreastDeformation.clampOriginY(0.05625f + breastOffsetY * 0.0625f,
                pitch, scaleY, scaleZ, lBreast.posZ2 - lBreast.posZ1,
                deformationY, deformationZ, jacket, this instanceof GenderArmorLayer);
        matrixStack.translate((left ? breastOffsetX : -breastOffsetX) * 0.0625f,
                originY, zOffset - 0.125f + breastOffsetZ * 0.0625f);
        if (isChestplateOccupied) matrixStack.translate(0, 0, 0.01f);
        matrixStack.multiply(new Quaternionf().rotationXYZ(0, (float)((left ? outwardAngle : -outwardAngle) * (Math.PI / 180f)), 0));
        matrixStack.multiply(new Quaternionf().rotationXYZ(pitch, 0, 0));
        matrixStack.scale(0.9995f, scaleY, scaleZ);
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

    protected void renderBox(SwakozaModelRenderer.ModelBox model, MatrixStack matrixStack, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn,
                                    float red, float green, float blue, float alpha) {
        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
        Matrix3f matrix3f = matrixStack.peek().getNormalMatrix();
        for (SwakozaModelRenderer.TexturedQuad quad : model.quads) {
            float depth = Math.max(0.001F, model.posZ2 - model.posZ1);
            Vector3f normal = new Vector3f(quad.normal.x, quad.normal.y, quad.normal.z);
            BreastDeformation.transformNormal(normal, depth, deformationX, deformationY, deformationZ);
            normal.mul(matrix3f).normalize();
            for (PositionTextureVertex vertex : quad.vertexPositions) {
                float weight = BreastDeformation.weight(vertex.z(), model.posZ2, depth);
                Vector4f position = new Vector4f((vertex.x() + deformationX * weight) / 16.0F,
                        (vertex.y() + deformationY * weight) / 16.0F,
                        (vertex.z() + deformationZ * weight) / 16.0F, 1.0F);
                position.mul(matrix4f);
                backPlane.keepInside(position);
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
