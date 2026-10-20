package com.steelaspect.stickytoggle.net;

import com.steelaspect.stickytoggle.StickyToggle;
import com.steelaspect.stickytoggle.config.Toggle;
import com.steelaspect.stickytoggle.config.ToggleState;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S2C: full toggle state, sent as key/value pairs so unknown keys are skipped safely. */
public record SyncPayload(Map<Toggle, Boolean> values) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SyncPayload> ID =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(StickyToggle.MOD_ID, "sync"));
	public static final StreamCodec<FriendlyByteBuf, SyncPayload> CODEC =
			CustomPacketPayload.codec(SyncPayload::write, SyncPayload::read);

	public static SyncPayload of(ToggleState state) {
		return new SyncPayload(state.snapshot());
	}

	private static SyncPayload read(FriendlyByteBuf buf) {
		return new SyncPayload(readValues(buf));
	}

	private void write(FriendlyByteBuf buf) {
		writeValues(buf, values);
	}

	/** Reads toggle key/value pairs, skipping keys this version doesn't know. */
	static Map<Toggle, Boolean> readValues(FriendlyByteBuf buf) {
		EnumMap<Toggle, Boolean> map = new EnumMap<>(Toggle.class);
		int n = buf.readVarInt();
		for (int i = 0; i < n; i++) {
			Toggle t = Toggle.byKey(buf.readUtf());
			boolean v = buf.readBoolean();
			if (t != null) map.put(t, v);
		}
		return map;
	}

	static void writeValues(FriendlyByteBuf buf, Map<Toggle, Boolean> values) {
		buf.writeVarInt(values.size());
		values.forEach((t, v) -> {
			buf.writeUtf(t.key);
			buf.writeBoolean(v);
		});
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
