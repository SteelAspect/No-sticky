package com.steelaspect.stickytoggle.config;

/**
 * Every toggle the mod exposes. All default to true (vanilla).
 * Fall damage is deliberately not toggleable: players always get vanilla
 * fall damage on slime (none) and honey (20%).
 */
public enum Toggle {
	SLIME_BOUNCE("slime.bounce"),
	SLIME_WALK_SLOWDOWN("slime.walkSlowdown"),
	SLIME_SLIPPERINESS("slime.slipperiness"),
	HONEY_VELOCITY_MULTIPLIER("honey.velocityMultiplier"),
	HONEY_JUMP_MULTIPLIER("honey.jumpMultiplier"),
	HONEY_WALL_SLIDE("honey.wallSlide");

	public final String key;

	Toggle(String key) {
		this.key = key;
	}

	public static Toggle byKey(String key) {
		for (Toggle t : values()) {
			if (t.key.equals(key)) return t;
		}
		return null;
	}
}
