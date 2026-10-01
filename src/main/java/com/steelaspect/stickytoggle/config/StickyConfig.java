package com.steelaspect.stickytoggle.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.steelaspect.stickytoggle.StickyToggle;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player toggle settings, saved to config/stickytoggle.json.
 * Each player only affects themselves; players without an entry use the defaults.
 */
public final class StickyConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("stickytoggle.json");

	/** Starting values for players who have never used the command (logical server). */
	public static final ToggleState DEFAULTS = new ToggleState();
	/** Each player's own settings (logical server). */
	private static final Map<UUID, ToggleState> PLAYERS = new HashMap<>();
	/** The local player's settings on the logical client; filled by the sync payload, reset on disconnect. */
	public static final ToggleState CLIENT = new ToggleState();

	private StickyConfig() {
	}

	/** Toggle value for this entity. Only called for players. */
	public static boolean get(Entity entity, Toggle toggle) {
		if (entity.getEntityWorld().isClient()) {
			// The client only simulates its own player; other players are positioned by the server.
			return !(entity instanceof PlayerEntity player && player.isMainPlayer()) || CLIENT.get(toggle);
		}
		return forPlayer(entity.getUuid()).get(toggle);
	}

	public static ToggleState forPlayer(UUID uuid) {
		return PLAYERS.computeIfAbsent(uuid, id -> {
			ToggleState state = new ToggleState();
			state.copyFrom(DEFAULTS);
			return state;
		});
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(FILE)) {
			JsonObject json = GSON.fromJson(reader, JsonObject.class);
			if (json != null) {
				// Older files stored the toggle keys at the top level; treat them as defaults.
				read(json.has("defaults") ? json.getAsJsonObject("defaults") : json, DEFAULTS);
				if (json.has("players")) {
					for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("players").entrySet()) {
						try {
							read(e.getValue().getAsJsonObject(), forPlayer(UUID.fromString(e.getKey())));
						} catch (IllegalArgumentException ex) {
							StickyToggle.LOGGER.warn("Skipping invalid player UUID {} in {}", e.getKey(), FILE);
						}
					}
				}
			}
		} catch (Exception e) {
			StickyToggle.LOGGER.error("Failed to read {}, using vanilla defaults", FILE, e);
		}
		save(); // rewrites the file in the current format
	}

	public static void save() {
		JsonObject json = new JsonObject();
		json.add("defaults", write(DEFAULTS));
		JsonObject players = new JsonObject();
		PLAYERS.forEach((uuid, state) -> players.add(uuid.toString(), write(state)));
		json.add("players", players);
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(json, writer);
			}
		} catch (IOException e) {
			StickyToggle.LOGGER.error("Failed to write {}", FILE, e);
		}
	}

	private static void read(JsonObject json, ToggleState into) {
		for (Toggle t : Toggle.values()) {
			JsonElement e = json.get(t.key);
			if (e != null && e.isJsonPrimitive()) into.set(t, e.getAsBoolean());
		}
	}

	private static JsonObject write(ToggleState state) {
		JsonObject json = new JsonObject();
		state.snapshot().forEach((t, v) -> json.addProperty(t.key, v));
		return json;
	}
}
