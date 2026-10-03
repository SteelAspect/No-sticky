# StickyToggle test checklist (Minecraft 1.21.11)

Setup: dedicated server + client, both with StickyToggle and Fabric API. Use a normal (non-op) account. Build a test area with slime and honey floors, a 2-high honey wall, and towers about 20 blocks tall above slime and above honey. Run `/stickytoggle all on` before each section. To test a single toggle, stop the server, edit your entry under `players` in `config/stickytoggle.json`, then start it again.

## Baseline (vanilla, all `true`)
- [ ] Tab-completing `/stickytoggle ` shows only `slime`, `honey` and `all`, and each one only offers `on` and `off`.
- [ ] `config/stickytoggle.json` has a `defaults` section with 6 keys and a `players` section.

## Slime
- [ ] **slime.bounce=false**: drop onto slime, no bounce. `true`: bounce returns. Sneaking still prevents bounce in both states.
- [ ] **slime.walkSlowdown=false**: walk on slime at normal speed. `true`: slowed.
- [ ] **slime.slipperiness=false**: sprint and release keys, you stop as fast as on stone. `true`: you slide.

- [ ] `/stickytoggle slime off` turns off all 3 slime keys without changing the honey keys. `slime on` turns them back on.

## Honey
- [ ] **honey.velocityMultiplier=false**: walk on honey at normal speed. `true`: slowed.
- [ ] **honey.jumpMultiplier=false**: full jump height on honey, auto-jump works. `true`: low jump.
- [ ] **honey.wallSlide=false**: hug a honey wall in mid-air, you fall at normal speed with no particles or slide sound, and take no fall damage on landing. `true`: slow slide with particles and sound.

- [ ] `/stickytoggle honey off` turns off all 3 honey keys without changing the slime keys. `honey on` turns them back on.

## Both blocks
- [ ] `/stickytoggle all off` turns off all 6 keys and says "Slime and honey effects off for you". Slime and honey both behave as when each is off, with no rubber-banding.
- [ ] `/stickytoggle all on` turns all 6 keys back on.
- [ ] After `slime off` only, `all on` turns the slime keys back on and leaves honey on.

## Fall damage always vanilla (run with `/stickytoggle all off`)
- [ ] 20-block drop onto slime with bounce off: no damage.
- [ ] 20-block drop onto honey: about 20% damage.

## Only players affected (run with `/stickytoggle all off`)
- [ ] Drop a zombie/cow onto slime from 10+ blocks: it bounces and takes no damage.
- [ ] A mob walking on slime is slowed, and a mob on honey is slowed and jumps low.
- [ ] Drop an item and an XP orb onto slime: they slide (0.8 slipperiness). Drop an item from height: it bounces.
- [ ] A minecart/boat beside a honey wall still gets honey effects. An arrow fired onto slime behaves vanilla.
- [ ] A mob dropped from 20 blocks onto honey takes reduced damage. A mob falling past a honey wall slides.
- [ ] Pistons still push and pull blocks stuck to slime/honey, and slime and honey still don't stick to each other.
- [ ] Redstone and crafting (slime ball ↔ block, honey bottle ↔ block) are unchanged.

## Per-player / sync
- [ ] A non-op player can run `/stickytoggle slime off`.
- [ ] With 2 players, A runs `/stickytoggle honey off`. A walks on honey at normal speed, B is still slowed, and neither one rubber-bands.
- [ ] B sees A move smoothly, without jitter.
- [ ] A changes a setting while standing on honey: A's speed changes instantly with no rubber-banding.
- [ ] A relogs: A's settings are kept and apply straight away.
- [ ] Restart the server: everyone's settings persist, and `players` in the JSON lists A's UUID.
- [ ] Running the command from the server console gives a "must be a player" error.
- [ ] Join with a client **without** the mod: the server logs a warning naming the player.
- [ ] Join a vanilla server with the modded client: everything is vanilla, with no leftover settings.
