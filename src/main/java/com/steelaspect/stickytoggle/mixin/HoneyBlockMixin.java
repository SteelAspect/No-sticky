package com.steelaspect.stickytoggle.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.HoneyBlock;
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
	@WrapOperation(method = "entityInside", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/HoneyBlock;isSlidingDown(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)Z"))
	private boolean stickytoggle$wallSlide(HoneyBlock block, BlockPos pos, Entity entity, Operation<Boolean> original) {
		if (entity instanceof Player && !StickyConfig.get(entity, Toggle.HONEY_WALL_SLIDE)) {
			if (original.call(block, pos, entity)) {
				entity.resetFallDistance();
			}
			return false;
		}
		return original.call(block, pos, entity);
	}
}
