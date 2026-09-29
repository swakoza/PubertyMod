/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.swakoza.pubertymod.api.IGenderArmor;
import com.swakoza.pubertymod.compat.EntityCompat;
import com.swakoza.pubertymod.compat.TorsoPhysicsCompat;
import com.swakoza.pubertymod.compat.PalPhysicsCompat;
import com.swakoza.pubertymod.gui.SwakozaPreviewPlayerEntity;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class GenderLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
	private BreastModelBox lBreast;
	private BreastModelBox rBreast;
	private final OverlayModelBox lBreastWear;
	private final OverlayModelBox rBreastWear;

	private float preBreastSize = 0f;
    private int previousDepth = -1;
    protected BreastDeformation.Displacement deformation = BreastDeformation.Displacement.ZERO;
    protected BreastDeformation.BackPlane backPlane;
    protected float lPhysPositionZ, rPhysPositionZ;
    protected float renderTickProgress;
    protected float rawBustSize;
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

	public GenderLayer(RenderLayerParent<S, M> render) {
		super(render);
		this.lBreast = new BreastModelBox(64, 64, 16, 17, -4F, 0.0F, 0F, 4, 5, 4, 0.0F, false);
		this.rBreast = new BreastModelBox(64, 64, 20, 17, 0, 0.0F, 0F, 4, 5, 4, 0.0F, false);
		this.lBreastWear = new OverlayModelBox(true, 64, 64, 17, 34, -4F, 0.0F, 0F, 4, 5, 3, 0.0F, false);
		this.rBreastWear = new OverlayModelBox(false, 64, 64, 21, 34, 0, 0.0F, 0F, 4, 5, 3, 0.0F, false);
	}

	@Override
	public void submit(PoseStack matrices, SubmitNodeCollector queue, int light, @Nonnull S state, float limbAngle, float limbDistance) {
		LivingEntity entity = getEntity(state);
		if(entity == null) return;

		this.entityConfig = getConfig(entity);
		if(this.entityConfig == null) return;

		float tickProgress = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		this.renderTickProgress = tickProgress;
		try {
			if(!setupRender(entity, state, tickProgress)) return;

			int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
			ModelPart body = getParentModel().body;
            TorsoPhysicsCompat.capture(entity, this.entityConfig, body);

			matrices.pushPose();
			try {
				setupTransformations(entity, state, body, matrices, BreastSide.LEFT);
				renderBreast(state, matrices, queue, light, overlay, BreastSide.LEFT);
			} finally {
				matrices.popPose();
			}

			matrices.pushPose();
			try {
				setupTransformations(entity, state, body, matrices, BreastSide.RIGHT);
				renderBreast(state, matrices, queue, light, overlay, BreastSide.RIGHT);
			} finally {
				matrices.popPose();
			}
		} catch(Exception e) {
			SwakozaPubertyMod.LOGGER.error("Failed to render breast layer", e);
		}
	}

	protected @Nullable LivingEntity getEntity(S state) {
		if(state instanceof SwakozaEntityRenderState accessor) {
			return accessor.swakozapuberty$getEntity();
		}
		return null;
	}

	protected @Nullable Identifier getTexture(S state) {
		if(state instanceof AvatarRenderState playerState) {
			return playerState.skin.body().texturePath();
		}
		if(state instanceof ArmorStandRenderState) {
			return ArmorStandRenderer.DEFAULT_SKIN_LOCATION;
		}
		return null;
	}

	protected @Nullable RenderType getRenderLayer(S state) {
		Identifier texture = getTexture(state);
		if(texture == null) return null;
		boolean bodyVisible = !state.isInvisible;
		boolean translucent = !bodyVisible && !state.isInvisibleToPlayer;
		if(translucent) {
			return RenderLayerCompat.itemEntityTranslucentCull(texture);
		}
		if(bodyVisible) {
			return RenderLayerCompat.entityTranslucent(texture);
		}
		return state.appearsGlowing() ? RenderLayerCompat.outlineNoCull(texture) : null;
	}

	protected @Nullable EntityConfig getConfig(LivingEntity entity) {
		try {
			return EntityConfig.getEntity(entity);
		} catch(ConcurrentModificationException e) {
			return null;
		}
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	protected boolean setupRender(LivingEntity entity, S state, float tickProgress) {
		if(entity.isBaby()) return false;
		boolean previewEntity = entity instanceof SwakozaPreviewPlayerEntity;

		this.armorStack = state.chestEquipment;
		this.genderArmor = SwakozaHelper.getArmorConfig(this.armorStack);
		this.isChestplateOccupied = this.genderArmor.coversBreasts() && !this.entityConfig.getArmorPhysicsOverride();
		if(this.genderArmor.alwaysHidesBreasts() || !this.entityConfig.showBreastsInArmor() && this.isChestplateOccupied) {
			return false;
		}

		RenderType renderLayer = getRenderLayer(state);
		if(renderLayer == null && !this.isChestplateOccupied) {
			return false;
		}

		this.breasts = this.entityConfig.getBreasts();
		this.breastOffsetX = Math.round((Math.round(this.breasts.getXOffset() * 100f) / 100f) * 10) / 10f;
		this.breastOffsetY = -Math.round((Math.round(this.breasts.getYOffset() * 100f) / 100f) * 10) / 10f;
		this.breastOffsetZ = -Math.round((Math.round(this.breasts.getZOffset() * 100f) / 100f) * 10) / 10f;

		BreastPhysics leftBreastPhysics = this.entityConfig.getLeftBreastPhysics();
		final float bSize = previewEntity ? getStaticBreastSize() : leftBreastPhysics.getBreastSize(tickProgress);
		this.rawBustSize = bSize;
        this.outwardAngle = Math.min((Math.round(this.breasts.getCleavage() * 100f) / 100f) * 100f, 30);

		float reducer = -1;
		if(bSize < 0.84f) reducer++;
		if(bSize < 0.72f) reducer++;

		int depth = Math.max(1, (int)(4 - this.breastOffsetZ - reducer));
        if(this.previousDepth != depth) {
			this.lBreast = new BreastModelBox(64, 64, 16, 17, -4F, 0.0F, 0F, 4, 5, depth, 0.0F, false);
			this.rBreast = new BreastModelBox(64, 64, 20, 17, 0, 0.0F, 0F, 4, 5, depth, 0.0F, false);
			this.preBreastSize = bSize;
            this.previousDepth = depth;
		}

		if(previewEntity) {
			this.lPhysPositionY = 0;
			this.lPhysPositionX = 0;
            this.lPhysPositionZ = 0;
			this.lPhysBounceRotation = 0;
			this.rPhysPositionY = 0;
			this.rTotalX = 0;
            this.rPhysPositionZ = 0;
			this.rPhysBounceRotation = 0;
		} else {
			this.lPhysPositionY = Mth.lerp(tickProgress, leftBreastPhysics.getPrePositionY(), leftBreastPhysics.getPositionY());
			this.lPhysPositionX = Mth.lerp(tickProgress, leftBreastPhysics.getPrePositionX(), leftBreastPhysics.getPositionX());
			this.lPhysPositionZ = Mth.lerp(tickProgress, leftBreastPhysics.getPrePositionZ(), leftBreastPhysics.getPositionZ());
			this.lPhysBounceRotation = Mth.lerp(tickProgress, leftBreastPhysics.getPreBounceRotation(), leftBreastPhysics.getBounceRotation());
			if(this.breasts.isUniboob()) {
				this.rPhysPositionY = this.lPhysPositionY;
				this.rTotalX = this.lPhysPositionX;
                this.rPhysPositionZ = this.lPhysPositionZ;
				this.rPhysBounceRotation = this.lPhysBounceRotation;
			} else {
				BreastPhysics rightBreastPhysics = this.entityConfig.getRightBreastPhysics();
				this.rPhysPositionY = Mth.lerp(tickProgress, rightBreastPhysics.getPrePositionY(), rightBreastPhysics.getPositionY());
				this.rTotalX = Mth.lerp(tickProgress, rightBreastPhysics.getPrePositionX(), rightBreastPhysics.getPositionX());
				this.rPhysPositionZ = Mth.lerp(tickProgress, rightBreastPhysics.getPrePositionZ(), rightBreastPhysics.getPositionZ());
				this.rPhysBounceRotation = Mth.lerp(tickProgress, rightBreastPhysics.getPreBounceRotation(), rightBreastPhysics.getBounceRotation());
			}
		}

		this.breastSize = bSize * 1.5f;
		if(this.breastSize > 0.7f) this.breastSize = 0.7f;
		if(bSize > 0.7f) this.breastSize = bSize;
		if(this.breastSize < 0.02f) return false;

		this.zOffset = 0.0625f - (bSize * 0.0625f);
		this.breastSize = bSize + 0.5f * Math.abs(bSize - 0.7f) * 2f;

		float resistance = Mth.clamp(this.genderArmor.physicsResistance(), 0, 1);
		this.breathingAnimation = !previewEntity && ((this.entityConfig.getArmorPhysicsOverride() || resistance <= 0.5F)
				&& (!entity.isUnderWater()
				|| MobEffectUtil.hasWaterBreathing(entity)
				|| EntityCompat.getWorld(entity).getBlockState(new BlockPos(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ())).is(Blocks.BUBBLE_COLUMN)));
		this.bounceEnabled = !previewEntity && this.entityConfig.hasBreastPhysics() && (!this.isChestplateOccupied || resistance < 1);
		return true;
	}

	protected float getStaticBreastSize() {
		if(!this.entityConfig.getGender().canHaveBreasts()) return 0f;
		float targetBreastSize = this.entityConfig.getBustSize();
		float tightness = Mth.clamp(this.genderArmor.tightness(), 0, 1);
		if(this.entityConfig.getArmorPhysicsOverride()) tightness = 0;
		return targetBreastSize * (1 - 0.15F * tightness);
	}

	protected void setupTransformations(LivingEntity entity, S state, ModelPart body, PoseStack matrices, BreastSide side) {
		boolean left = side == BreastSide.LEFT;
		body.translateAndRotate(matrices);
        PalPhysicsCompat.applyTorsoBend(entity, matrices, this.renderTickProgress,
                BreastTorsoAttachment.followsUpperHalf(this.breastOffsetY, this.rawBustSize,
                        this.lBreast.posZ2 - this.lBreast.posZ1));
        this.backPlane = BreastDeformation.BackPlane.atTorsoBack(matrices.last().pose(), matrices.last().normal());

        this.deformation = this.bounceEnabled ? new BreastDeformation.Displacement(
                Mth.clamp((left ? this.lPhysPositionX : this.rTotalX) * 0.4F
                        + (left ? this.lPhysBounceRotation : this.rPhysBounceRotation) * 0.08F, -1.25F, 1.25F),
                Mth.clamp((left ? this.lPhysPositionY : this.rPhysPositionY) * 0.5F, -1.0F, 1.5F),
                Mth.clamp((left ? this.lPhysPositionZ : this.rPhysPositionZ) * 0.25F, -0.75F, 0.75F))
                : BreastDeformation.Displacement.ZERO;
        float totalRotation = Math.min(this.breastSize, 1.0F);
        float pitch = (float)(-35f * totalRotation * (Math.PI / 180f));
        if (this.breathingAnimation) {
            float breathing = -Mth.cos((entity.tickCount + this.renderTickProgress) * 0.09F) * 0.45F + 0.45F;
            pitch += (float)(breathing * (Math.PI / 180f));
        }
        float extraSize = Math.max(0.0F, Math.min(1.2F, this.rawBustSize - 0.8F));
        float scaleY = 1.0F + extraSize * 0.25F;
        float scaleZ = 1.0F + extraSize * 0.7F;
        float originY = BreastDeformation.clampOriginY(0.05625f + this.breastOffsetY * 0.0625f,
                pitch, scaleY, scaleZ, this.lBreast.posZ2 - this.lBreast.posZ1,
                this.deformation.y(), this.deformation.z(), hasJacketLayer(state, entity), this instanceof GenderArmorLayer);
        matrices.translate((left ? this.breastOffsetX : -this.breastOffsetX) * 0.0625f,
                originY, this.zOffset - 0.125f + this.breastOffsetZ * 0.0625f);
        if (this.isChestplateOccupied) matrices.translate(0, 0, 0.01f);
        matrices.mulPose(new Quaternionf().rotationXYZ(0, (float)((left ? this.outwardAngle : -this.outwardAngle) * (Math.PI / 180f)), 0));
        matrices.mulPose(new Quaternionf().rotationXYZ(pitch, 0, 0));
        matrices.scale(0.9995f, scaleY, scaleZ);
	}

	protected boolean hasJacketLayer(S state, LivingEntity entity) {
		if(state instanceof AvatarRenderState playerState) {
			return playerState.showJacket;
		}
		return entity instanceof ArmorStand && this.entityConfig.hasJacketLayer();
	}

	protected void renderBreast(S state, PoseStack matrices, SubmitNodeCollector queue, int light, int overlay, BreastSide side) {
		BreastDeformation.Displacement deformation = this.deformation;
        BreastDeformation.BackPlane backPlane = this.backPlane;
        LivingEntity entity = getEntity(state);
		RenderType renderLayer = getRenderLayer(state);
		if(entity == null || renderLayer == null) return;

		float alpha = state.isInvisible ? 0.15F : 1f;
		SwakozaModelRenderer.ModelBox box = side == BreastSide.LEFT ? this.lBreast : this.rBreast;
		queue.submitCustomGeometry(matrices, renderLayer, (entry, vertexConsumer) -> renderBox(box, entry, vertexConsumer, light, overlay, 1f, 1f, 1f, alpha, deformation, backPlane));

		if(hasJacketLayer(state, entity)) {
			matrices.pushPose();
			try {
				matrices.translate(0, 0, -0.015f);
				matrices.scale(1.05f, 1.05f, 1.05f);
				SwakozaModelRenderer.ModelBox wearBox = side == BreastSide.LEFT ? this.lBreastWear : this.rBreastWear;
				queue.submitCustomGeometry(matrices, renderLayer, (entry, vertexConsumer) -> renderBox(wearBox, entry, vertexConsumer, light, overlay, 1f, 1f, 1f, alpha, deformation, backPlane));
			} finally {
				matrices.popPose();
			}
		}
	}

	protected static void renderBox(SwakozaModelRenderer.ModelBox model, PoseStack.Pose matrixEntry, VertexConsumer consumer, int light, int overlay,
			float red, float green, float blue, float alpha, BreastDeformation.Displacement deformation, BreastDeformation.BackPlane backPlane) {
		Matrix4f positionMatrix = matrixEntry.pose();
		Matrix3f normalMatrix = matrixEntry.normal();
		int color = ARGB.colorFromFloat(alpha, red, green, blue);
		for(SwakozaModelRenderer.TexturedQuad quad : model.quads) {
			Vector3f normal = new Vector3f(quad.normal.x, quad.normal.y, quad.normal.z);
			float depth = Math.max(0.001F, model.posZ2 - model.posZ1);
            BreastDeformation.transformNormal(normal, depth, deformation.x(), deformation.y(), deformation.z());
            normal.mul(normalMatrix).normalize();
			float normalX = normal.x;
			float normalY = normal.y;
			float normalZ = normal.z;
			for(PositionTextureVertex vertex : quad.vertexPositions) {
				float weight = BreastDeformation.weight(vertex.z(), model.posZ2, depth);
                float x = (vertex.x() + deformation.x() * weight) / 16.0F;
				float y = (vertex.y() + deformation.y() * weight) / 16.0F;
				float z = (vertex.z() + deformation.z() * weight) / 16.0F;
				Vector4f transformed = new Vector4f(x, y, z, 1.0F);
				transformed.mul(positionMatrix);
                backPlane.keepInside(transformed);
				consumer.addVertex(transformed.x, transformed.y, transformed.z, color, vertex.texturePositionX(), vertex.texturePositionY(), overlay, light, normalX, normalY, normalZ);
			}
		}
	}
}
