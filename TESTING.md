# StickyToggle test checklist (Minecraft 1.21.11)

Setup: dedicated server + client, both with StickyToggle and Fabric API. Use a normal (non-op) account. Build a test area with slime and honey floors, a 2-high honey wall, towers about 20 blocks tall above slime and above honey, and the 2.0 test lanes listed under *New blocks* below.

Most of the movement checks also run automatically with `./gradlew runClientGameTest` (28 checks: every new toggle on and off, freezing and thorn damage, server/client sync, `all`). The manual list covers what that test doesn't: dedicated servers, two players, relogs, mobs and items. Run `/stickytoggle all on` before each section. To test a single toggle, stop the server, edit your entry under `players` in `config/stickytoggle.json`, then start it again.

## Baseline (vanilla, all `true`)
- [ ] Tab-completing `/stickytoggle ` shows `slime`, `honey`, `soulsand`, `ice`, `cobweb`, `powdersnow`, `berrybush`, `water`, `bubblecolumn` and `all`, and each one only offers `on` and `off`.
- [ ] `config/stickytoggle.json` has a `defaults` section with 13 keys and a `players` section.

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

## New blocks (2.0)
Lanes: a soul sand floor, an ice / packed ice / blue ice floor, a stone corridor filled with cobwebs, one with powder snow at feet level, one with sweet berry bushes (age 1+) on grass, a channel of flowing water, and a 10-deep soul sand bubble column plus a magma one.
- [ ] **soulsand.slowdown=false**: walk on soul sand at stone speed. `true`: slowed. Soul Speed boots still work with `true`.
- [ ] **ice.slipperiness=false**: sprint on each ice type and release keys, you stop as fast as on stone. `true`: you slide.
- [ ] **cobweb.slowdown=false**: walk through cobwebs at normal speed. Falling into a cobweb still cancels fall damage. `true`: nearly stuck.
- [ ] **powderSnow.slowdown=false**: walk through powder snow at normal speed and you still freeze (frost overlay, damage after a while). `true`: slowed.
- [ ] **berryBush.slowdown=false**: walk through berry bushes at normal speed and the thorns still hurt. `true`: slowed.
- [ ] **water.current=false**: stand in flowing water, you aren't pushed, and you can still swim and float. Lava flow still pushes you. `true`: pushed along.
- [ ] **bubbleColumn.push=false**: in a soul sand column you are not carried up, in a magma column you are not pulled down, and you get no launch at the top. Breathing and drowning are vanilla. `true`: vanilla push and pull.
- [ ] Each word (`soulsand`, `ice`, `cobweb`, `powdersnow`, `berrybush`, `water`, `bubblecolumn`) changes only its own key(s).

## Only players affected, new blocks (run with `/stickytoggle all off`)
- [ ] A mob on soul sand is slowed, and a mob in cobwebs, powder snow or berry bushes is slowed.
- [ ] An item or mob on ice slides, a boat on ice is fast.
- [ ] Items and mobs are pushed by flowing water and carried by bubble columns.

## Every block
- [ ] `/stickytoggle all off` turns off all 13 keys and says "All block effects off for you". Every block behaves as when its own toggle is off, with no rubber-banding.
- [ ] `/stickytoggle all on` turns all 13 keys back on.
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
