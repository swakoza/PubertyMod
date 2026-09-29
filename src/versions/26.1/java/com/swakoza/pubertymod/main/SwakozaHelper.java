/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.main;

import com.swakoza.pubertymod.api.IGenderArmor;
import com.swakoza.pubertymod.api.SwakozaAPI;
import com.swakoza.pubertymod.compat.PlayerCompat;
import com.swakoza.pubertymod.main.entitydata.Breasts;
import com.swakoza.pubertymod.main.entitydata.EntityConfig;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import com.swakoza.pubertymod.render.armor.SimpleGenderArmor;
import com.swakoza.pubertymod.render.armor.EmptyGenderArmor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.equipment.Equippable;
import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class SwakozaHelper {
    public static int ensureOpaqueArgb(int color) {
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }

    public static int randInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
    public static float randFloat(float min, float max) {
        return (float) ThreadLocalRandom.current().nextDouble(min, (double) max + 1);
    }

    public static IGenderArmor getArmorConfig(ItemStack stack) {
        if (stack.isEmpty()) {
            return EmptyGenderArmor.INSTANCE;
        }

        if (SwakozaAPI.getGenderArmors().get(stack.getItem()) != null) {
            return SwakozaAPI.getGenderArmors().get(stack.getItem());
        } else {
            //TODO: Fabric Alternative to Capabilities? Maybe someone can help with this?
            Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
            if (equippable != null && equippable.slot() == EquipmentSlot.CHEST) {
                // Start by checking if it is a vanilla chestplate as we have custom configurations for those we check against.
                if (stack.is(Items.LEATHER_CHESTPLATE)) {
                    return SimpleGenderArmor.LEATHER;
                } else if (stack.is(Items.CHAINMAIL_CHESTPLATE)) {
                    return SimpleGenderArmor.CHAIN_MAIL;
                } else if (stack.is(Items.GOLDEN_CHESTPLATE)) {
                    return SimpleGenderArmor.GOLD;
                } else if (stack.is(Items.IRON_CHESTPLATE)) {
                    return SimpleGenderArmor.IRON;
                } else if (stack.is(Items.DIAMOND_CHESTPLATE)) {
                    return SimpleGenderArmor.DIAMOND;
                } else if (stack.is(Items.NETHERITE_CHESTPLATE)) {
                    return SimpleGenderArmor.NETHERITE;
                }
                //Otherwise just fallback to our default armor implementation
                return SimpleGenderArmor.FALLBACK;
            }
            //If it is not an armor item default as if "nothing is being worn that covers the breast area"
            // this might not be fully accurate and may need some tweaks but in general is likely relatively
            // close to the truth of if it should render or not. This covers cases such as the elytra and
            // other wearables
            return EmptyGenderArmor.INSTANCE;
        }
    }

    @Environment(EnvType.CLIENT)
    public static void drawCenteredText(GuiGraphicsExtractor ctx, Font textRenderer, Component text, int x, int y, int color) {
        int centeredX = x - textRenderer.width(text) / 2;
        ctx.text(textRenderer, text, centeredX, y, ensureOpaqueArgb(color), false);
    }

    @Environment(EnvType.CLIENT)
    public static void drawScrollableText(GuiGraphicsExtractor context, Font textRenderer, Component text, int left, int top, int right, int bottom, int color) {
        int textColor = ensureOpaqueArgb(color);
        int i = textRenderer.width(text);
        int var10000 = top + bottom;
        Objects.requireNonNull(textRenderer);
        int j = (var10000 - 9) / 2 + 1;
        int k = right - left;
        if (i > k) {
            int l = i - k;
            double d = (double) Util.getMillis() / 1000.0;
            double e = Math.max((double)l * 0.5, 3.0);
            double f = Math.sin(1.5707963267948966 * Math.cos(6.283185307179586 * d / e)) / 2.0 + 0.5;
            double g = Mth.lerp(f, 0.0, (double)l);
            context.enableScissor(left, top, right, bottom);
            context.text(textRenderer, text, left - (int)g, j, textColor, false);
            context.disableScissor();
        } else {
            drawCenteredText(context, textRenderer, text, (left + right) / 2, j, textColor);
        }
    }

    /**
     * <p>Write a player's gender config to NBT on the given item stack.</p>
     *
     * <p>This only copies enough data to render breasts similarly to how they'd appear on the given player, which includes:</p>
     * <ul>
     *     <li>{@link EntityConfig#getBustSize() Breast size}</li>
     *     <li>{@link Breasts#getCleavage() Cleavage}</li>
     *     <li>{@link Breasts#isUniboob() Uniboob}</li>
     *     <li>{@link Breasts#getXOffset() X}, {@link Breasts#getYOffset() Y}, and {@link Breasts#getZOffset() Z} offsets</li>
     *     <li>Whether the {@link Player#isPartVisible player's jacket layer is visible}</li>
     * </ul>
     *
     * @see EntityConfig#readFromStack
     */
    public static void writeToNbt(@Nonnull Player player, @Nonnull PlayerConfig config, @Nonnull ItemStack armor) {
        CompoundTag nbt = new CompoundTag();
        nbt.putFloat("BreastSize", config.getGender().canHaveBreasts() && config.showBreastsInArmor() ? config.getBustSize() : 0f);
        nbt.putFloat("Cleavage", config.getBreasts().getCleavage());
        nbt.putBoolean("Uniboob", config.getBreasts().isUniboob());
        nbt.putFloat("XOffset", config.getBreasts().getXOffset());
        nbt.putFloat("YOffset", config.getBreasts().getYOffset());
        nbt.putFloat("ZOffset", config.getBreasts().getZOffset());
        // note that we also copy this to properly copy the exact size, as the player model will push the breast armor
        // layer out a bit if they have a visible jacket layer
        nbt.putBoolean("Jacket", PlayerCompat.isModelPartVisible(player, PlayerModelPart.JACKET));
        CustomData.update(DataComponents.CUSTOM_DATA, armor, customData -> customData.put("pubertymod", nbt));
    }
}
