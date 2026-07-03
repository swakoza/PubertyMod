/*
    Puberty-Mod is a female gender mod created for Minecraft.
    Copyright (C) 2023 swakoza

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

package com.swakoza.pubertymod.physics;

import com.swakoza.pubertymod.api.IGenderArmor;
import com.swakoza.pubertymod.compat.EntityCompat;
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

public class BreastPhysics {

	//X-Axis
	private float bounceVelX = 0, targetBounceX = 0, velocityX = 0, positionX, prePositionX;
	//Y-Axis
	private float bounceVel = 0, targetBounceY = 0, velocity = 0, positionY, prePositionY;
	//Rotation
	private float bounceRotVel = 0, targetRotVel = 0, rotVelocity = 0, wfg_bounceRotation, wfg_preBounceRotation;

	private boolean justSneaking = false, alreadySleeping = false;

	private float breastSize = 0, preBreastSize = 0;

	private Vec3d prePos;
	private final EntityConfig entityConfig;

	public BreastPhysics(EntityConfig entityConfig) {
		this.entityConfig = entityConfig;
	}

	private int randomB = 1;
	private boolean alreadyFalling = false;

	public void update(LivingEntity entity, IGenderArmor armor) {
		if(entity instanceof ArmorStandEntity && !armor.armorStandsCopySettings()) {
			// optimization: skip physics on armor stands that either don't have a chestplate,
			// or have a chestplate we wouldn't copy player settings to
			return;
		}

		this.prePositionY = this.positionY;
		this.prePositionX = this.positionX;
		this.wfg_preBounceRotation = this.wfg_bounceRotation;
		this.preBreastSize = this.breastSize;

		if(this.prePos == null) {
			this.prePos = EntityCompat.getPos(entity);
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
		float horizVel = (float) Math.sqrt(Math.pow(motion.x, 2) + Math.pow(motion.z, 2)) * (bounceIntensity);
		this.targetRotVel = -((entity.bodyYaw - EntityCompat.getPreviousBodyYaw(entity)) / 15f) * bounceIntensity;

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
		if(targetBounceY < -1.5f) targetBounceY = -1.5f;
		if(targetBounceY > 2.5f) targetBounceY = 2.5f;
		if(targetRotVel < -25f) targetRotVel = -25f;
		if(targetRotVel > 25f) targetRotVel = 25f;

		this.velocity = MathHelper.lerp(bounceAmount, this.velocity, (this.targetBounceY - this.bounceVel) * delta);
		this.bounceVel += this.velocity * percent * 1.1625f;

		//X
		this.velocityX = MathHelper.lerp(bounceAmount, this.velocityX, (this.targetBounceX - this.bounceVelX) * delta);
		this.bounceVelX += this.velocityX * percent;

		this.rotVelocity = MathHelper.lerp(bounceAmount, this.rotVelocity, (this.targetRotVel - this.bounceRotVel) * delta);
		this.bounceRotVel += this.rotVelocity * percent;

		this.wfg_bounceRotation = this.bounceRotVel;
		this.positionX = this.bounceVelX;
		this.positionY = this.bounceVel;

		if(this.positionY < -0.5f) this.positionY = -0.5f;
		if(this.positionY > 1.5f) {
			this.positionY = 1.5f;
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
