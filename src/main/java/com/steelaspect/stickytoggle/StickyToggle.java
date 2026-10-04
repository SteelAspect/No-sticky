package com.steelaspect.stickytoggle;

import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.ToggleState;
import com.steelaspect.stickytoggle.net.SetPayload;
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
		PayloadTypeRegistry.playC2S().register(SetPayload.ID, SetPayload.CODEC);

		// Config menu and hotkeys: a player changing their own toggles, same as the command.
		ServerPlayNetworking.registerGlobalReceiver(SetPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ToggleState state = StickyConfig.forPlayer(player.getUuid());
			payload.values().forEach(state::set);
			StickyConfig.save();
			sync(player);
		});

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
					+ "will not match server toggles (expect rubber-banding where toggles are off).",
					player.getName().getString());
		}
	}
}
