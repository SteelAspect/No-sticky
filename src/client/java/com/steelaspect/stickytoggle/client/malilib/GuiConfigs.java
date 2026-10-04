package com.steelaspect.stickytoggle.client.malilib;

import com.steelaspect.stickytoggle.StickyToggle;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

/** Blocks tab (one row per block) and Settings tab (one row per setting), each with on/off and a hotkey. */
public class GuiConfigs extends GuiConfigsBase {
	private static Tab tab = Tab.BLOCKS;

	public GuiConfigs(Screen parent) {
		super(10, 50, StickyToggle.MOD_ID, parent, StickyToggle.MOD_ID + ".gui.title.configs");
	}

	public GuiConfigs() {
		this(null);
	}

	@Override
	public void initGui() {
		super.initGui();
		this.clearOptions();

		int x = 10;
		int y = 26;
		for (Tab t : Tab.values()) {
			ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, StringUtils.translate(t.translationKey));
			button.setEnabled(t != tab);
			this.addButton(button, new TabListener(t, this));
			x += button.getWidth() + 2;
		}
	}

	/** Redraws the rows, e.g. after the server confirmed a change that also moved other rows. */
	void refresh() {
		this.reCreateListWidget();
		this.initGui();
	}

	@Override
	protected int getConfigWidth() {
		return 204;
	}

	@Override
	public List<ConfigOptionWrapper> getConfigs() {
		return ConfigOptionWrapper.createFor(Configs.tab(tab == Tab.BLOCKS));
	}

	private enum Tab {
		BLOCKS(StickyToggle.MOD_ID + ".gui.button.config_gui.blocks"),
		SETTINGS(StickyToggle.MOD_ID + ".gui.button.config_gui.settings");

		private final String translationKey;

		Tab(String translationKey) {
			this.translationKey = translationKey;
		}
	}

	private record TabListener(Tab tab, GuiConfigs parent) implements IButtonActionListener {
		@Override
		public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
			GuiConfigs.tab = this.tab;
			this.parent.reCreateListWidget();
			this.parent.getListWidget().resetScrollbarPosition();
			this.parent.initGui();
		}
	}
}
