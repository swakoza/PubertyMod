/*
    Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
    Copyright (C) 2023 WildfireRomeo

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.swakoza.pubertymod.physics;

import com.swakoza.pubertymod.api.IGenderArmor;
import com.swakoza.pubertymod.compat.EntityCompat;
import com.swakoza.pubertymod.compat.PalPhysicsCompat;
import com.swakoza.pubertymod.main.config.ExtraSetting;
import com.swakoza.pubertymod.main.entitydata.EntityConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Breast motion as a damped spring attached to the chest.
 *
 * <p>Every tick the acceleration of the chest is measured in the torso frame and applied as an
 * inertial force (the breast lags behind whatever the torso does), together with gravity.
 * Sources of chest acceleration:</p>
 * <ul>
 *     <li>movement of the entity in the world (jumps, landings, starting, stopping, strafing, vehicles);</li>
 *     <li>footsteps: Minecraft moves players at constant speed without any vertical bob, so the
 *     bob, sway and surge of a human gait are reconstructed from the limb animation;</li>
 *     <li>torso turns;</li>
 *     <li>poses and animations (sneaking, emotes) through the rendered or animated torso pose.</li>
 * </ul>
 *
 * <p>Displacements are simulated in model pixels and exposed in the units the renderer expects
 * (see {@link #RENDER_SCALE_X}, {@link #RENDER_SCALE_Y}, {@link #RENDER_SCALE_Z}).</p>
 */
public class BreastPhysics {
	// Renderer multiplies positions by these factors to get pixels (GenderLayer#setupTransformations).
	public static final float RENDER_SCALE_X = 0.4F;
	public static final float RENDER_SCALE_Y = 0.5F;
	public static final float RENDER_SCALE_Z = 0.25F;

	private static final float TICK = 1.0F / 20.0F;
	private static final int SUBSTEPS = 6;
	private static final float PIXELS_PER_BLOCK = 16.0F;
	private static final float GRAVITY = 9.81F * PIXELS_PER_BLOCK; // px/s²
	/**
	 * Minecraft gravity is 32 blocks/s² (over three times real gravity) and its jumps and landings are
	 * correspondingly violent; world accelerations are scaled to what a real body would feel.
	 */
	private static final float WORLD_TO_REAL = 9.81F / 32.0F;
	/** Chest acceleration above this (blocks/s²) is treated as absorbed by the legs. */
	private static final float MAX_BODY_ACCELERATION = 25.0F;
	/** Distance of the breast from the spine, used for turns (blocks). */
	private static final float TURN_RADIUS = 0.16F;
	private static final float TELEPORT_DISTANCE = 4.0F;

	// Base travel limits in pixels (scaled by the "limit" setting).
	private static final float LIMIT_DOWN = 1.5F;
	private static final float LIMIT_UP = 1.1F;
	private static final float LIMIT_SIDE = 1.2F;
	private static final float LIMIT_DEPTH = 0.8F;

	private final EntityConfig entityConfig;

	// Spring state, model frame: +X entity's left, +Y down, +Z backwards. Pixels and pixels/second.
	private float dispX, dispY, dispZ;
	private float velX, velY, velZ;

	// Output (render units) for interpolation.
	private float positionX, positionY, positionZ, prePositionX, prePositionY, prePositionZ;
	private float breastSize, preBreastSize;

	// Motion history.
	private Vec3 prePos;
	private Vec3 preVelocity;
	private float filteredAccX, filteredAccY, filteredAccZ;
	private float previousBodyYaw;
	private float previousYawVelocity;
	private float previousGaitX, previousGaitY, previousGaitZ;
	private float previousGaitVelX, previousGaitVelY, previousGaitVelZ;
	private float previousProbeX, previousProbeY, previousProbeZ;
	private float previousProbeVelX, previousProbeVelY, previousProbeVelZ;
	private boolean hasProbe;
	private boolean wasSwinging;
	private float previousSwingPhase;

	public BreastPhysics(EntityConfig entityConfig) {
		this.entityConfig = entityConfig;
	}

	public void update(LivingEntity entity, IGenderArmor armor) {
		update(entity, armor, null, true);
	}

	public void update(LivingEntity entity, IGenderArmor armor, PalPhysicsCompat.AttachmentPose animationPose, boolean left) {
		if (entity instanceof ArmorStand && !armor.armorStandsCopySettings()) {
			// optimization: skip physics on armor stands that either don't have a chestplate,
			// or have a chestplate we wouldn't copy player settings to
			return;
		}

		this.prePositionX = this.positionX;
		this.prePositionY = this.positionY;
		this.prePositionZ = this.positionZ;
		this.preBreastSize = this.breastSize;

		// ---- size (armor squeezes it a little) ----
		float targetBreastSize = this.entityConfig.getBustSize();
		float resistance = Mth.clamp(armor.physicsResistance(), 0, 1);
		float tightness = Mth.clamp(armor.tightness(), 0, 1);
		if (this.entityConfig.getArmorPhysicsOverride()) {
			resistance = 0;
			tightness = 0;
		}
		if (!this.entityConfig.getGender().canHaveBreasts()) {
			targetBreastSize = 0;
		} else {
			targetBreastSize *= 1 - 0.15F * tightness;
		}
		this.breastSize += (targetBreastSize - this.breastSize) * 0.5F;

		Vec3 pos = EntityCompat.getPos(entity);
		if (this.prePos == null || pos.distanceToSqr(this.prePos) > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
			reset(entity, pos);
			return;
		}

		// ---- settings ----
		float size = targetBreastSize;
		float strength = this.entityConfig.getBounceMultiplier() * 3.0F;               // 0..3, default ≈1
		float softness = (this.entityConfig.getFloppiness() - 0.1F) / 1.1F;            // 0..1
		float damping = setting(ExtraSetting.PHYSICS_DAMPING);
		float walk = setting(ExtraSetting.PHYSICS_WALK);
		float vertical = setting(ExtraSetting.PHYSICS_VERTICAL);
		float horizontal = setting(ExtraSetting.PHYSICS_HORIZONTAL);
		float depth = setting(ExtraSetting.PHYSICS_DEPTH);
		float twist = setting(ExtraSetting.PHYSICS_TWIST);
		float sag = setting(ExtraSetting.PHYSICS_SAG);
		float limit = setting(ExtraSetting.PHYSICS_LIMIT);

		// Heavier breasts are slower and move further; small ones barely move.
		float sizeFactor = Mth.clamp(size / 0.6F, 0.0F, 3.4F);
		float mass = (float) Math.pow(sizeFactor, 0.8);
		float frequency = Mth.lerp(softness, 4.4F, 1.6F) / (float) Math.sqrt(0.6F + 0.4F * sizeFactor);
		frequency *= 1.0F + 2.0F * resistance;                                         // armor holds them in place
		boolean dual = !this.entityConfig.getBreasts().isUniboob();
		if (dual) frequency *= left ? 0.97F : 1.03F;                                   // sides drift slightly apart
		float omega = (float) (2 * Math.PI * frequency);
		float stiffness = omega * omega;
		float dampingCoefficient = 2 * damping * omega;
		float gain = strength * mass * (1 - resistance);

		// ---- chest acceleration, model frame (blocks/s²) ----
		Vec3 velocity = pos.subtract(this.prePos);
		Vec3 acceleration = velocity.subtract(this.preVelocity).scale(WORLD_TO_REAL / (TICK * TICK));
		this.prePos = pos;
		this.preVelocity = velocity;

		float yawRad = entity.yBodyRot * Mth.DEG_TO_RAD;
		float sin = Mth.sin(yawRad);
		float cos = Mth.cos(yawRad);
		float accForward = (float) (acceleration.x * -sin + acceleration.z * cos);
		float accRight = (float) (acceleration.x * -cos + acceleration.z * -sin);
		float accUp = (float) acceleration.y;
		// Remote players move in network steps; smooth out the jitter.
		this.filteredAccX = Mth.lerp(0.6F, this.filteredAccX, -accRight);
		this.filteredAccY = Mth.lerp(0.6F, this.filteredAccY, -accUp);
		this.filteredAccZ = Mth.lerp(0.6F, this.filteredAccZ, -accForward);
		float bodyX = this.filteredAccX;
		float bodyY = this.filteredAccY;
		float bodyZ = this.filteredAccZ;

		// Footsteps.
		// Softer breasts follow the quick footstep rhythm less by nature; compensate a little.
		float[] gait = gaitAcceleration(entity, velocity, walk * (0.8F + 0.6F * softness));
		bodyX += gait[0];
		bodyY += gait[1];
		bodyZ += gait[2];

		float magnitude = Mth.sqrt(bodyX * bodyX + bodyY * bodyY + bodyZ * bodyZ);
		if (magnitude > MAX_BODY_ACCELERATION) {
			float scale = MAX_BODY_ACCELERATION / magnitude;
			bodyX *= scale;
			bodyY *= scale;
			bodyZ *= scale;
		}

		// Torso turns: tangential lag and a little centrifugal push forward.
		float yawVelocity = Mth.wrapDegrees(entity.yBodyRot - this.previousBodyYaw) * Mth.DEG_TO_RAD / TICK;
		float yawAcceleration = Mth.clamp((yawVelocity - this.previousYawVelocity) / TICK, -150.0F, 150.0F);
		this.previousBodyYaw = entity.yBodyRot;
		this.previousYawVelocity = yawVelocity;
		bodyX += -TURN_RADIUS * yawAcceleration * twist;
		bodyZ += TURN_RADIUS * Math.min(yawVelocity * yawVelocity, 120.0F) * twist;

		// Pose changes and animations, measured on the breast itself.
		float[] probe = probeAcceleration(animationPose, left);
		bodyX += probe[0];
		bodyY += probe[1];
		bodyZ += probe[2];

		// Arm swings shake the chest a little.
		boolean swinging = EntityCompat.isSwinging(entity);
		float swingPhase = EntityCompat.swingPhase(entity);
		boolean swingStarted = swinging && (!this.wasSwinging || swingPhase < this.previousSwingPhase);
		if (swingStarted) {
			bodyY -= 8.0F;
			bodyX += left ? 4.0F : -4.0F;
		}
		this.wasSwinging = swinging;
		this.previousSwingPhase = swingPhase;

		// Into the (possibly bent) torso frame.
		Vector3f local = new Vector3f(bodyX, bodyY, bodyZ);
		Vector3f gravity = new Vector3f(0, 1, 0);
		if (animationPose != null) {
			animationPose.neutralToLocal().transform(local);
			gravity.set(animationPose.gravityX(), animationPose.gravityY(), animationPose.gravityZ());
		}
		poseGravity(entity, gravity);
		float buoyancy = entity.isUnderWater() ? 0.25F : 1.0F;
		if (entity.isUnderWater()) dampingCoefficient += omega * 0.8F;

		float sagFactor = sag * buoyancy * Mth.clamp((float) Math.sqrt(sizeFactor), 0.0F, 1.6F) * 0.55F;
		float forceX = -local.x * PIXELS_PER_BLOCK * gain * horizontal + gravity.x * GRAVITY * sagFactor;
		float forceY = -local.y * PIXELS_PER_BLOCK * gain * vertical + gravity.y * GRAVITY * sagFactor;
		float forceZ = -local.z * PIXELS_PER_BLOCK * gain * depth + gravity.z * GRAVITY * sagFactor;
		if (!this.entityConfig.hasBreastPhysics() || size < 0.02F) {
			forceX = forceY = forceZ = 0;
		}

		// ---- integrate (semi-implicit Euler) ----
		float h = TICK / SUBSTEPS;
		float hardLimit = 3.0F * limit * LIMIT_DOWN;
		// Damping is applied exactly (exponential decay) so heavy damping can never become unstable.
		float decay = (float) Math.exp(-dampingCoefficient * h);
		for (int i = 0; i < SUBSTEPS; i++) {
			this.velX = (this.velX + (forceX - stiffness * this.dispX) * h) * decay;
			this.velY = (this.velY + (forceY - stiffness * this.dispY) * h) * decay;
			this.velZ = (this.velZ + (forceZ - stiffness * this.dispZ) * h) * decay;
			this.dispX = Mth.clamp(this.dispX + this.velX * h, -hardLimit, hardLimit);
			this.dispY = Mth.clamp(this.dispY + this.velY * h, -hardLimit, hardLimit);
			this.dispZ = Mth.clamp(this.dispZ + this.velZ * h, -hardLimit, hardLimit);
		}
		if (!Float.isFinite(this.dispX + this.dispY + this.dispZ + this.velX + this.velY + this.velZ)) {
			this.dispX = this.dispY = this.dispZ = this.velX = this.velY = this.velZ = 0;
		}

		// ---- soft limits, then render units ----
		float outX = softLimit(this.dispX, LIMIT_SIDE * limit, LIMIT_SIDE * limit);
		float outY = softLimit(this.dispY, LIMIT_UP * limit, LIMIT_DOWN * limit);
		float outZ = softLimit(this.dispZ, LIMIT_DEPTH * limit, LIMIT_DEPTH * limit);
		this.positionX = outX / RENDER_SCALE_X;
		this.positionY = outY / RENDER_SCALE_Y;
		this.positionZ = outZ / RENDER_SCALE_Z;
	}

	private float setting(ExtraSetting setting) {
		return this.entityConfig.getExtra(setting);
	}

	private void reset(LivingEntity entity, Vec3 pos) {
		this.prePos = pos;
		this.preVelocity = Vec3.ZERO;
		this.filteredAccX = this.filteredAccY = this.filteredAccZ = 0;
		this.previousBodyYaw = entity.yBodyRot;
		this.previousYawVelocity = 0;
		this.hasProbe = false;
		this.previousGaitX = this.previousGaitY = this.previousGaitZ = 0;
		this.previousGaitVelX = this.previousGaitVelY = this.previousGaitVelZ = 0;
	}

	/**
	 * Torso acceleration of a human gait, reconstructed from the limb animation (model frame, blocks/s²).
	 * Two vertical bobs and two forward surges per stride, one sideways sway.
	 */
	private float[] gaitAcceleration(LivingEntity entity, Vec3 velocity, float walk) {
		float speed = 0;
		float phase = 0;
		float horizontal = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
		Entity vehicle = entity.getVehicle();
		if (vehicle instanceof LivingEntity mount) {
			// Riding: the mount's steps shake the rider, stronger than walking.
			speed = Mth.clamp(mount.walkAnimation.speed(), 0, 1.2F) * 1.4F;
			phase = EntityCompat.getLimbAnimationProgress(mount.walkAnimation) * 0.6662F;
			horizontal = Math.max(horizontal, 0.216F * speed);
		} else if (vehicle == null && entity.onGround() && !entity.isFallFlying() && !entity.isSwimming()
				&& entity.getPose() != Pose.SLEEPING) {
			speed = Mth.clamp(entity.walkAnimation.speed(), 0, 1.2F);
			phase = EntityCompat.getLimbAnimationProgress(entity.walkAnimation) * 0.6662F;
			if (entity.isCrouching()) speed *= 0.6F;
		}

		// Bob grows quickly with speed: barely anything while sneaking, clearly visible while sprinting.
		float pace = Mth.clamp(horizontal / 0.216F, 0.0F, 1.6F);
		float amplitude = 0.014F * pace * pace * pace * speed * walk;
		float bob = -amplitude * Mth.cos(2 * phase);         // up is negative in the model frame
		float sway = 1.4F * amplitude * Mth.sin(phase);
		float surge = 0.5F * amplitude * Mth.sin(2 * phase);

		float velX = (sway - this.previousGaitX) / TICK;
		float velY = (bob - this.previousGaitY) / TICK;
		float velZ = (surge - this.previousGaitZ) / TICK;
		float[] result = {
				(velX - this.previousGaitVelX) / TICK,
				(velY - this.previousGaitVelY) / TICK,
				(velZ - this.previousGaitVelZ) / TICK
		};
		// Model Y is down; the bob above was written with "up" negative, so it is already in that frame.
		this.previousGaitX = sway;
		this.previousGaitY = bob;
		this.previousGaitZ = surge;
		this.previousGaitVelX = velX;
		this.previousGaitVelY = velY;
		this.previousGaitVelZ = velZ;
		return result;
	}

	/** Acceleration of a point on the breast caused by the torso pose changing (model frame, blocks/s²). */
	private float[] probeAcceleration(PalPhysicsCompat.AttachmentPose pose, boolean left) {
		if (pose == null) {
			this.hasProbe = false;
			return new float[3];
		}
		Vector3f probe = pose.breastProbe(left);
		// Pose coordinates use +Y up; convert to the model frame.
		float x = probe.x, y = -probe.y, z = probe.z;
		float[] result = new float[3];
		if (this.hasProbe) {
			float vx = x - this.previousProbeX, vy = y - this.previousProbeY, vz = z - this.previousProbeZ;
			result[0] = Mth.clamp((vx - this.previousProbeVelX) / (TICK * TICK), -30.0F, 30.0F);
			result[1] = Mth.clamp((vy - this.previousProbeVelY) / (TICK * TICK), -30.0F, 30.0F);
			result[2] = Mth.clamp((vz - this.previousProbeVelZ) / (TICK * TICK), -30.0F, 30.0F);
			this.previousProbeVelX = vx;
			this.previousProbeVelY = vy;
			this.previousProbeVelZ = vz;
		} else {
			this.previousProbeVelX = this.previousProbeVelY = this.previousProbeVelZ = 0;
		}
		this.previousProbeX = x;
		this.previousProbeY = y;
		this.previousProbeZ = z;
		this.hasProbe = true;
		return result;
	}

	/** Gravity direction for poses where the whole model is rotated by the renderer. */
	private static void poseGravity(LivingEntity entity, Vector3f gravity) {
		if (entity.getPose() == Pose.SLEEPING) {
			gravity.set(0, 0, 1);                       // lying on the back
		} else if (entity.isFallFlying()) {
			gravity.set(0, 0.35F, -0.94F);              // face down, slightly head first
		} else {
			float lean = entity.getSwimAmount(0.0F);  // swimming / crawling
			if (lean > 0) {
				gravity.lerp(new Vector3f(0, 0, -1), Mth.clamp(lean, 0, 1));
			}
		}
	}

	/** Smoothly saturating limit, different in the negative and positive direction. */
	private static float softLimit(float value, float negativeLimit, float positiveLimit) {
		float limit = value < 0 ? negativeLimit : positiveLimit;
		if (limit <= 0) return 0;
		return limit * (float) Math.tanh(value / limit);
	}

	public float getBreastSize(float partialTicks) {
		return Mth.lerp(partialTicks, this.preBreastSize, this.breastSize);
	}

	public float getPrePositionY() {
		return this.prePositionY;
	}

	public float getPositionY() {
		return this.positionY;
	}

	public float getPrePositionX() {
		return this.prePositionX;
	}

	public float getPositionX() {
		return this.positionX;
	}

	public float getPrePositionZ() {
		return this.prePositionZ;
	}

	public float getPositionZ() {
		return this.positionZ;
	}

	/** Rotation is now part of the sideways displacement; kept for API compatibility. */
	public float getBounceRotation() {
		return 0;
	}

	public float getPreBounceRotation() {
		return 0;
	}
}
