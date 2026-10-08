# Status (handoff for a new session)

## 0.4.0 in progress (branch `v0.4.0`)
Work follows `docs/PROMPT-ui-overhaul-0.4.0.md` step by step, stopping for the owner at each ★.
- Step 1 done: `docs/DESIGN.md`, mockups in `docs/design/` (owner picked **A: rail + list + detail**).
- Step 2 done: sharp text (linear-filtered Shard font atlases, per-density rasters) and the Lucide
  icon atlas; proof in `docs/screenshots/0.4.0/`. Owner approved.
- Step 3 done: new menu (rail + list/grid + detail column, search over settings, favorites,
  keyboard, segmented controls). Run the smoke test with `-PwindowSize=1920x1080` too; the wide
  layout only appears from 1640 px. Owner approved.
- Step 4 done: HUD editor (snapping + guides, multi-select, align/distribute, 1 px nudge, undo/redo,
  grid, settings side panel) and layout presets; HUD positions are now anchor + offset.
  Screenshots `docs/screenshots/0.4.0/step4-*`. Owner approved.
- Step 5 done: module audit (table in DECISIONS.md), renames with `legacyKey()`, upgrades to FPS,
  Ping, Keystrokes, Armor, Totem Counter, Item Counter, Effects, Attack Cooldown, Totem Pops,
  Nametags and the Crystal Optimizer readout. Waiting for the owner's OK on step 5.
- Next: step 6, the owner's must-have list (Low Fire incl. ground fire + colours, Crosshair
  preview, Shield, Anchor Glow, Crystal visuals, separate GUI scales, borderless fullscreen, HUD
  styles), then steps 7-8.
- The 0.3.0 notes below still describe the released build.

Last updated 2026-10-07 (0.3.0).

- Fabric mod for Minecraft 1.21.11 (Loader 0.19.5, Fabric API 0.141.6, Mojang official mappings,
  Loom 1.18.2, Gradle 9.8, JDK 25 on this machine compiling for 21).
- **0.3.0 is built, verified and committed locally.** The settings page, popovers and HUD editor
  are drawn in fixed-density design units with Inter (the launcher's font) and anti-aliased
  corners, so they look identical at GUI scale 1, 2, 3, 4 and Auto; the HUD has a shared style
  group with global defaults and a global HUD scale; the Settings entry has Appearance, HUD,
  Keybinds, Profiles with descriptions, a wildcard server-rule editor, Export/Import and Reset
  all; every module has an About paragraph; config schema 3 migrates 0.2.0 files. Details and
  reasons in DECISIONS.md ("0.3.0").
- Verified: `./gradlew build` passes with 53 JUnit tests (scale maths, corner masks, HUD style
  inheritance, config migration, export/import, placeholders, item ids, server-rule editor plus
  the 0.2.0 suites). The smoke run screenshotted the HUD, grid, panel, colour picker, Settings
  page and HUD editor at GUI scale 1, 2, 3, 4 and Auto (on the 1280x720 dev window vanilla caps
  "4" at 3) and recorded `layoutIdenticalAcrossScales: true`; 4x crops show smooth text; the
  crystal place/hit check passed. All PNGs were looked at. Curated screenshots are in
  docs/screenshots/, the summary in docs/smoke-summary-2026-10-07-0.3.0.json. The benchmark
  shows no frame-time regression against a same-day 0.2.0 baseline (BENCHMARKS.md).
- Jar: build/libs/shard-0.3.0.jar (+ .sha512, value
  3cce6f7f5183aeac0e2b029e0d5f541b81d94e22e4b515052674efc128dd7a3f4d4ad1d1493d76ed7925586e678edc80a24bfd530a5fc23cca519ee62a495287).
  ../meta/shard-manifest.json lists 0.3.0 as latest (committed locally in meta).
- Mojang approved the launcher's Azure app on 2026-10-07 (owner's report), so Microsoft sign-in
  in the launcher should work; see the launcher's STATUS.md.
- Published 2026-10-08: https://github.com/OhMarker/shard-client with releases v0.2.0 and v0.3.0
  (downloaded jars match the local sha512s), and https://github.com/OhMarker/meta serves the
  manifest with `latest` 0.3.0. The owner ran the steps below via a Desktop script because
  Claude's sandbox may not create public repositories or releases; pushing to the existing
  repos works.

## Publish (owner runs once, from this folder)

```bash
gh repo create OhMarker/shard-client --public --source=. --remote=origin --push --description "Shard Client: a legit, server-safe Fabric client for crystal PvP (Minecraft 1.21.11)"
```

```bash
gh release create v0.2.0 build/libs/shard-0.2.0.jar build/libs/shard-0.2.0.jar.sha512 --title "Shard Client 0.2.0" --notes "Lunar-style settings page, Crystal and Anchor Optimizer, Explosion Optimizer, Totem Pop Tweaks, No Death Animation, Hit Color, Low Fire, Low Shield, Fullbright, Hitboxes, Nametags, Crosshair, per-server rules and conflict detection for installed mods. Minecraft 1.21.11, Fabric Loader 0.19+, Fabric API. Shard Launcher installs it automatically."
```

```bash
gh release create v0.3.0 build/libs/shard-0.3.0.jar build/libs/shard-0.3.0.jar.sha512 --title "Shard Client 0.3.0" --notes "Smooth Inter text everywhere in Shard's UI, a settings page that looks the same at every GUI scale, roomier layout, anti-aliased corners, HUD styles with global defaults and HUD scale, Appearance/Keybinds/Profiles/Server rules/Export/Reset settings, new options for Crosshair, Nametags, Hit Color, Low Fire, Crystal Optimizer, Totem Pop Tweaks, Explosion Optimizer, Zoom, Keystrokes and Item Counter. 0.2.0 configs migrate automatically. Minecraft 1.21.11, Fabric Loader 0.19+, Fabric API."
```

Run the v0.2.0 release first only if it does not exist yet (0.2.0 was never published). The jar
URLs must stay `https://github.com/OhMarker/shard-client/releases/download/v<version>/shard-<version>.jar`
(that is what ../meta/shard-manifest.json references). Then push ../meta (see its STATUS.md).

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
   screenshots listed above at every GUI scale plus smoke-summary.json and quits (about 70 s in
   the world). Delete run/config/shard/config.json
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
- Opening a screen recentres the cursor; the smoke test parks it in the page margin afterwards or
  hover states leak into screenshots.
- `GuiGraphics.enableScissor` transforms by the pose in 1.21.11, so scissors inside a scaled
  pose are given in the pose's units.
- A TTF font provider has one fixed size; each text size needs its own font definition. Vanilla
  places a glyph's baseline 7 units below the draw y, whatever the font.
- Windows refuses to delete deep worktrees with plain `git worktree remove`; use PowerShell
  `Remove-Item -LiteralPath "\\?\C:\full\path" -Recurse -Force`, then `git worktree prune`.
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
  ghosts, more Minecraft targets).
- Launcher follow-up: consider adding Ixeris to the bundled set (legit, measurable, not worth
  cloning); see DECISIONS.md.
