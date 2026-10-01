package com.steelaspect.stickytoggle.config;

import java.util.EnumMap;
import java.util.Map;

/** A set of toggle values. Missing entries count as true (vanilla). */
public class ToggleState {
	private final EnumMap<Toggle, Boolean> values = new EnumMap<>(Toggle.class);

	public boolean get(Toggle t) {
		return values.getOrDefault(t, true);
	}

	public void set(Toggle t, boolean value) {
		values.put(t, value);
	}

	public void setAll(boolean value) {
		for (Toggle t : Toggle.values()) values.put(t, value);
	}

	public void copyFrom(ToggleState other) {
		for (Toggle t : Toggle.values()) values.put(t, other.get(t));
	}

	public Map<Toggle, Boolean> snapshot() {
		EnumMap<Toggle, Boolean> copy = new EnumMap<>(Toggle.class);
		for (Toggle t : Toggle.values()) copy.put(t, get(t));
		return copy;
	}
}
