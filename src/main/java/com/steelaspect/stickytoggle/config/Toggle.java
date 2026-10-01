package com.steelaspect.stickytoggle.config;

/** Every toggle the mod exposes. All default to true (vanilla). */
public enum Toggle {
	SLIME_BOUNCE("slime.bounce"),
	SLIME_FALL_DAMAGE_NEGATION("slime.fallDamageNegation"),
	SLIME_WALK_SLOWDOWN("slime.walkSlowdown"),
	SLIME_SLIPPERINESS("slime.slipperiness"),
	HONEY_VELOCITY_MULTIPLIER("honey.velocityMultiplier"),
	HONEY_JUMP_MULTIPLIER("honey.jumpMultiplier"),
	HONEY_WALL_SLIDE("honey.wallSlide"),
	HONEY_FALL_DAMAGE_REDUCTION("honey.fallDamageReduction");

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
