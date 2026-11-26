package com.swakoza.pubertymod.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class EntityCompat {
    private EntityCompat() {}
    public static Vec3 getPos(Entity entity) { return entity.position(); }
    public static Level getWorld(Entity entity) { return entity.level(); }
    public static float getPreviousBodyYaw(LivingEntity entity) { return entity.yBodyRotO; }
    public static float getLimbAnimationProgress(WalkAnimationState animator) { return animator.position(); }
    public static boolean isSwinging(LivingEntity entity) { return entity.isSwinging(); }
    /** Grows while an arm swing runs; a drop means a new swing started. */
    public static float swingPhase(LivingEntity entity) { return entity.getSwingAnimation(0.0F); }
}
