package com.steelaspect.stickytoggle.net;

import com.steelaspect.stickytoggle.StickyToggle;
import com.steelaspect.stickytoggle.config.Toggle;
import com.steelaspect.stickytoggle.config.ToggleState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.Map;

/** S2C: full toggle state, sent as key/value pairs so unknown keys are skipped safely. */
public record SyncPayload(Map<Toggle, Boolean> values) implements CustomPayload {
	public static final CustomPayload.Id<SyncPayload> ID =
			new CustomPayload.Id<>(Identifier.of(StickyToggle.MOD_ID, "sync"));
	public static final PacketCodec<PacketByteBuf, SyncPayload> CODEC =
			CustomPayload.codecOf(SyncPayload::write, SyncPayload::read);

	public static SyncPayload of(ToggleState state) {
		return new SyncPayload(state.snapshot());
	}

	private static SyncPayload read(PacketByteBuf buf) {
		return new SyncPayload(readValues(buf));
	}

	private void write(PacketByteBuf buf) {
		writeValues(buf, values);
	}

	/** Reads toggle key/value pairs, skipping keys this version doesn't know. */
	static Map<Toggle, Boolean> readValues(PacketByteBuf buf) {
		EnumMap<Toggle, Boolean> map = new EnumMap<>(Toggle.class);
		int n = buf.readVarInt();
		for (int i = 0; i < n; i++) {
			Toggle t = Toggle.byKey(buf.readString());
			boolean v = buf.readBoolean();
			if (t != null) map.put(t, v);
		}
		return map;
	}

	static void writeValues(PacketByteBuf buf, Map<Toggle, Boolean> values) {
		buf.writeVarInt(values.size());
		values.forEach((t, v) -> {
			buf.writeString(t.key);
			buf.writeBoolean(v);
		});
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
