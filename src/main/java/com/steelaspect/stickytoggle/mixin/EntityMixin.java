package com.steelaspect.stickytoggle.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SoulSandBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
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
	@WrapOperation(method = "getBlockSpeedFactor", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/Block;getSpeedFactor()F"))
	private float stickytoggle$velocity(Block block, Operation<Float> original) {
		Entity self = (Entity) (Object) this;
		if (self instanceof Player) {
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
	@WrapOperation(method = "getBlockJumpFactor", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/Block;getJumpFactor()F"))
	private float stickytoggle$honeyJump(Block block, Operation<Float> original) {
		Entity self = (Entity) (Object) this;
		if (block instanceof HoneyBlock && self instanceof Player
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
	@Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
	private void stickytoggle$slowMovement(BlockState state, Vec3 multiplier, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (!(self instanceof Player)) return;
		Block block = state.getBlock();
		Toggle toggle = block instanceof WebBlock ? Toggle.COBWEB_SLOWDOWN
				: block instanceof PowderSnowBlock ? Toggle.POWDER_SNOW_SLOWDOWN
				: block instanceof SweetBerryBushBlock ? Toggle.BERRY_BUSH_SLOWDOWN
				: null;
		if (toggle != null && !StickyConfig.get(self, toggle)) {
			self.resetFallDistance();
			ci.cancel();
		}
	}

	/**
	 * water.current — vanilla adds the flow vector of flowing water when isPushedByFluids() is true.
	 * When off, players are not pushed by water. Fluid height (swimming, floating, breathing) is still
	 * calculated, and lava is unchanged.
	 */
	@WrapOperation(method = "updateFluidHeightAndDoFluidPushing", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;isPushedByFluid()Z"))
	private boolean stickytoggle$waterCurrent(Entity entity, Operation<Boolean> original,
			@Local(argsOnly = true) TagKey<Fluid> fluid) {
		if (fluid == FluidTags.WATER && entity instanceof Player
				&& !StickyConfig.get(entity, Toggle.WATER_CURRENT)) {
			return false;
		}
		return original.call(entity);
	}

	/**
	 * bubbleColumn.push — inside a column vanilla pushes up (soul sand) or pulls down (magma).
	 * When off, players are left alone, as in still water. Fall distance is still reset like vanilla.
	 */
	@Inject(method = "onInsideBubbleColumn", at = @At("HEAD"), cancellable = true)
	private void stickytoggle$bubbleColumn(boolean drag, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (self instanceof Player && !StickyConfig.get(self, Toggle.BUBBLE_COLUMN_PUSH)) {
			self.resetFallDistance();
			ci.cancel();
		}
	}

	/** bubbleColumn.push — the stronger launch/pull at the top of a column, also skipped for players. */
	@Inject(method = "onAboveBubbleColumn", at = @At("HEAD"), cancellable = true)
	private void stickytoggle$bubbleColumnSurface(boolean drag, BlockPos pos, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (self instanceof Player && !StickyConfig.get(self, Toggle.BUBBLE_COLUMN_PUSH)) {
			ci.cancel();
		}
	}
}
