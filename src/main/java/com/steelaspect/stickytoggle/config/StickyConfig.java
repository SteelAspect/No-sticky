package com.steelaspect.stickytoggle.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.steelaspect.stickytoggle.StickyToggle;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Holds the server-authoritative config (saved to config/stickytoggle.json)
 * and the client copy received from the server.
 */
public final class StickyConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("stickytoggle.json");

	/** Used by the logical server. */
	public static final ToggleState SERVER = new ToggleState();
	/** Used by the logical client; filled by the sync payload, reset to vanilla on disconnect. */
	public static final ToggleState CLIENT = new ToggleState();

	private StickyConfig() {
	}

	/** Toggle value that applies to this entity's side. */
	public static boolean get(Entity entity, Toggle toggle) {
		return (entity.getEntityWorld().isClient() ? CLIENT : SERVER).get(toggle);
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(FILE)) {
			JsonObject json = GSON.fromJson(reader, JsonObject.class);
			if (json != null) {
				for (Toggle t : Toggle.values()) {
					JsonElement e = json.get(t.key);
					if (e != null && e.isJsonPrimitive()) SERVER.set(t, e.getAsBoolean());
				}
			}
		} catch (Exception e) {
			StickyToggle.LOGGER.error("Failed to read {}, using vanilla defaults", FILE, e);
		}
		save(); // writes any keys missing from an older file
	}

	public static void save() {
		JsonObject json = new JsonObject();
		SERVER.snapshot().forEach((t, v) -> json.addProperty(t.key, v));
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(json, writer);
			}
		} catch (IOException e) {
			StickyToggle.LOGGER.error("Failed to write {}", FILE, e);
		}
	}
}
