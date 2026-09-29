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
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
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

	private Vec3 prePos;
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
		if(entity instanceof ArmorStand && !armor.armorStandsCopySettings()) {
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
			this.previousVerticalVelocity = entity.getDeltaMovement().y;
			this.previousTorsoYaw = entity.yBodyRot;
			return;
		}

		{
			float h = 0; //tickDelta
			float i = entity.getSwimAmount(0);
			if(entity instanceof AbstractClientPlayer) {
				entity.getViewVector(h);
			} else if (i > 0.0F || entity.isSleeping() || entity.getPose() == Pose.CROUCHING) {
				// The original values were unused even before the port; keep the branch as a no-op
				// so the surrounding flow stays intact without relying on removed elytra helpers.
			}
		} //unused currently, might be later

		float breastWeight = entityConfig.getBustSize() * 1.25f;
		float targetBreastSize = entityConfig.getBustSize();

		if (!entityConfig.getGender().canHaveBreasts()) {
			targetBreastSize = 0;
		} else {
			float tightness = Mth.clamp(armor.tightness(), 0, 1);
			if(entityConfig.getArmorPhysicsOverride()) tightness = 0; //override resistance
			//Scale breast size by how tight the armor is, clamping at a max adjustment of shrinking by 0.15
			targetBreastSize *= 1 - 0.15F * tightness;
		}

		breastSize += (breastSize < targetBreastSize) ? Math.abs(breastSize - targetBreastSize) / 2f : -Math.abs(breastSize - targetBreastSize) / 2f;

		Vec3 motion = EntityCompat.getPos(entity).subtract(this.prePos);
		this.prePos = EntityCompat.getPos(entity);
		float verticalAcceleration = (float) (entity.getDeltaMovement().y - this.previousVerticalVelocity);
		this.previousVerticalVelocity = entity.getDeltaMovement().y;

		float bounceIntensity = (targetBreastSize * 3f) * Math.round((entityConfig.getBounceMultiplier() * 3) * 100) / 100f;
		float resistance = Mth.clamp(armor.physicsResistance(), 0, 1);
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
			this.targetBounceY += Mth.clamp(verticalAcceleration * bounceIntensity * 4.0F,
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
				animationImpulseX = Mth.clamp(-acceleration.x * bounceIntensity,
						-MAX_ANIMATION_IMPULSE, MAX_ANIMATION_IMPULSE);
				animationImpulseY = Mth.clamp(-acceleration.y * bounceIntensity,
						-MAX_ANIMATION_IMPULSE, MAX_ANIMATION_IMPULSE);
				animationImpulseZ = Mth.clamp(-acceleration.z * bounceIntensity,
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
		this.targetRotVel = -(Mth.wrapDegrees(entity.yBodyRot - this.previousTorsoYaw) / 15.0F)
				* bounceIntensity;
		this.previousTorsoYaw = entity.yBodyRot;

		float f2 = (float) entity.getDeltaMovement().lengthSqr() / 0.2F;
		f2 = f2 * f2 * f2;
		if(f2 < 1.0F) f2 = 1.0F;

		this.targetBounceY += Mth.cos(EntityCompat.getLimbAnimationProgress(entity.walkAnimation) * 0.6662F + (float)Math.PI) * 0.5F * entity.walkAnimation.speed() * 0.5F / f2;

		this.targetRotVel += (float) motion.y * bounceIntensity * randomB;


		if(entity.getPose() == Pose.CROUCHING && !this.justSneaking) {
			this.justSneaking = true;
			this.targetBounceY += bounceIntensity;
		}
		if(entity.getPose() != Pose.CROUCHING && this.justSneaking) {
			this.justSneaking = false;
			this.targetBounceY += bounceIntensity;
		}

		//button option for extra entities
		if(entity.getVehicle() != null) {
			if(entity.getVehicle() instanceof Boat boat) {
				float movement = (float) boat.getDeltaMovement().lengthSqr();
				if(movement > 0.02f) {
					this.targetBounceY = bounceIntensity / 3.25f;
				}
			}

			if(entity.getVehicle() instanceof Minecart cart) {
				float speed = (float) cart.getDeltaMovement().lengthSqr();
				if(Math.random() * speed < 0.5f && speed > 0.2f) {
					this.targetBounceY = (Math.random() > 0.5 ? -bounceIntensity : bounceIntensity) / 6f;
				}
			}
			if(entity.getVehicle() instanceof Horse horse) {
				float movement = (float) horse.getDeltaMovement().lengthSqr();
				if(horse.tickCount % clampMovement(movement) == 5 && movement > 0.1f) {
					this.targetBounceY = bounceIntensity / 4f;
				}
			}
			if(entity.getVehicle() instanceof Pig pig) {
				float movement = (float) pig.getDeltaMovement().lengthSqr();
				if(pig.tickCount % clampMovement(movement) == 5 && movement > 0.08f) {
					this.targetBounceY = bounceIntensity / 4f;
				}
			}
			if(entity.getVehicle() instanceof Strider strider) {
				double heightOffset = (double)strider.getBbHeight() - 0.19
						+ (double)(0.12F * Mth.cos(EntityCompat.getLimbAnimationProgress(strider.walkAnimation) * 1.5f)
						* 2F * Math.min(0.25F, strider.walkAnimation.speed()));
				this.targetBounceY += ((float) (heightOffset * 3f) - 4.5f) * bounceIntensity;
			}
		}
		if(entity.swinging && entity.tickCount % 5 == 0 && entity.getPose() != Pose.SLEEPING) {
			this.targetBounceY += (Math.random() > 0.5 ? -0.25f : 0.25f) * bounceIntensity;
		}
		if(entity.getPose() == Pose.SLEEPING && !this.alreadySleeping) {
			this.targetBounceY = bounceIntensity;
			this.alreadySleeping = true;
		}
		if(entity.getPose() != Pose.SLEEPING && this.alreadySleeping) {
			this.targetBounceY = bounceIntensity;
			this.alreadySleeping = false;
		}
		/*if(plr.getPose() == Pose.SWIMMING) {
			//System.out.println(1 - plr.getViewVector(tickDelta).getY());
			rotationMultiplier = 1 - (float) plr.getViewVector(tickDelta).getY();
		}*/


		float percent =  entityConfig.getFloppiness();
		float bounceAmount = 0.45f * (1f - percent) + 0.15f; //0.6f * percent - 0.15f;
		bounceAmount = Mth.clamp(bounceAmount, 0.15f, 0.6f);
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
			this.targetBounceX = Mth.clamp(
					(verticalForce + postureSag) * animationPose.gravityX() + animationImpulseX,
					-MAX_POSTURE_OFFSET, MAX_POSTURE_OFFSET);
			this.targetBounceY = verticalForce * animationPose.gravityY() + animationImpulseY;
			this.targetBounceZ = Mth.clamp(
					(verticalForce + postureSag) * animationPose.gravityZ() + animationImpulseZ,
					-MAX_POSTURE_OFFSET, MAX_POSTURE_OFFSET);
		}

		this.velocity = Mth.lerp(bounceAmount, this.velocity, (this.targetBounceY - this.bounceVel) * delta);
		this.bounceVel += this.velocity * percent * 1.1625f;

		//X
		this.velocityX = Mth.lerp(bounceAmount, this.velocityX, (this.targetBounceX - this.bounceVelX) * delta);
		this.bounceVelX += this.velocityX * percent;

		//Z
		this.velocityZ = Mth.lerp(bounceAmount, this.velocityZ, (this.targetBounceZ - this.bounceVelZ) * delta);
		this.bounceVelZ += this.velocityZ * percent;

		this.rotVelocity = Mth.lerp(bounceAmount, this.rotVelocity, (this.targetRotVel - this.bounceRotVel) * delta);
		this.bounceRotVel += this.rotVelocity * percent;

		this.wfg_bounceRotation = this.bounceRotVel;
		this.positionX = Mth.clamp(this.bounceVelX, -MAX_POSTURE_OFFSET, MAX_POSTURE_OFFSET);
		this.positionY = this.bounceVel;
		this.positionZ = Mth.clamp(this.bounceVelZ, -MAX_POSTURE_OFFSET, MAX_POSTURE_OFFSET);

		if(this.positionY < -2.0f) this.positionY = -2.0f;
		if(this.positionY > 3.5f) {
			this.positionY = 3.5f;
			this.velocity = 0;
		}

	}

	public float getBreastSize(float partialTicks) {
		return Mth.lerp(partialTicks, preBreastSize, breastSize);
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
