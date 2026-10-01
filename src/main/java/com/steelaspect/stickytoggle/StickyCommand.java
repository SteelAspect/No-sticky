package com.steelaspect.stickytoggle;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

/** /stickytoggle <slime|honey> <on|off>  (permission level 2) */
public final class StickyCommand {
	private StickyCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(literal("stickytoggle")
				.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
				.then(block("slime"))
				.then(block("honey")));
	}

	private static LiteralArgumentBuilder<ServerCommandSource> block(String block) {
		return literal(block)
				.then(literal("on").executes(ctx -> set(ctx, block, true)))
				.then(literal("off").executes(ctx -> set(ctx, block, false)));
	}

	/** Sets every toggle of one block; the other block is left alone. */
	private static int set(CommandContext<ServerCommandSource> ctx, String block, boolean value) {
		for (Toggle t : Toggle.values()) {
			if (t.key.startsWith(block + ".")) StickyConfig.SERVER.set(t, value);
		}
		StickyConfig.save();
		StickyToggle.syncAll(ctx.getSource().getServer());
		ctx.getSource().sendFeedback(() -> Text.literal("All " + block + " effects " + (value ? "on" : "off")), true);
		return 1;
	}
}
