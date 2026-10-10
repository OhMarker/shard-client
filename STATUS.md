# Status (handoff for a new session)

## 0.11.0: in-game cosmetics, menu opens on Mods, hover crash fixed, Shards (released 2026-10-10)
- Released: https://github.com/OhMarker/shard-client/releases/tag/v0.11.0 (17 jars built from a clean
  worktree of commit 43f8067, so another session's uncommitted menu work is not in them); meta latest 0.11.0.
- Crash fix: hovering a segmented control in the Shard menu (0.10.0, every version) threw an NPE
  in `ClickGuiScreen.hoverable`. Why: DECISIONS.md "0.11.0".
- The menu always opens on Mods ("Open where you left off" keeps only the Mods category).
- Cosmetics tab: Wearing cards (Take off), filter (All/Capes/Shields/Bandanas), item grid with
  previews, rarity and Equip/Take off (`POST /v1/equip` via `CosmeticsModule.equip`), locked items
  point to the launcher shop, then the display switches. New: `CosmeticsTab`, `PreviewFit`,
  `CapeLibrary.preview/catalogue`, `Render2D.image`, catalogue `Item` gains name/rarity/previewUrl.
- Verified: all 17 versions build (`./gradlew build --continue`; run with `--no-parallel` if Gradle
  runs out of heap), 148 JUnit tests on 1.21, 1.21.11, 26.3. New smoke pass
  `-PsmokeOnly=wardrobe` (local API from shard-api `npx wrangler dev --local --port 8787 --var
  DEV_AUTH:1`, seeded with `node tools/smoke-wardrobe-seed.mjs`, plus `-PapiBase` and
  `-PcatalogueUrl`) passed on 1.21.11, 1.21 and 26.3: opens on Mods, survives 20 frames hovering a
  segment, equips the Halloween cape from the tab (worn in F5), takes it off. Shots in
  `smoke-wardrobe*/`. Passed on all 17 versions (1.21.4 needed a second run: the JVM died with
  0xC0000005 during vanilla bootstrap before Shard loaded, then passed).
- To release: meta `shard-manifest.json` builds for 0.11.0 and a GitHub release with the 17 jars
  (`build/libs/0.11.0/`), as for 0.10.0.

## 0.10.0: every Minecraft version from 1.21 to 26.3 (2026-10-10, released)
- One source tree, built for 17 Minecraft versions with Stonecutter (1.21, 1.21.1 … 1.21.11,
  26.1, 26.1.1, 26.1.2, 26.2, 26.3). `src/` is written for 1.21.11; per-version code uses
  versioned comments, rules in `stonecutter.gradle.kts` and helpers in `gg.shard.client.compat`.
  **Read docs/PORTING.md** ("Maintaining every version" and the Version notes) before changing
  anything: a new feature must build and smoke-test on every node.
- Build one: `./gradlew :<mc>:build` → `build/libs/<modVersion>/shard-<modVersion>+<mc>.jar`
  (+ .sha512). All: `./gradlew build --continue`. Run: `./gradlew :<mc>:runClient ...` (run dir
  `run-<mc>/`; 1.21.11 keeps `run/`), with `.smoke-server-<mc>/` for that version.
- Released: https://github.com/OhMarker/shard-client/releases/tag/v0.10.0 with one jar per version
  (`shard-0.10.0-mc<mc>.jar`); meta's shard-manifest.json has one 0.10.0 build per version and
  `latest` 0.10.0. Each version was smoke-tested in-game (default pass with -PcountInjections,
  plus features/screens/cosmetics/drop2 on at least one version per group) and compared to
  1.21.11 screenshots. Launcher 0.6.0 installed and started Shard on 26.3 and 1.21 end to end
  (with the bundled Sodium/Iris/Lithium set) and reached the title screen.
- Also fixed in 0.10.0: Low Fire's see-through flames on burning mobs were invisible on 1.21.11.
- Branch `multiversion` is merged into `main`.

## 0.9.0: OhMarker set, shield and bandana (2026-10-09, committed locally, not released)
- Shield cosmetic retextures held shields (first and third person, both hands, banner shields too;
  Shield module tint/opacity on top); bandana is a head feature layer (BandanaMesh/BandanaLayer).
  Why and how: DECISIONS.md "0.9.0". Settings: Show my shield / Show my bandana / Show other
  players' cosmetics (old "show-shard-capes" key migrates).
- Catalogue: cosmetics-v2.json (fallback v1). meta's cosmetics-v2.json and the shield/bandana PNGs
  were only local when this was built; push meta before releasing or the new items will not load.
- Verify: `node tools/smoke-set-seed.mjs` against the local API, then `./gradlew runClient
  -PquickPlay=localhost:25599 -PsmokeDir=$PWD/smoke-set -PsmokeOnly=set -PwindowSize=1920x1080
  -PapiBase=http://127.0.0.1:8787 -PcatalogueUrl=file:///C:/Users/OhMar/New%20folder/meta/cosmetics-v2.json`.
  19 shots in docs/screenshots/0.9.0/. 140 JUnit tests.

## 0.8.0: title screen, server list, account switcher, faster start-up (2026-10-09, released)
- Shard title screen + server list (`gui/menu/`, `MinecraftScreenSwapMixin`, hidden
  `MenuScreensModule`, Settings → Menus); account switcher via the launcher bridge
  (`launcher/AccountBridge`, `account/AccountManager`, docs/ACCOUNT-SWITCH-API.md; launcher 0.4.0).
  Only tested with `-PfakeBridge`, not a real Microsoft account.
- Fonts are built lazily (`gui/LazyFonts`, `FontManagerMixin`, `FontSetMixin`; no font JSONs) at
  the exact on-screen density, baselines snapped to pixels; start-up reload ~1.2 s, one reload per
  launch. Fire texture is sprites `shard:block/fire_0/1`, no resource pack.
- Small Items default "Small Items mod" look (uniform 0.6, items + empty arm).
- Smoke passes: full, menu, audit (click every switch/segment; 0 dead), screens (-PfakeBridge),
  features, hudscale, fire — all run at 1920x1080 on 2026-10-09. 124 JUnit tests.

## 0.7.1: bigger menu, inventory model, Settings → Window (2026-10-08, released)
- Menu density: `Scale.menuPixelsPerUnit` (1.25 px/unit at 1080p, quarter steps, capped to fit);
  font rasters in quarter steps (`tools/fonts/gen_fonts.py`, 330 definitions).
- Inventory scale: `GuiGraphicsEntityMixin` runs the player model's picture-in-picture box through
  the pose (1.21.11 ignores the pose for it). Smoke: scales pass now shoots
  smoke-inventory-model-scale3.png in survival.
- `-PsmokeOnly=features` screenshots Sky, Hitboxes, Small Items, Shield, armour Hit Color,
  Entity Optimizer and Zoom (docs/screenshots/0.7.1/). All looked right.
- Verified: 115 JUnit tests; menu and scales passes at 1920x1080; features pass.

## 0.7.0: compact menu and visual mods (2026-10-08, released)
- https://github.com/OhMarker/shard-client/releases/tag/v0.7.0 (downloaded jar's sha512 matches), merged into main, meta `latest` 0.7.0. Jar: build/libs/shard-0.7.0.jar. What changed:
  CHANGELOG.md 0.7.0; why: DECISIONS.md "0.7.0". Screenshots: docs/screenshots/0.7.0/.
- New mod menu matching the owner's mockup (launcher repo design/lunar-style-mockup.png): tabs
  Mods/Settings/Cosmetics/Friends, category tabs, 6-column tiles, settings view in the panel,
  footer with Edit HUD Layout. Target HUD and Nametags removed. Cosmetics always on.
- Verified: `./gradlew build` 114 JUnit tests pass; full smoke at 1280x720 passes
  (layoutIdenticalAcrossScales true, inventoryScaleSlotMisses 0 on the final run; one earlier run
  showed 4, a timing flake: the isolated scales pass gave 0), no mixin injection failures in the
  log. `-PsmokeOnly=menu` shoots the new menu (tabs, hover, settings view, search, eased scroll).
- Not verified in-game by a person: Sky colours, Hitbox styling, Entity Optimizer limits, Small
  Items, shield tint/opacity and armour hit tint have no smoke screenshots yet.

## 0.6.0: Shard online (2026-10-08, released)
- https://github.com/OhMarker/shard-client/releases/tag/v0.6.0, meta `latest` 0.6.0. Everyone's
  capes and tokens for play via the Shard API (../shard-api, API.md); address from meta
  services.json. 96 JUnit tests; full smoke at 1080p passes; dev runs use `-PapiBase=http://127.0.0.1:8787`
  against `npx wrangler dev --local --port 8787 --var DEV_AUTH:1` in shard-api (seed with
  `node test/seed-smoke.mjs`). Works with Shard Launcher 0.3.0 (shop, friends, admin).

## 0.5.0: the launcher cape in-game (2026-10-08)
- The Cosmetics module (Visuals, on by default) draws the cape equipped in Shard Launcher on your
  own player, mipmapped and smooth (DECISIONS.md "0.5.0"). 91 JUnit tests pass; the full smoke test
  passes at 1280x720 and 1920x1080 with `capeStatus: Wearing cape-ohmarker (4096x2048, 7 mip levels)`
  and every earlier check; screenshots in docs/screenshots/0.5.0/.
- `-PsmokeOnly=cosmetics|scales` runs one pass; `-PequippedPath=<equipped.json>` stands in for the
  launcher in dev (run/dev-cosmetics/ holds one with the OhMarker cape).

## 0.4.0: published 2026-10-08
- Merged into `main` (0b6cd41) and pushed; release https://github.com/OhMarker/shard-client/releases/tag/v0.4.0
  (the downloaded jar's sha512 matches the one below); ../meta pushed with `latest` 0.4.0, and the live
  raw manifest reports it. Every Shard Launcher installs 0.4.0 on its next launch.

### How 0.4.0 was built and verified
Last updated 2026-10-08. All eight steps of `docs/PROMPT-ui-overhaul-0.4.0.md` are done; the
owner approved steps 1-7. What changed is in CHANGELOG.md, the reasons in DECISIONS.md ("0.4.0",
including "Step 8: final verification").
- **Jar:** build/libs/shard-0.4.0.jar (fabric.mod.json says 0.4.0), sha512
  c4058524568ff2fc266af76282f10b16dcb4a5e3313513681cc24fafc47cba8bd80599f4bf6abd3ca00825f75e4f159c11eea752e2276664e2e82135d6dab1b3
  (also in build/libs/shard-0.4.0.jar.sha512). Note: build/libs/shard-0.3.0.jar was rebuilt
  from later code during step 7 and is NOT the published 0.3.0; the GitHub release is.
- **Tests:** `./gradlew build` passes, 86 JUnit tests, 0 failures.
- **Smoke test** at 1280x720, 1920x1080 and 2560x1440 (this monitor is 1920x1080, so the
  1440 window is 2560x1061): `layoutIdenticalAcrossScales: true`, `inventoryScaleSlotMisses: 0`
  (47 slots), `borderlessCoversMonitor: true`, `borderlessRestoresWindow: true`,
  `fightLogKills: 1`, HUD editor undo and the crystal place/hit check pass at all three sizes.
  The new `clippedTexts` list contains no module name at any size, only one-line descriptions,
  placeholders and long hints. All 216 PNGs were looked at; 4x crops are smooth. Summaries in
  docs/smoke-summary-0.4.0-{720,1080,1440}.json.
- **Benchmark:** HUD layer 0.18-0.19 ms per frame with the busy HUD, frame rates at the cap,
  1% lows 99 fps (BENCHMARKS.md, "0.4.0 final check").
- **Screenshots:** docs/screenshots/0.4.0/final-* (before/after of the owner's 0.3.0 view:
  final-before-after-hud.png).
- **Known limits (recorded, not fixed):** fire tints multiply instead of recolouring, no shield
  opacity, alt-tab speed in borderless is not automated, a wide HUD editor toolbar can cover
  top-left elements, chat on a 720p window can reach the kit counter.

### How 0.4.0 was published (the same steps work for later versions)
1. Merge `v0.4.0` into `main` (`git checkout main && git merge --no-ff v0.4.0`) and push main.
2. Release (the automation sandbox may not create releases; if refused, the owner runs it from a
   Desktop .cmd):

```bash
gh release create v0.4.0 build/libs/shard-0.4.0.jar build/libs/shard-0.4.0.jar.sha512 --repo OhMarker/shard-client --title "Shard Client 0.4.0" --notes-file docs/release-notes-0.4.0.md
```

3. In ../meta, add the 0.4.0 build to shard-manifest.json (url
   `https://github.com/OhMarker/shard-client/releases/download/v0.4.0/shard-0.4.0.jar`, the
   sha512 above, `"minecraft": ["1.21.11"]`, `"fabricLoader": ">=0.19.0"`, the changelog,
   `releasedAt`), set `latest` to 0.4.0, commit and push. Check the downloaded jar's sha512
   matches before pushing meta.

## 0.3.0 (released)

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
  the automation sandbox may not create public repositories or releases; pushing to the existing
  repos works.

## Publish (owner runs once, from this folder)

```bash
gh repo create OhMarker/shard-client --public --source=. --remote=origin --push --description "Shard Client: a legit, server-safe Fabric client for crystal PvP (Minecraft 1.21.11)"
```

```bash
gh release create v0.2.0 build/libs/shard-0.2.0.jar build/libs/shard-0.2.0.jar.sha512 --title "Shard Client 0.2.0" --notes "Settings page, Crystal and Anchor Optimizer, Explosion Optimizer, Totem Pop Tweaks, No Death Animation, Hit Color, Low Fire, Low Shield, Fullbright, Hitboxes, Nametags, Crosshair, per-server rules and conflict detection for installed mods. Minecraft 1.21.11, Fabric Loader 0.19+, Fabric API. Shard Launcher installs it automatically."
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
