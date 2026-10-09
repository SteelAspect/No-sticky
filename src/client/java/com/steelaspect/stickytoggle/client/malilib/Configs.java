package com.steelaspect.stickytoggle.client.malilib;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.steelaspect.stickytoggle.StickyToggle;
import com.steelaspect.stickytoggle.client.StickyToggleClient;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The MaLiLib menu: an on/off button and an optional hotkey for every block and every setting.
 * The on/off values are not stored here: they mirror the player's settings on the server
 * ({@link StickyConfig#CLIENT}), and changing one sends a request to the server. Only the hotkeys
 * are saved, to config/stickytoggle-client.json (the server's config is config/stickytoggle.json).
 */
public final class Configs implements IConfigHandler {
	public static final Configs INSTANCE = new Configs();
	private static final String FILE_NAME = StickyToggle.MOD_ID + "-client.json";
	private static final String PREFIX = StickyToggle.MOD_ID + ".config";

	/** Which toggles each on/off option controls. */
	private static final Map<ConfigBooleanHotkeyed, List<Toggle>> TARGETS = new IdentityHashMap<>();

	/** One row per block (and "All"), switching every setting of that block. */
	public static final ImmutableList<ConfigBooleanHotkeyed> BLOCKS;
	/** One row per individual setting. */
	public static final ImmutableList<ConfigBooleanHotkeyed> SETTINGS;
	/** Unbound by default, like every hotkey of this mod. */
	public static final ConfigHotkey OPEN_MENU = new ConfigHotkey("openConfigMenu", "").apply(PREFIX);

	static {
		ImmutableList.Builder<ConfigBooleanHotkeyed> blocks = ImmutableList.builder();
		for (Toggle.Group group : Toggle.Group.values()) {
			List<Toggle> toggles = Arrays.stream(Toggle.values()).filter(t -> t.group == group).toList();
			blocks.add(option("block" + camel(group.name()), toggles));
		}
		blocks.add(option("blockAll", List.of(Toggle.values())));
		BLOCKS = blocks.build();

		ImmutableList.Builder<ConfigBooleanHotkeyed> settings = ImmutableList.builder();
		for (Toggle toggle : Toggle.values()) {
			settings.add(option(toggle.key.replace(".", "_"), List.of(toggle)));
		}
		SETTINGS = settings.build();
	}

	/** Every hotkey, for the keybind manager and the config file. */
	public static final ImmutableList<IHotkey> HOTKEYS = ImmutableList.<IHotkey>builder()
			.addAll(BLOCKS).addAll(SETTINGS).add(OPEN_MENU).build();

	private static boolean refreshing;

	private Configs() {
	}

	private static ConfigBooleanHotkeyed option(String name, List<Toggle> toggles) {
		ConfigBooleanHotkeyed option = new ConfigBooleanHotkeyed(name, true, "").apply(PREFIX);
		option.setValueChangeCallback(Configs::onChanged);
		TARGETS.put(option, toggles);
		return option;
	}

	/** SOUL_SAND -> SoulSand */
	private static String camel(String enumName) {
		StringBuilder sb = new StringBuilder();
		for (String part : enumName.toLowerCase(Locale.ROOT).split("_")) {
			sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}
		return sb.toString();
	}

	/** A button click, a hotkey or a reset changed an option: ask the server for it. */
	private static void onChanged(ConfigBoolean option) {
		if (refreshing) return;
		boolean value = option.getBooleanValue();
		Map<Toggle, Boolean> request = new EnumMap<>(Toggle.class);
		TARGETS.get(option).forEach(t -> request.put(t, value));
		if (!StickyToggleClient.request(request)) {
			InfoUtils.showGuiOrInGameMessage(fi.dy.masa.malilib.gui.Message.MessageType.WARNING,
					StickyToggle.MOD_ID + ".message.not_available");
			refreshFromServer();
		}
		// Otherwise the server answers with a sync, which calls refreshFromServer().
	}

	/** Makes every on/off option show the player's current server-side settings. */
	public static void refreshFromServer() {
		refreshing = true;
		try {
			TARGETS.forEach((option, toggles) ->
					option.setBooleanValue(toggles.stream().allMatch(StickyConfig.CLIENT::get)));
		} finally {
			refreshing = false;
		}
		if (MinecraftClient.getInstance().currentScreen instanceof GuiConfigs gui) {
			gui.refresh();
		}
	}

	@Override
	public void load() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		if (Files.isRegularFile(file) && Files.isReadable(file)) {
			JsonElement element = JsonUtils.parseJsonFile(file);
			if (element != null && element.isJsonObject()) {
				// MaLiLib loads configs on every world join and readHotkeys fires each row's change callback;
				// without this guard every row asked the server to turn its toggles back ON.
				refreshing = true;
				try {
					ConfigUtils.readHotkeys(element.getAsJsonObject(), "Hotkeys", HOTKEYS);
				} finally {
					refreshing = false;
				}
			}
		}
		refreshFromServer();
	}

	@Override
	public void save() {
		Path dir = FabricLoader.getInstance().getConfigDir();
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			StickyToggle.LOGGER.error("Could not create config directory {}", dir, e);
			return;
		}
		JsonObject root = new JsonObject();
		ConfigUtils.writeHotkeys(root, "Hotkeys", HOTKEYS);
		JsonUtils.writeJsonToFile(root, dir.resolve(FILE_NAME));
	}

	/** Rows for one tab. */
	static List<IConfigBase> tab(boolean blocks) {
		List<IConfigBase> list = new ArrayList<>(blocks ? BLOCKS : SETTINGS);
		if (blocks) list.add(OPEN_MENU);
		return list;
	}
}
