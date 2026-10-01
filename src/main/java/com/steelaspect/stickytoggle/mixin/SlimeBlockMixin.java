package com.steelaspect.stickytoggle.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import net.minecraft.block.SlimeBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
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
	@Inject(method = "onEntityLand", at = @At("HEAD"), cancellable = true)
	private void stickytoggle$bounce(BlockView world, Entity entity, CallbackInfo ci) {
		if (entity instanceof PlayerEntity && !StickyConfig.get(entity, Toggle.SLIME_BOUNCE)) {
			entity.setVelocity(entity.getVelocity().multiply(1.0, 0.0, 1.0));
			ci.cancel();
		}
	}

	/**
	 * slime.walkSlowdown — vanilla multiplies horizontal velocity by 0.4–0.6 while walking
	 * on slime. When off, that setVelocity call is skipped for players.
	 */
	@WrapWithCondition(method = "onSteppedOn", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/entity/Entity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"))
	private boolean stickytoggle$walkSlowdown(Entity entity, Vec3d velocity) {
		return !(entity instanceof PlayerEntity) || StickyConfig.get(entity, Toggle.SLIME_WALK_SLOWDOWN);
	}
}
