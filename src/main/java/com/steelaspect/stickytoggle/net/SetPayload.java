package com.steelaspect.stickytoggle.net;

import com.steelaspect.stickytoggle.StickyToggle;
import com.steelaspect.stickytoggle.config.Toggle;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S: the player asks to change some of their own toggles (from the config menu or a hotkey).
 * The server applies it exactly like /stickytoggle and answers with a {@link SyncPayload}.
 */
public record SetPayload(Map<Toggle, Boolean> values) implements FabricPacket {
	public static final PacketType<SetPayload> ID =
			PacketType.create(new ResourceLocation(StickyToggle.MOD_ID, "set"), buf -> new SetPayload(SyncPayload.readValues(buf)));

	@Override
	public void write(FriendlyByteBuf buf) {
		SyncPayload.writeValues(buf, values);
	}

	@Override
	public PacketType<?> getType() {
		return ID;
	}
}
