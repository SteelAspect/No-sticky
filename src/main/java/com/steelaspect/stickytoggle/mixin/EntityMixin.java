package com.steelaspect.stickytoggle.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CobwebBlock;
import net.minecraft.block.HoneyBlock;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.block.SoulSandBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
	/**
	 * honey.velocityMultiplier / soulsand.slowdown — vanilla reads the block's 0.4 velocity multiplier
	 * (checked at the feet block, then the block below). When off, players get 1.0 from that block.
	 */
	@WrapOperation(method = "getVelocityMultiplier", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/block/Block;getVelocityMultiplier()F"))
	private float stickytoggle$velocity(Block block, Operation<Float> original) {
		Entity self = (Entity) (Object) this;
		if (self instanceof PlayerEntity) {
			Toggle toggle = block instanceof HoneyBlock ? Toggle.HONEY_VELOCITY_MULTIPLIER
					: block instanceof SoulSandBlock ? Toggle.SOUL_SAND_SLOWDOWN
					: null;
			if (toggle != null && !StickyConfig.get(self, toggle)) {
				return 1.0F;
			}
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

	/**
	 * cobweb.slowdown / powderSnow.slowdown / berryBush.slowdown — these three blocks call slowMovement
	 * from onEntityCollision with their multiplier. When off, the multiplier is skipped for players, but
	 * fall distance is still reset like vanilla, so landing in them never deals more damage. Freezing and
	 * thorn damage happen outside this method and stay vanilla.
	 */
	@Inject(method = "slowMovement", at = @At("HEAD"), cancellable = true)
	private void stickytoggle$slowMovement(BlockState state, Vec3d multiplier, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (!(self instanceof PlayerEntity)) return;
		Block block = state.getBlock();
		Toggle toggle = block instanceof CobwebBlock ? Toggle.COBWEB_SLOWDOWN
				: block instanceof PowderSnowBlock ? Toggle.POWDER_SNOW_SLOWDOWN
				: block instanceof SweetBerryBushBlock ? Toggle.BERRY_BUSH_SLOWDOWN
				: null;
		if (toggle != null && !StickyConfig.get(self, toggle)) {
			self.onLanding();
			ci.cancel();
		}
	}

	/**
	 * water.current — vanilla adds the flow vector of flowing water when isPushedByFluids() is true.
	 * When off, players are not pushed by water. Fluid height (swimming, floating, breathing) is still
	 * calculated, and lava is unchanged.
	 */
	@WrapOperation(method = "updateMovementInFluid", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/entity/Entity;isPushedByFluids()Z"))
	private boolean stickytoggle$waterCurrent(Entity entity, Operation<Boolean> original,
			@Local(argsOnly = true) TagKey<Fluid> fluid) {
		if (fluid == FluidTags.WATER && entity instanceof PlayerEntity
				&& !StickyConfig.get(entity, Toggle.WATER_CURRENT)) {
			return false;
		}
		return original.call(entity);
	}

	/**
	 * bubbleColumn.push — inside a column vanilla pushes up (soul sand) or pulls down (magma).
	 * When off, players are left alone, as in still water. Fall distance is still reset like vanilla.
	 */
	@Inject(method = "onBubbleColumnCollision", at = @At("HEAD"), cancellable = true)
	private void stickytoggle$bubbleColumn(boolean drag, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (self instanceof PlayerEntity && !StickyConfig.get(self, Toggle.BUBBLE_COLUMN_PUSH)) {
			self.onLanding();
			ci.cancel();
		}
	}

	/** bubbleColumn.push — the stronger launch/pull at the top of a column, also skipped for players. */
	@Inject(method = "onBubbleColumnSurfaceCollision", at = @At("HEAD"), cancellable = true)
	private void stickytoggle$bubbleColumnSurface(boolean drag, BlockPos pos, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (self instanceof PlayerEntity && !StickyConfig.get(self, Toggle.BUBBLE_COLUMN_PUSH)) {
			ci.cancel();
		}
	}
}
