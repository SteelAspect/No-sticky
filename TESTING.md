# StickyToggle test checklist (Minecraft 1.21.11)

Setup: dedicated server + client, both with StickyToggle and Fabric API. You need to be an op. Build a test area with slime and honey floors, a 2-high honey wall, and towers about 20 blocks tall above slime and above honey. Run `/stickytoggle reset` before each section.

## Baseline (vanilla, all `true`)
- [ ] `/stickytoggle list` shows 6 keys, all `true`.
- [ ] `config/stickytoggle.json` exists with 6 keys.

## Slime
- [ ] **slime.bounce=false**: drop onto slime, no bounce. `true`: bounce returns. Sneaking still prevents bounce in both states.
- [ ] **slime.walkSlowdown=false**: walk on slime at normal speed. `true`: slowed.
- [ ] **slime.slipperiness=false**: sprint and release keys, you stop as fast as on stone. `true`: you slide.
- [ ] Preset `noslime` turns off all 3 slime keys and leaves the honey keys `true`.

- [ ] `/stickytoggle slime off` turns off all 3 slime keys without changing the honey keys. `slime on` turns them back on.

## Honey
- [ ] **honey.velocityMultiplier=false**: walk on honey at normal speed. `true`: slowed.
- [ ] **honey.jumpMultiplier=false**: full jump height on honey, auto-jump works. `true`: low jump.
- [ ] **honey.wallSlide=false**: hug a honey wall in mid-air, you fall at normal speed with no particles or slide sound, and take no fall damage on landing. `true`: slow slide with particles and sound.
- [ ] Preset `nohoney` turns off all 3 honey keys and leaves the slime keys `true`.

- [ ] `/stickytoggle honey off` turns off all 3 honey keys without changing the slime keys. `honey on` turns them back on.
- [ ] Typing `/stickytoggle slime.` still tab-completes the individual keys.

## Fall damage always vanilla (run with `/stickytoggle preset allOff`)
- [ ] 20-block drop onto slime with bounce off: no damage.
- [ ] 20-block drop onto honey: about 20% damage.

## Only players affected (run with `/stickytoggle preset allOff`)
- [ ] Drop a zombie/cow onto slime from 10+ blocks: it bounces and takes no damage.
- [ ] A mob walking on slime is slowed, and a mob on honey is slowed and jumps low.
- [ ] Drop an item and an XP orb onto slime: they slide (0.8 slipperiness). Drop an item from height: it bounces.
- [ ] A minecart/boat beside a honey wall still gets honey effects. An arrow fired onto slime behaves vanilla.
- [ ] A mob dropped from 20 blocks onto honey takes reduced damage. A mob falling past a honey wall slides.
- [ ] Pistons still push and pull blocks stuck to slime/honey, and slime and honey still don't stick to each other.
- [ ] Redstone and crafting (slime ball ↔ block, honey bottle ↔ block) are unchanged.

## Sync / live changes
- [ ] Toggle while standing on honey: speed changes instantly with no rubber-banding and no restart.
- [ ] Relog: the client receives the current state, and behaviour matches the server immediately.
- [ ] A second player sees no desync after another op changes a toggle.
- [ ] Restart the server: the toggles persist from the JSON.
- [ ] Join with a client **without** the mod: the server log shows a warning naming the player.
- [ ] Join a vanilla server with the modded client: everything is vanilla, with no leftover toggles.
- [ ] A non-op running `/stickytoggle` is denied.
- [ ] `/stickytoggle foo true` gives an "Unknown toggle" error. `/stickytoggle preset foo` gives an "Unknown preset" error.
