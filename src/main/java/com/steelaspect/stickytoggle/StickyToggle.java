package com.steelaspect.stickytoggle;

import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.net.SyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StickyToggle implements ModInitializer {
	public static final String MOD_ID = "stickytoggle";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		StickyConfig.load();
		PayloadTypeRegistry.playS2C().register(SyncPayload.ID, SyncPayload.CODEC);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.getPlayer()));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				StickyCommand.register(dispatcher));
	}

	/** Sends a player their own settings, warning if their client lacks the mod. */
	public static void sync(ServerPlayerEntity player) {
		if (ServerPlayNetworking.canSend(player, SyncPayload.ID)) {
			ServerPlayNetworking.send(player, SyncPayload.of(StickyConfig.forPlayer(player.getUuid())));
		} else {
			LOGGER.warn("Player {} joined without StickyToggle installed; their movement prediction "
					+ "will not match server toggles (expect rubber-banding on slime/honey).",
					player.getName().getString());
		}
	}
}
