package com.steelaspect.stickytoggle.client;

import com.steelaspect.stickytoggle.StickyToggle;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The simple settings screen used when MaLiLib isn't installed (Mod Menu's config button opens it).
 * One ON/OFF button per block plus All Blocks. Like the MaLiLib menu it mirrors the player's settings on
 * the server ({@link StickyConfig#CLIENT}) and a click sends a request; nothing is saved on the client.
 */
public class StickyToggleScreen extends Screen {
	private static final int BUTTON_WIDTH = 150;
	private static final int ROW_HEIGHT = 24;

	private final Screen parent;
	private final List<Row> rows = new ArrayList<>();
	private boolean refused;

	public StickyToggleScreen(Screen parent) {
		super(Text.translatable(StickyToggle.MOD_ID + ".gui.title.configs"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		rows.clear();
		List<Toggle.Group> groups = Arrays.asList(Toggle.Group.values());
		int top = 40;
		int left = width / 2 - BUTTON_WIDTH - 5;
		for (int i = 0; i < groups.size(); i++) {
			Toggle.Group group = groups.get(i);
			int x = left + (i % 2) * (BUTTON_WIDTH + 10);
			int y = top + (i / 2) * ROW_HEIGHT;
			addRow(group, label("block" + camel(group.name())), x, y);
		}
		int allY = top + ((groups.size() + 1) / 2) * ROW_HEIGHT + 6;
		addRow(null, label("blockAll"), width / 2 - BUTTON_WIDTH / 2, allY);

		addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, b -> close())
				.dimensions(width / 2 - 100, height - 28, 200, 20).build());
		updateLabels();
	}

	private void addRow(Toggle.Group group, Text name, int x, int y) {
		ButtonWidget button = addDrawableChild(ButtonWidget.builder(name, b -> toggle(group))
				.dimensions(x, y, BUTTON_WIDTH, 20).build());
		rows.add(new Row(group, name, button));
	}

	/** Flips one block (or every block when {@code group} is null): OFF if it's fully on, otherwise back ON. */
	public void toggle(Toggle.Group group) {
		boolean value = !isOn(group);
		Map<Toggle, Boolean> values = new EnumMap<>(Toggle.class);
		for (Toggle t : Toggle.values()) {
			if (group == null || t.group == group) values.put(t, value);
		}
		refused = !StickyToggleClient.request(values);
	}

	/** A block shows ON only while all of its settings are on, like the MaLiLib menu. */
	public static boolean isOn(Toggle.Group group) {
		for (Toggle t : Toggle.values()) {
			if ((group == null || t.group == group) && !StickyConfig.CLIENT.get(t)) return false;
		}
		return true;
	}

	@Override
	public void tick() {
		updateLabels();
	}

	private void updateLabels() {
		for (Row row : rows) {
			row.button.setMessage(ScreenTexts.composeToggleText(row.name, isOn(row.group)));
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, 0xFFFFFFFF);
		context.drawCenteredTextWithShadow(textRenderer, Text.translatable(StickyToggle.MOD_ID + ".gui.screen.hint"),
				width / 2, 27, 0xFFA0A0A0);
		if (refused) {
			context.drawCenteredTextWithShadow(textRenderer, Text.translatable(StickyToggle.MOD_ID + ".message.not_available"),
					width / 2, height - 44, 0xFFFF6060);
		}
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	private static Text label(String option) {
		return Text.translatable(StickyToggle.MOD_ID + ".config.prettyName." + option);
	}

	private static String camel(String enumName) {
		StringBuilder sb = new StringBuilder();
		for (String part : enumName.toLowerCase(Locale.ROOT).split("_")) {
			sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}
		return sb.toString();
	}

	private record Row(Toggle.Group group, Text name, ButtonWidget button) {
	}
}
