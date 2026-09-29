package com.swakoza.pubertymod.compat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LimbAnimator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class EntityCompat {
    private EntityCompat() {}
    public static Vec3d getPos(Entity entity) { return entity.getPos(); }
    public static World getWorld(Entity entity) { return entity.getWorld(); }
    public static float getPreviousBodyYaw(LivingEntity entity) { return entity.lastBodyYaw; }
    public static float getLimbAnimationProgress(LimbAnimator animator) { return animator.getAnimationProgress(); }
}
