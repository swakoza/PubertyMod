/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.trim.ArmorTrim;

/** Armor glint and trim drawing. 26.1/26.2: trims come from the trim atlas; 26.3 uses paletted textures. */
public final class ArmorRenderCompat {
    private ArmorRenderCompat() {}

    public static RenderType layerGlint(Identifier layerTexture) {
        return RenderTypes.armorEntityGlint();
    }

    public static RenderType trimGlint() {
        return RenderTypes.armorEntityGlint();
    }

    public static void submitTrim(SubmitNodeCollector queue, PoseStack matrices, ArmorTrim trim, ResourceKey<EquipmentAsset> assetKey,
                                  EquipmentClientInfo equipmentInfo, SubmitNodeCollector.CustomGeometryRenderer draw) {
        TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(Sheets.ARMOR_TRIMS_SHEET);
        TextureAtlasSprite sprite = atlas.getSprite(trim.layerAssetId(EquipmentClientInfo.LayerType.HUMANOID.trimAssetPrefix(), assetKey));
        queue.submitCustomGeometry(matrices, Sheets.armorTrimsSheet(trim.pattern().value().decal()),
                (entry, vertexConsumer) -> draw.render(entry, sprite.wrap(vertexConsumer)));
    }
}
