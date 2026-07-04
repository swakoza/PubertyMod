/*
    Puberty Mod is a female gender mod created for Minecraft.
    Copyright (C) 2023 swakoza

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.swakoza.pubertymod.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.render.SwakozaModelRenderer.BreastModelBox;
import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;

public class GenderArmorLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends GenderLayer<S, M> {
	private final EquipmentAssetManager equipmentModelLoader;
	private final TextureAtlas armorTrimsAtlas;
	protected final BreastModelBox lBoobArmor;
	protected final BreastModelBox rBoobArmor;
	protected final BreastModelBox lTrim;
	protected final BreastModelBox rTrim;

	public GenderArmorLayer(RenderLayerParent<S, M> render, EquipmentAssetManager equipmentModelLoader) {
		super(render);
		this.equipmentModelLoader = equipmentModelLoader;
		this.armorTrimsAtlas = (TextureAtlas)Minecraft.getInstance().getTextureManager().getTexture(Sheets.ARMOR_TRIMS_SHEET);
		this.lBoobArmor = new BreastModelBox(64, 32, 16, 17, -4F, 0.0F, 0F, 4, 5, 3, 0.0F, false);
		this.rBoobArmor = new BreastModelBox(64, 32, 20, 17, 0, 0.0F, 0F, 4, 5, 3, 0.0F, false);
		this.lTrim = new BreastModelBox(64, 32, 16, 17, -4F, 0.0F, 0F, 4, 5, 4, 0.001F, false);
		this.rTrim = new BreastModelBox(64, 32, 20, 17, 0, 0.0F, 0F, 4, 5, 4, 0.001F, false);
	}

	@Override
	public void submit(PoseStack matrices, SubmitNodeCollector queue, int light, @Nonnull S state, float limbAngle, float limbDistance) {
		if(state.chestEquipment.isEmpty()) return;
		super.submit(matrices, queue, light, state, limbAngle, limbDistance);
	}

	@Override
	protected void renderBreast(S state, PoseStack matrices, SubmitNodeCollector queue, int light, int overlay, BreastSide side) {
		LivingEntity entity = getEntity(state);
		if(entity == null) return;
		if(entity instanceof net.minecraft.world.entity.decoration.ArmorStand && !this.genderArmor.armorStandsCopySettings()) return;
		renderBreastArmor(state, matrices, queue, light, side);
	}

	protected void renderBreastArmor(S state, PoseStack matrices, SubmitNodeCollector queue, int light, BreastSide side) {
		Equippable equippable = this.armorStack.get(DataComponents.EQUIPPABLE);
		if(equippable == null || equippable.slot() != EquipmentSlot.CHEST) return;

		ResourceKey<EquipmentAsset> assetKey = equippable.assetId().orElse(null);
		if(assetKey == null) return;

		boolean hasGlint = this.armorStack.hasFoil();
		matrices.pushPose();
		try {
			if(getEntity(state) instanceof net.minecraft.world.entity.decoration.ArmorStand && this.entityConfig.hasJacketLayer()
					|| state instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState playerState && playerState.showJacket) {
				matrices.translate(0, 0, -0.015f);
				matrices.scale(1.05f, 1.05f, 1.05f);
			}
			matrices.translate(side == BreastSide.LEFT ? 0.001f : -0.001f, 0.015f, -0.015f);
			matrices.scale(1.05f, 1f, 1f);

			BreastModelBox armorBox = side == BreastSide.LEFT ? this.lBoobArmor : this.rBoobArmor;
			EquipmentClientInfo equipmentModel = this.equipmentModelLoader.get(assetKey);
			for(EquipmentClientInfo.Layer layer : equipmentModel.getLayers(EquipmentClientInfo.LayerType.HUMANOID)) {
				Identifier texture = layer.usePlayerTexture() ? getTexture(state) : layer.getTextureLocation(EquipmentClientInfo.LayerType.HUMANOID);
				if(texture == null) continue;
				int color = getLayerColor(layer, this.armorStack);
				queue.submitCustomGeometry(matrices, RenderLayerCompat.armorCutoutNoCull(texture), (entry, vertexConsumer) -> renderBox(armorBox, entry, vertexConsumer, light, OverlayTexture.NO_OVERLAY,
						net.minecraft.util.ARGB.redFloat(color),
						net.minecraft.util.ARGB.greenFloat(color),
						net.minecraft.util.ARGB.blueFloat(color),
						net.minecraft.util.ARGB.alphaFloat(color)));
				if(hasGlint) {
					queue.submitCustomGeometry(matrices, RenderLayerCompat.armorEntityGlint(), (entry, vertexConsumer) -> renderBox(armorBox, entry, vertexConsumer, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f));
				}
			}

			ArmorTrim trim = this.armorStack.get(DataComponents.TRIM);
			if(trim != null) {
				renderArmorTrim(assetKey, matrices, queue, light, trim, hasGlint, side);
			}
		} catch(Exception e) {
			SwakozaPubertyMod.LOGGER.error("Failed to render breast armor", e);
		} finally {
			matrices.popPose();
		}
	}

	private static int getLayerColor(EquipmentClientInfo.Layer layer, ItemStack stack) {
		if(layer.dyeable().isEmpty()) return CommonColors.WHITE;
		int defaultColor = layer.dyeable().get().colorWhenUndyed().orElse(-6265536);
		return DyedItemColor.getOrDefault(stack, defaultColor);
	}

	protected void renderArmorTrim(ResourceKey<EquipmentAsset> assetKey, PoseStack matrices, SubmitNodeCollector queue, int light,
			ArmorTrim trim, boolean hasGlint, BreastSide side) {
		BreastModelBox trimBox = side == BreastSide.LEFT ? this.lTrim : this.rTrim;
		TextureAtlasSprite sprite = this.armorTrimsAtlas.getSprite(trim.layerAssetId(EquipmentClientInfo.LayerType.HUMANOID.trimAssetPrefix(), assetKey));
		queue.submitCustomGeometry(matrices, Sheets.armorTrimsSheet(trim.pattern().value().decal()), (entry, vertexConsumer) -> {
			VertexConsumer spriteConsumer = sprite.wrap(vertexConsumer);
			renderBox(trimBox, entry, spriteConsumer, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
		});
		if(hasGlint) {
			queue.submitCustomGeometry(matrices, RenderLayerCompat.armorEntityGlint(), (entry, vertexConsumer) -> renderBox(trimBox, entry, vertexConsumer, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f));
		}
	}
}
