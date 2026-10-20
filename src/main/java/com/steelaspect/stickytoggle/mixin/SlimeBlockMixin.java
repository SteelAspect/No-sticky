package com.steelaspect.stickytoggle.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlimeBlock.class)
public abstract class SlimeBlockMixin {
	/**
	 * slime.bounce — vanilla bounce() flips downward Y velocity. When off, players land like
	 * on any other block (Block.onEntityLand zeroes Y velocity).
	 */
	@Inject(method = "updateEntityMovementAfterFallOn", at = @At("HEAD"), cancellable = true)
	private void stickytoggle$bounce(BlockGetter world, Entity entity, CallbackInfo ci) {
		if (entity instanceof Player && !StickyConfig.get(entity, Toggle.SLIME_BOUNCE)) {
			entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 0.0, 1.0));
			ci.cancel();
		}
	}

	/**
	 * slime.walkSlowdown — vanilla multiplies horizontal velocity by 0.4–0.6 while walking
	 * on slime. When off, that setVelocity call is skipped for players.
	 */
	@WrapWithCondition(method = "stepOn", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
	private boolean stickytoggle$walkSlowdown(Entity entity, Vec3 velocity) {
		return !(entity instanceof Player) || StickyConfig.get(entity, Toggle.SLIME_WALK_SLOWDOWN);
	}
}
