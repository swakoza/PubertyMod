/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.swakoza.pubertymod.main.networking;

import com.swakoza.pubertymod.main.config.ExtraSetting;
import com.swakoza.pubertymod.main.entitydata.PlayerConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Detailed physics and body-shape values, sent on their own channel next to the original
 * sync packet. Values travel by name, so unknown keys from newer versions are skipped and
 * peers without this channel simply never receive it.
 */
abstract class ExtraSyncPacket implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 64;

    protected final UUID uuid;
    private final Map<String, Float> values;

    protected ExtraSyncPacket(PlayerConfig plr) {
        this.uuid = plr.uuid;
        this.values = new LinkedHashMap<>();
        for (ExtraSetting setting : ExtraSetting.all()) {
            this.values.put(setting.key, plr.getExtra(setting));
        }
    }

    protected ExtraSyncPacket(RegistryFriendlyByteBuf buffer) {
        this.uuid = buffer.readUUID();
        int count = Math.min(buffer.readVarInt(), MAX_ENTRIES);
        this.values = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String key = buffer.readUtf(64);
            float value = buffer.readFloat();
            this.values.put(key, value);
        }
    }

    protected void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(this.uuid);
        buffer.writeVarInt(this.values.size());
        this.values.forEach((key, value) -> {
            buffer.writeUtf(key, 64);
            buffer.writeFloat(value);
        });
    }

    protected void updatePlayerFromPacket(PlayerConfig plr) {
        this.values.forEach((key, value) -> {
            ExtraSetting setting = ExtraSetting.byKey(key);
            if (setting != null) plr.updateExtra(setting, setting.clamp(value));
        });
    }
}
