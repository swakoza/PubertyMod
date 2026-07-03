package com.swakoza.pubertymod.compat;

import java.lang.reflect.Method;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;

public final class PlayerCompat {
    private static final Method IS_MODEL_PART_VISIBLE = find("isModelPartVisible", "isPartVisible");

    private PlayerCompat() {}

    public static boolean isModelPartVisible(Player player, PlayerModelPart part) {
        if (player == null || part == null || IS_MODEL_PART_VISIBLE == null) {
            return true;
        }
        try {
            Object value = IS_MODEL_PART_VISIBLE.invoke(player, part);
            return value instanceof Boolean bool ? bool : true;
        } catch (ReflectiveOperationException e) {
            return true;
        }
    }

    private static Method find(String... names) {
        for (String name : names) {
            try {
                return Player.class.getMethod(name, PlayerModelPart.class);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
