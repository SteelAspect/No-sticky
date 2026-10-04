package com.steelaspect.stickytoggle.test;

import com.steelaspect.stickytoggle.client.malilib.Configs;
import com.steelaspect.stickytoggle.client.malilib.GuiConfigs;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import com.terraformersmc.modmenu.api.ModMenuApi;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.registry.Registry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** The MaLiLib menu: rows send requests to the server, the server's answer updates the rows, hotkeys work. */
public class StickyToggleMenuGameTest implements FabricClientGameTest {
	private static final Logger LOG = LoggerFactory.getLogger("StickyToggleTest");

	private final List<String> failures = new ArrayList<>();
	private int passes;

	@Override
	public void runTest(ClientGameTestContext context) {
		testRegistration(context);

		// Not connected: a click is refused and the row snaps back to ON.
		context.runOnClient(client -> block(Toggle.Group.SOUL_SAND).setBooleanValue(false));
		check("title screen: click refused, row back to ON", context.computeOnClient(c -> block(Toggle.Group.SOUL_SAND).getBooleanValue()), "row stayed off");

		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getClientWorld().waitForChunksRender();

			// A block row (what a click on its ON/OFF button does).
			context.runOnClient(client -> block(Toggle.Group.SOUL_SAND).setBooleanValue(false));
			context.waitTicks(5);
			check("soul sand row off -> server setting off", !server(world, Toggle.SOUL_SAND_SLOWDOWN), "server still on");
			check("soul sand row off -> settings row off", !context.computeOnClient(c -> setting(Toggle.SOUL_SAND_SLOWDOWN).getBooleanValue()), "settings row on");
			check("soul sand row off -> client movement state off", !StickyConfig.CLIENT.get(Toggle.SOUL_SAND_SLOWDOWN), "client still on");

			// A multi-setting block row switches all of its settings, and "All Blocks" shows OFF.
			context.runOnClient(client -> block(Toggle.Group.SLIME).setBooleanValue(false));
			context.waitTicks(5);
			check("slime row off -> all 3 slime settings off on the server",
					!server(world, Toggle.SLIME_BOUNCE) && !server(world, Toggle.SLIME_WALK_SLOWDOWN) && !server(world, Toggle.SLIME_SLIPPERINESS), "some still on");
			check("honey untouched by the slime row", server(world, Toggle.HONEY_WALL_SLIDE), "honey changed");
			check("All Blocks row shows OFF while something is off", !context.computeOnClient(c -> Configs.BLOCKS.getLast().getBooleanValue()), "shows on");

			// A single setting row: only that setting, and the slime block row shows OFF while one is off.
			context.runOnClient(client -> block(Toggle.Group.SLIME).setBooleanValue(true));
			context.waitTicks(5);
			context.runOnClient(client -> setting(Toggle.SLIME_BOUNCE).setBooleanValue(false));
			context.waitTicks(5);
			check("slime bounce row off -> only bounce off",
					!server(world, Toggle.SLIME_BOUNCE) && server(world, Toggle.SLIME_SLIPPERINESS), "wrong slime state");
			check("slime block row shows OFF while one slime setting is off", !context.computeOnClient(c -> block(Toggle.Group.SLIME).getBooleanValue()), "shows on");

			// The command updates the menu too.
			context.runOnClient(client -> client.getNetworkHandler().sendChatCommand("stickytoggle ice off"));
			context.waitTicks(5);
			check("/stickytoggle ice off -> ice rows off",
					!context.computeOnClient(c -> block(Toggle.Group.ICE).getBooleanValue() || setting(Toggle.ICE_SLIPPERINESS).getBooleanValue()), "ice row on");

			// Hotkeys: bind the cobweb setting and the bubble column block, press them in game.
			context.runOnClient(client -> {
				client.setScreen(null);
				setting(Toggle.COBWEB_SLOWDOWN).getKeybind().setValueFromString("K");
				block(Toggle.Group.BUBBLE_COLUMN).getKeybind().setValueFromString("LEFT_SHIFT,B");
				InputEventHandler.getKeybindManager().updateUsedKeys();
			});
			context.waitTicks(2);
			context.getInput().pressKey(GLFW.GLFW_KEY_K);
			context.waitTicks(5);
			check("cobweb hotkey -> server cobweb off", !server(world, Toggle.COBWEB_SLOWDOWN), "still on");
			context.getInput().pressKey(GLFW.GLFW_KEY_K);
			context.waitTicks(5);
			check("cobweb hotkey again -> back on", server(world, Toggle.COBWEB_SLOWDOWN), "still off");
			context.getInput().holdShift();
			context.getInput().pressKey(GLFW.GLFW_KEY_B);
			context.getInput().releaseShift();
			context.waitTicks(5);
			check("shift+B block hotkey -> bubble column off", !server(world, Toggle.BUBBLE_COLUMN_PUSH), "still on");

			// Screenshots of both tabs with a few rows off.
			for (String tab : new String[]{"BLOCKS", "SETTINGS"}) {
				context.runOnClient(client -> {
					setTab(tab);
					client.setScreen(new GuiConfigs());
				});
				context.waitTicks(10);
				LOG.info("Menu screenshot: {}", context.takeScreenshot(TestScreenshotOptions.of("stickytoggle-menu-" + tab.toLowerCase())));
			}
			// While the menu is open, a server change redraws it.
			context.runOnClient(client -> client.getNetworkHandler().sendChatCommand("stickytoggle all on"));
			context.waitTicks(5);
			check("menu open + all on -> every row ON",
					context.computeOnClient(c -> Configs.BLOCKS.stream().allMatch(ConfigBooleanHotkeyed::getBooleanValue)
							&& Configs.SETTINGS.stream().allMatch(ConfigBooleanHotkeyed::getBooleanValue)), "a row is off");
			context.runOnClient(client -> client.setScreen(null));

			// Only hotkeys are saved, to the client file.
			context.runOnClient(client -> Configs.INSTANCE.save());
			Path file = FabricLoader.getInstance().getConfigDir().resolve("stickytoggle-client.json");
			String json = read(file);
			check("hotkeys saved to stickytoggle-client.json", json.contains("\"K\"") && json.contains("LEFT_SHIFT,B"), json);

			// Leave the world off, to check the menu resets on disconnect.
			context.runOnClient(client -> client.getNetworkHandler().sendChatCommand("stickytoggle water off"));
			context.waitTicks(5);
		}
		context.waitTicks(5);
		check("after leaving: every row back to ON (vanilla)",
				context.computeOnClient(c -> Configs.SETTINGS.stream().allMatch(ConfigBooleanHotkeyed::getBooleanValue)), "a row is still off");

		LOG.info("==== StickyToggle menu test summary: {} passed, {} failed ====", passes, failures.size());
		failures.forEach(f -> LOG.error("FAIL {}", f));
		if (!failures.isEmpty()) throw new AssertionError(String.join(" | ", failures));
	}

	private void testRegistration(ClientGameTestContext context) {
		boolean inMalilibList = Registry.CONFIG_SCREEN.getAllModsWithConfigScreens().stream()
				.anyMatch(info -> info.modId().equals("stickytoggle"));
		check("listed in MaLiLib's config screen mod list", inMalilibList, "not registered");

		Screen fromModMenu = context.computeOnClient(client -> FabricLoader.getInstance()
				.getEntrypointContainers("modmenu", ModMenuApi.class).stream()
				.filter(c -> c.getProvider().getMetadata().getId().equals("stickytoggle"))
				.findFirst().map(c -> (Screen) c.getEntrypoint().getModConfigScreenFactory().create(null)).orElse(null));
		check("Mod Menu config button opens the menu", fromModMenu instanceof GuiConfigs, String.valueOf(fromModMenu));
	}

	private static ConfigBooleanHotkeyed block(Toggle.Group group) {
		return Configs.BLOCKS.get(group.ordinal());
	}

	private static ConfigBooleanHotkeyed setting(Toggle toggle) {
		return Configs.SETTINGS.get(toggle.ordinal());
	}

	private static boolean server(TestSingleplayerContext world, Toggle toggle) {
		return world.getServer().computeOnServer(server -> StickyConfig.forPlayer(
				server.getPlayerManager().getPlayerList().get(0).getUuid()).get(toggle));
	}

	private static String read(Path file) {
		try {
			return Files.readString(file);
		} catch (Exception e) {
			return "<unreadable: " + e + ">";
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void setTab(String name) {
		try {
			Field field = GuiConfigs.class.getDeclaredField("tab");
			field.setAccessible(true);
			Class<? extends Enum> type = (Class<? extends Enum>) field.getType();
			field.set(null, Enum.valueOf(type, name));
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private void check(String name, boolean ok, String detail) {
		if (ok) {
			passes++;
			LOG.info("PASS {}", name);
		} else {
			failures.add(name + ": " + detail);
			LOG.error("FAIL {}: {}", name, detail);
		}
	}
}
