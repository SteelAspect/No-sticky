package com.steelaspect.stickytoggle.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlimeBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	/**
	 * slime.slipperiness / ice.slipperiness — vanilla ground movement uses the block's slipperiness
	 * (slime = 0.8, ice/packed ice/frosted ice = 0.98, blue ice = 0.989). When off, players use the
	 * default 0.6. The block itself is not changed, so items, mobs, boats etc. still slide.
	 */
	@WrapOperation(method = "travel", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/Block;getFriction()F"))
	private float stickytoggle$slipperiness(Block block, Operation<Float> original) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof Player) {
			Toggle toggle = block instanceof SlimeBlock ? Toggle.SLIME_SLIPPERINESS
					: block.defaultBlockState().is(BlockTags.ICE) ? Toggle.ICE_SLIPPERINESS
					: null;
			if (toggle != null && !StickyConfig.get(self, toggle)) {
				return 0.6F;
			}
		}
		return original.call(block);
	}
}
