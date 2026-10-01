package com.steelaspect.stickytoggle.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import net.minecraft.block.HoneyBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HoneyBlock.class)
public abstract class HoneyBlockMixin {
	/**
	 * honey.wallSlide — vanilla onEntityCollision checks isSliding() and then slows the fall,
	 * resets fall distance, plays slide sound/drip particles and triggers the advancement.
	 * When off, isSliding() reports false for players so they are not slowed and get no
	 * particles/sound/advancement. Fall distance is still reset while touching the wall,
	 * so players never take more fall damage than vanilla.
	 */
	@WrapOperation(method = "onEntityCollision", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/block/HoneyBlock;isSliding(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)Z"))
	private boolean stickytoggle$wallSlide(HoneyBlock block, BlockPos pos, Entity entity, Operation<Boolean> original) {
		if (entity instanceof PlayerEntity && !StickyConfig.get(entity, Toggle.HONEY_WALL_SLIDE)) {
			if (original.call(block, pos, entity)) {
				entity.onLanding();
			}
			return false;
		}
		return original.call(block, pos, entity);
	}
}
