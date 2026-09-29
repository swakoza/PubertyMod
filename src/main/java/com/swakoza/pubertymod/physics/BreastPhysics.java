/*
 * Copyright (c) 2023-2026 swakoza
 * SPDX-License-Identifier: MIT
 */

package com.swakoza.pubertymod.physics;

import com.swakoza.pubertymod.api.IGenderArmor;
import com.swakoza.pubertymod.compat.EntityCompat;
import com.swakoza.pubertymod.compat.PalPhysicsCompat;
import com.swakoza.pubertymod.main.entitydata.EntityConfig;
import com.swakoza.pubertymod.main.SwakozaHelper;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public class BreastPhysics {

	//X-Axis
	private float bounceVelX = 0, targetBounceX = 0, velocityX = 0, positionX, prePositionX;
	//Y-Axis
	private float bounceVel = 0, targetBounceY = 0, velocity = 0, positionY, prePositionY;
	//Z-Axis (animation-driven depth lag)
	private float bounceVelZ = 0, targetBounceZ = 0, velocityZ = 0, positionZ, prePositionZ;
	//Rotation
	private float bounceRotVel = 0, targetRotVel = 0, rotVelocity = 0, wfg_bounceRotation, wfg_preBounceRotation;

	private boolean justSneaking = false, alreadySleeping = false;

	private float breastSize = 0, preBreastSize = 0;

	private Vec3d prePos;
	private double previousVerticalVelocity;
	private float previousTorsoYaw;
	private PalPhysicsCompat.AttachmentPose previousAnimationPose;
	private float previousProbeX, previousProbeY, previousProbeZ;
	private float previousAnimationVelocityX, previousAnimationVelocityY, previousAnimationVelocityZ;
	private final EntityConfig entityConfig;
	private static final float MAX_ANIMATION_IMPULSE = 1.0F;
	private static final float MAX_POSTURE_OFFSET = 6.0F;
	private static final float MAX_JUMP_IMPULSE = 3.0F;

	public BreastPhysics(EntityConfig entityConfig) {
		this.entityConfig = entityConfig;
	}

	private int randomB = 1;
	private boolean alreadyFalling = false;

	public void update(LivingEntity entity, IGenderArmor armor) {
		update(entity, armor, null, true);
	}

	public void update(LivingEntity entity, IGenderArmor armor, PalPhysicsCompat.AttachmentPose animationPose, boolean left) {
		if(entity instanceof ArmorStandEntity && !armor.armorStandsCopySettings()) {
			// optimization: skip physics on armor stands that either don't have a chestplate,
			// or have a chestplate we wouldn't copy player settings to
			return;
		}

		this.prePositionY = this.positionY;
		this.prePositionX = this.positionX;
		this.prePositionZ = this.positionZ;
		this.wfg_preBounceRotation = this.wfg_bounceRotation;
		this.preBreastSize = this.breastSize;

		if(this.prePos == null) {
			this.prePos = EntityCompat.getPos(entity);
			this.previousVerticalVelocity = entity.getVelocity().y;
			this.previousTorsoYaw = entity.bodyYaw;
			return;
		}

		{
			float h = 0; //tickDelta
			float i = entity.getLeaningPitch(0);
			if(entity instanceof AbstractClientPlayerEntity) {
				entity.getRotationVec(h);
			} else if (i > 0.0F || entity.isSleeping() || entity.getPose() == EntityPose.CROUCHING) {
				// The original values were unused even before the port; keep the branch as a no-op
				// so the surrounding flow stays intact without relying on removed elytra helpers.
			}
		} //unused currently, might be later

		float breastWeight = entityConfig.getBustSize() * 1.25f;
		float targetBreastSize = entityConfig.getBustSize();

		if (!entityConfig.getGender().canHaveBreasts()) {
			targetBreastSize = 0;
		} else {
			float tightness = MathHelper.clamp(armor.tightness(), 0, 1);
			if(entityConfig.getArmorPhysicsOverride()) tightness = 0; //override resistance
			//Scale breast size by how tight the armor is, clamping at a max adjustment of shrinking by 0.15
			targetBreastSize *= 1 - 0.15F * tightness;
		}

		breastSize += (breastSize < targetBreastSize) ? Math.abs(breastSize - targetBreastSize) / 2f : -Math.abs(breastSize - targetBreastSize) / 2f;

		Vec3d motion = EntityCompat.getPos(entity).subtract(this.prePos);
		this.prePos = EntityCompat.getPos(entity);
		float verticalAcceleration = (float) (entity.getVelocity().y - this.previousVerticalVelocity);
		this.previousVerticalVelocity = entity.getVelocity().y;

		float bounceIntensity = (targetBreastSize * 3f) * Math.round((entityConfig.getBounceMultiplier() * 3) * 100) / 100f;
		float resistance = MathHelper.clamp(armor.physicsResistance(), 0, 1);
		if(entityConfig.getArmorPhysicsOverride()) resistance = 0; //override resistance

		//Adjust bounce intensity by physics resistance of the worn armor
		bounceIntensity *= 1 - resistance;

		if(!entityConfig.getBreasts().isUniboob()) {
			bounceIntensity = bounceIntensity * SwakozaHelper.randFloat(0.5f, 1.5f);
		}
		if(entity.fallDistance > 0 && !alreadyFalling) {
			randomB = entity.getRandom().nextBoolean() ? -1 : 1;
			alreadyFalling = true;
		}
		if(entity.fallDistance == 0) alreadyFalling = false;


		this.targetBounceY = (float) motion.y * bounceIntensity;
		this.targetBounceY += breastWeight;
		// Jump takeoff and landing change vertical velocity sharply. Position
		// alone produces less than one pixel of movement at the old spring limits.
		if (Math.abs(verticalAcceleration) > 0.03F) {
			this.targetBounceY += MathHelper.clamp(verticalAcceleration * bounceIntensity * 4.0F,
					-MAX_JUMP_IMPULSE, MAX_JUMP_IMPULSE);
		}
		this.targetBounceX = 0.0F;
		this.targetBounceZ = 0.0F;
		float animationImpulseX = 0.0F, animationImpulseY = 0.0F, animationImpulseZ = 0.0F;
		if (animationPose != null) {
			// Follow a point on the breast rather than only the torso center. A dance
			// can rotate the chest without translating its center at all.
			Vector3f probe = animationPose.breastProbe(left);
			if (previousAnimationPose != null) {
				float animationVelocityX = probe.x - previousProbeX;
				float animationVelocityY = probe.y - previousProbeY;
				float animationVelocityZ = probe.z - previousProbeZ;
				Vector3f acceleration = animationPose.neutralToLocal().transform(new Vector3f(
						animationVelocityX - previousAnimationVelocityX,
						animationVelocityY - previousAnimationVelocityY,
						animationVelocityZ - previousAnimationVelocityZ));
				animationImpulseX = MathHelper.clamp(-acceleration.x * bounceIntensity,
						-MAX_ANIMATION_IMPULSE, MAX_ANIMATION_IMPULSE);
				animationImpulseY = MathHelper.clamp(-acceleration.y * bounceIntensity,
						-MAX_ANIMATION_IMPULSE, MAX_ANIMATION_IMPULSE);
				animationImpulseZ = MathHelper.clamp(-acceleration.z * bounceIntensity,
						-MAX_ANIMATION_IMPULSE, MAX_ANIMATION_IMPULSE);
				previousAnimationVelocityX = animationVelocityX;
				previousAnimationVelocityY = animationVelocityY;
				previousAnimationVelocityZ = animationVelocityZ;
			} else {
				previousAnimationVelocityX = previousAnimationVelocityY = previousAnimationVelocityZ = 0.0F;
			}
			previousProbeX = probe.x;
			previousProbeY = probe.y;
			previousProbeZ = probe.z;
		} else {
			previousAnimationVelocityX = previousAnimationVelocityY = previousAnimationVelocityZ = 0.0F;
		}
		this.previousAnimationPose = animationPose;
		// The torso follows sufficiently large head turns even while stationary.
		// Use its actual rotation, never headYaw: a head-only turn adds no force.
		this.targetRotVel = -(MathHelper.wrapDegrees(entity.bodyYaw - this.previousTorsoYaw) / 15.0F)
				* bounceIntensity;
		this.previousTorsoYaw = entity.bodyYaw;

		float f2 = (float) entity.getVelocity().lengthSquared() / 0.2F;
		f2 = f2 * f2 * f2;
		if(f2 < 1.0F) f2 = 1.0F;

		this.targetBounceY += MathHelper.cos(EntityCompat.getLimbAnimationProgress(entity.limbAnimator) * 0.6662F + (float)Math.PI) * 0.5F * entity.limbAnimator.getSpeed() * 0.5F / f2;

		this.targetRotVel += (float) motion.y * bounceIntensity * randomB;


		if(entity.getPose() == EntityPose.CROUCHING && !this.justSneaking) {
			this.justSneaking = true;
			this.targetBounceY += bounceIntensity;
		}
		if(entity.getPose() != EntityPose.CROUCHING && this.justSneaking) {
			this.justSneaking = false;
			this.targetBounceY += bounceIntensity;
		}

		//button option for extra entities
		if(entity.getVehicle() != null) {
			if(entity.getVehicle() instanceof BoatEntity boat) {
				float movement = (float) boat.getVelocity().lengthSquared();
				if(movement > 0.02f) {
					this.targetBounceY = bounceIntensity / 3.25f;
				}
			}

			if(entity.getVehicle() instanceof MinecartEntity cart) {
				float speed = (float) cart.getVelocity().lengthSquared();
				if(Math.random() * speed < 0.5f && speed > 0.2f) {
					this.targetBounceY = (Math.random() > 0.5 ? -bounceIntensity : bounceIntensity) / 6f;
				}
			}
			if(entity.getVehicle() instanceof HorseEntity horse) {
				float movement = (float) horse.getVelocity().lengthSquared();
				if(horse.age % clampMovement(movement) == 5 && movement > 0.1f) {
					this.targetBounceY = bounceIntensity / 4f;
				}
			}
			if(entity.getVehicle() instanceof PigEntity pig) {
				float movement = (float) pig.getVelocity().lengthSquared();
				if(pig.age % clampMovement(movement) == 5 && movement > 0.08f) {
					this.targetBounceY = bounceIntensity / 4f;
				}
			}
			if(entity.getVehicle() instanceof StriderEntity strider) {
				double heightOffset = (double)strider.getHeight() - 0.19
						+ (double)(0.12F * MathHelper.cos(EntityCompat.getLimbAnimationProgress(strider.limbAnimator) * 1.5f)
						* 2F * Math.min(0.25F, strider.limbAnimator.getSpeed()));
				this.targetBounceY += ((float) (heightOffset * 3f) - 4.5f) * bounceIntensity;
			}
		}
		if(entity.handSwinging && entity.age % 5 == 0 && entity.getPose() != EntityPose.SLEEPING) {
			this.targetBounceY += (Math.random() > 0.5 ? -0.25f : 0.25f) * bounceIntensity;
		}
		if(entity.getPose() == EntityPose.SLEEPING && !this.alreadySleeping) {
			this.targetBounceY = bounceIntensity;
			this.alreadySleeping = true;
		}
		if(entity.getPose() != EntityPose.SLEEPING && this.alreadySleeping) {
			this.targetBounceY = bounceIntensity;
			this.alreadySleeping = false;
		}
		/*if(plr.getPose() == EntityPose.SWIMMING) {
			//System.out.println(1 - plr.getRotationVec(tickDelta).getY());
			rotationMultiplier = 1 - (float) plr.getRotationVec(tickDelta).getY();
		}*/


		float percent =  entityConfig.getFloppiness();
		float bounceAmount = 0.45f * (1f - percent) + 0.15f; //0.6f * percent - 0.15f;
		bounceAmount = MathHelper.clamp(bounceAmount, 0.15f, 0.6f);
		float delta = 2.25f - bounceAmount;
		//if(plr.isInWater()) delta = 0.75f - (1f * bounceAmount); //water resistance

		float distanceFromMin = Math.abs(bounceVel + 1.5f) * 0.5f;
		float distanceFromMax = Math.abs(bounceVel - 2.65f) * 0.5f;

		if(bounceVel < -0.5f) {
			targetBounceY += distanceFromMin;
		}
		if(bounceVel > 2.5f) {
			targetBounceY -= distanceFromMax;
		}
		if(targetBounceY < -2.5f) targetBounceY = -2.5f;
		if(targetBounceY > 4.0f) targetBounceY = 4.0f;
		if(targetRotVel < -25f) targetRotVel = -25f;
		if(targetRotVel > 25f) targetRotVel = 25f;
		if (animationPose != null) {
			// Jump, landing and other vertical forces follow world gravity after
			// projection into the current torso frame. A held bend also sags forward.
			float verticalForce = this.targetBounceY;
			float postureSag = targetBreastSize * 4.0F * (1.0F - resistance);
			this.targetBounceX = MathHelper.clamp(
					(verticalForce + postureSag) * animationPose.gravityX() + animationImpulseX,
					-MAX_POSTURE_OFFSET, MAX_POSTURE_OFFSET);
			this.targetBounceY = verticalForce * animationPose.gravityY() + animationImpulseY;
			this.targetBounceZ = MathHelper.clamp(
					(verticalForce + postureSag) * animationPose.gravityZ() + animationImpulseZ,
					-MAX_POSTURE_OFFSET, MAX_POSTURE_OFFSET);
		}

		this.velocity = MathHelper.lerp(bounceAmount, this.velocity, (this.targetBounceY - this.bounceVel) * delta);
		this.bounceVel += this.velocity * percent * 1.1625f;

		//X
		this.velocityX = MathHelper.lerp(bounceAmount, this.velocityX, (this.targetBounceX - this.bounceVelX) * delta);
		this.bounceVelX += this.velocityX * percent;

		//Z
		this.velocityZ = MathHelper.lerp(bounceAmount, this.velocityZ, (this.targetBounceZ - this.bounceVelZ) * delta);
		this.bounceVelZ += this.velocityZ * percent;

		this.rotVelocity = MathHelper.lerp(bounceAmount, this.rotVelocity, (this.targetRotVel - this.bounceRotVel) * delta);
		this.bounceRotVel += this.rotVelocity * percent;

		this.wfg_bounceRotation = this.bounceRotVel;
		this.positionX = MathHelper.clamp(this.bounceVelX, -MAX_POSTURE_OFFSET, MAX_POSTURE_OFFSET);
		this.positionY = this.bounceVel;
		this.positionZ = MathHelper.clamp(this.bounceVelZ, -MAX_POSTURE_OFFSET, MAX_POSTURE_OFFSET);

		if(this.positionY < -2.0f) this.positionY = -2.0f;
		if(this.positionY > 3.5f) {
			this.positionY = 3.5f;
			this.velocity = 0;
		}

	}

	public float getBreastSize(float partialTicks) {
		return MathHelper.lerp(partialTicks, preBreastSize, breastSize);
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

	public float getBounceRotation() {
		return this.wfg_bounceRotation;
	}
	public float getPreBounceRotation() {
		return this.wfg_preBounceRotation;
	}

	private int clampMovement(float movement) {
		int val = (int) (10 - movement*2f);
		if(val < 1) val = 1;
		return val;
	}
}
