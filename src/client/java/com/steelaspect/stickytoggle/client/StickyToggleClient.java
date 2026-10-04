package com.steelaspect.stickytoggle.client;

import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import com.steelaspect.stickytoggle.net.SetPayload;
import com.steelaspect.stickytoggle.net.SyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StickyToggleClient implements ClientModInitializer {
	/** Called on the client thread whenever {@link StickyConfig#CLIENT} changes (sync, join, leave). */
	private static final List<Runnable> STATE_LISTENERS = new ArrayList<>();

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(SyncPayload.ID, (payload, context) -> {
			StickyConfig.CLIENT.setAll(true);
			payload.values().forEach(StickyConfig.CLIENT::set);
			stateChanged();
		});
		// Servers without the mod are vanilla, so never carry toggles over between servers.
		// The state is reset right away (as before); only the menu refresh waits for the client thread.
		ClientPlayConnectionEvents.INIT.register((handler, client) -> resetToVanilla(client));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetToVanilla(client));

		// The config menu and hotkeys are MaLiLib-based; without MaLiLib the commands still work.
		if (FabricLoader.getInstance().isModLoaded("malilib")) {
			com.steelaspect.stickytoggle.client.malilib.MalilibSetup.init();
		}
	}

	public static void addStateListener(Runnable listener) {
		STATE_LISTENERS.add(listener);
	}

	/**
	 * Asks the server to change some of the player's toggles. Returns false (and sends nothing) when not
	 * connected to a server that has StickyToggle 2.0+, which is the only place the settings live.
	 */
	public static boolean request(Map<Toggle, Boolean> values) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.getNetworkHandler() == null || !ClientPlayNetworking.canSend(SetPayload.ID)) {
			return false;
		}
		ClientPlayNetworking.send(new SetPayload(values));
		return true;
	}

	private static void resetToVanilla(MinecraftClient client) {
		StickyConfig.CLIENT.setAll(true);
		client.execute(StickyToggleClient::stateChanged);
	}

	private static void stateChanged() {
		STATE_LISTENERS.forEach(Runnable::run);
	}
}
