package com.steelaspect.stickytoggle.smoke.mixin;

import com.steelaspect.stickytoggle.smoke.StickySmokeTest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts the server's position corrections (teleports and rubber-banding). */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(method = "handleMovePlayer", at = @At("HEAD"))
	private void stickysmoke$count(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
		// Called once on the network thread (which then hands it to the client thread): count only the second call.
		if (Minecraft.getInstance().isSameThread()) StickySmokeTest.positionPackets++;
	}
}
