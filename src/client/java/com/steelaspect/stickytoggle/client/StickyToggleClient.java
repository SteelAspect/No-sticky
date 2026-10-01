package com.steelaspect.stickytoggle.client;

import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.net.SyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class StickyToggleClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(SyncPayload.ID, (payload, context) -> {
			StickyConfig.CLIENT.setAll(true);
			payload.values().forEach(StickyConfig.CLIENT::set);
		});
		// Servers without the mod are vanilla, so never carry toggles over between servers.
		ClientPlayConnectionEvents.INIT.register((handler, client) -> StickyConfig.CLIENT.setAll(true));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> StickyConfig.CLIENT.setAll(true));
	}
}
