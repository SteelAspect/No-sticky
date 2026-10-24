package com.steelaspect.stickytoggle.smoke;

import com.steelaspect.stickytoggle.client.StickyOptions;
import com.steelaspect.stickytoggle.client.StickyToggleScreen;
import com.steelaspect.stickytoggle.client.malilib.Configs;
import com.steelaspect.stickytoggle.client.malilib.GuiConfigs;
import com.steelaspect.stickytoggle.config.StickyConfig;
import com.steelaspect.stickytoggle.config.Toggle;
import com.terraformersmc.modmenu.api.ModMenuApi;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.event.InputEventHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The 1.21.11 client game tests (movement, MaLiLib menu, Nocturne screen, persistence) ported to a dedicated 1.20.4
 * server: server A (25598) has the mod, server B (25599) only Fabric API. Every lane is a 1-wide stone corridor high in
 * the air, running east (+X), with barrier walls, built with /fill as op.
 */
final class StickyChecks {
	private static final Logger LOG = LoggerFactory.getLogger("StickySmoke");
	private static final String SERVER_A = "localhost:25598";
	private static final String SERVER_B = "localhost:25599";
	private static final int Y = 100;
	private static final int LANE_LENGTH = 30;

	private final Ctx t;
	private final List<String> failures = new ArrayList<>();
	private int passes;
	private BlockPos origin;
	private int nextLane;

	StickyChecks(Ctx t) {
		this.t = t;
	}

	int run() {
		titleScreen();
		if (join(SERVER_A)) {
			serverA();
			leave();
			// Rejoin: the server kept the player's settings (slime was left off).
			if (join(SERVER_A)) {
				check("rejoin: slime still off", !StickyConfig.CLIENT.get(Toggle.SLIME_BOUNCE), "client on");
				check("rejoin: honey still on", StickyConfig.CLIENT.get(Toggle.HONEY_WALL_SLIDE), "honey off");
				check("rejoin: menu slime row OFF", !t.get(c -> block(Toggle.Group.SLIME).getBooleanValue()), "row on");
				// Leave slime off for the config file check in smoketest.sh, the rest on.
				leave();
			}
		}
		check("after leaving: every row back to ON (vanilla)",
				t.get(c -> Configs.SETTINGS.stream().allMatch(ConfigBooleanHotkeyed::getBooleanValue)), "a row is still off");
		if (join(SERVER_B)) {
			serverB();
			leave();
		}
		LOG.info("[StickySmoke] summary: {} passed, {} failed", passes, failures.size());
		failures.forEach(f -> LOG.error("[StickySmoke] FAIL {}", f));
		return failures.size();
	}

	// ---- phases ----

	private void titleScreen() {
		// Not connected: a click is refused and the row snaps back to ON.
		t.client(c -> block(Toggle.Group.SOUL_SAND).setBooleanValue(false));
		check("title screen: click refused, row back to ON", t.get(c -> block(Toggle.Group.SOUL_SAND).getBooleanValue()), "row stayed off");
		Screen fromModMenu = t.get(c -> FabricLoader.getInstance()
				.getEntrypointContainers("modmenu", ModMenuApi.class).stream()
				.filter(e -> e.getProvider().getMetadata().getId().equals("stickytoggle"))
				.findFirst().map(e -> (Screen) e.getEntrypoint().getModConfigScreenFactory().create(null)).orElse(null));
		check("Mod Menu config button opens the MaLiLib menu", fromModMenu instanceof GuiConfigs, String.valueOf(fromModMenu));
	}

	private void serverA() {
		check("server A: settings available", t.get(c -> StickyOptions.available()), "not available");
		for (String c : List.of("gamemode survival", "difficulty easy", "time set day", "gamerule doDaylightCycle false",
				"gamerule doMobSpawning false", "effect give @s minecraft:saturation infinite 1 true", "stickytoggle all on")) {
			cmd(c);
		}
		t.waitTicks(10);
		BlockPos feet = t.get(c -> c.player.blockPosition());
		origin = new BlockPos(feet.getX() - 10, Y, feet.getZ() - 10);

		testCommandTree();
		BlockPos stone = lane("stone", null);
		double stoneWalk = walk(stone, 40);
		double stoneSlide = slide(stone);
		LOG.info("[StickySmoke] baseline: stone walk {} slide {}", fmt(stoneWalk), fmt(stoneSlide));

		testSoulSand(stoneWalk);
		testHoney(stoneWalk);
		testSlime();
		testIce(stoneSlide);
		testCobweb(stoneWalk);
		testPowderSnow();
		testBerryBush();
		testWaterCurrent();
		testBubbleColumn();
		testAllOnOff();
		testScreen();
		testMenu();
		testHub();
		cmd("stickytoggle all on");
		cmd("stickytoggle slime off");
		t.waitTicks(5);
	}

	private void serverB() {
		check("server B: settings not available", !t.get(c -> StickyOptions.available()), "available");
		check("server B: client state is vanilla", StickyOptions.isOn(null), "something off");
		t.client(c -> block(Toggle.Group.SOUL_SAND).setBooleanValue(false));
		check("server B: menu click refused, row back to ON", t.get(c -> block(Toggle.Group.SOUL_SAND).getBooleanValue()), "row stayed off");
		check("server B: screen request refused", !t.get(c -> StickyOptions.set(Toggle.Group.SLIME, false)), "sent");
		t.client(c -> c.setScreen(new StickyToggleScreen(null)));
		t.waitTicks(10);
		LOG.info("[StickySmoke] screenshot {}", t.shot("stickytoggle-screen-no-server-mod"));
		t.client(c -> c.setScreen(null));
		// Vanilla movement still applies on a server without the mod (and nothing rubber-bands).
		for (String c : List.of("gamemode survival", "time set day")) cmd(c);
		t.waitTicks(10);
		BlockPos feet = t.get(c -> c.player.blockPosition());
		origin = new BlockPos(feet.getX() - 10, Y, feet.getZ() - 10);
		nextLane = 0;
		double stoneWalk = walk(lane("stone", null), 40);
		int before = StickySmokeTest.positionPackets;
		double soul = walk(lane("soul_sand", null), 40);
		check("server B: soul sand slows (vanilla)", soul < stoneWalk * 0.6, "soul " + fmt(soul) + " stone " + fmt(stoneWalk));
		check("server B: no rubber-banding", StickySmokeTest.positionPackets - before <= 1, (StickySmokeTest.positionPackets - before) + " position packets");
	}

	// ---- movement tests (as the 1.21.11 client game test) ----

	private void testCommandTree() {
		List<String> words = t.get(c -> c.getConnection().getCommands()
				.getRoot().getChild("stickytoggle").getChildren().stream().map(n -> n.getName()).sorted().toList());
		check("command lists every group", words.equals(List.of("all", "berrybush", "bubblecolumn", "cobweb",
				"honey", "ice", "powdersnow", "slime", "soulsand", "water")), words.toString());
	}

	private void testSoulSand(double stoneWalk) {
		BlockPos start = lane("soul_sand", null);
		double on = walk(start, 40);
		toggle("soulsand", false);
		int before = StickySmokeTest.positionPackets;
		double off = walk(start, 40);
		toggle("soulsand", true);
		check("soulsand.slowdown on: slowed", on < stoneWalk * 0.6, "on " + fmt(on) + " stone " + fmt(stoneWalk));
		check("soulsand.slowdown off: stone speed", near(off, stoneWalk, 0.1), "off " + fmt(off) + " stone " + fmt(stoneWalk));
		noRubberBand("soulsand", before);
	}

	private void testHoney(double stoneWalk) {
		BlockPos start = lane("honey_block", null);
		double on = walk(start, 40);
		toggle("honey", false);
		int before = StickySmokeTest.positionPackets;
		double off = walk(start, 40);
		toggle("honey", true);
		check("honey.velocityMultiplier on: slowed", on < stoneWalk * 0.6, "on " + fmt(on));
		check("honey.velocityMultiplier off: stone speed", near(off, stoneWalk, 0.1), "off " + fmt(off));
		noRubberBand("honey", before);
	}

	/** New for 1.20.4 (bounce has a renamed target there): drop onto slime and see how high the player comes back. */
	private void testSlime() {
		BlockPos start = lane("slime_block", null);
		double on = bounce(start.east(5));
		toggle("slime", false);
		int before = StickySmokeTest.positionPackets;
		double off = bounce(start.east(5));
		toggle("slime", true);
		check("slime.bounce on: bounces", on > 1.0, "rebound " + fmt(on));
		check("slime.bounce off: lands flat", off < 0.2, "rebound " + fmt(off));
		noRubberBand("slime", before);
	}

	private void testIce(double stoneSlide) {
		for (String ice : List.of("ice", "packed_ice", "blue_ice")) {
			BlockPos start = lane(ice, null);
			double on = slide(start);
			toggle("ice", false);
			int before = StickySmokeTest.positionPackets;
			double off = slide(start);
			toggle("ice", true);
			check("ice.slipperiness on: slides on " + ice, on > stoneSlide * 3, "on " + fmt(on) + " stone " + fmt(stoneSlide));
			check("ice.slipperiness off: stops like stone on " + ice, near(off, stoneSlide, 0.15),
					"off " + fmt(off) + " stone " + fmt(stoneSlide));
			noRubberBand(ice, before);
		}
	}

	private void testCobweb(double stoneWalk) {
		BlockPos start = lane("stone", "cobweb");
		double on = walk(start, 40);
		toggle("cobweb", false);
		int before = StickySmokeTest.positionPackets;
		double off = walk(start, 40);
		toggle("cobweb", true);
		check("cobweb.slowdown on: nearly stuck", on < stoneWalk * 0.4, "on " + fmt(on));
		check("cobweb.slowdown off: stone speed", near(off, stoneWalk, 0.1), "off " + fmt(off) + " stone " + fmt(stoneWalk));
		noRubberBand("cobweb", before);
	}

	private void testPowderSnow() {
		BlockPos start = lane("stone", "powder_snow");
		double on = walk(start, 40);
		toggle("powdersnow", false);
		int before = StickySmokeTest.positionPackets;
		double off = walk(start, 40);
		int frozen = t.get(c -> c.player.getTicksFrozen());
		toggle("powdersnow", true);
		check("powderSnow.slowdown off: faster than on", off > on * 1.05, "on " + fmt(on) + " off " + fmt(off));
		check("powderSnow.slowdown off: still freezes", frozen > 0, "frozen ticks " + frozen);
		noRubberBand("powdersnow", before);
	}

	private void testBerryBush() {
		BlockPos start = lane("grass_block", "sweet_berry_bush[age=1]");
		double on = walk(start, 40);
		toggle("berrybush", false);
		cmd("effect give @s minecraft:instant_health 1 5 true");
		t.waitTicks(10);
		float healthBefore = t.get(c -> c.player.getHealth());
		int before = StickySmokeTest.positionPackets;
		double off = walk(start, 40);
		float health = t.get(c -> c.player.getHealth());
		toggle("berrybush", true);
		check("berryBush.slowdown off: faster than on", off > on * 1.15, "on " + fmt(on) + " off " + fmt(off));
		check("berryBush.slowdown off: thorns still hurt", health < healthBefore, "health " + healthBefore + " -> " + health);
		noRubberBand("berrybush", before);
	}

	private void testWaterCurrent() {
		BlockPos start = lane("stone", null);
		cmd(String.format(Locale.ROOT, "setblock %d %d %d minecraft:water", start.getX(), start.getY(), start.getZ()));
		t.waitTicks(60); // let it flow down the corridor
		BlockPos stand = start.east(3);
		double on = drift(stand);
		toggle("water", false);
		int before = StickySmokeTest.positionPackets;
		double off = drift(stand);
		toggle("water", true);
		check("water.current on: pushed along", on > 0.3, "drift " + fmt(on));
		check("water.current off: not pushed", Math.abs(off) < 0.05, "drift " + fmt(off));
		noRubberBand("water", before);
	}

	private void testBubbleColumn() {
		BlockPos b = origin.offset(0, 0, nextLane++ * 4);
		fill(b.offset(-1, -1, -1), b.offset(1, 12, 1), "barrier");
		fill(b, b, "soul_sand");
		fill(b.above(1), b.above(10), "bubble_column[drag=false]");
		fill(b.above(11), b.above(11), "air");
		t.waitTicks(5);
		double on = rise(b.above(1));
		toggle("bubblecolumn", false);
		double off = rise(b.above(1));
		toggle("bubblecolumn", true);
		check("bubbleColumn.push on: carried up", on > 3, "rise " + fmt(on));
		check("bubbleColumn.push off: not carried up", off < 1, "rise " + fmt(off));
	}

	private void testAllOnOff() {
		BlockPos start = lane("soul_sand", "cobweb");
		double on = walk(start, 40);
		toggle("all", false);
		double off = walk(start, 40);
		toggle("all", true);
		double back = walk(start, 40);
		check("all off: soul sand + cobweb both lifted", off > on * 4, "on " + fmt(on) + " off " + fmt(off));
		check("all on: back to vanilla", near(back, on, 0.15), "on " + fmt(on) + " back " + fmt(back));
	}

	// ---- Nocturne screen (StickyToggleScreenGameTest) ----

	private void testScreen() {
		StickyToggleScreen screen = t.get(c -> {
			StickyToggleScreen s = new StickyToggleScreen(null);
			c.setScreen(s);
			return s;
		});
		t.waitTicks(2);
		t.client(c -> screen.toggle(Toggle.Group.SLIME));
		t.waitTicks(10);
		check("screen: slime button -> all 3 slime settings off",
				!StickyConfig.CLIENT.get(Toggle.SLIME_BOUNCE) && !StickyConfig.CLIENT.get(Toggle.SLIME_WALK_SLOWDOWN)
						&& !StickyConfig.CLIENT.get(Toggle.SLIME_SLIPPERINESS), "some still on");
		check("screen: honey untouched", StickyConfig.CLIENT.get(Toggle.HONEY_WALL_SLIDE), "honey changed");
		check("screen: slime and All Blocks show OFF",
				!t.get(c -> StickyToggleScreen.isOn(Toggle.Group.SLIME) || StickyToggleScreen.isOn(null)), "shows on");
		t.client(c -> screen.toggle(Toggle.Group.ICE));
		t.waitTicks(10);
		LOG.info("[StickySmoke] screenshot {}", t.shot("stickytoggle-screen"));
		t.client(c -> screen.toggle(null));
		t.waitTicks(10);
		check("screen: All Blocks -> every setting on", StickyOptions.isOn(null), "something still off");
		t.client(c -> screen.toggle(null));
		t.waitTicks(10);
		check("screen: All Blocks again -> every setting off", allClient(false), "something still on");
		cmd("stickytoggle all on");
		t.waitTicks(5);
		check("screen: /stickytoggle all on -> shows all ON", t.get(c -> StickyToggleScreen.isOn(null)), "shows off");
		t.client(c -> c.setScreen(null));
	}

	// ---- MaLiLib menu (StickyToggleMenuGameTest) ----

	private void testMenu() {
		t.client(c -> block(Toggle.Group.SOUL_SAND).setBooleanValue(false));
		t.waitTicks(10);
		check("menu: soul sand row off -> setting off", !StickyConfig.CLIENT.get(Toggle.SOUL_SAND_SLOWDOWN), "still on");
		check("menu: soul sand row off -> settings row off", !t.get(c -> setting(Toggle.SOUL_SAND_SLOWDOWN).getBooleanValue()), "settings row on");

		t.client(c -> block(Toggle.Group.SLIME).setBooleanValue(false));
		t.waitTicks(10);
		check("menu: slime row off -> all 3 slime settings off",
				!StickyConfig.CLIENT.get(Toggle.SLIME_BOUNCE) && !StickyConfig.CLIENT.get(Toggle.SLIME_WALK_SLOWDOWN)
						&& !StickyConfig.CLIENT.get(Toggle.SLIME_SLIPPERINESS), "some still on");
		check("menu: honey untouched by the slime row", StickyConfig.CLIENT.get(Toggle.HONEY_WALL_SLIDE), "honey changed");
		check("menu: All Blocks row shows OFF", !t.get(c -> Configs.BLOCKS.get(Configs.BLOCKS.size() - 1).getBooleanValue()), "shows on");

		t.client(c -> block(Toggle.Group.SLIME).setBooleanValue(true));
		t.waitTicks(10);
		t.client(c -> setting(Toggle.SLIME_BOUNCE).setBooleanValue(false));
		t.waitTicks(10);
		check("menu: slime bounce row off -> only bounce off",
				!StickyConfig.CLIENT.get(Toggle.SLIME_BOUNCE) && StickyConfig.CLIENT.get(Toggle.SLIME_SLIPPERINESS), "wrong slime state");
		check("menu: slime block row OFF while one slime setting is off", !t.get(c -> block(Toggle.Group.SLIME).getBooleanValue()), "shows on");

		cmd("stickytoggle ice off");
		t.waitTicks(10);
		check("menu: /stickytoggle ice off -> ice rows off",
				!t.get(c -> block(Toggle.Group.ICE).getBooleanValue() || setting(Toggle.ICE_SLIPPERINESS).getBooleanValue()), "ice row on");

		// Hotkeys: bind the cobweb setting and the bubble column block, press them in game.
		t.client(c -> {
			c.setScreen(null);
			setting(Toggle.COBWEB_SLOWDOWN).getKeybind().setValueFromString("K");
			block(Toggle.Group.BUBBLE_COLUMN).getKeybind().setValueFromString("LEFT_SHIFT,B");
			InputEventHandler.getKeybindManager().updateUsedKeys();
		});
		t.waitTicks(2);
		press(GLFW.GLFW_KEY_K);
		t.waitTicks(10);
		check("menu: cobweb hotkey -> cobweb off", !StickyConfig.CLIENT.get(Toggle.COBWEB_SLOWDOWN), "still on");
		press(GLFW.GLFW_KEY_K);
		t.waitTicks(10);
		check("menu: cobweb hotkey again -> back on", StickyConfig.CLIENT.get(Toggle.COBWEB_SLOWDOWN), "still off");
		// One client call: MaLiLib re-reads the real GLFW key state every tick, which drops a faked held Shift.
		t.client(c -> {
			long w = c.getWindow().getWindow();
			c.keyboardHandler.keyPress(w, GLFW.GLFW_KEY_LEFT_SHIFT, 0, GLFW.GLFW_PRESS, 0);
			c.keyboardHandler.keyPress(w, GLFW.GLFW_KEY_B, 0, GLFW.GLFW_PRESS, GLFW.GLFW_MOD_SHIFT);
			c.keyboardHandler.keyPress(w, GLFW.GLFW_KEY_B, 0, GLFW.GLFW_RELEASE, GLFW.GLFW_MOD_SHIFT);
			c.keyboardHandler.keyPress(w, GLFW.GLFW_KEY_LEFT_SHIFT, 0, GLFW.GLFW_RELEASE, 0);
		});
		t.waitTicks(10);
		check("menu: shift+B block hotkey -> bubble column off", !StickyConfig.CLIENT.get(Toggle.BUBBLE_COLUMN_PUSH), "still on");

		for (String tab : new String[]{"BLOCKS", "SETTINGS"}) {
			t.client(c -> {
				setTab(tab);
				c.setScreen(new GuiConfigs());
			});
			t.waitTicks(10);
			LOG.info("[StickySmoke] screenshot {}", t.shot("stickytoggle-menu-" + tab.toLowerCase(Locale.ROOT)));
		}
		cmd("stickytoggle all on");
		t.waitTicks(10);
		check("menu: open + all on -> every row ON",
				t.get(c -> Configs.BLOCKS.stream().allMatch(ConfigBooleanHotkeyed::getBooleanValue)
						&& Configs.SETTINGS.stream().allMatch(ConfigBooleanHotkeyed::getBooleanValue)), "a row is off");
		t.client(c -> c.setScreen(null));

		t.client(c -> Configs.INSTANCE.save());
		String json = read(FabricLoader.getInstance().getConfigDir().resolve("stickytoggle-client.json"));
		check("menu: hotkeys saved to stickytoggle-client.json", json.contains("\"K\"") && json.contains("LEFT_SHIFT,B"), json);
		t.client(c -> {
			setting(Toggle.COBWEB_SLOWDOWN).getKeybind().setValueFromString("");
			block(Toggle.Group.BUBBLE_COLUMN).getKeybind().setValueFromString("");
			InputEventHandler.getKeybindManager().updateUsedKeys();
			Configs.INSTANCE.save();
		});
	}

	// ---- Alloy Hub page ----

	private void testHub() {
		if (!FabricLoader.getInstance().isModLoaded("cytra-hub")) {
			LOG.info("[StickySmoke] Alloy Hub not loaded, skipping the Hub page");
			return;
		}
		var e = dev.steelaspect.cytrahub.client.model.ModRegistry.find("stickytoggle");
		check("hub page registered", e != null && e.hasPlugin, String.valueOf(e));
		if (e != null) check("hub page has settings", !e.settings.isEmpty(), "none");
		t.client(c -> {
			dev.steelaspect.cytrahub.client.HubClientConfig.get().layout = "grid";
			var hs = new dev.steelaspect.cytrahub.client.screen.HubScreen(null, dev.steelaspect.cytrahub.client.screen.HubView.Page.MODS);
			c.setScreen(hs);
			hs.openMod("stickytoggle");
		});
		for (String tab : List.of("status", "settings")) {
			t.waitTicks(5);
			check("hub tab " + tab + " shown", t.get(c -> hubSelect(c, tab)), "not found");
			t.waitTicks(10);
			LOG.info("[StickySmoke] screenshot {}", t.shot("stickytoggle-hub-" + tab));
		}
		t.client(c -> c.setScreen(null));
	}

	private static boolean hubSelect(Minecraft mc, String tabId) {
		if (!(mc.screen instanceof dev.steelaspect.cytrahub.client.screen.HubScreen hs)) return false;
		var d = hs.detailPopup();
		if (d == null) return false;
		try {
			Class<?> detail = dev.steelaspect.cytrahub.client.screen.detail.ModDetail.class;
			Method visible = detail.getDeclaredMethod("visibleTabs");
			visible.setAccessible(true);
			List<?> tabs = (List<?>) visible.invoke(d);
			for (int i = 0; i < tabs.size(); i++) {
				if (((dev.steelaspect.cytrahub.client.screen.detail.ModDetail.Tab) tabs.get(i)).id().equals(tabId)) {
					Method select = detail.getDeclaredMethod("select", int.class);
					select.setAccessible(true);
					select.invoke(d, i);
					return true;
				}
			}
		} catch (ReflectiveOperationException ex) {
			LOG.error("[StickySmoke] hub tab lookup failed", ex);
		}
		return false;
	}

	// ---- helpers ----

	private boolean join(String address) {
		LOG.info("[StickySmoke] joining {}", address);
		t.client(mc -> ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(address),
				new ServerData("smoke", address, ServerData.Type.OTHER), false));
		if (!t.waitFor(20 * 60, () -> t.mc.player != null && t.mc.level != null && t.mc.screen == null)) {
			check("joined " + address, false, "timed out");
			t.client(mc -> mc.disconnect(new TitleScreen()));
			return false;
		}
		t.waitTicks(40);
		return true;
	}

	private void leave() {
		t.client(mc -> {
			mc.level.disconnect();
			mc.disconnect(new TitleScreen());
		});
		t.waitTicks(20);
	}

	private void cmd(String command) {
		t.client(c -> c.player.connection.sendCommand(command));
		t.waitTicks(2);
	}

	private void fill(BlockPos a, BlockPos b, String block) {
		cmd(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:%s", a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(), block));
	}

	/** Builds the next lane: floor of {@code floor}, optional {@code inLane} block at feet level. Returns the first feet position. */
	private BlockPos lane(String floor, String inLane) {
		BlockPos s = origin.offset(0, 0, nextLane++ * 4);
		fill(s.offset(-2, -1, -1), s.offset(LANE_LENGTH + 1, 3, 1), "barrier");
		fill(s.offset(0, 0, 0), s.offset(LANE_LENGTH - 1, 2, 0), "air");
		fill(s.offset(0, -1, 0), s.offset(LANE_LENGTH - 1, -1, 0), floor);
		if (inLane != null) fill(s.offset(1, 0, 0), s.offset(LANE_LENGTH - 1, 0, 0), inLane);
		t.waitTicks(5);
		return s;
	}

	private void teleport(BlockPos feet) {
		cmd(String.format(Locale.ROOT, "tp @s %.1f %d %.1f -90 0", feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5));
		t.waitTicks(15);
	}

	private Vec3 pos() {
		return t.get(c -> c.player.position());
	}

	/** Holds forward for {@code ticks} ticks from the lane start and returns the distance walked east. */
	private double walk(BlockPos start, int ticks) {
		teleport(start);
		double x0 = pos().x;
		holdForward(ticks);
		double d = pos().x - x0;
		t.waitTicks(10);
		return d;
	}

	/** Walks 20 ticks, lets go, and returns how far the player keeps going in the next 30 ticks. */
	private double slide(BlockPos start) {
		teleport(start);
		holdForward(20);
		double x0 = pos().x;
		t.waitTicks(30);
		return pos().x - x0;
	}

	/** Stands still for 40 ticks and returns the distance drifted east. */
	private double drift(BlockPos stand) {
		teleport(stand);
		double x0 = pos().x;
		t.waitTicks(40);
		return pos().x - x0;
	}

	/** Stands still for 30 ticks and returns how far the player rose. */
	private double rise(BlockPos feet) {
		cmd(String.format(Locale.ROOT, "tp @s %.1f %d %.1f -90 0", feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5));
		double y0 = pos().y;
		t.waitTicks(30);
		return pos().y - y0;
	}

	/** Drops the player from 2 blocks above the slime floor and returns the highest point after the first landing. */
	private double bounce(BlockPos feet) {
		double floor = feet.getY();
		cmd(String.format(Locale.ROOT, "tp @s %.1f %.1f %.1f -90 0", feet.getX() + 0.5, floor + 2.0, feet.getZ() + 0.5));
		boolean landed = false;
		double max = 0;
		for (int i = 0; i < 40; i++) {
			double y = pos().y - floor;
			if (!landed && y < 0.05) landed = true;
			else if (landed) max = Math.max(max, y);
			t.waitTick();
		}
		t.waitTicks(20);
		return landed ? max : -1;
	}

	private void holdForward(int ticks) {
		t.client(c -> c.options.keyUp.setDown(true));
		t.waitTicks(ticks);
		t.client(c -> c.options.keyUp.setDown(false));
	}

	private void press(int key) {
		key(key, GLFW.GLFW_PRESS);
		key(key, GLFW.GLFW_RELEASE);
	}

	private void key(int key, int action) {
		t.client(c -> c.keyboardHandler.keyPress(c.getWindow().getWindow(), key, 0, action, 0));
		t.waitTick();
	}

	/** Runs /stickytoggle as the player and waits for the sync packet. */
	private void toggle(String group, boolean on) {
		cmd("stickytoggle " + group + (on ? " on" : " off"));
		t.waitTicks(5);
	}

	/** With a toggle off, client and server must agree on movement, or the server corrects (rubber-bands) the player. */
	private void noRubberBand(String name, int before) {
		int n = StickySmokeTest.positionPackets - before;
		check(name + ": no rubber-banding", n <= 1, n + " position packets (1 = the /tp)");
	}

	private static boolean allClient(boolean value) {
		for (Toggle tg : Toggle.values()) {
			if (StickyConfig.CLIENT.get(tg) != value) return false;
		}
		return true;
	}

	private static ConfigBooleanHotkeyed block(Toggle.Group group) {
		return Configs.BLOCKS.get(group.ordinal());
	}

	private static ConfigBooleanHotkeyed setting(Toggle toggle) {
		return Configs.SETTINGS.get(toggle.ordinal());
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

	private static boolean near(double value, double expected, double fraction) {
		return Math.abs(value - expected) <= Math.abs(expected) * fraction;
	}

	private static String fmt(double d) {
		return String.format(Locale.ROOT, "%.3f", d);
	}

	private void check(String name, boolean ok, String detail) {
		if (ok) {
			passes++;
			LOG.info("[StickySmoke] PASS {} ({})", name, detail);
		} else {
			failures.add(name + ": " + detail);
			LOG.error("[StickySmoke] FAIL {}: {}", name, detail);
		}
	}
}
