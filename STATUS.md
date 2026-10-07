# Status (handoff for a new session)

Last updated 2026-10-07 (0.2.0).

- Fabric mod for Minecraft 1.21.11 (Loader 0.19.5, Fabric API 0.141.6, Mojang official mappings,
  Loom 1.18.2, Gradle 9.8, JDK 25 on this machine compiling for 21).
- **0.2.0 is built and verified.** Lunar-style settings page (sidebar, mod cards with toggle
  switches, sliding settings panel, dropdowns, colour picker, keybind capture, keyboard
  navigation, narrow layout), 30 modules including Crystal Optimizer, Anchor Optimizer, Explosion
  Optimizer, Totem Pop Tweaks, No Death Animation, Hit Color, Low Fire, Low Shield, Fullbright,
  Hitboxes, Nametags and Crosshair, per-server rules, installed-mod conflict detection. Module
  list in README.md, every decision and the Modrinth research outcome in DECISIONS.md, benchmark
  method and numbers in BENCHMARKS.md, raw research in docs/modrinth-research-2026-10-07.md.
- Verified: `./gradlew build` passes with 30 JUnit tests; the smoke harness joined the offline
  server, screenshotted the HUD, the mod grid, a settings panel and the colour picker at GUI
  scale 3 (427x240, narrow layout) and 2 (640x360, sidebar layout), the HUD editor, then placed
  a crystal (instant client-side stand-in confirmed, replaced by the server's crystal) and hit it
  (removed client-side immediately). Screenshots and result files are committed under
  docs/screenshots/, docs/smoke-summary-2026-10-07.json and docs/bench-2026-10-07.json.
- Jar: build/libs/shard-0.2.0.jar (+ .sha512, value
  288ac4e512fbbab2e7ba5ddd29931b7b5a3c4a241467a583b688799bb13867f1ae3084f873a4c42b89d59bb1cc450e145da7b46e2a97eecdde823e66451318f0).
  A copy was placed on the owner's Desktop (C:\Users\OhMar\OneDrive\Desktop\shard-0.2.0.jar) for
  the Modrinth App profile "1.21.11 9_11". Note: that profile also has Marlow's Crystal Optimizer,
  Client Side Crystals, both anchor optimizers, No Death Animation, BetterHurtCam, WI Zoom,
  SprintByDefault, Custom Crosshair, TotemCounter and Sodium Fullbright installed, so the
  matching Shard modules will show "Off: <mod> is installed" there by design.
- Git: committed locally on `main`; NOT pushed. Claude's sandbox is not allowed to create public
  repositories, so the owner runs the publish steps below. ../meta/shard-manifest.json already
  points at the v0.2.0 release URL these steps create.

## Publish (owner runs once, from this folder)

```bash
gh repo create OhMarker/shard-client --public --source=. --remote=origin --push --description "Shard Client: a legit, server-safe Fabric client for crystal PvP (Minecraft 1.21.11)"
```

```bash
gh release create v0.2.0 build/libs/shard-0.2.0.jar build/libs/shard-0.2.0.jar.sha512 --title "Shard Client 0.2.0" --notes "Lunar-style settings page, Crystal and Anchor Optimizer, Explosion Optimizer, Totem Pop Tweaks, No Death Animation, Hit Color, Low Fire, Low Shield, Fullbright, Hitboxes, Nametags, Crosshair, per-server rules and conflict detection for installed mods. Minecraft 1.21.11, Fabric Loader 0.19+, Fabric API. Shard Launcher installs it automatically."
```

The jar URL must stay `https://github.com/OhMarker/shard-client/releases/download/v0.2.0/shard-0.2.0.jar`
(that is what ../meta/shard-manifest.json references). Then publish ../meta (see its STATUS.md).

## Fair-play rule
Only HUD, visuals, vanilla-legal input helpers, server-authoritative prediction and measured
performance tweaks. No combat or inventory automation, no movement/timing changes, no extra or
dropped packets, no hidden-info rendering. The owner explicitly rejected hacked-client modules.
Mods that were researched and deliberately not mirrored (and why) are listed in DECISIONS.md.

## How to verify changes
1. Start the offline server: `cd .smoke-server && java -Xmx2G -jar server.jar nogui`
   (online-mode=false, port 25599, creative; `ops.json` ops the fixed dev username `ShardSmoke`,
   the folder is gitignored, so recreate ops.json with uuid cc971d24-1e2e-3c7e-9140-8043ba1bd54a
   on a fresh checkout).
2. `./gradlew runClient -PquickPlay=localhost:25599 -PsmokeDir="$PWD/smoke-out"` writes the
   screenshots listed above plus smoke-summary.json and quits. Delete run/config/shard/config.json
   first to test defaults. Look at the PNGs; the dev window is 1280x720.
3. `./gradlew runClient -PquickPlay=localhost:25599 -PsmokeDir="$PWD/bench-out" -PsmokeBench`
   runs the BENCHMARKS.md scenario (one warm-up, six runs per configuration) and writes bench.json.
4. `./gradlew build` for the jar + tests.

## Releasing a new client version later
1. Bump `modVersion` in gradle.properties, `./gradlew build`, read build/libs/shard-<v>.jar.sha512.
2. `gh release create v<v> build/libs/shard-<v>.jar build/libs/shard-<v>.jar.sha512 --title ...`
3. In OhMarker/meta, add a build object to shard-manifest.json (url, sha512, minecraft list,
   changelog, releasedAt), bump `latest`, commit and push. Every launcher picks it up on the next
   launch; no launcher release is needed.

## Gotchas learned on 2026-10-07
- 1.21.11 handles the totem event (35) inside `ClientPacketListener.handleEntityEvent` and never
  calls `LivingEntity.handleEntityEvent` for it; hook the packet listener, after
  `ensureRunningOnSameThread`.
- Vanilla blurs in `Screen.renderBackground` before `render`; a screen that blurs again in
  `render` crashes with "Can only blur once per frame". Override `renderBackground` instead.
- In the dev client, `handleCreativeModeItemAdd` updates the server but not the client hotbar;
  use `/give` (the smoke player is op) so both sides agree, otherwise client-side placement
  returns PASS and nothing can be predicted.
- `Options.framerateLimit()` stores tens (range 1..26); `set(260)` is rejected and leaves the
  game at 10 fps. Leave it alone in automated runs.
- End crystals are two blocks wide: a benchmark grid needs two-block spacing.
- The Gradle-launched window has no focus; set `pauseOnLostFocus = false` and clear toasts
  before screenshots.

## Next steps the owner may ask for
- Roadmap in README.md (cosmetics + emote wheel, environment colours, shield state colours, pop
  ghosts, wildcard server-rule editor, more Minecraft targets).
- Launcher follow-up: consider adding Ixeris to the bundled set (legit, measurable, not worth
  cloning); see DECISIONS.md.
