package com.steelaspect.stickytoggle;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/** /stickytoggle list | reset | preset <name> | <key> <true|false>  (permission level 2) */
public final class StickyCommand {
	private static final String[] PRESETS = {"vanilla", "noslime", "nohoney", "allOff"};

	private StickyCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(literal("stickytoggle")
				.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
				.then(literal("list").executes(StickyCommand::list))
				.then(literal("reset").executes(ctx -> preset(ctx, "vanilla")))
				.then(literal("preset")
						.then(argument("name", StringArgumentType.word())
								.suggests((ctx, b) -> CommandSource.suggestMatching(PRESETS, b))
								.executes(ctx -> preset(ctx, StringArgumentType.getString(ctx, "name")))))
				.then(argument("key", StringArgumentType.string())
						.suggests((ctx, b) -> CommandSource.suggestMatching(
								Arrays.stream(Toggle.values()).map(t -> t.key), b))
						.then(argument("value", BoolArgumentType.bool())
								.executes(StickyCommand::set))));
	}

	private static int list(CommandContext<ServerCommandSource> ctx) {
		ctx.getSource().sendFeedback(() -> Text.literal("StickyToggle (players only):").formatted(Formatting.GOLD), false);
		for (Toggle t : Toggle.values()) {
			boolean v = StickyConfig.SERVER.get(t);
			ctx.getSource().sendFeedback(() -> Text.literal(" " + t.key + " = ")
					.append(Text.literal(String.valueOf(v)).formatted(v ? Formatting.GREEN : Formatting.RED)), false);
		}
		return Toggle.values().length;
	}

	private static int set(CommandContext<ServerCommandSource> ctx) {
		String key = StringArgumentType.getString(ctx, "key");
		Toggle t = Toggle.byKey(key);
		if (t == null) {
			ctx.getSource().sendError(Text.literal("Unknown toggle: " + key));
			return 0;
		}
		boolean value = BoolArgumentType.getBool(ctx, "value");
		StickyConfig.SERVER.set(t, value);
		apply(ctx);
		ctx.getSource().sendFeedback(() -> Text.literal("Set " + t.key + " = " + value), true);
		return 1;
	}

	private static int preset(CommandContext<ServerCommandSource> ctx, String name) {
		switch (name) {
			case "vanilla" -> StickyConfig.SERVER.setAll(true);
			case "allOff" -> StickyConfig.SERVER.setAll(false);
			case "noslime", "nohoney" -> {
				StickyConfig.SERVER.setAll(true);
				String prefix = name.equals("noslime") ? "slime." : "honey.";
				for (Toggle t : Toggle.values()) {
					if (t.key.startsWith(prefix)) StickyConfig.SERVER.set(t, false);
				}
			}
			default -> {
				ctx.getSource().sendError(Text.literal("Unknown preset: " + name + " (vanilla, noslime, nohoney, allOff)"));
				return 0;
			}
		}
		apply(ctx);
		ctx.getSource().sendFeedback(() -> Text.literal("Applied preset " + name), true);
		return 1;
	}

	private static void apply(CommandContext<ServerCommandSource> ctx) {
		StickyConfig.save();
		StickyToggle.syncAll(ctx.getSource().getServer());
	}
}
