package com.steelaspect.stickytoggle.smoke;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.20.4 stand-in for the 1.21.11 client game tests (fabric-client-gametest-api-v1 starts at 1.21.4): once the title
 * screen is up, {@link StickyChecks} runs on its own thread, driving the client through {@link Ctx}. Logs
 * {@code [StickySmoke] PASS} or {@code FAIL}, then quits. Only active with -Dstickytoggle.smoke=true.
 */
public class StickySmokeTest implements ClientModInitializer {
	private static final Logger LOG = LoggerFactory.getLogger("StickySmoke");
	/** Position packets the server sent (see the mixin). */
	public static volatile int positionPackets;
	private boolean started;

	@Override
	public void onInitializeClient() {
		if (!Boolean.getBoolean("stickytoggle.smoke")) return;
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			Ctx.ticks++;
			if (started || !(mc.screen instanceof TitleScreen)) return;
			started = true;
			Thread thread = new Thread(StickySmokeTest::run, "Sticky Toggle smoke test");
			thread.setDaemon(true);
			thread.start();
		});
	}

	private static void run() {
		Ctx t = new Ctx();
		int failures;
		try {
			failures = new StickyChecks(t).run();
		} catch (Throwable e) {
			LOG.error("[StickySmoke] test threw", e);
			failures = -1;
		}
		if (failures == 0) LOG.info("[StickySmoke] PASS");
		else LOG.error("[StickySmoke] FAIL: {}", failures < 0 ? "exception" : failures + " check(s) failed, see log");
		t.mc.execute(() -> {
			if (t.mc.level != null) t.mc.level.disconnect();
			t.mc.disconnect();
			t.mc.stop();
		});
	}
}
