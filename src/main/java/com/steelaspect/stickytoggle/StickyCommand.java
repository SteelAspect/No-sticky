package com.steelaspect.stickytoggle;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import com.steelaspect.stickytoggle.config.ToggleState;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

/** /stickytoggle <slime|honey> <on|off>: any player, changes only their own settings. */
public final class StickyCommand {
	private StickyCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(literal("stickytoggle")
				.then(block("slime"))
				.then(block("honey")));
	}

	private static LiteralArgumentBuilder<ServerCommandSource> block(String block) {
		return literal(block)
				.then(literal("on").executes(ctx -> set(ctx, block, true)))
				.then(literal("off").executes(ctx -> set(ctx, block, false)));
	}

	/** Sets every toggle of one block for the player running the command; the other block is left alone. */
	private static int set(CommandContext<ServerCommandSource> ctx, String block, boolean value) throws CommandSyntaxException {
		ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
		ToggleState state = StickyConfig.forPlayer(player.getUuid());
		for (Toggle t : Toggle.values()) {
			if (t.key.startsWith(block + ".")) state.set(t, value);
		}
		StickyConfig.save();
		StickyToggle.sync(player);
		ctx.getSource().sendFeedback(() -> Text.literal(capitalize(block) + " effects " + (value ? "on" : "off") + " for you"), false);
		return 1;
	}

	private static String capitalize(String s) {
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
