package com.steelaspect.stickytoggle.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import net.minecraft.block.Block;
import net.minecraft.block.SlimeBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	/**
	 * slime.slipperiness — vanilla ground movement uses the block's slipperiness (slime = 0.8).
	 * When off, players use the default 0.6. The block itself is not changed, so items,
	 * mobs, boats etc. still slide.
	 */
	@WrapOperation(method = "travelMidAir", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/block/Block;getSlipperiness()F"))
	private float stickytoggle$slimeSlipperiness(Block block, Operation<Float> original) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (block instanceof SlimeBlock && self instanceof PlayerEntity
				&& !StickyConfig.get(self, Toggle.SLIME_SLIPPERINESS)) {
			return 0.6F;
		}
		return original.call(block);
	}
}
