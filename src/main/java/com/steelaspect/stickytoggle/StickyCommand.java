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

/** /stickytoggle <group|all> <on|off>: any player, changes only their own settings. */
public final class StickyCommand {
	private StickyCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> root = literal("stickytoggle");
		for (Toggle.Group group : Toggle.Group.values()) {
			root.then(onOff(group.command, group));
		}
		dispatcher.register(root.then(onOff("all", null)));
	}

	private static LiteralArgumentBuilder<ServerCommandSource> onOff(String word, Toggle.Group group) {
		return literal(word)
				.then(literal("on").executes(ctx -> set(ctx, group, true)))
				.then(literal("off").executes(ctx -> set(ctx, group, false)));
	}

	/**
	 * Sets every toggle of one group for the player running the command; other groups are left alone.
	 * A null group ("all") sets every toggle at once.
	 */
	private static int set(CommandContext<ServerCommandSource> ctx, Toggle.Group group, boolean value) throws CommandSyntaxException {
		ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
		ToggleState state = StickyConfig.forPlayer(player.getUuid());
		for (Toggle t : Toggle.values()) {
			if (group == null || t.group == group) state.set(t, value);
		}
		StickyConfig.save();
		StickyToggle.sync(player);
		String name = group == null ? "All block" : group.displayName;
		ctx.getSource().sendFeedback(() -> Text.literal(name + " effects " + (value ? "on" : "off") + " for you"), false);
		return 1;
	}
}
