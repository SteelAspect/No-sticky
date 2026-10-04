package com.steelaspect.stickytoggle.client.malilib;

import com.steelaspect.stickytoggle.StickyToggle;
import com.steelaspect.stickytoggle.client.StickyToggleClient;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import net.minecraft.client.gui.screen.Screen;

/** Registers the menu with MaLiLib. Only loaded when MaLiLib is installed. */
public final class MalilibSetup implements IKeybindProvider {
	private static final MalilibSetup INSTANCE = new MalilibSetup();

	private MalilibSetup() {
	}

	public static void init() {
		InitializationHandler.getInstance().registerInitializationHandler(() -> {
			ConfigManager.getInstance().registerConfigHandler(StickyToggle.MOD_ID, Configs.INSTANCE);
			// This is what puts StickyToggle in the mod list at the top of every MaLiLib config screen.
			Registry.CONFIG_SCREEN.registerConfigScreenFactory(new ModInfo(StickyToggle.MOD_ID, "StickyToggle", GuiConfigs::new));
			InputEventHandler.getKeybindManager().registerKeybindProvider(INSTANCE);
			// On/off hotkeys keep MaLiLib's own toggle callback (it flips the option and shows a message);
			// the option's change callback then sends the request to the server.
			Configs.OPEN_MENU.getKeybind().setCallback((action, key) -> {
				GuiBase.openGui(new GuiConfigs());
				return true;
			});
			StickyToggleClient.addStateListener(Configs::refreshFromServer);
		});
	}

	/** Used by the Mod Menu button. */
	public static Screen createConfigScreen(Screen parent) {
		return new GuiConfigs(parent);
	}

	@Override
	public void addKeysToMap(IKeybindManager manager) {
		Configs.HOTKEYS.forEach(hotkey -> manager.addKeybindToMap(hotkey.getKeybind()));
	}

	@Override
	public void addHotkeys(IKeybindManager manager) {
		manager.addHotkeysForCategory("StickyToggle", StickyToggle.MOD_ID + ".hotkeys.category.generic", Configs.HOTKEYS);
	}
}
