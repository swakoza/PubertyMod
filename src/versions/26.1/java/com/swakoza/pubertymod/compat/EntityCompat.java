package com.swakoza.pubertymod.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class EntityCompat {
    private static final Method GET_POS = findNoArgReturnType(Entity.class, Vec3.class, "getEntityPos", "getPos");
    private static final Method GET_WORLD = findNoArgReturnType(Entity.class, Level.class, "getEntityWorld", "getWorld");
    private static final Method GET_LIMB_ANIMATION_PROGRESS = findNoArgReturnType(WalkAnimationState.class, float.class, "getAnimationProgress", "getPos");
    private static final Field PREVIOUS_BODY_YAW = findField(LivingEntity.class, "lastBodyYaw", "prevBodyYaw");

    private EntityCompat() {}

    public static Vec3 getPos(Entity entity) {
        Vec3 pos = invoke(entity, GET_POS, Vec3.class);
        return pos == null ? Vec3.ZERO : pos;
    }

    public static Level getWorld(Entity entity) {
        return invoke(entity, GET_WORLD, Level.class);
    }

    public static float getPreviousBodyYaw(LivingEntity entity) {
        Float value = getFloat(entity, PREVIOUS_BODY_YAW);
        return value == null ? entity.yBodyRot : value;
    }

    public static float getLimbAnimationProgress(WalkAnimationState limbAnimator) {
        Float value = invoke(limbAnimator, GET_LIMB_ANIMATION_PROGRESS, Float.class);
        return value == null ? 0f : value;
    }

    private static Method findNoArgReturnType(Class<?> owner, Class<?> returnType, String... names) {
        for (String name : names) {
            try {
                Method method = owner.getMethod(name);
                if (method.getReturnType() == returnType) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }

        for (Method method : owner.getMethods()) {
            if (method.getParameterCount() == 0
                    && method.getReturnType() == returnType
                    && Modifier.isPublic(method.getModifiers())) {
                return method;
            }
        }
        return null;
    }

    private static <T> T invoke(Entity entity, Method method, Class<T> type) {
        return invoke((Object) entity, method, type);
    }

    private static <T> T invoke(Object target, Method method, Class<T> type) {
        if (target == null || method == null) {
            return null;
        }
        try {
            Object value = method.invoke(target);
            return type.isInstance(value) ? type.cast(value) : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Field findField(Class<?> owner, String... names) {
        for (String name : names) {
            try {
                Field field = owner.getField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static Float getFloat(Object target, Field field) {
        if (target == null || field == null) {
            return null;
        }
        try {
            return field.getFloat(target);
        } catch (IllegalAccessException e) {
            return null;
        }
    }
}
