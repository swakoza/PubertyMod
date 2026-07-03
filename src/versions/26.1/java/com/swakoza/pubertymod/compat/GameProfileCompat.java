package com.swakoza.pubertymod.compat;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Method;
import java.util.UUID;

public final class GameProfileCompat {
    private static final Method ID = find("id", "getId");
    private static final Method NAME = find("name", "getName");

    private GameProfileCompat() {}

    public static UUID id(GameProfile profile) {
        return invoke(profile, ID, UUID.class);
    }

    public static String name(GameProfile profile) {
        return invoke(profile, NAME, String.class);
    }

    private static Method find(String... names) {
        for (String name : names) {
            try {
                return GameProfile.class.getMethod(name);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static <T> T invoke(GameProfile profile, Method method, Class<T> type) {
        if (profile == null || method == null) {
            return null;
        }
        try {
            Object value = method.invoke(profile);
            return type.isInstance(value) ? type.cast(value) : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
