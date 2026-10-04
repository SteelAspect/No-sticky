package com.steelaspect.stickytoggle.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BubbleColumnBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Walks the local player through each block with the toggle on and off and compares the distances.
 * Every lane is a 1-wide stone corridor high in the air, running east (+X), with barrier walls.
 */
public class StickyToggleClientGameTest implements FabricClientGameTest {
	private static final Logger LOG = LoggerFactory.getLogger("StickyToggleTest");
	private static final int Y = 120;
	private static final int LANE_LENGTH = 30;

	private final List<String> failures = new ArrayList<>();
	private int passes;
	private BlockPos origin;
	private int nextLane;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getClientWorld().waitForChunksRender();
			BlockPos feet = world.getServer().computeOnServer(server -> player(server).getBlockPos());
			origin = new BlockPos(feet.getX() - 10, Y, feet.getZ() - 10);
			world.getServer().runCommand("gamemode survival @a");
			// Not peaceful: peaceful regeneration would hide berry-bush thorn damage.
			world.getServer().runCommand("difficulty easy");
			world.getServer().runCommand("time set day");
			context.runOnClient(client -> client.options.getAutoJump().setValue(false));

			testCommandTree(context);
			BlockPos stone = lane(world, Blocks.STONE, null);
			double stoneWalk = walk(context, world, stone, 40);
			double stoneSlide = slide(context, world, stone);
			LOG.info("baseline: stone walk {} slide {}", fmt(stoneWalk), fmt(stoneSlide));

			testSoulSand(context, world, stoneWalk);
			testHoney(context, world, stoneWalk);
			testIce(context, world, stoneSlide);
			testCobweb(context, world, stoneWalk);
			testPowderSnow(context, world);
			testBerryBush(context, world);
			testWaterCurrent(context, world);
			testBubbleColumn(context, world);
			testAllOnOff(context, world);
		}

		LOG.info("==== StickyToggle game test summary: {} passed, {} failed ====", passes, failures.size());
		failures.forEach(f -> LOG.error("FAIL {}", f));
		if (!failures.isEmpty()) throw new AssertionError(String.join(" | ", failures));
	}

	// ---- tests ----

	private void testCommandTree(ClientGameTestContext context) {
		List<String> words = context.computeOnClient(client -> client.getNetworkHandler().getCommandDispatcher()
				.getRoot().getChild("stickytoggle").getChildren().stream().map(n -> n.getName()).sorted().toList());
		check("command lists every group", words.equals(List.of("all", "berrybush", "bubblecolumn", "cobweb",
				"honey", "ice", "powdersnow", "slime", "soulsand", "water")), words.toString());
	}

	private void testSoulSand(ClientGameTestContext context, TestSingleplayerContext world, double stoneWalk) {
		BlockPos start = lane(world, Blocks.SOUL_SAND, null);
		double on = walk(context, world, start, 40);
		toggle(context, "soulsand", false);
		double off = walk(context, world, start, 40);
		toggle(context, "soulsand", true);
		check("soulsand.slowdown on: slowed", on < stoneWalk * 0.6, "on " + fmt(on) + " stone " + fmt(stoneWalk));
		check("soulsand.slowdown off: stone speed", near(off, stoneWalk, 0.1), "off " + fmt(off) + " stone " + fmt(stoneWalk));
		checkInSync(world, context, "soulsand");
	}

	private void testHoney(ClientGameTestContext context, TestSingleplayerContext world, double stoneWalk) {
		BlockPos start = lane(world, Blocks.HONEY_BLOCK, null);
		double on = walk(context, world, start, 40);
		toggle(context, "honey", false);
		double off = walk(context, world, start, 40);
		toggle(context, "honey", true);
		check("honey.velocityMultiplier still works (on: slowed)", on < stoneWalk * 0.6, "on " + fmt(on));
		check("honey.velocityMultiplier still works (off: stone speed)", near(off, stoneWalk, 0.1), "off " + fmt(off));
	}

	private void testIce(ClientGameTestContext context, TestSingleplayerContext world, double stoneSlide) {
		for (BlockState ice : List.of(Blocks.ICE.getDefaultState(), Blocks.PACKED_ICE.getDefaultState(), Blocks.BLUE_ICE.getDefaultState())) {
			String name = ice.getBlock().getName().getString();
			BlockPos start = lane(world, ice.getBlock(), null);
			double on = slide(context, world, start);
			toggle(context, "ice", false);
			double off = slide(context, world, start);
			toggle(context, "ice", true);
			check("ice.slipperiness on: slides on " + name, on > stoneSlide * 3, "on " + fmt(on) + " stone " + fmt(stoneSlide));
			check("ice.slipperiness off: stops like stone on " + name, near(off, stoneSlide, 0.15),
					"off " + fmt(off) + " stone " + fmt(stoneSlide));
		}
		checkInSync(world, context, "ice");
	}

	private void testCobweb(ClientGameTestContext context, TestSingleplayerContext world, double stoneWalk) {
		BlockPos start = lane(world, Blocks.STONE, Blocks.COBWEB.getDefaultState());
		double on = walk(context, world, start, 40);
		toggle(context, "cobweb", false);
		double off = walk(context, world, start, 40);
		toggle(context, "cobweb", true);
		check("cobweb.slowdown on: nearly stuck", on < stoneWalk * 0.4, "on " + fmt(on));
		check("cobweb.slowdown off: stone speed", near(off, stoneWalk, 0.1), "off " + fmt(off) + " stone " + fmt(stoneWalk));
		checkInSync(world, context, "cobweb");
	}

	private void testPowderSnow(ClientGameTestContext context, TestSingleplayerContext world) {
		BlockPos start = lane(world, Blocks.STONE, Blocks.POWDER_SNOW.getDefaultState());
		double on = walk(context, world, start, 40);
		toggle(context, "powdersnow", false);
		world.getServer().runOnServer(server -> player(server).setFrozenTicks(0));
		double off = walk(context, world, start, 40);
		int frozen = world.getServer().computeOnServer(server -> player(server).getFrozenTicks());
		toggle(context, "powdersnow", true);
		check("powderSnow.slowdown off: faster than on", off > on * 1.05, "on " + fmt(on) + " off " + fmt(off));
		check("powderSnow.slowdown off: still freezes", frozen > 0, "frozen ticks " + frozen);
		checkInSync(world, context, "powdersnow");
	}

	private void testBerryBush(ClientGameTestContext context, TestSingleplayerContext world) {
		BlockPos start = lane(world, Blocks.GRASS_BLOCK, Blocks.SWEET_BERRY_BUSH.getDefaultState().with(SweetBerryBushBlock.AGE, 1));
		double on = walk(context, world, start, 40);
		toggle(context, "berrybush", false);
		world.getServer().runOnServer(server -> player(server).setHealth(20.0F));
		double off = walk(context, world, start, 40);
		float health = world.getServer().computeOnServer(server -> player(server).getHealth());
		toggle(context, "berrybush", true);
		check("berryBush.slowdown off: faster than on", off > on * 1.15, "on " + fmt(on) + " off " + fmt(off));
		check("berryBush.slowdown off: thorns still hurt", health < 20.0F, "health " + health);
		checkInSync(world, context, "berrybush");
	}

	private void testWaterCurrent(ClientGameTestContext context, TestSingleplayerContext world) {
		BlockPos start = lane(world, Blocks.STONE, null);
		world.getServer().runOnServer(server -> server.getOverworld().setBlockState(start, Blocks.WATER.getDefaultState()));
		context.waitTicks(60); // let it flow down the corridor
		BlockPos stand = start.east(3);
		double on = drift(context, world, stand);
		toggle(context, "water", false);
		double off = drift(context, world, stand);
		toggle(context, "water", true);
		check("water.current on: pushed along", on > 0.3, "drift " + fmt(on));
		check("water.current off: not pushed", Math.abs(off) < 0.05, "drift " + fmt(off));
	}

	private void testBubbleColumn(ClientGameTestContext context, TestSingleplayerContext world) {
		BlockPos bottom = origin.add(0, 0, nextLane++ * 4);
		world.getServer().runOnServer(server -> {
			ServerWorld w = server.getOverworld();
			for (int y = -1; y <= 12; y++) {
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						w.setBlockState(bottom.add(dx, y, dz), Blocks.BARRIER.getDefaultState());
					}
				}
			}
			w.setBlockState(bottom, Blocks.SOUL_SAND.getDefaultState());
			for (int y = 1; y <= 10; y++) {
				w.setBlockState(bottom.up(y), Blocks.BUBBLE_COLUMN.getDefaultState().with(BubbleColumnBlock.DRAG, false));
			}
			w.setBlockState(bottom.up(11), Blocks.AIR.getDefaultState());
		});
		context.waitTicks(5);
		double on = rise(context, world, bottom.up(1));
		toggle(context, "bubblecolumn", false);
		double off = rise(context, world, bottom.up(1));
		toggle(context, "bubblecolumn", true);
		check("bubbleColumn.push on: carried up", on > 3, "rise " + fmt(on));
		check("bubbleColumn.push off: not carried up", off < 1, "rise " + fmt(off));
	}

	private void testAllOnOff(ClientGameTestContext context, TestSingleplayerContext world) {
		BlockPos start = lane(world, Blocks.SOUL_SAND, Blocks.COBWEB.getDefaultState());
		double on = walk(context, world, start, 40);
		toggle(context, "all", false);
		double off = walk(context, world, start, 40);
		toggle(context, "all", true);
		double back = walk(context, world, start, 40);
		check("all off: soul sand + cobweb both lifted", off > on * 4, "on " + fmt(on) + " off " + fmt(off));
		check("all on: back to vanilla", near(back, on, 0.15), "on " + fmt(on) + " back " + fmt(back));
	}

	// ---- helpers ----

	/** Builds the next lane: floor of {@code floor}, optional {@code inLane} block at feet level. Returns the first feet position. */
	private BlockPos lane(TestSingleplayerContext world, net.minecraft.block.Block floor, BlockState inLane) {
		BlockPos start = origin.add(0, 0, nextLane++ * 4);
		world.getServer().runOnServer(server -> {
			ServerWorld w = server.getOverworld();
			for (int x = -2; x < LANE_LENGTH + 2; x++) {
				for (int y = -1; y <= 3; y++) {
					for (int dz = -1; dz <= 1; dz++) {
						BlockPos p = start.add(x, y, dz);
						BlockState s;
						if (y == -1) s = (dz == 0 && x >= 0 && x < LANE_LENGTH) ? floor.getDefaultState() : Blocks.BARRIER.getDefaultState();
						else if (dz != 0 || x < 0 || x >= LANE_LENGTH || y == 3) s = Blocks.BARRIER.getDefaultState();
						else if (y == 0 && inLane != null && x >= 1) s = inLane;
						else s = Blocks.AIR.getDefaultState();
						w.setBlockState(p, s);
					}
				}
			}
		});
		return start;
	}

	private static void teleport(ClientGameTestContext context, TestSingleplayerContext world, BlockPos feet) {
		world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f -90 0", feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5));
		context.waitTicks(15);
	}

	private static Vec3d clientPos(ClientGameTestContext context) {
		return context.computeOnClient(client -> client.player.getEntityPos());
	}

	/** Holds forward for {@code ticks} ticks from the lane start and returns the distance walked east. */
	private static double walk(ClientGameTestContext context, TestSingleplayerContext world, BlockPos start, int ticks) {
		teleport(context, world, start);
		double x0 = clientPos(context).x;
		context.getInput().holdKeyFor(options -> options.forwardKey, ticks);
		double d = clientPos(context).x - x0;
		context.waitTicks(10);
		return d;
	}

	/** Walks 20 ticks, lets go, and returns how far the player keeps going in the next 30 ticks. */
	private static double slide(ClientGameTestContext context, TestSingleplayerContext world, BlockPos start) {
		teleport(context, world, start);
		context.getInput().holdKeyFor(options -> options.forwardKey, 20);
		double x0 = clientPos(context).x;
		context.waitTicks(30);
		return clientPos(context).x - x0;
	}

	/** Stands still for 40 ticks and returns the distance drifted east. */
	private static double drift(ClientGameTestContext context, TestSingleplayerContext world, BlockPos stand) {
		teleport(context, world, stand);
		double x0 = clientPos(context).x;
		context.waitTicks(40);
		return clientPos(context).x - x0;
	}

	/** Stands still for 30 ticks and returns how far the player rose. */
	private static double rise(ClientGameTestContext context, TestSingleplayerContext world, BlockPos feet) {
		world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f -90 0", feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5));
		context.waitTicks(1);
		double y0 = clientPos(context).y;
		context.waitTicks(30);
		return clientPos(context).y - y0;
	}

	/** Runs /stickytoggle as the player (like a real player would) and waits for the sync packet. */
	private static void toggle(ClientGameTestContext context, String group, boolean on) {
		context.runOnClient(client -> client.getNetworkHandler().sendChatCommand("stickytoggle " + group + (on ? " on" : " off")));
		context.waitTicks(5);
	}

	/** Server and client should agree on where the player is, or the server rubber-banded them. */
	private void checkInSync(TestSingleplayerContext world, ClientGameTestContext context, String name) {
		Vec3d server = world.getServer().computeOnServer(s -> player(s).getEntityPos());
		Vec3d client = clientPos(context);
		check(name + ": no rubber-banding", server.distanceTo(client) < 0.1, "server " + server + " client " + client);
	}

	private static ServerPlayerEntity player(MinecraftServer server) {
		return server.getPlayerManager().getPlayerList().get(0);
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
			LOG.info("PASS {} ({})", name, detail);
		} else {
			failures.add(name + ": " + detail);
			LOG.error("FAIL {}: {}", name, detail);
		}
	}
}
