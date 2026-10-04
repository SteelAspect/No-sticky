package com.steelaspect.stickytoggle.net;

import com.steelaspect.stickytoggle.StickyToggle;
import com.steelaspect.stickytoggle.config.Toggle;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * C2S: the player asks to change some of their own toggles (from the config menu or a hotkey).
 * The server applies it exactly like /stickytoggle and answers with a {@link SyncPayload}.
 */
public record SetPayload(Map<Toggle, Boolean> values) implements CustomPayload {
	public static final CustomPayload.Id<SetPayload> ID =
			new CustomPayload.Id<>(Identifier.of(StickyToggle.MOD_ID, "set"));
	public static final PacketCodec<PacketByteBuf, SetPayload> CODEC =
			CustomPayload.codecOf((p, buf) -> SyncPayload.writeValues(buf, p.values), buf -> new SetPayload(SyncPayload.readValues(buf)));

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
