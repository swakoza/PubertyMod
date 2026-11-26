/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.palette.PalettedTextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.trim.ArmorTrim;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Armor glint and trim drawing for 26.3: trims are paletted textures prepared by
 * {@link PalettedTextureManager}. The key type that knows how to prepare them is private in
 * {@link EquipmentLayerRenderer}, so it is used through reflection, exactly as vanilla does.
 */
public final class ArmorRenderCompat {
    private static Constructor<?> keyConstructor;
    private static Method prepare;
    private static boolean failed;

    private ArmorRenderCompat() {}

    public static RenderType layerGlint(Identifier layerTexture) {
        return RenderTypes.armorCutoutNoCullGlint(layerTexture);
    }

    public static RenderType trimGlint() {
        return RenderTypes.trimmedArmorGlint();
    }

    public static void submitTrim(SubmitNodeCollector queue, PoseStack matrices, ArmorTrim trim, ResourceKey<EquipmentAsset> assetKey,
                                  EquipmentClientInfo equipmentInfo, SubmitNodeCollector.CustomGeometryRenderer draw) {
        PalettedTextureManager.Handle handle = handle(trim, equipmentInfo);
        if (handle == null) return;
        queue.submitCustomGeometry(matrices, RenderTypes.armorTrim(handle.textureLocation(), trim.pattern().value().decal()),
                (entry, vertexConsumer) -> draw.render(entry, handle.wrap(vertexConsumer)));
    }

    private static PalettedTextureManager.Handle handle(ArmorTrim trim, EquipmentClientInfo equipmentInfo) {
        if (failed) return null;
        try {
            if (keyConstructor == null) {
                Class<?> keyClass = Class.forName(EquipmentLayerRenderer.class.getName() + "$TrimTextureKey");
                keyConstructor = keyClass.getDeclaredConstructor(ArmorTrim.class, EquipmentClientInfo.LayerType.class, EquipmentClientInfo.class);
                keyConstructor.setAccessible(true);
                prepare = keyClass.getDeclaredMethod("getOrPrepareTexture", PalettedTextureManager.class);
                prepare.setAccessible(true);
            }
            Object key = keyConstructor.newInstance(trim, EquipmentClientInfo.LayerType.HUMANOID, equipmentInfo);
            return (PalettedTextureManager.Handle) prepare.invoke(key, Minecraft.getInstance().getPalettedTextureManager());
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            SwakozaPubertyMod.LOGGER.warn("Armor trims on breasts are disabled: cannot prepare trim textures", e);
            return null;
        }
    }
}
