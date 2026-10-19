package com.steelaspect.stickytoggle.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

/** Mod Menu's config button opens the MaLiLib menu, or the simple {@link StickyToggleScreen} without MaLiLib. */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		if (FabricLoader.getInstance().isModLoaded("malilib")) {
			return com.steelaspect.stickytoggle.client.malilib.MalilibSetup::createConfigScreen;
		}
		return StickyToggleScreen::new;
	}
}
