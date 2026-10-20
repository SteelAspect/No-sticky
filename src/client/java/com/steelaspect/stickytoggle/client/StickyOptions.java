package com.steelaspect.stickytoggle.client;

import com.steelaspect.stickytoggle.StickyToggle;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import com.steelaspect.stickytoggle.net.SetPayload;
import dev.steelaspect.alloyui.widget.Options;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * The settings rows, shared by {@link StickyToggleScreen} and the Alloy Hub page ({@link HubPlugin}).
 * Values are the player's settings on the server, mirrored in {@link StickyConfig#CLIENT}; changing one sends a request.
 * Labels and hints are {@code stickytoggle.option.<id>} and {@code .hint}.
 */
public final class StickyOptions {
	public static final String LANG_PREFIX = StickyToggle.MOD_ID + ".option.";
	public static final Component UNAVAILABLE = Component.translatable(LANG_PREFIX + "unavailable");

	private StickyOptions() {
	}

	public static Options options() {
		Options o = new Options(LANG_PREFIX).section("blocks");
		for (Toggle.Group group : Toggle.Group.values()) {
			o.toggle(id(group), () -> isOn(group), v -> set(group, v), true).disabledWhen(() -> !available(), UNAVAILABLE);
		}
		o.toggle(id(null), () -> isOn(null), v -> set(null, v), true).disabledWhen(() -> !available(), UNAVAILABLE);
		o.section("settings");
		for (Toggle toggle : Toggle.values()) {
			o.toggle(toggle.key, () -> StickyConfig.CLIENT.get(toggle), v -> StickyToggleClient.request(Map.of(toggle, v)), true)
					.disabledWhen(() -> !available(), UNAVAILABLE);
		}
		return o;
	}

	/** Option id of a block row: its command word, or "all" for every block. */
	public static String id(Toggle.@Nullable Group group) {
		return group == null ? "all" : group.command;
	}

	/** True when the server has the mod, so there are settings to change. */
	public static boolean available() {
		return Minecraft.getInstance().getConnection() != null && ClientPlayNetworking.canSend(SetPayload.ID);
	}

	/** A block (or every block when {@code group} is null) shows ON only while all of its settings are on. */
	public static boolean isOn(Toggle.@Nullable Group group) {
		for (Toggle t : Toggle.values()) {
			if ((group == null || t.group == group) && !StickyConfig.CLIENT.get(t)) return false;
		}
		return true;
	}

	/** Asks the server to set every setting of a block (or of every block). Returns false when not available. */
	public static boolean set(Toggle.@Nullable Group group, boolean value) {
		Map<Toggle, Boolean> values = new EnumMap<>(Toggle.class);
		for (Toggle t : Toggle.values()) {
			if (group == null || t.group == group) values.put(t, value);
		}
		return StickyToggleClient.request(values);
	}
}
