package com.steelaspect.stickytoggle.client;

import com.steelaspect.stickytoggle.StickyToggle;
import com.steelaspect.stickytoggle.config.Toggle;
import dev.steelaspect.alloyui.AlloyScreen;
import dev.steelaspect.alloyui.widget.Button;
import dev.steelaspect.alloyui.widget.Card;
import dev.steelaspect.alloyui.widget.Container;
import dev.steelaspect.alloyui.widget.ScrollPanel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The Nocturne settings screen: a toggle per block and per setting ({@link StickyOptions}, also used by the Hub page).
 * Mod Menu opens it when MaLiLib isn't installed; with MaLiLib, Mod Menu opens the MaLiLib menu (it has hotkeys).
 */
public class StickyToggleScreen extends AlloyScreen {
	private static final float MAX_WIDTH = 360;
	private static final float FOOTER = 24;

	private final Card panel = new Card(Component.translatable(StickyToggle.MOD_ID + ".gui.title.configs"));
	private final ScrollPanel scroll = new ScrollPanel(StickyOptions.options().build());
	private final Button done = new Button(Component.translatable("gui.done"), this::onClose).style(Button.Style.PRIMARY);

	public StickyToggleScreen(@Nullable Screen parent) {
		super(Component.translatable(StickyToggle.MOD_ID + ".gui.title.configs"), parent);
		Container root = new Container() {
			@Override
			public float measure(float width) {
				return 0;
			}
		};
		root.add(panel);
		root.add(scroll);
		root.add(done);
		setRoot(root);
	}

	@Override
	protected void layout(float width, float height) {
		super.layout(width, height);
		float[] frame = frameBounds(width, height);
		float w = Math.min(MAX_WIDTH, frame[2]);
		float inner = w - 12;
		float h = Math.min(14 + 6 + scroll.measure(inner) + FOOTER + 6, frame[3]);
		float x = frame[0] + (frame[2] - w) / 2;
		float y = frame[1] + (frame[3] - h) / 2;
		panel.setBounds(x, y, w, h);
		scroll.setBounds(x + 6, y + 20, inner, h - 20 - FOOTER - 6);
		float bw = Math.min(120, inner);
		done.setBounds(x + (w - bw) / 2, y + h - FOOTER - 2, bw, 18);
	}

	/** Flips one block (or every block when {@code group} is null): OFF if it's fully on, otherwise back ON. */
	public void toggle(Toggle.@Nullable Group group) {
		StickyOptions.set(group, !StickyOptions.isOn(group));
	}

	/** A block shows ON only while all of its settings are on. */
	public static boolean isOn(Toggle.@Nullable Group group) {
		return StickyOptions.isOn(group);
	}
}
