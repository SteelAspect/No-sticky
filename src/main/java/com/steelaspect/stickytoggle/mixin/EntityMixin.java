package com.steelaspect.stickytoggle.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import net.minecraft.block.Block;
import net.minecraft.block.HoneyBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityMixin {
	/**
	 * honey.velocityMultiplier — vanilla reads the block's 0.4 velocity multiplier
	 * (checked at the feet block, then the block below). When off, players get 1.0 from honey.
	 */
	@WrapOperation(method = "getVelocityMultiplier", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/block/Block;getVelocityMultiplier()F"))
	private float stickytoggle$honeyVelocity(Block block, Operation<Float> original) {
		Entity self = (Entity) (Object) this;
		if (block instanceof HoneyBlock && self instanceof PlayerEntity
				&& !StickyConfig.get(self, Toggle.HONEY_VELOCITY_MULTIPLIER)) {
			return 1.0F;
		}
		return original.call(block);
	}

	/**
	 * honey.jumpMultiplier — vanilla reads the block's 0.5 jump multiplier.
	 * When off, players get 1.0 from honey (this also re-enables auto-jump on honey).
	 */
	@WrapOperation(method = "getJumpVelocityMultiplier", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/block/Block;getJumpVelocityMultiplier()F"))
	private float stickytoggle$honeyJump(Block block, Operation<Float> original) {
		Entity self = (Entity) (Object) this;
		if (block instanceof HoneyBlock && self instanceof PlayerEntity
				&& !StickyConfig.get(self, Toggle.HONEY_JUMP_MULTIPLIER)) {
			return 1.0F;
		}
		return original.call(block);
	}
}
