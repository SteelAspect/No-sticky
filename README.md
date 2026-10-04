# StickyToggle

A Fabric mod for **Minecraft Java 1.21.11** that lets each player choose how slime, honey, soul sand, ice, cobwebs, powder snow, sweet berry bushes, water currents and bubble columns physically affect **them**.

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
| `soulsand.slowdown` | Movement × 0.4 on soul sand (Soul Speed offsets it) | Normal walking speed |
| `ice.slipperiness` | Ice, packed ice and frosted ice 0.98, blue ice 0.989 (slides) | Default 0.6 (no slide) |
| `cobweb.slowdown` | Movement × 0.25 sideways and × 0.05 vertically in cobwebs | Move through cobwebs at normal speed |
| `powderSnow.slowdown` | Movement × 0.9 sideways and × 1.5 vertically inside powder snow | Normal movement inside powder snow |
| `berryBush.slowdown` | Movement × 0.8 sideways and × 0.75 vertically in sweet berry bushes | Normal movement |
| `water.current` | Flowing water pushes you along its current | Not pushed (you still swim and float as normal; lava is unchanged) |
| `bubbleColumn.push` | Soul sand columns push you up, magma columns pull you down, and the top of a column launches you | Bubble columns act like still water |

Not toggleable:
- **Fall damage.** Players always get vanilla fall damage: none on slime, 20% on honey. No setting can make these blocks deal full fall damage.
- **Other damage.** Freezing in powder snow, sweet berry thorns, drowning and lava damage stay vanilla. Cobwebs, powder snow, berry bushes and bubble columns still reset fall distance like vanilla.
- Honey's slightly smaller hitbox and soul sand's lower top. They are block shapes shared by every entity.

## Commands (everyone)
```
/stickytoggle slime <on|off>          slime bounce, walk slowdown and slipperiness
/stickytoggle honey <on|off>          honey speed, jump and wall slide
/stickytoggle soulsand <on|off>       soul sand slowdown
/stickytoggle ice <on|off>            ice slipperiness
/stickytoggle cobweb <on|off>         cobweb slowdown
/stickytoggle powdersnow <on|off>     powder snow slowdown
/stickytoggle berrybush <on|off>      sweet berry bush slowdown
/stickytoggle water <on|off>          water current push
/stickytoggle bubblecolumn <on|off>   bubble column push and pull
/stickytoggle all <on|off>            everything above at once
```
- Each word only changes its own block. `all` changes every toggle. Every command only affects the player who ran it.
- It has to be run by a player. The server console can't use it.

## Menu and hotkeys (optional, needs MaLiLib)
With [MaLiLib](https://modrinth.com/mod/malilib) installed on your client, StickyToggle gets a menu:
- open it from **Mod Menu** (the config button), from the mod list at the top right of any MaLiLib config screen (Litematica, Tweakeroo, MiniHUD...), or with the *Open Menu* hotkey;
- **Blocks** tab: one row per block (plus *All Blocks*), switching every setting of that block;
- **Settings** tab: one row per setting;
- every row has an ON/OFF button and a hotkey. Every hotkey is unbound by default.

The menu shows and changes **your settings on the server**, exactly like the command (a block row shows ON only while all of its settings are on). It only works on a server with StickyToggle 2.2+, or in singleplayer. Only the hotkeys are saved on your computer, in `config/stickytoggle-client.json`. Without MaLiLib everything still works through the command.

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
    "honey.wallSlide": true,
    "soulsand.slowdown": true,
    "ice.slipperiness": true,
    "cobweb.slowdown": true,
    "powderSnow.slowdown": true,
    "berryBush.slowdown": true,
    "water.current": true,
    "bubbleColumn.push": true
  },
  "players": {
    "<player uuid>": { "slime.bounce": false, "...": "..." }
  }
}
```
- `defaults` sets the starting values for players who have never used the command.
- `players` holds each player's own settings. To change a single effect for one player, edit their entry while the server is stopped.

## Download
Prebuilt jar: [`releases/stickytoggle-2.2.0.jar`](releases/stickytoggle-2.2.0.jar)

## Install (both sides required)
Requirements: Minecraft 1.21.11, Fabric Loader ≥ 0.19.3, Fabric API (0.141.6+1.21.11 tested), Java 21. Optional on clients: MaLiLib ≥ 0.27.20 for the menu and hotkeys, Mod Menu for the config button.

1. Put `stickytoggle-<version>.jar` and Fabric API in the `mods/` folder of the **server**.
2. Put the same jars in the `mods/` folder of **every client**.

Player movement is predicted by the client. If a client joins without the mod, the server logs a warning. That player can't change their settings and always gets vanilla behaviour, unless their `defaults` or `players` entry has effects turned off. In that case they will rubber-band on those blocks. Clients reset to vanilla on disconnect, so the mod is harmless on servers that don't have it.

## Building
```
./gradlew build
```
Output: `build/libs/stickytoggle-<version>.jar`.

In-game tests (opens a game window, walks a player through every block with each toggle on and off and checks the distances, damage and server/client sync, then drives the menu, hotkeys and Mod Menu button; screenshots land in `build/run/clientGameTest/screenshots/`):
```
./gradlew runClientGameTest
```
To check the client still starts without MaLiLib: `./gradlew runClient -PwithoutMalilib`.

## Compatibility notes
- MixinExtras is used (bundled with Fabric Loader, so it's not an extra dependency).
- No vanilla method is overwritten. Each mixin is small and documents the behaviour it gates (`src/main/java/com/steelaspect/stickytoggle/mixin`).

Author: steelaspect
