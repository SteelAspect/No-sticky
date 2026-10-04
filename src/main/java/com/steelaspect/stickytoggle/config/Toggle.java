package com.steelaspect.stickytoggle.config;

/**
 * Every toggle the mod exposes. All default to true (vanilla).
 * Fall damage and other damage are deliberately not toggleable: players always get vanilla
 * fall damage on slime (none) and honey (20%), and vanilla freezing, thorn and drowning damage.
 */
public enum Toggle {
	SLIME_BOUNCE("slime.bounce", Group.SLIME),
	SLIME_WALK_SLOWDOWN("slime.walkSlowdown", Group.SLIME),
	SLIME_SLIPPERINESS("slime.slipperiness", Group.SLIME),
	HONEY_VELOCITY_MULTIPLIER("honey.velocityMultiplier", Group.HONEY),
	HONEY_JUMP_MULTIPLIER("honey.jumpMultiplier", Group.HONEY),
	HONEY_WALL_SLIDE("honey.wallSlide", Group.HONEY),
	SOUL_SAND_SLOWDOWN("soulsand.slowdown", Group.SOUL_SAND),
	ICE_SLIPPERINESS("ice.slipperiness", Group.ICE),
	COBWEB_SLOWDOWN("cobweb.slowdown", Group.COBWEB),
	POWDER_SNOW_SLOWDOWN("powderSnow.slowdown", Group.POWDER_SNOW),
	BERRY_BUSH_SLOWDOWN("berryBush.slowdown", Group.BERRY_BUSH),
	WATER_CURRENT("water.current", Group.WATER),
	BUBBLE_COLUMN_PUSH("bubbleColumn.push", Group.BUBBLE_COLUMN);

	public final String key;
	public final Group group;

	Toggle(String key, Group group) {
		this.key = key;
		this.group = group;
	}

	public static Toggle byKey(String key) {
		for (Toggle t : values()) {
			if (t.key.equals(key)) return t;
		}
		return null;
	}

	/** The blocks /stickytoggle switches as one: the command word and the name used in feedback. */
	public enum Group {
		SLIME("slime", "Slime"),
		HONEY("honey", "Honey"),
		SOUL_SAND("soulsand", "Soul sand"),
		ICE("ice", "Ice"),
		COBWEB("cobweb", "Cobweb"),
		POWDER_SNOW("powdersnow", "Powder snow"),
		BERRY_BUSH("berrybush", "Sweet berry bush"),
		WATER("water", "Water current"),
		BUBBLE_COLUMN("bubblecolumn", "Bubble column");

		public final String command;
		public final String displayName;

		Group(String command, String displayName) {
			this.command = command;
			this.displayName = displayName;
		}
	}
}
