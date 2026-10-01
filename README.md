# StickyToggle

A Fabric mod for **Minecraft Java 1.21.11** that lets server operators toggle how slime blocks and honey blocks physically affect **players**.

> **Only players are affected.** Mobs, items, XP orbs, minecarts, boats, TNT, arrows and every game mechanic (pistons, block sticking, redstone, crafting, mob AI) stay 100% vanilla. Every hook checks `entity instanceof PlayerEntity` and falls through to vanilla code for everything else.

## Features
- 6 independent toggles, all defaulting to `true` (vanilla behaviour).
- Live changes via commands, no restart needed.
- The server syncs the toggle state to every client on join and on change, so client-side movement prediction matches the server.
- Config saved to `config/stickytoggle.json`.

## Toggle keys

| Key | Vanilla behaviour (when `true`) | When `false` (players only) |
|---|---|---|
| `slime.bounce` | Bounce when landing on slime | Land like on a normal block |
| `slime.walkSlowdown` | Slowed while walking on slime | Normal walking speed |
| `slime.slipperiness` | Slipperiness 0.8 (slides) | Default 0.6 (no slide) |
| `honey.velocityMultiplier` | Movement × 0.4 on honey | Normal speed |
| `honey.jumpMultiplier` | Jump height × 0.5 on honey (auto-jump disabled) | Normal jump |
| `honey.wallSlide` | Slow slide down honey walls, with particles and sound | Fall past honey walls at normal speed (fall distance is still reset, so no extra damage) |

Not toggleable:
- **Fall damage.** Players always get vanilla fall damage: none on slime, 20% on honey. No setting can make these blocks deal full fall damage.
- Honey's slightly smaller hitbox. It is a block property shared by every entity.

## Commands (permission level 2 / ops)
```
/stickytoggle list                      show all toggles
/stickytoggle <key> <true|false>        set one toggle (keys tab-complete)
/stickytoggle slime <on|off>            all slime toggles at once
/stickytoggle honey <on|off>            all honey toggles at once
/stickytoggle reset                     everything back to vanilla
/stickytoggle preset <vanilla|noslime|nohoney|allOff>
```
- `slime off` / `honey off` only change that block and leave the other block as it is.
- `noslime` turns off every `slime.*` toggle and turns every `honey.*` toggle back on. `nohoney` does the reverse. `allOff` turns off everything.
- Changes are saved immediately and pushed to all connected clients.

## Config
`config/stickytoggle.json`, created on first start:
```json
{
  "slime.bounce": true,
  "slime.walkSlowdown": true,
  "slime.slipperiness": true,
  "honey.velocityMultiplier": true,
  "honey.jumpMultiplier": true,
  "honey.wallSlide": true
}
```

## Install (both sides required)
Requirements: Minecraft 1.21.11, Fabric Loader ≥ 0.19.5, Fabric API (0.141.6+1.21.11 tested), Java 21.

1. Put `stickytoggle-<version>.jar` and Fabric API in the `mods/` folder of the **server**.
2. Put the same jars in the `mods/` folder of **every client**.

Player movement is predicted by the client. If a client joins without the mod, the server logs a warning. That player will rubber-band on slime/honey whenever a toggle is off. Clients reset to vanilla on disconnect, so the mod is harmless on servers that don't have it.

## Building
```
./gradlew build
```
Output: `build/libs/stickytoggle-<version>.jar`.

## Compatibility notes
- MixinExtras is used (bundled with Fabric Loader, so it's not an extra dependency).
- No vanilla method is overwritten. Each mixin is small and documents the behaviour it gates (`src/main/java/com/steelaspect/stickytoggle/mixin`).

Author: steelaspect
