# StickyToggle

A Fabric mod for **Minecraft Java 1.21.11** that lets each player choose how slime blocks and honey blocks physically affect **them**.

> **Only players are affected.** Mobs, items, XP orbs, minecarts, boats, TNT, arrows and every game mechanic (pistons, block sticking, redstone, crafting, mob AI) stay 100% vanilla. Every hook checks `entity instanceof PlayerEntity` and falls through to vanilla code for everything else.

## Features
- Any player can run the command. No op needed.
- Settings are **per player**: your command only changes your own movement, and other players are unaffected.
- Changes apply live, and your settings are remembered across relogs and server restarts.
- The server syncs your settings to your client on join and on change, so client-side movement prediction matches the server.
- Saved in `config/stickytoggle.json`.

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

## Commands (everyone)
```
/stickytoggle slime <on|off>    all slime effects, for you only
/stickytoggle honey <on|off>    all honey effects, for you only
```
- Each command only changes its own block, and only for the player who ran it.
- It has to be run by a player. The server console can't use it.

## Config
`config/stickytoggle.json` is managed by the mod:
```json
{
  "defaults": {
    "slime.bounce": true,
    "slime.walkSlowdown": true,
    "slime.slipperiness": true,
    "honey.velocityMultiplier": true,
    "honey.jumpMultiplier": true,
    "honey.wallSlide": true
  },
  "players": {
    "<player uuid>": { "slime.bounce": false, "...": "..." }
  }
}
```
- `defaults` sets the starting values for players who have never used the command.
- `players` holds each player's own settings. To change a single effect for one player, edit their entry while the server is stopped.

## Download
Prebuilt jar: [`releases/stickytoggle-1.0.0.jar`](releases/stickytoggle-1.0.0.jar)

## Install (both sides required)
Requirements: Minecraft 1.21.11, Fabric Loader ≥ 0.19.5, Fabric API (0.141.6+1.21.11 tested), Java 21.

1. Put `stickytoggle-<version>.jar` and Fabric API in the `mods/` folder of the **server**.
2. Put the same jars in the `mods/` folder of **every client**.

Player movement is predicted by the client. If a client joins without the mod, the server logs a warning. That player can't change their settings and always gets vanilla behaviour, unless their `defaults` or `players` entry has effects turned off. In that case they will rubber-band on slime and honey. Clients reset to vanilla on disconnect, so the mod is harmless on servers that don't have it.

## Building
```
./gradlew build
```
Output: `build/libs/stickytoggle-<version>.jar`.

## Compatibility notes
- MixinExtras is used (bundled with Fabric Loader, so it's not an extra dependency).
- No vanilla method is overwritten. Each mixin is small and documents the behaviour it gates (`src/main/java/com/steelaspect/stickytoggle/mixin`).

Author: steelaspect
