/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.api;

import com.swakoza.pubertymod.main.config.Configuration;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import com.swakoza.pubertymod.main.SwakozaPubertyMod;
import com.swakoza.pubertymod.main.Gender;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;

@SuppressWarnings("unused")
public class SwakozaAPI {

    private static final Map<Item, IGenderArmor> GENDER_ARMORS = new HashMap<>();

    /**
     * Add custom physics resistance attributes to a chestplate
     *
     * @param  item  the item that you are linking this {@link IGenderArmor} to
     * @param  genderArmor the class implementing the {@link IGenderArmor} to apply to the item
     * @see    IGenderArmor
     */
    public static void addGenderArmor(Item item, IGenderArmor genderArmor) {
        GENDER_ARMORS.put(item, genderArmor);
    }

    /**
     * Get the config for a {@link Player}
     *
     * @param  uuid  the uuid of the target {@link Player}
     * @see    IGenderArmor
     */
    public static @Nullable PlayerConfig getPlayerById(UUID uuid) {
        return SwakozaPubertyMod.getPlayerById(uuid);
    }

    /**
     * Get the player's {@link Gender}
     *
     * @param  uuid  the uuid of the target {@link Player}.
     * @see    Gender
     */
    public static @Nonnull Gender getPlayerGender(UUID uuid) {
        PlayerConfig cfg = SwakozaPubertyMod.getPlayerById(uuid);
        if(cfg == null) return Configuration.GENDER.getDefault();
        return cfg.getGender();
    }

    /**
     * <p>Load the cached Gender Settings file for the specified {@link UUID}</p>
     *
     * <p>You should avoid using this unless you need to, as the mod will do this for you when loading a player entity.</p>
     *
     * @param  uuid  the uuid of the target {@link Player}
     * @param  markForSync true if you want to send the gender settings to the server upon loading.
     */
    public static Future<Optional<PlayerConfig>> loadGenderInfo(UUID uuid, boolean markForSync) {
        return SwakozaPubertyMod.loadGenderInfo(uuid, markForSync);
    }

    /**
     * Get every registered {@link IGenderArmor custom armor configuration}
     *
     * @implNote This does not provide vanilla armor configurations; see {@link com.swakoza.pubertymod.render.armor.SimpleGenderArmor} for that.
     */
    public static Map<Item, IGenderArmor> getGenderArmors() {
        return GENDER_ARMORS;
    }

}
