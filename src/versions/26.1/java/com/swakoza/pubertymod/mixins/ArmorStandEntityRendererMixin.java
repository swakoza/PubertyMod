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

package com.swakoza.pubertymod.mixins;

import com.swakoza.pubertymod.render.GenderArmorLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ArmorStandRenderer.class)
public abstract class ArmorStandEntityRendererMixin extends LivingEntityRenderer<ArmorStand, ArmorStandRenderState, ArmorStandArmorModel> {
	public ArmorStandEntityRendererMixin(EntityRendererProvider.Context ctx, ArmorStandArmorModel model, float shadow) {
		super(ctx, model, shadow);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void swakozapuberty$armorStandBreastArmor(EntityRendererProvider.Context ctx, CallbackInfo ci) {
		this.addLayer(new GenderArmorLayer<>(this, ctx.getEquipmentAssets()));
	}
}
