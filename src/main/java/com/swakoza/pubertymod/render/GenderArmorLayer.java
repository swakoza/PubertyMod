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

import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.render.SwakozaModelRenderer.BreastModelBox;
import javax.annotation.Nonnull;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentModelLoader;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

public class GenderArmorLayer<S extends BipedEntityRenderState, M extends BipedEntityModel<S>> extends GenderLayer<S, M> {
	private final EquipmentModelLoader equipmentModelLoader;
	private final SpriteAtlasTexture armorTrimsAtlas;
	protected final BreastModelBox lBoobArmor;
	protected final BreastModelBox rBoobArmor;
	protected final BreastModelBox lTrim;
	protected final BreastModelBox rTrim;

	public GenderArmorLayer(FeatureRendererContext<S, M> render, EquipmentModelLoader equipmentModelLoader) {
		super(render);
		this.equipmentModelLoader = equipmentModelLoader;
		this.armorTrimsAtlas = (SpriteAtlasTexture)MinecraftClient.getInstance().getTextureManager().getTexture(TexturedRenderLayers.ARMOR_TRIMS_ATLAS_TEXTURE);
		this.lBoobArmor = new BreastModelBox(64, 32, 16, 17, -4F, 0.0F, 0F, 4, 5, 3, 0.0F, false);
		this.rBoobArmor = new BreastModelBox(64, 32, 20, 17, 0, 0.0F, 0F, 4, 5, 3, 0.0F, false);
		this.lTrim = new BreastModelBox(64, 32, 16, 17, -4F, 0.0F, 0F, 4, 5, 4, 0.001F, false);
		this.rTrim = new BreastModelBox(64, 32, 20, 17, 0, 0.0F, 0F, 4, 5, 4, 0.001F, false);
	}

	@Override
	public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, @Nonnull S state, float limbAngle, float limbDistance) {
		if(state.equippedChestStack.isEmpty()) return;
		super.render(matrices, queue, light, state, limbAngle, limbDistance);
	}

	@Override
	protected void renderBreast(S state, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, BreastSide side) {
		LivingEntity entity = getEntity(state);
		if(entity == null) return;
		if(entity instanceof net.minecraft.entity.decoration.ArmorStandEntity && !this.genderArmor.armorStandsCopySettings()) return;
		renderBreastArmor(state, matrices, queue, light, side);
	}

	protected void renderBreastArmor(S state, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, BreastSide side) {
		EquippableComponent equippable = this.armorStack.get(DataComponentTypes.EQUIPPABLE);
		if(equippable == null || equippable.slot() != EquipmentSlot.CHEST) return;

		RegistryKey<EquipmentAsset> assetKey = equippable.assetId().orElse(null);
		if(assetKey == null) return;

		boolean hasGlint = this.armorStack.hasGlint();
		matrices.push();
		try {
			if(getEntity(state) instanceof net.minecraft.entity.decoration.ArmorStandEntity && this.entityConfig.hasJacketLayer()
					|| state instanceof net.minecraft.client.render.entity.state.PlayerEntityRenderState playerState && playerState.jacketVisible) {
				matrices.translate(0, 0, -0.015f);
				matrices.scale(1.05f, 1.05f, 1.05f);
			}
			matrices.translate(side == BreastSide.LEFT ? 0.001f : -0.001f, 0.015f, -0.015f);
			matrices.scale(1.05f, 1f, 1f);

			BreastModelBox armorBox = side == BreastSide.LEFT ? this.lBoobArmor : this.rBoobArmor;
			EquipmentModel equipmentModel = this.equipmentModelLoader.get(assetKey);
			for(EquipmentModel.Layer layer : equipmentModel.getLayers(EquipmentModel.LayerType.HUMANOID)) {
				Identifier texture = layer.usePlayerTexture() ? getTexture(state) : layer.getFullTextureId(EquipmentModel.LayerType.HUMANOID);
				if(texture == null) continue;
				int color = getLayerColor(layer, this.armorStack);
				queue.submitCustom(matrices, RenderLayerCompat.armorCutoutNoCull(texture), (entry, vertexConsumer) -> renderBox(armorBox, entry, vertexConsumer, light, OverlayTexture.DEFAULT_UV,
						net.minecraft.util.math.ColorHelper.getRedFloat(color),
						net.minecraft.util.math.ColorHelper.getGreenFloat(color),
						net.minecraft.util.math.ColorHelper.getBlueFloat(color),
						net.minecraft.util.math.ColorHelper.getAlphaFloat(color)));
				if(hasGlint) {
					queue.submitCustom(matrices, RenderLayerCompat.armorEntityGlint(), (entry, vertexConsumer) -> renderBox(armorBox, entry, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f));
				}
			}

			ArmorTrim trim = this.armorStack.get(DataComponentTypes.TRIM);
			if(trim != null) {
				renderArmorTrim(assetKey, matrices, queue, light, trim, hasGlint, side);
			}
		} catch(Exception e) {
			SwakozaPubertyMod.LOGGER.error("Failed to render breast armor", e);
		} finally {
			matrices.pop();
		}
	}

	private static int getLayerColor(EquipmentModel.Layer layer, ItemStack stack) {
		if(layer.dyeable().isEmpty()) return Colors.WHITE;
		int defaultColor = layer.dyeable().get().colorWhenUndyed().orElse(-6265536);
		return DyedColorComponent.getColor(stack, defaultColor);
	}

	protected void renderArmorTrim(RegistryKey<EquipmentAsset> assetKey, MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
			ArmorTrim trim, boolean hasGlint, BreastSide side) {
		BreastModelBox trimBox = side == BreastSide.LEFT ? this.lTrim : this.rTrim;
		Sprite sprite = this.armorTrimsAtlas.getSprite(trim.getTextureId(EquipmentModel.LayerType.HUMANOID.getTrimsDirectory(), assetKey));
		queue.submitCustom(matrices, TexturedRenderLayers.getArmorTrims(trim.pattern().value().decal()), (entry, vertexConsumer) -> {
			VertexConsumer spriteConsumer = sprite.getTextureSpecificVertexConsumer(vertexConsumer);
			renderBox(trimBox, entry, spriteConsumer, light, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f);
		});
		if(hasGlint) {
			queue.submitCustom(matrices, RenderLayerCompat.armorEntityGlint(), (entry, vertexConsumer) -> renderBox(trimBox, entry, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f));
		}
	}
}
