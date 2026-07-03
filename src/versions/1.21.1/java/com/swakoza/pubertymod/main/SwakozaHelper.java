package com.swakoza.pubertymod.main;

import com.swakoza.pubertymod.api.IGenderArmor;
import com.swakoza.pubertymod.api.SwakozaAPI;
import com.swakoza.pubertymod.compat.PlayerCompat;
import com.swakoza.pubertymod.main.entitydata.Breasts;
import com.swakoza.pubertymod.main.entitydata.EntityConfig;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import com.swakoza.pubertymod.render.armor.EmptyGenderArmor;
import com.swakoza.pubertymod.render.armor.SimpleGenderArmor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

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
        return (float)ThreadLocalRandom.current().nextDouble(min, (double)max + 1);
    }

    public static IGenderArmor getArmorConfig(ItemStack stack) {
        if (stack.isEmpty()) return EmptyGenderArmor.INSTANCE;
        if (SwakozaAPI.getGenderArmors().get(stack.getItem()) != null) return SwakozaAPI.getGenderArmors().get(stack.getItem());

        if (stack.getItem() instanceof ArmorItem armorItem && armorItem.getSlotType() == EquipmentSlot.CHEST) {
            if (stack.isOf(Items.LEATHER_CHESTPLATE)) return SimpleGenderArmor.LEATHER;
            if (stack.isOf(Items.CHAINMAIL_CHESTPLATE)) return SimpleGenderArmor.CHAIN_MAIL;
            if (stack.isOf(Items.GOLDEN_CHESTPLATE)) return SimpleGenderArmor.GOLD;
            if (stack.isOf(Items.IRON_CHESTPLATE)) return SimpleGenderArmor.IRON;
            if (stack.isOf(Items.DIAMOND_CHESTPLATE)) return SimpleGenderArmor.DIAMOND;
            if (stack.isOf(Items.NETHERITE_CHESTPLATE)) return SimpleGenderArmor.NETHERITE;
            return SimpleGenderArmor.FALLBACK;
        }

        return EmptyGenderArmor.INSTANCE;
    }

    @Environment(EnvType.CLIENT)
    public static void drawCenteredText(DrawContext ctx, TextRenderer textRenderer, Text text, int x, int y, int color) {
        int centeredX = x - textRenderer.getWidth(text) / 2;
        ctx.drawText(textRenderer, text, centeredX, y, ensureOpaqueArgb(color), false);
    }

    @Environment(EnvType.CLIENT)
    public static void drawScrollableText(DrawContext context, TextRenderer textRenderer, Text text, int left, int top, int right, int bottom, int color) {
        int textColor = ensureOpaqueArgb(color);
        int i = textRenderer.getWidth(text);
        int j = (top + bottom - 9) / 2 + 1;
        int k = right - left;
        if (i > k) {
            int l = i - k;
            double d = (double)Util.getMeasuringTimeMs() / 1000.0;
            double e = Math.max((double)l * 0.5, 3.0);
            double f = Math.sin(1.5707963267948966 * Math.cos(6.283185307179586 * d / e)) / 2.0 + 0.5;
            double g = MathHelper.lerp(f, 0.0, (double)l);
            context.enableScissor(left, top, right, bottom);
            context.drawText(textRenderer, text, left - (int)g, j, textColor, false);
            context.disableScissor();
        } else {
            drawCenteredText(context, textRenderer, text, (left + right) / 2, j, textColor);
        }
    }

    public static void writeToNbt(@Nonnull PlayerEntity player, @Nonnull PlayerConfig config, @Nonnull ItemStack armor) {
        NbtCompound nbt = new NbtCompound();
        nbt.putFloat("BreastSize", config.getGender().canHaveBreasts() && config.showBreastsInArmor() ? config.getBustSize() : 0f);
        nbt.putFloat("Cleavage", config.getBreasts().getCleavage());
        nbt.putBoolean("Uniboob", config.getBreasts().isUniboob());
        nbt.putFloat("XOffset", config.getBreasts().getXOffset());
        nbt.putFloat("YOffset", config.getBreasts().getYOffset());
        nbt.putFloat("ZOffset", config.getBreasts().getZOffset());
        nbt.putBoolean("Jacket", PlayerCompat.isModelPartVisible(player, PlayerModelPart.JACKET));
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, armor, customData -> customData.put("pubertymod", nbt));
    }
}
