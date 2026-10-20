package com.steelaspect.stickytoggle.test;

import com.steelaspect.stickytoggle.client.StickyToggleScreen;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/** The simple screen Mod Menu opens without MaLiLib: buttons send requests, labels follow the server. */
public class StickyToggleScreenGameTest implements FabricClientGameTest {
	private static final Logger LOG = LoggerFactory.getLogger("StickyToggleTest");

	private final List<String> failures = new ArrayList<>();
	private int passes;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getClientWorld().waitForChunksRender();
			StickyToggleScreen screen = context.computeOnClient(client -> {
				StickyToggleScreen s = new StickyToggleScreen(null);
				client.setScreen(s);
				return s;
			});
			context.waitTicks(2);

			context.runOnClient(client -> screen.toggle(Toggle.Group.SLIME));
			context.waitTicks(5);
			check("slime button -> all 3 slime settings off on the server",
					!server(world, Toggle.SLIME_BOUNCE) && !server(world, Toggle.SLIME_WALK_SLOWDOWN) && !server(world, Toggle.SLIME_SLIPPERINESS), "some still on");
			check("honey untouched by the slime button", server(world, Toggle.HONEY_WALL_SLIDE), "honey changed");
			check("slime shows OFF, All Blocks shows OFF",
					!context.computeOnClient(c -> StickyToggleScreen.isOn(Toggle.Group.SLIME) || StickyToggleScreen.isOn(null)), "shows on");
			check("client movement state follows", !StickyConfig.CLIENT.get(Toggle.SLIME_BOUNCE), "client still on");

			context.runOnClient(client -> screen.toggle(Toggle.Group.ICE));
			context.waitTicks(10);
			LOG.info("Screen screenshot: {}", context.takeScreenshot(TestScreenshotOptions.of("stickytoggle-screen")));

			// All Blocks while something is off turns everything back on.
			context.runOnClient(client -> screen.toggle(null));
			context.waitTicks(5);
			check("All Blocks button -> every setting on", allServer(world, true), "something still off");
			context.runOnClient(client -> screen.toggle(null));
			context.waitTicks(5);
			check("All Blocks again -> every setting off", allServer(world, false), "something still on");

			// The command updates the open screen too.
			context.runOnClient(client -> client.getConnection().sendCommand("stickytoggle all on"));
			context.waitTicks(5);
			check("/stickytoggle all on -> screen shows all ON", context.computeOnClient(c -> StickyToggleScreen.isOn(null)), "shows off");
			context.runOnClient(client -> client.setScreen(null));
		}

		LOG.info("==== StickyToggle screen test summary: {} passed, {} failed ====", passes, failures.size());
		failures.forEach(f -> LOG.error("FAIL {}", f));
		if (!failures.isEmpty()) throw new AssertionError(String.join(" | ", failures));
	}

	private static boolean server(TestSingleplayerContext world, Toggle toggle) {
		return world.getServer().computeOnServer(server -> StickyConfig.forPlayer(
				server.getPlayerList().getPlayers().get(0).getUUID()).get(toggle));
	}

	private static boolean allServer(TestSingleplayerContext world, boolean value) {
		for (Toggle t : Toggle.values()) {
			if (server(world, t) != value) return false;
		}
		return true;
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
