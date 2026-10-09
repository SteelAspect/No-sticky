package com.steelaspect.stickytoggle.test;

import com.steelaspect.stickytoggle.client.malilib.Configs;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Toggles survive leaving and rejoining the world, and a game restart (config file read again). */
public class StickyTogglePersistGameTest implements FabricClientGameTest {
	private static final Logger LOG = LoggerFactory.getLogger("StickyToggleTest");

	private final List<String> failures = new ArrayList<>();
	private int passes;

	@Override
	public void runTest(ClientGameTestContext context) {
		TestWorldSave save;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getClientWorld().waitForChunksRender();
			context.runOnClient(client -> client.getNetworkHandler().sendChatCommand("stickytoggle all on"));
			context.waitTicks(5);
			context.runOnClient(client -> client.getNetworkHandler().sendChatCommand("stickytoggle slime off"));
			context.waitTicks(5);
			check("slime off before leaving", !StickyConfig.CLIENT.get(Toggle.SLIME_BOUNCE), "client on");
			save = world.getWorldSave();
		}
		context.waitTicks(5);

		try (TestSingleplayerContext world = save.open()) {
			world.getClientWorld().waitForChunksRender();
			context.waitTicks(20);
			expectSlimeOff(context, "rejoin");
			check("rejoin: server still has slime off", !server(world, Toggle.SLIME_BOUNCE), "server on");
		}

		// Game restart: forget everything in memory and read the config file again.
		clearPlayers();
		StickyConfig.load();
		try (TestSingleplayerContext world = save.open()) {
			world.getClientWorld().waitForChunksRender();
			context.waitTicks(20);
			expectSlimeOff(context, "restart");
			check("restart: server still has slime off", !server(world, Toggle.SLIME_BOUNCE), "server on");
			context.runOnClient(client -> client.getNetworkHandler().sendChatCommand("stickytoggle all on"));
			context.waitTicks(5);
		}

		LOG.info("==== StickyToggle persist test summary: {} passed, {} failed ====", passes, failures.size());
		failures.forEach(f -> LOG.error("FAIL {}", f));
		if (!failures.isEmpty()) throw new AssertionError(String.join(" | ", failures));
	}

	private void expectSlimeOff(ClientGameTestContext context, String when) {
		check(when + ": client still has slime off", !StickyConfig.CLIENT.get(Toggle.SLIME_BOUNCE), "client on");
		check(when + ": honey still on", StickyConfig.CLIENT.get(Toggle.HONEY_WALL_SLIDE), "honey off");
		check(when + ": menu slime row OFF",
				!context.computeOnClient(c -> Configs.BLOCKS.get(Toggle.Group.SLIME.ordinal()).getBooleanValue()), "row on");
	}

	private static boolean server(TestSingleplayerContext world, Toggle toggle) {
		return world.getServer().computeOnServer(server -> StickyConfig.forPlayer(
				server.getPlayerManager().getPlayerList().get(0).getUuid()).get(toggle));
	}

	@SuppressWarnings("unchecked")
	private static void clearPlayers() {
		try {
			Field f = StickyConfig.class.getDeclaredField("PLAYERS");
			f.setAccessible(true);
			((Map<?, ?>) f.get(null)).clear();
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
