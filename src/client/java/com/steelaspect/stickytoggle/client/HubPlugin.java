package com.steelaspect.stickytoggle.client;

import com.steelaspect.stickytoggle.StickyToggle;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import dev.steelaspect.cytrahub.api.CytraHubPlugin;
import dev.steelaspect.cytrahub.api.StatusLevel;
import dev.steelaspect.cytrahub.api.client.ClientHub;
import dev.steelaspect.cytrahub.api.client.SettingsBuilder;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * Alloy Hub page: the same rows as {@link StickyToggleScreen} (from {@link StickyOptions}) plus whether the server has
 * the mod. Only loaded with Alloy Hub. The Hub also creates this on a dedicated server, so client classes are only
 * touched inside {@link #registerClient}.
 */
public final class HubPlugin implements CytraHubPlugin {
	private static final String P = StickyOptions.LANG_PREFIX;

	@Override
	public void registerClient(ClientHub hub) {
		hub.page(StickyToggle.MOD_ID)
				.category("Movement")
				.description(Component.translatable(StickyToggle.MOD_ID + ".hub.description"))
				.needsServer(true)
				.commands("stickytoggle")
				.status(s -> s.row(Component.translatable(StickyToggle.MOD_ID + ".hub.server"),
						() -> Component.translatable(StickyToggle.MOD_ID + (StickyOptions.available() ? ".hub.server.ok" : ".hub.server.missing")),
						() -> StickyOptions.available() ? StatusLevel.GOOD : StatusLevel.WARN))
				.settings(HubPlugin::settings);
	}

	private static void settings(SettingsBuilder s) {
		s.section(Component.translatable(P + "blocks"));
		for (Toggle.Group group : Toggle.Group.values()) {
			blockRow(s, group);
		}
		blockRow(s, null);
		s.section(Component.translatable(P + "settings"));
		for (Toggle toggle : Toggle.values()) {
			s.toggle(toggle.key, Component.translatable(P + toggle.key), () -> StickyConfig.CLIENT.get(toggle),
							v -> StickyToggleClient.request(Map.of(toggle, v)))
					.description(Component.translatable(P + toggle.key + ".hint"))
					.defaultValue(true)
					.enabledWhen(StickyOptions::available, StickyOptions.UNAVAILABLE);
		}
	}

	private static void blockRow(SettingsBuilder s, Toggle.Group group) {
		String id = StickyOptions.id(group);
		s.toggle(id, Component.translatable(P + id), () -> StickyOptions.isOn(group), v -> StickyOptions.set(group, v))
				.description(Component.translatable(P + id + ".hint"))
				.defaultValue(true)
				.enabledWhen(StickyOptions::available, StickyOptions.UNAVAILABLE);
	}
}
