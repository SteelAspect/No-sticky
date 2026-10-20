# Alloy Sticky Toggle: own screen + Alloy Hub page (2026-10-20)

Goal: settings work without MaLiLib, look like the rest of Alloy (Nocturne via Alloy UI), and also show in Alloy Hub,
both built from the same list so they never disagree. CLAUDE.md "Menus, screens and HUDs".

## Where settings live (unchanged)
The player's toggles live on the **server** (`config/stickytoggle.json`). The client mirrors them in
`StickyConfig.CLIENT` and changes them with a `SetPayload` request (`StickyToggleClient.request`). Nothing new is
saved on the client. When the server doesn't have the mod (or on the title screen), every row is disabled with the
reason "not on this server".

## One option list: `StickyOptions` (client)
- Section **Blocks**: one toggle per block group (Slime, Honey, Soul Sand, Ice, Cobweb, Powder Snow, Sweet Berry Bush,
  Water Current, Bubble Column) + **All Blocks**. ON = vanilla (all of the group's settings on).
- Section **Settings**: one toggle per individual setting (13).
- Labels/hints reuse the existing lang keys (`stickytoggle.config.prettyName.*`, `stickytoggle.config.comment.*`).

## Own screen: `StickyToggleScreen extends AlloyScreen`
- One Card (max 360 wide) titled "Alloy Sticky Toggle", the option list in a scroll area, a primary Done button.
- Opened by Mod Menu's config button when MaLiLib is missing. With MaLiLib, Mod Menu keeps opening the MaLiLib menu
  (it has hotkeys); the MaLiLib menu stays as is.

## Alloy Hub page: `HubPlugin` (`cytra-hub` entrypoint, optional)
- `hub.page("stickytoggle")`, category "Movement", `needsServer(true)`, `commands("stickytoggle")`.
- Settings: same two sections from the same group/toggle data, each row `enabledWhen(server has mod)`.
- Status: one row "Server" → "Has Alloy Sticky Toggle" / "Missing (vanilla movement)".
- Compiled against the Hub jar in `libs/` (compile-only); only loaded when the Hub is installed.

## Not doing now
- Hotkeys in the Nocturne screen (MaLiLib keeps them).
- Removing MaLiLib support.

## Tests
- `StickyToggleScreenGameTest`: buttons send requests, labels follow the server, `/stickytoggle` updates the screen,
  screenshot.
