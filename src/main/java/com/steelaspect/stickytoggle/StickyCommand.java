package com.steelaspect.stickytoggle;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import com.steelaspect.stickytoggle.config.ToggleState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.literal;

/** /stickytoggle <group|all> <on|off>: any player, changes only their own settings. */
public final class StickyCommand {
	private StickyCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> root = literal("stickytoggle");
		for (Toggle.Group group : Toggle.Group.values()) {
			root.then(onOff(group.command, group));
		}
		dispatcher.register(root.then(onOff("all", null)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> onOff(String word, Toggle.Group group) {
		return literal(word)
				.then(literal("on").executes(ctx -> set(ctx, group, true)))
				.then(literal("off").executes(ctx -> set(ctx, group, false)));
	}

	/**
	 * Sets every toggle of one group for the player running the command; other groups are left alone.
	 * A null group ("all") sets every toggle at once.
	 */
	private static int set(CommandContext<CommandSourceStack> ctx, Toggle.Group group, boolean value) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ToggleState state = StickyConfig.forPlayer(player.getUUID());
		for (Toggle t : Toggle.values()) {
			if (group == null || t.group == group) state.set(t, value);
		}
		StickyConfig.save();
		StickyToggle.sync(player);
		String name = group == null ? "All block" : group.displayName;
		ctx.getSource().sendSuccess(() -> Component.literal(name + " effects " + (value ? "on" : "off") + " for you"), false);
		return 1;
	}
}
