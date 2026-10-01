package com.steelaspect.stickytoggle.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import net.minecraft.block.BlockState;
import net.minecraft.block.HoneyBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HoneyBlock.class)
public abstract class HoneyBlockMixin {
	/**
	 * honey.wallSlide — vanilla onEntityCollision checks isSliding() and then slows the fall,
	 * resets fall distance, plays slide sound/drip particles and triggers the advancement.
	 * When off, isSliding() reports false for players so none of that happens.
	 */
	@WrapOperation(method = "onEntityCollision", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/block/HoneyBlock;isSliding(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)Z"))
	private boolean stickytoggle$wallSlide(HoneyBlock block, BlockPos pos, Entity entity, Operation<Boolean> original) {
		if (entity instanceof PlayerEntity && !StickyConfig.get(entity, Toggle.HONEY_WALL_SLIDE)) {
			return false;
		}
		return original.call(block, pos, entity);
	}

	/**
	 * honey.fallDamageReduction — vanilla plays the slide sound, drips particles and calls
	 * handleFallDamage(fallDistance, 0.2F, ...). When off, players land like on a normal
	 * block (Block.onLandedUpon: multiplier 1.0F, no honey effects).
	 */
	@Inject(method = "onLandedUpon", at = @At("HEAD"), cancellable = true)
	private void stickytoggle$fallDamage(World world, BlockState state, BlockPos pos, Entity entity, double fallDistance, CallbackInfo ci) {
		if (entity instanceof PlayerEntity && !StickyConfig.get(entity, Toggle.HONEY_FALL_DAMAGE_REDUCTION)) {
			entity.handleFallDamage(fallDistance, 1.0F, entity.getDamageSources().fall());
			ci.cancel();
		}
	}
}
