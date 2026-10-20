package com.steelaspect.stickytoggle.net;

import com.steelaspect.stickytoggle.StickyToggle;
import com.steelaspect.stickytoggle.config.Toggle;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: the player asks to change some of their own toggles (from the config menu or a hotkey).
 * The server applies it exactly like /stickytoggle and answers with a {@link SyncPayload}.
 */
public record SetPayload(Map<Toggle, Boolean> values) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SetPayload> ID =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(StickyToggle.MOD_ID, "set"));
	public static final StreamCodec<FriendlyByteBuf, SetPayload> CODEC =
			CustomPacketPayload.codec((p, buf) -> SyncPayload.writeValues(buf, p.values), buf -> new SetPayload(SyncPayload.readValues(buf)));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
