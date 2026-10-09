# Porting Shard Client to other Minecraft versions

Shard Client is one source tree built for every Minecraft release the launcher lists
(1.21 → 1.21.11 and 26.1 → 26.3) with [Stonecutter](https://stonecutter.kikugie.dev) 0.9.8 and
loom-back-compat 0.4.2. The goal is full feature parity: every module, screen, cosmetic and HUD
element that exists on 1.21.11 works on every version.

## Layout
- `src/` is the shared source, written for **1.21.11** (the `vcsVersion` and `stonecutter active`
  version). Always commit with 1.21.11 active.
- `versions/<mc>/gradle.properties`: per-version `mcCompat` (fabric.mod.json range), `mcReleases`,
  `fabricApiVersion`, `modmenuVersion`, optional dev companions (`sodiumVersion` ...).
- `stonecutter.gradle.kts`: global string/regex replacements for pure renames.
- `build.gradle.kts` runs once per node. Minecraft < 26.1 uses fabric-loom-remap with Mojang
  mappings; 26.1+ is unobfuscated (fabric-loom, Java 25).
- Jars: `./gradlew :<mc>:build` → `build/libs/<modVersion>/shard-<modVersion>+<mc>.jar` (+ .sha512).

## Maintaining every version
All 17 nodes (1.21 → 1.21.11, 26.1 → 26.3) are ported and released from the same `src/`; every
change has to keep all of them building and behaving the same.

**Adding a feature**
1. Write it in `src/` against 1.21.11 (never switch the active version). Use what already absorbs
   the version differences: GUI code draws through `Render2D`/`Fonts` and names its graphics `g`
   (the `g.pose()`, `g.blit(...)`, `g.enableScissor(...)` and `g.nextStratum()` rules depend on it),
   world lines go through `compat.Lines`/`compat.WorldDraw`, pixels through `image.setPixel`
   (`compat.NativeImages` before 1.21.2), colours through `ARGB` (`compat.Argb`, `FastColor`), and
   so on. Anything else that differs gets a helper in `compat` or a versioned block.
2. A new mixin: `javap` the target on the oldest (1.21) and newest (26.3) jars and on the nodes
   where Shard's code paths split; add `"versioned"` entries when the target only exists on some.
   Then `./gradlew :<mc>:compileJava :<mc>:processResources` and `python tools/mixin-targets.py <mc>`
   on those nodes (it checks `method`/`target`, including `method = "*"`, but not @Shadow,
   @Accessor or handler argument types).
3. `./gradlew build --continue`: all 17 nodes must compile and pass the JUnit tests.
4. Run the smoke pass that exercises the feature (default, `features`, `screens -PfakeBridge`,
   `cosmetics`, `drop2`; see Verification) with `-PcountInjections` (not on 1.21.11, whose dev
   companions trip it) on one node of every family the change touches. The families, where the
   code paths split: 26.3 · 26.2 · 26.1.x · 1.21.11 · 1.21.9-1.21.10 · 1.21.6-1.21.8 · 1.21.5 ·
   1.21.4 · 1.21.2-1.21.3 · 1.21-1.21.1. Unversioned code: at least 1.21.11, 1.21.5 (PoseStack
   GUI), 1.21.1 (no render states, texture-id blits) and 26.3. A new versioned block: both sides of
   its boundary. Compare summaries and screenshots with that node's previous run.

**Adding the next Minecraft release as a node**
1. `versions/<mc>/gradle.properties` (copy the newest node's; set `mcCompat`, `mcReleases`,
   `fabricApiVersion`, `modmenuVersion`) and the version in `settings.gradle.kts` `versions(...)`.
2. `./gradlew :<mc>:genSources`, then `python tools/mixin-sigdiff.py <previous> <mc>` and
   `python tools/mixin-targets.py <mc>`; diff the sources of every mixin target and of the classes
   the compile errors name against the previous node.
3. Fix in the order of the workflow below: global rules (`since` = the new version, so they stay in
   force after it), `compat` helpers, then `//? if >=<mc>` blocks. 1.21.11 stays the active version.
4. Runtime: a `.smoke-server-<mc>/` from Mojang's manifest and the full pass set with
   `-PcountInjections`, compared with the previous newest node; then the default pass on 1.21.11
   and on the previous newest node, and `./gradlew build --continue`.
5. Notes here, CHANGELOG.md and the release notes. The launcher picks the build per instance from
   `mcReleases` (shard-manifest.json); publishing it is a separate step.

## Workflow for one version
1. `./gradlew :<mc>:compileJava` compiles the generated copy in
   `versions/<mc>/build/generated/stonecutter/main/java/...`; the line numbers match `src/`.
   Do not switch the active version to fix errors; edit `src/` directly.
2. Fix differences, in this order of preference:
   - **Pure renames** (class moved package, class renamed) → a global replacement in
     `stonecutter.gradle.kts`, using regex with word boundaries when the name is short or common.
   - **Small API differences used in many places** → one helper in
     `gg.shard.client.compat` (e.g. `Mc.id(ns, path)`), versioned once inside the helper.
   - **Everything else** → versioned comments in place:
     ```java
     //? if >=1.21.11 {
     newCall();
     //?} else {
     /*oldCall();
     *///?}
     ```
     Code for other versions lives inside `/* */`, so the 1.21.11 view stays compilable.
     Nested conditions use `/^ ^/`.
3. Mixins: read the real target signatures of that version with `javap -p -c` on the mapped
   Minecraft jar in the Loom cache (`~/.gradle/caches/fabric-loom/` or `.gradle/loom-cache/`).
   A mixin whose target only exists on some versions: wrap the whole mixin class body/file in a
   versioned block (a fully commented-out .java file compiles fine) and list it under
   `"versioned"` in `src/main/resources/shard.mixins.json`, e.g.
   `"versioned": { "AvatarRendererMixin": ">=1.21.9", "PlayerRendererMixin": "<1.21.9" }`.
   The build drops non-matching entries from that version's mixin config (predicates are
   space-separated and all must match: `>=1.21.6 <26.1`).
   `defaultRequire: 1` means a failed injection crashes start-up, which is the signal you want.
4. `./gradlew :1.21.11:build` must still pass (all JUnit tests) after every change.
5. Run the game on that version (see Verification) and look at the screenshots.

## Verification per version
- Server: `.smoke-server-<mc>/` with that version's `server.jar` (download URL from Mojang's
  version manifest), `server.properties` copied from `.smoke-server/` (offline, port 25599,
  creative), `ops.json` copied, `eula.txt` with `eula=true`. Start it with
  `java -Xmx2G -jar server.jar nogui` (Java 25 works for all versions).
- Client: `./gradlew :<mc>:runClient -PquickPlay=localhost:25599 -PsmokeDir=<abs dir>`
  (run dir `run-<mc>/`). The smoke test writes PNGs and `smoke-summary.json`, then quits.
- Pass criteria: no crash, no mixin errors in the log, summary checks the same as 1.21.11, and
  the screenshots look like the 1.21.11 ones.
- Make the comparison fair: copy `run/config/shard/` into `run-<mc>/config/` (a fresh config has
  other HUD elements and modules on), and give the new world the 1.21.11 world's obsidian floor
  at y = -23 (the smoke player is teleported to -22): set `enable-rcon=true`,
  `rcon.password=...`, `broadcast-rcon-to-ops=false` in that server's properties and send
  `forceload add -64 -64 63 63`, `fill -60 -23 -60 60 -23 60 minecraft:obsidian`,
  `forceload remove all`. Without the floor the features pass's zombie falls out of view.
- Add `-PcountInjections` on versions without the dev companions (Sodium's own mixins trip it).
- Besides the default pass, run `-PsmokeOnly=features` (Sky, Hitboxes, Shield...),
  `screens -PfakeBridge` (title screen, server list, accounts), `cosmetics -PequippedPath=...`
  (mipmapped cape) and `drop2` (capes, shield, bandana; local Shard API seeded with
  `node tools/smoke-drop2-seed.mjs`, plus `-PapiBase=http://127.0.0.1:8787 -PcatalogueUrl=<meta
  cosmetics-v2.json>`), and compare with the 1.21.11 folders (smoke-features, smoke-scr,
  smoke-drop2).

## Version notes
Record every non-obvious difference here (what changed, which version, how it was handled).

### Tooling
- Versioned blocks: a commented line must not start with `/*?` (Stonecutter reads it as one of
  its own comments). Restructure the code (e.g. pull a lambda into a local) instead.
- Most Shard injectors are `require = 0`, so a moved target fails silently. Dev runs pass
  `-Dmixin.debug.countInjections=true`; grep the client log for `Injection warning` / `expected`
  and compare with a 1.21.11 run (the baseline has none for Shard).
- Quick static check before running: `javap -p -s` every `@Mixin` target and every
  `method =` / `target =` descriptor on the version's mapped jar (Loom cache
  `minecraft-merged/<mc>-loom.mappings...`). 1.21.9 and 1.21.10 have identical class lists.

### 1.21.10 (also applies to 1.21.9 unless noted)
- **Pure renames** (stonecutter.gradle.kts, `moved(...)` helper matches `.` and `/`
  separators so imports and mixin descriptors both follow): `Identifier` was
  `ResourceLocation` (regex on the word), `net.minecraft.util.Util` → `net.minecraft.Util`,
  `client.renderer.rendertype.RenderType` → `client.renderer.RenderType`, `EndCrystalModel`,
  `ShieldModel`, `PlayerModel` directly in `client.model`, `boss.EnderDragonPart`,
  `projectile.AbstractArrow`, `monster.Zombie`, `SoundInstance.getIdentifier()` →
  `getLocation()`, `GuiGraphics.renderOutline` → `submitOutline`, `Screen.init(II)V` →
  `init(Lnet/minecraft/client/Minecraft;II)V` (mixin descriptor string).
- `@Nullable`: JSpecify is not on the classpath; mapped to `org.jetbrains.annotations.Nullable`
  (also TYPE_USE, so `<T> @Nullable T` still compiles).
- `RenderTypes` (the static factories) does not exist; `compat.RenderTypes` forwards to the
  static methods on `RenderType` and the import is redirected there. Add a method to it when
  new code uses another factory.
- Line width: 1.21.11 passes it per vertex (`ShapeRenderer.renderShape(..., width)`); before,
  it is one value per draw (`RenderSystem.getShaderLineWidth()`, set by the type's line state;
  vanilla = max(2.5, windowWidth / 1920 * 2.5)). `compat.Lines.type(width)` returns a cached
  `RenderType` subclass per width that draws through `RenderType.lines()`, and
  `CompositeRenderTypeMixin` (<1.21.11) swaps that width in. Use `Lines.type/shape` for any
  world line drawing.
- Gizmos (`net.minecraft.gizmos`) and `EntityHitboxDebugRenderer` do not exist. Hitboxes:
  `EntityRendererHitboxMixin` (<1.21.11) cancels `EntityRenderer.extractHitboxes(T,S,F)` (only
  called for entities vanilla would box) and clears the state's hitbox fields; the module queues
  the entity and draws box / fill / eye line / look arrow after the entities with
  `ShapeRenderer.renderLineBox`, `addChainedFilledBoxVertices` + `RenderType.debugFilledBox()`
  and plain line pairs (a four-line arrow head stands in for the arrow gizmo).
- Samplers (`GpuSampler`, `SamplerCache`, `AbstractTexture.sampler`) do not exist: filtering
  and address mode are set on the `GpuTexture` (`setTextureFilter`, `setAddressMode`). Cape
  texture and `FontTextureMixin` (wraps `GpuTexture.setTextureFilter` in `FontTexture.<init>`).
- Environment attributes (`net.minecraft.world.attribute`) do not exist. Sky module:
  `SkyRenderState.skyType == DimensionSpecialEffects.SkyType.OVERWORLD` (was `skybox`),
  `level.effects().skyType()` (was `dimensionType().skybox()`), `SkyRenderer.extractRenderState`
  takes a `Vec3` camera position, sun angle `level.getSunAngle(pt)` (radians, as before),
  sunrise colour `level.effects().getSunriseOrSunsetColor(level.getTimeOfDay(pt))` when
  `isSunriseOrSunset`, fog end = render distance, no panoramic-screenshot camera. Fog colour:
  `getBaseColor` is declared on `AirBasedFogEnvironment` (shared with the dimension/boss fog),
  so the mixin targets that and checks `instanceof AtmosphericFogEnvironment`.
- `Screen.init/resize` take a `Minecraft` first (HudEditorScreen versioned).
- `ServerStatusPinger.pingServer` has no `EventLoopGroupHolder` argument.

Verified on 1.21.10 (2026-10-09): build + all JUnit tests; default, features, screens,
cosmetics and drop2 passes match 1.21.11 (summaries equal; screenshots compared side by side),
no injection failures with `-PcountInjections`.

### 1.21.9
- Same Minecraft code as 1.21.10 for everything Shard touches, but Fabric API (latest
  0.134.1+1.21.9) has **no world render events** (`rendering.v1.world` arrived in the 1.21.10
  builds). `LevelRendererEventsMixin` (<1.21.10) hooks the spots Fabric uses on 1.21.10: after
  `OutlineBufferSource.endOutlineBatch()` in the main-pass lambda `method_62214` (AFTER_ENTITIES)
  and at the `CameraRenderState.pos` read in `renderBlockOutline` (BEFORE_BLOCK_OUTLINE).
  Modules take `compat.WorldDraw` (pose, buffers, camera) and ShardClient routes both paths
  through `afterEntities` / `beforeBlockOutline`. The lambda name is version-specific; check it
  again (javap) on older versions, where Fabric's older `rendering.v1.WorldRenderEvents` may be
  usable instead.
- Verified (2026-10-09): same passes and results as 1.21.10.

### 26.1 (also 26.1.1 and 26.1.2)
- Unobfuscated: the Loom cache has `minecraftMaven/net/minecraft/minecraft-merged-deobf/<mc>/...jar`
  (javap it directly), and `./gradlew :<mc>:genSources` gives readable sources with real parameter
  names (`.gradle/loom-cache/minecraftMaven/.../minecraft-merged-*-<mc>-sources.jar`). Lambdas are
  `lambda$method$N`. The 1.21.x global replacements are `>= since`, so they stay in force on 26.x.
- **Stonecutter replacements never chain**: the matches of all replacements are collected first
  and overlapping ones lose. `public void render(GuiGraphics g, ...` with one rule for `render(`
  and one for `GuiGraphics` only applies one; use zero-width lookaheads
  (`render(?=\(\w+ \w+, int ...)`, `resources([./])model\1(?=Material\b)`) so matches do not
  overlap. They do apply inside commented-out code, which is fine.
- **GUI**: `GuiGraphics` → `GuiGraphicsExtractor`; `drawString` → `text`, `renderItem` → `item`,
  `renderOutline` → `outline`, `submitEntityRenderState` → `entity`; `Screen.render` →
  `extractRenderState`, `renderBackground` → `extractBackground` (+ `extractMenuBackground`,
  `extractBlurredBackground`, `extractPanorama`), `renderWithTooltipAndSubtitles` →
  `extractRenderStateWithTooltipAndSubtitles` (called from `GameRenderer.extractGui`), `Gui.render*`
  overlays/crosshair/subtitles → `extract*`, `HudElement.render` → `extractRenderState`,
  `PlayerFaceRenderer.draw` → `PlayerFaceExtractor.extractRenderState`. All global except the last
  two (in place). Fabric still draws `addLast` HUD layers with the (deferred) subtitles, as on 1.21.11.
- **Package moves**: `GuiMessage(Tag)` → `client.multiplayer.chat`, `GuiEntityRenderState` →
  `renderer.state.gui.pip`, `Block/Camera/Level/SkyRenderState` → `renderer.state.level`,
  `BlockStateModel` → `renderer.block.dispatch`, `BlockAndTintGetter` → `client.renderer.block`,
  `AtlasManager` → `resources.model.sprite`; the sprite record `Material` is `SpriteId` in
  `resources.model.sprite` (a different `Material` exists there: a block-model material).
- **Fabric API**: `client.keybinding.v1.KeyBindingHelper.registerKeyBinding` →
  `client.keymapping.v1.KeyMappingHelper.registerKeyMapping`; `fabric.api.renderer.v1` →
  `fabric.api.client.renderer.v1` (`renderLayer` on quads is `chunkLayer`);
  `LivingEntityFeatureRendererRegistrationCallback` → `LivingEntityRenderLayerRegistrationCallback`;
  `rendering.v1.world.WorldRenderEvents` → `rendering.v1.level.LevelRenderEvents` with
  `LevelRenderContext` (`poseStack()`, `bufferSource()`, `levelState()`). AFTER_ENTITIES is gone:
  entities render as solid then translucent features, and `AFTER_TRANSLUCENT_FEATURES` fires where
  AFTER_ENTITIES did (after both, before the block outline). `BEFORE_BLOCK_OUTLINE` is unchanged.
- **Small API changes**: `ChatComponent.addMessage(Component)` → `addClientSystemMessage`, and
  every line goes through the private `addMessage(contents, signature, GuiMessageSource, tag)`
  (ChatComponentMixin); `Minecraft.resizeDisplay()` → `resizeGui()`; `Level.random` is protected
  (use `getRandom()`, fine everywhere); `OptionsScreen(parent, options, inWorld)`;
  `RenderTypes.entityCutoutNoCull` → `entityCutout` (the culling one is `entityCutoutCull`).
- **ItemStacks cannot be built before the item components are bound** (mod init runs earlier):
  no `static final ItemStack` fields (CooldownsModule builds its stacks on first use).
- **Mixins that moved**: `GameRenderer.getFov` is gone, the world FOV is
  `Camera.calculateFov(F)F` (CameraMixin, >=26.1); `GameRenderer.bobHurt` takes
  `(CameraRenderState, PoseStack)` (the handler now takes only the CallbackInfo on all versions);
  `LightTexture` is gone, gamma is the first `Double.floatValue()` in
  `LightmapRenderStateExtractor.extract` (LightTextureMixin versioned); `Level.getDayTime` is gone,
  the sky follows world clocks (`ClientClockManager.getTotalTicks`, ClientClockManagerMixin,
  >=26.1) for Weather and Time; `EndCrystalRenderer.submit` submits with an `Identifier` texture
  (`model.renderType(texture)` gives the old RenderType); `ShieldSpecialRenderer.submit` has no
  display context, the item model applies the (1, -1, -1) flip, and the base is one
  `submitModel(model, state, pose, light, overlay, color, SpriteId, SpriteGetter, outline, crumbling)`
  (tinted/translucent there), so `BannerRenderer.submitPatterns` no longer draws the base and only
  the pattern-layer tint is left in BannerRendererMixin.
- Finding them: the static check from the Tooling notes (every `@Mixin` target, `method =` and
  `target =` against the jar) caught the renamed/moved targets; handler-signature and injection
  point changes (bobHurt, EndCrystalRenderer, ShieldSpecialRenderer) only show at run time, so
  start the client with `-PcountInjections` early (26.x has no dev companions) and compare each
  target method body between `genSources` of 1.21.11 and 26.x.
- For 26.2 / 26.3: start from these replacements (all `>= 26.1`); re-run the static check and
  `genSources` diff of every mixin target, check Fabric's `LevelRenderEvents`/`HudElement` and the
  renderer API, and watch for further `render*` → `extract*` renames in `Gui`/`Screen`.

- Vanilla differences seen in the smoke screenshots (not Shard): new title panorama; first-person
  right-hand blocking shield sits lower (`shield_blocking.json` translation 5 → 3.25); gamerules
  are snake_case since 1.21.11 (`gamerule doFireTick` fails in the smoke log there too).
- Fix found while comparing (all versions >= 1.21.11): see-through flames on burning mobs used
  `Sheets.translucentItemSheet()`, which is the item atlas from 1.21.11, so they were invisible;
  now `translucentBlockItemSheet()`.
- 26.1.1 and 26.1.2 change only `DetectedVersion`, `SharedConstants`, `PlayerEntry`, `Checkbox`
  and report-screen classes (none touched by Shard); their Fabric API builds (renderer API 13,
  rendering 23.3) are signature-compatible for everything Shard uses. Same source, no versioned
  code for them.
- Smoke tip: copy `run/config/shard/` into `run-<mc>/config/` before **every** pass; a run that
  crashes half-way leaves its own changed config behind (other modules on, HUD moved).

Verified on 26.1 (2026-10-09): build + all JUnit tests; default (with `-PcountInjections`),
features, screens, cosmetics and drop2 passes give the same summaries as 1.21.10; screenshots
compared side by side. 26.1.1: default pass with `-PcountInjections`. 26.1.2: default pass with
`-PcountInjections` and drop2 (newer Fabric API). Same summaries, no injection failures.
After the 26.1 changes, 1.21.11 (default pass) and 1.21.10 (default pass, `-PcountInjections`)
were run again: same summaries, no injection failures, and the burning-mob flames now show.
For 1.21.8 and older: the new mixins (CameraMixin, ClientClockManagerMixin) are `>=26.1` only;
everything else 26.1 added sits in `>=26.1` blocks, so the 1.21.10 notes above still apply.

### Tooling for 26.x (static checks)
- `python tools/mixin-targets.py <mc>` (after `:<mc>:compileJava :<mc>:processResources`): for
  every injector in the generated mixins of that node, the `method` must exist on the target and
  its `@At` target (INVOKE/FIELD with the exact owner, NEW) must be in that method's bytecode.
  The owner matters: 26.2 moved calls from `SubmitNodeCollector` to `OrderedSubmitNodeCollector`.
- `python tools/mixin-sigdiff.py <older> <newer>`: every mixin target method whose descriptor
  changed; those handlers (Inject arguments, ModifyReturnValue types) need a versioned block.
  Accessor/invoker descriptors are not covered; `-PcountInjections` at run time catches the rest.
- Stonecutter applies a rule's reverse pattern on the versions where its predicate is false, so a
  reverse pattern must not match anything `src/` contains (an `OptionsScreen` rule for 26.3
  rewrote the 1.21.x call; that one is versioned in place now).

### 26.2
- **Gui split**: the HUD class `Gui` is `Hud`; the new `Gui` (`Minecraft.gui`) owns the screen,
  overlay, toasts and chat listener; the HUD is `Minecraft.gui.hud`. Global rules: `Gui` -> `Hud`
  (a quoted `"net.minecraft.client.gui.Gui"` is left alone, for `@Mixin(targets = ...)` on the
  new class), `mc.screen`/`setScreen` -> `mc.gui.screen()`/`gui.setScreen`, `getOverlay`,
  `getToastManager`, `gui.getChat` -> `gui.hud.getChat`, F1 (`options.hideGui`) ->
  `gui.hud.isHidden()`/`toggle()`, `getMainRenderTarget` -> `gameRenderer.mainRenderTarget()`,
  `levelRenderer.allChanged` -> `levelExtractor.allChanged`, `EntityType.X` -> `EntityTypes.X`,
  `TextureFormat.RGBA8` -> `GpuFormat.RGBA8_UNORM`. MinecraftScreenSwapMixin and the scaled
  screen wrap (now ScreenRenderScaleMixin, all versions) target the new Gui.
- **No immediate buffers in the level pass** (`MultiBufferSource` and `ShapeRenderer` are gone;
  Fabric's `LevelRenderContext` only has `submitNodeCollector()`). `compat.WorldDraw` carries the
  collector and has `outline(shape, x, y, z, colour, width, afterTerrain)` (`submitShapeOutline`
  with `RenderTypes.lines()`; the block outline passes the state's `isTranslucent`). Anchor glow
  moved from AFTER_TRANSLUCENT_FEATURES to COLLECT_SUBMITS (fired after vanilla's submits; the
  later events have nothing left to submit into).
- **Mixins**: flames are batched (`buildGroup` gets the sprites and the `entityCutoutCull` buffer,
  `prepare` scales one); the screen fire is custom geometry (`submitFire` sprite,
  `lambda$submitFire$0` Matrix4f.translate, `buildFireQuad` packed colour);
  `extractVisibleEntities` is on `LevelExtractor`; `renderArmWithItem` -> `submitArmWithItem`;
  the inventory model's `GuiEntityRenderState` takes `Vector3fc`/`Quaternionfc`; banner layers go
  to `OrderedSubmitNodeCollector`.
- **Account switch**: `PlayerSocialManager(mc, api, friendsService, RemoteFriendListUpdateHandler)`;
  the switch closes the old friends updater and builds a new one for the new token.
- Entities are numbered by the level (`Level.getNextEntityId`, 0 on the client): client-side
  stand-ins need `setId` (the crystal fakes already had one; the smoke players now too).
- The client exits through a post-main watchdog that crashes it after a few seconds if a
  non-daemon thread is left; the dev fake account bridge (an `HttpServer`) stops on CLIENT_STOPPING.
- Vanilla differences in the screenshots: new title panorama, a friends button on the title screen.

Verified on 26.2 (2026-10-09): build + all JUnit tests; default pass with `-PcountInjections`,
features, screens (`-PfakeBridge`), cosmetics and drop2 give the same summaries as 26.1, no
injection failures; screenshots compared side by side.

### 26.3
- **SDL3 instead of GLFW** (`org.lwjgl.glfw` is not on the classpath). `compat.GLFW` stands in
  for the LWJGL class (the import is rewritten): key, mouse button and modifier constants are
  Minecraft's SDL values from `InputConstants` (scancodes; left button 1, right 3; shift 3,
  control 192), so event comparisons work unchanged; cursor, window, monitor and attribute calls
  go to SDL (`SDL_WarpMouseInWindow`, `SDL_SetWindowBordered`, display bounds as the "video
  mode"). Saved keybinds stay GLFW codes (the launcher shares the config between instances):
  `compat.KeyCodes` converts on load/save and `Keys` names through it (identity before 26.3).
  `GLFW_KEY_UNKNOWN` is 0 there (SDL's unknown scancode; -1 is out of range for
  `InputConstants.isKeyDown`). `InputConstants.Type.KEYSYM` -> `KEYBOARD`, `isKeyDown(key)`.
- **Display**: F11 is `Minecraft.toggleFullscreen` (it flips the fullscreen option; Window has no
  toggle); leaving vanilla fullscreen sets the option and applies it right away. Vanilla's raw
  mouse input option is gone (SDL reads relative motion raw); the setting now sets
  `SDL_HINT_MOUSE_RELATIVE_SYSTEM_SCALE` (off = Windows acceleration), the closest equivalent.
  SDL gives the 1920x1080 dev window its full height (1061 before), so screenshots are 1080 tall.
- **GPU API** moved to `com.mojang.renderpearl.api.*` (global `moved` rules); authlib 10 has no
  `YggdrasilAuthenticationService` (`MinecraftServicesDiscoveryService` creates the user API and
  friends services) and `ProfileResult` is in `authlib.services`.
- **First-person hands**: `ItemInHandRenderer` is gone; FirstPersonHandsMixin (`>=26.3`, replacing
  ItemInHandRendererMixin there) hooks `FirstPersonHandsAndItemsRenderer.renderPlayerArm` and
  `submitArmWithItem` and wraps the held item's `ItemStackRenderState.submit`. It draws from
  render states, so Low Shield and Small Items get the camera's player.
- **Submit API**: `submitModel` has no crumbling argument and takes a `UvMapping`; glints are part
  of the render type (`entitySolidGlint`, `armorCutoutNoCullGlint`; `entityGlint` and
  `armorEntityGlint` are gone). EndCrystal, Shield (cosmetic shield: `entitySolidGlint` /
  `itemTranslucentGlint` when enchanted), Banner and Equipment mixins have 26.3 blocks; the armour
  hit tint skips `trimmedArmorGlint`.
- **Other**: sky and fog colours are `Vector3fc`/`Vector4fc` (converted with `ARGB`); world clocks
  answer through `ClientClockInstance.totalTicks()`; the totem animation is
  `LocalPlayer.displayItemActivation`; `EntityRenderDispatcher.shouldRender` and
  `MouseHandler.onMove` (relative motion) gained arguments; `swing(hand, SwingAnimation, boolean)`;
  `ClientboundRemoveEntitiesPacket` is a record; `OptionsScreen(parent, options)` again;
  `Window.isIconified`.
- Smoke tips: a crashed run can leave a broken key in `run-<mc>/options.txt` (`key.keyboard.-1`);
  passes after cosmetics/drop2 start with FOV 30 in options.txt and the drop2 inventory on the
  server, so reset the FOV before comparing a re-run.

Verified on 26.3 (2026-10-09): build + all JUnit tests; default pass with `-PcountInjections`,
features, screens (`-PfakeBridge`), cosmetics and drop2 give the same summaries as 26.2 (only the
taller window's layout numbers differ); screenshots compared side by side. After the 26.2/26.3
changes, 1.21.11 (default pass), 1.21.10 and 26.1 (default pass, `-PcountInjections`) were run
again: same summaries, no injection failures, same screenshots.

### 1.21.8 (also 1.21.7 and 1.21.6)
- 1.21.6, 1.21.7 and 1.21.8 have the same classes and the same descriptors for every Shard mixin
  target (`tools/mixin-sigdiff.py 1.21.6 1.21.8` is empty; 1.21.7 only adds
  `TrackingItemStackRenderState`, 1.21.8 `GraphicsWorkarounds`). Same source, no code between them.
  GUI is already the render-state GuiGraphics (Matrix3x2fStack, pipelines), so screens and HUD
  needed almost nothing.
- **No submit API**: entities, layers, items, banners, shields and crystals draw straight into a
  `MultiBufferSource` (`render(state, pose, buffers, light)` instead of `submit`). Every mixin
  that wraps a `submitModel`/`submitModelPart` has a `<1.21.9` block wrapping `Model.renderToBuffer`
  / `ModelPart.render` (the 4-int overload has no colour: call the 5-int one to tint), and
  `Material.buffer` / `ShieldModel.renderType` for the translucent shield. BandanaLayer overrides
  `render(PoseStack, MultiBufferSource, ...)`; EndCrystalRendererMixin draws the two tinted models
  into the same (or a translucent) buffer; armour Hit Color reads the wearer's hurt state from
  `ItemTints.wearerHurt()` (set around `LivingEntityRenderer.render`), since `renderLayers` gets
  no render state.
- **Glow outlines belong to the entity**, not the render state (`EntityRenderState.outlineColor`
  is 1.21.9+): LevelRendererEntitiesMixin wraps `Minecraft.shouldEntityAppearGlowing` and
  `Entity.getTeamColor` in `collectVisibleEntities`/`renderEntities` for crystals with a Shard
  outline (`CrystalTweaksModule.outline`). `extractVisibleEntities` is `collectVisibleEntities`.
- **Players**: `PlayerRenderer`/`PlayerRenderState`/`AbstractClientPlayer` (no Avatar);
  `PlayerSkin` is `client.resources.PlayerSkin(texture, textureUrl, capeTexture, elytraTexture,
  model, secure)` with ids (global `moved` rule; `compat.ClientAsset` stands in for
  `ClientAsset.Texture` so CapeLibrary is unchanged); `ItemOwner` is `LivingEntity`. No
  `PlayerSkinRenderCache`: the account switcher resolves the profile with
  `ResolvableProfile.pollResolve()` and asks the `SkinManager`.
- **Input events** (`MouseButtonEvent`, `KeyEvent`, `CharacterEvent`) are 1.21.9: compat records
  of the same names (global `moved` rules) and `compat.InputScreen`, which DesignScreen extends
  before 1.21.9, turn vanilla's loose `mouseClicked(x, y, button)`/`keyPressed(key, scan, mods)`
  into the event methods (double click = same button within 250 ms, as 1.21.9's MouseHandler).
  `minecraft.hasShiftDown()` is static `Screen.hasShiftDown()`; `Window.handle()` is
  `getWindow()`; `InputConstants.isKeyDown` takes the handle (all global rules).
- **World events**: Fabric API 0.128-0.136 has the old `rendering.v1.WorldRenderEvents`:
  AFTER_ENTITIES and BLOCK_OUTLINE (`BlockOutlineContext`: pos, state, entity). No
  `BlockOutlineRenderState`: `compat.BlockOutlineRenderState` (pos, shape for the camera entity,
  translucent pass) is built from it. LevelRendererEventsMixin is now `>=1.21.9 <1.21.10`.
  `HudElementRegistry` exists (the HUD is unchanged). `KeyMapping.Category` is a translation key.
- **Smaller ones**: no `AtlasManager` (`compat.Atlases.sprite(material)` = `material.sprite()`);
  no `SkyRenderState` (SkyRendererMixin wraps `ClientLevel.getSkyColor` and
  `getSunriseOrSunsetColor` in LevelRenderer's sky-pass lambda with `method = "*"`); flames are
  `EntityRenderDispatcher.renderFlame` (`PoseStack.scale`, `Material.sprite()`); the screen fire
  looks its sprite up itself; no `debugEntries` (F3+B is `EntityRenderDispatcher.setRenderHitBoxes`,
  3D crosshair `Gui.shouldRenderDebugCrosshair`); `ShapeRenderer.renderLineBox` takes the PoseStack;
  `RenderType.pipeline()` does not exist; `FontDescription` does not exist (`Style.withFont(id)`);
  `FontManager.createFontSet` does not exist (`new FontSet(textureManager, id)` + `reload`);
  `Screen.renderWithTooltip` (was renamed AndSubtitles in 1.21.9) and `renderBackground` does not
  draw the HUD/subtitles (the unscale wrap is 1.21.9+); `ManageServerScreen` is
  `EditServerScreen(parent, callback, data)` without a title; `User` takes a `User.Type` (MSA);
  no offline developer mode and no `Minecraft.services()` (`getMinecraftSessionService()`);
  `PacketUtils.ensureRunningOnSameThread` takes a `BlockableEventLoop`.
- Static checks: `tools/mixin-targets.py` now finds the 1.21.x mapped jar, fully qualified
  annotations (`@com.llamalad7...WrapOperation`) and `target = CONSTANT` strings. It still does not
  check @Shadow members or @Accessor names: the FontManager shadow and `offlineDeveloperMode`
  accessor only failed at start-up, so run `-PcountInjections` early.
- Vanilla differences in the screenshots: different title panorama; with no world clouds at FOV 70
  the horizon sits slightly higher than on 1.21.10. FOV 26 (drop2) is out of range here and on
  1.21.10 (the smoke pass logs "Illegal option value"; harmless).

Verified (2026-10-09): build + all JUnit tests on every node 1.21.6 to 26.3. 1.21.8: default
(`-PcountInjections`), features, screens (`-PfakeBridge`), cosmetics and drop2 give the same
summaries as 1.21.10, screenshots compared side by side. 1.21.7: default pass; 1.21.6: default
pass and drop2 (oldest Fabric API), all with `-PcountInjections`, same summaries as 1.21.8. After
the changes, 1.21.11 (default), 1.21.9 and 26.3 (default, `-PcountInjections`) were run again:
same summaries and screenshots.
For 1.21.5 and older: re-run `tools/mixin-sigdiff.py 1.21.5 1.21.6` and the static check first;
1.21.5 still has the pre-1.21.6 GUI (PoseStack GuiGraphics, no render pipelines in GUI, Fabric
`HudLayerRegistrationCallback`/`HudRenderCallback` instead of `HudElementRegistry`), so expect the
HUD, screens and the GUI-scale mixins to need the most work there.

### 1.21.5
- **GUI layer** (the big step; everything Shard draws goes through these, so call sites are unversioned):
  - `GuiGraphics.pose()` is a 3D `PoseStack`. `compat.Matrix3x2fStack` wraps it with the 2D calls
    Shard uses (`pushMatrix`, `popMatrix`, `translate(x, y)`, `scale(x, y)`, `m00..m21` read from the
    4x4 pose); global rules point the `org.joml.Matrix3x2fStack` import there and turn `g.pose()` into
    `Matrix3x2fStack.of(g)`. GUI code must keep naming its graphics `g` for the rule to apply.
  - No strata: drawing is immediate (in call order through the shared buffer source) and depth-tested.
    `g.nextStratum()` becomes `compat.GuiDraw.nextStratum(g)` = `flush()` + clear the main depth
    buffer (what vanilla does between the HUD and the screen). Shadowed vanilla text sits 0.03 towards
    the camera, so a later fill at the same depth does not cover it (the HUD editor's element label);
    the HUD editor starts its chrome with a stratum there (`<1.21.6` only).
  - Blits take a `Function<ResourceLocation, RenderType>`: `RenderPipelines.GUI_TEXTURED` →
    `RenderType::guiTextured` (global rule; same argument order for `blit`/`blitSprite`).
  - `Screen.renderBlurredBackground()` post-processes the main target at once: `renderBlurredBackground(g)`
    becomes `GuiDraw.blur(g)` (flush, then `GameRenderer.processBlurEffect`).
  - `Screen.render` draws the background itself and `renderWithTooltip` only calls `render`; Shard's
    screens never call `super.render`, so ScreenScaleMixin (`<1.21.6`) calls `renderBackground` for
    `DesignScreen`s at the head of `renderWithTooltip` (without it: no blur, no tint, no HUD editor dim).
  - Scissor: `enableScissor` already transforms by the pose (`ScreenRectangle.transformAxisAligned`),
    as on 1.21.11, so design-unit clipping works unchanged.
  - The inventory player model is drawn in place under the pose (no picture-in-picture):
    GuiGraphicsEntityMixin is `>=1.21.6`.
  - `Window.getGuiScale()` is a `double` (rule casts `mc/minecraft/window...getGuiScale()` to int);
    `WorldVersion` getters are `getName()`/`getProtocolVersion()` (rules).
- **Fabric API 0.128.2**: no `HudElementRegistry`. HudManager adds its layer with
  `HudLayerRegistrationCallback` + `IdentifiedLayer.of` (`addLayer` = last root layer, after the
  subtitles). GUI Scales wraps the coarser vanilla layers with `replaceLayer`: `HOTBAR_AND_BARS`
  (hotbar, all bars, mount health and held item name together) and `EXPERIENCE_LEVEL` for the hotbar
  scale; scoreboard, player list, title, overlay message and boss bar as before. Fabric's renderer API
  6 sets the chunk layer through the material: Low Fire's translucent ground fire copies the quad's
  material with `BlendMode.TRANSLUCENT`.
- **Other API**: textures have no usage flags or views (`createTexture(label, format, w, h, mips)`,
  9-argument `writeToTexture`); `ResolvableProfile.resolve()` future instead of `pollResolve`
  (AccountSwitcher); `Gui.getMobEffectSprite` → `mc.getMobEffectTextures().get(effect)`;
  `Gui.shouldRenderDebugCrosshair` does not exist (same condition inlined);
  `ClientLevel.disconnect()` + `Minecraft.disconnect()`.
- **Mixins**: no fog environments; AtmosphericFogEnvironmentMixin targets `FogRenderer.computeFogColor`
  and rewrites the air colour (locals 7-9) where that branch resets `biomeChangedTime`
  (PUTSTATIC ordinal 4); the shared colour maths is now a static method. Line width is set in the line
  state's setup lambda (`RenderStateShard$LineStateShard.method_23554`, new LineStateShardMixin;
  CompositeRenderTypeMixin is `>=1.21.6 <1.21.11`); WideLines also forwards `getRenderPipeline` and
  `getRenderTarget`. The totem animation is `GameRenderer.renderItemActivationAnimation` (scale
  `(o, -o, o)`; GameRendererMixin `<1.21.6`). Everything else (world events, entities, crystals,
  shields, banners, flames, fonts, skins) is the same as 1.21.6.
- Vanilla differences in the screenshots: clouds (no cloud range option; from below they are flat and
  take the fog/sky colour, so the Sky module's sunset tints them), title panorama.
- Smoke tips: the smoke server keeps the player's inventory and placed blocks between passes; clear
  them (`clear ShardSmoke` once the player is online, air layers above the floor) or the HUD
  editor/crystal shots are taken looking at a leftover obsidian block. `fill` is limited to 32768
  blocks, so clear one layer per command.

Verified on 1.21.5 (2026-10-09): build + all JUnit tests; default (`-PcountInjections`), features,
screens (`-PfakeBridge`), cosmetics and drop2 give the same summaries as 1.21.8 (layouts identical
across scales, 0 inventory slot misses, same clipped texts, crystal and HUD undo checks), no
injection failures; screenshots compared side by side. After the shared changes, 1.21.11 (default),
1.21.8 (default with `-PcountInjections`, features) and 26.3 (default, `-PcountInjections`) were run
again: same summaries and screenshots. Builds and tests pass on every node 1.21.5 to 26.3.
For 1.21.4 → 1.21: the whole 1.21.5 GUI layer (`compat.Matrix3x2fStack`, `compat.GuiDraw`, the
`<1.21.6` rules and blocks, the HUD layer registration and the background mixin) should carry over
as is; check first that `GuiGraphics.blit`/`blitSprite` still take a RenderType function with the
same argument order (1.21.2+ added it; 1.21.1 and older blit by texture id with no function),
`enableScissor` still transforms by the pose, the blur entry point (`processBlurEffect` and its
arguments), `RenderType.getRenderPipeline` (1.21.5 only: RenderPipeline is new in 1.21.5, so 1.21.4
world rendering is shader-instance based and Lines/LineStateShardMixin, CapeTexture, FontTextureMixin
and every world mixin need a fresh look), and Fabric's HUD API (`HudLayerRegistrationCallback` exists
from 1.21.2; 1.21/1.21.1 only have `HudRenderCallback`).

### 1.21.4, 1.21.3 and 1.21.2
- The 1.21.5 GUI layer carries over unchanged (PoseStack GuiGraphics, `compat.Matrix3x2fStack`,
  `GuiDraw`, `RenderType::guiTextured` blits with the same argument order, `processBlurEffect()`).
  `GuiDraw.nextStratum` clears depth with `RenderSystem.clear(GL_DEPTH_BUFFER_BIT)`; `GuiDraw.blur`
  rebinds the main target afterwards (`bindWrite(false)`, as vanilla's `renderBlurredBackground`).
- **No RenderPipeline / GpuDevice** (1.21.5): the `RenderPipelines` import becomes a duplicate
  `RenderType` import (rule); `WideLines` uses the old `RenderType(name, format, mode, size, ...)`
  constructor and LineStateShardMixin (`method_23554`, same intermediary name) still swaps the width.
  Textures are GL names: CapeTexture `TextureUtil.prepareImage(id, maxLevel, w, h)` + one
  `NativeImage.upload` per level, and overrides `AbstractTexture.setFilter`, because every entity
  render type's TextureStateShard sets NEAREST on each draw. `DynamicTexture(image)` has no label;
  `Screenshot.takeScreenshot(target)` returns the image (`compat.Screenshots`, rule).
- **Fonts**: `FontTexture` has no label and no `GpuTexture`; FontTextureMixin is `>=1.21.5` and
  FontSetMixin (`<1.21.5`) wraps `FontTexture.add` in `FontSet.stitch` and sets GL_LINEAR on the atlas
  straight through GlStateManager (the text render types keep resetting the AbstractTexture's cached
  filter to NEAREST; 1.21.2/1.21.3 glyph uploads also set NEAREST on every upload).
- **1.21.4**: hitboxes are drawn by `EntityRenderDispatcher.renderHitbox` inside the private
  `render(Entity, DDDF, PoseStack, MultiBufferSource, I, EntityRenderer)`; a bare `method = "render"`
  picks the public overload (Mixin selects the first match), so the descriptor is spelled out.
  `Inventory.setSelectedSlot/getSelectedItem` are `setSelectedHotbarSlot/getSelected` (rules);
  RealmsClient has no cached instance (RealmsClientAccessor `>=1.21.5`, `RealmsClient.create(mc)`);
  Low Fire wraps vanilla's `DelegateBakedModel` (Fabric `emitBlockQuads(QuadEmitter, ...)`, model
  loading 4.x names block models by `ModelResourceLocation`, not BlockState); `ItemInHandRenderer.renderItem`
  takes a `boolean leftHand` (before 1.21.5). Mob equipment in smoke commands is `ArmorItems`.
- **1.21.3 and 1.21.2** (Fabric API 0.114.1 / 0.106.1; the two Minecraft jars differ only in
  `SharedConstants`/`ReportEnvironment` for Shard):
  - No `HudLayerRegistrationCallback`/`IdentifiedLayer` (only `HudRenderCallback`, drawn after Gui's
    layer stack at z 0). GuiLayersMixin adds Shard's HUD as the last root layer of `Gui.layers`
    (what Fabric's `addLayer` does on 1.21.4) and wraps `renderHotbarAndDecorations`,
    `renderExperienceLevel`, `renderScoreboardSidebar`, `renderTabList`, `renderTitle`,
    `renderOverlayMessage` (`@WrapMethod`); BossHealthOverlayMixin wraps `render`. GuiScalesModule's
    scaling is `drawPart(Part, g, draw)`.
  - `GuiGraphics.enableScissor` does not transform by the pose (1.21.4 added
    `ScreenRectangle.transformAxisAligned`): `g.enableScissor` -> `GuiDraw.enableScissor(g, ...)` (rule).
    Without it the menu's scrolled lists were cut off and the compass was empty.
  - No item model system: shields are drawn by `BlockEntityWithoutLevelRenderer.renderByItem`
    (ShieldItemRendererMixin: cosmetic, tint, translucency; ShieldSpecialRendererMixin and
    ItemModelResolverMixin are `>=1.21.4`). The holder is not known at draw time: first person pushes
    it in ItemInHandRendererMixin, third person keeps it on `LivingEntityRenderState`
    (LivingEntityRenderStateMixin, set in LivingEntityRendererMixin) and ItemInHandLayerMixin pushes it.
  - `LivingEntityRenderState.headItem` is the head slot's stack (no `headEquipment`/`wornHeadType`);
    `EquipmentLayerRenderer.renderLayers(EquipmentModel.LayerType, ResourceLocation, ...)` (constant);
    `ARGB.redFloat` etc. arrived in 1.21.4 (`compat.Argb`, rule); no `Window.isMinimized`;
    Fabric's BLOCK_OUTLINE context has no `translucentBlockOutline()` (computed from the block's
    chunk layer); model loading 3.x: `modifyModelAfterBake` with `topLevelId()`, `ForwardingBakedModel`,
    `emitBlockQuads(..., RenderContext)` and `RendererAccess.INSTANCE.getRenderer()`.
  - Vanilla 1.21.2 logs "Missing sound for event: minecraft:block.spawner.fall" at start-up.
- Vanilla differences in the screenshots: title panorama (pale garden on 1.21.4, trial chambers on
  1.21.2/1.21.3), the window gets its full 1080 px height (as on 26.3), so the HUD editor's mouse
  hover label shows on the FPS element as it does on 26.3.

Verified (2026-10-09): build + all JUnit tests on every node 1.21.2 to 26.3 (1.21 and 1.21.1 are
not ported yet and do not compile). 1.21.4 and 1.21.2: default (`-PcountInjections`), features,
screens (`-PfakeBridge`), cosmetics and drop2 give the same summaries as 1.21.5 (only the taller
window's layout heights differ), no injection failures; screenshots compared side by side.
1.21.3: default and drop2 with `-PcountInjections`, same summaries as 1.21.2. After the shared
changes, 1.21.11 (default), 1.21.5 and 26.3 (default, `-PcountInjections`) were run again: same
summaries and screenshots.
For 1.21.1 and 1.21: there are no render states (EntityRenderState arrived in 1.21.2): every entity,
layer, player and crystal mixin needs a pre-render-state block (render(entity, yaw, partialTick,
pose, buffers, light)), `GuiGraphics.blit`/`blitSprite` take a texture id and no RenderType function
(the `RenderType::guiTextured` rule has to become another helper), Fabric API has only
`HudRenderCallback` (GuiLayersMixin's approach should work, check `Gui.layers` exists on 1.21),
the blur is `processBlurEffect(float)`, and `NativeImage.setPixel` is ABGR (`setPixelRGBA`).
Start with `-PcountInjections`, `tools/mixin-targets.py` and `tools/mixin-sigdiff.py 1.21.1 1.21.2`;
bare `method = "name"` selectors that become ambiguous only show at run time.

### 1.21.1 and 1.21
- 1.21.1 is a hotfix: for everything Shard touches the two jars are the same (the differences are
  `DetectedVersion`, `SharedConstants`, command arguments, tags and a few server-side classes), and
  Fabric API 0.102.0 (1.21) and 0.116.17 (1.21.1) are signature-compatible for Shard. Same source,
  no code between them.
- **No render states** (they arrived in 1.21.2); renderers and layers get the entity:
  - Bandana: BandanaLayer is a `RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>`,
    looks the texture up itself, hides under any head-slot item, and runs inside the body transform
    (no `model.root()`). AvatarRendererMixin / AvatarRenderStateMixin are `>=1.21.2`.
  - Crystals: no `EndCrystalModel`; the renderer draws its own base, two glass frames and cube.
    EndCrystalRendererMixin's `<1.21.2` body scales at the head of `render(EndCrystal, ...)`, spins
    through `@ModifyConstant(3.0F)`, stops the bounce at the static `getY(EndCrystal, F)`, hides the
    base at `showsBottom()` (`CrystalTweaksModule.hidesBase`) and recolours by tinting each
    `ModelPart.render` (the `cube` field gets the core tint, the rest the frame tint) into the same or
    a translucent buffer. EndCrystalModelMixin is `>=1.21.2`.
  - Glow outlines and the Entity Optimizer frame: `renderLevel` walks and draws the entities itself
    (LevelRendererEntitiesMixin wraps `shouldEntityAppearGlowing` / `getTeamColor` there and starts
    the frame at `entitiesForRendering()`).
  - No Death Animation / Hit Color: LivingEntityRendererMixin reads `deathTime` out of
    `setupRotations` (@ModifyExpressionValue) and packs the overlay without red at `getOverlayCoords`
    in `render`; the armour tint's wearer flag is set around `render`.
  - Armour: `HumanoidArmorLayer.renderModel` (coloured) and `renderTrim` (plain; the glint pass is left
    alone); elytra have their own layer (ElytraLayerMixin, `<1.21.2`).
  - Third-person shield holder: `ItemInHandLayer.renderArmWithItem` takes the LivingEntity
    (ItemInHandLayerMixin `<1.21.2` block); LivingEntityRenderStateMixin is `>=1.21.2 <1.21.4`.
  - Hitboxes: `EntityRenderDispatcher.render(Entity, DDDFF, PoseStack, MultiBufferSource, I)` (with
    the yaw, no renderer argument); `renderHitbox` is unchanged.
- **GUI**: `blit`/`blitSprite` take a texture id (or sprite id) and no RenderType function or colour.
  Rules turn `g.blit(GUI_TEXTURED, ...)` / `g.blitSprite(GUI_TEXTURED, ...)` into
  `compat.GuiDraw.blit/blitSprite(g, GuiDraw.GUI_TEXTURED, ...)` with 1.21.2's argument order; they
  draw a POSITION_TEX_COLOR quad with blending, as vanilla's coloured `innerBlit`. The blur is
  `processBlurEffect(partialTick)`, `RenderSystem.clear` takes `ON_OSX`. Everything else of the
  1.21.5 GUI layer carries over (Gui.layers exists, so GuiLayersMixin works with Fabric's
  `HudRenderCallback`-only API).
- **Mouse**: MouseHandler sends clicks, releases and moves from **static** lambdas. A non-static
  handler with `method = "*"` silently skips static targets ("Scanned 0 target(s)" under
  `-PcountInjections`), so MouseHandlerScaleMixin has static handlers there.
- **Sky and fog**: `LevelRenderer.renderSky` reads `ClientLevel.getSkyColor` (a `Vec3`) and
  `DimensionSpecialEffects.getSunriseColor(FF)` (RGBA `float[]`, null = none); the Overworld sky type
  is `NORMAL`. The fog colour is computed in `FogRenderer.setupColor` into the static
  `fogRed/Green/Blue` (shadowed), finished at the same PUTSTATIC (ordinal 4) as 1.21.2-1.21.5.
- **Explosions**: `handleExplosion` finalizes an `Explosion`, which plays the sound and adds the
  particle through `Level`; ClientPacketListenerMixin marks that call (`compat.ServerExplosions`) and
  ExplosionMixin (`<1.21.2`) applies the Anchor and Explosion Optimizer filters. The packet has
  loose `getX/Y/Z`.
- **Smaller API differences**: no `FramerateLimitTracker` (the mixin targets
  `Minecraft.getFramerateLimit`; no inactivity limit option); `GameRenderer.getFov` returns a
  double; `AbstractArrow.inGround` is a field (accessor); totem event 35 is `TALISMAN_ACTIVATE`;
  `ClientboundSetTimePacket.getGameTime()`; item cooldowns per `Item`; `Input.up`;
  `getViewYRot(pt)`; no `ShapeRenderer` (its line helpers are on `LevelRenderer`, rule; the shape
  outline is private there, so `Lines.shape` draws the edges itself); `FastColor.ARGB32` (rule);
  `getToasts()`, `getTimer()` (rules); NativeImage only has ABGR `setPixelRGBA/getPixelRGBA`
  (`compat.NativeImages`, rule) and `makePixelArray()` instead of `getPixels()`;
  `Inventory.selected` is a public field with no setter (rule); `BannerRenderer.renderPatterns` and
  `Material.buffer` have no "sheeted" flag; the BEWLR shield uses `getFoilBufferDirect`; no
  `Window.isIconified/isMinimized`; `ParticleStatus` is in `net.minecraft.client`.
- Tooling: `tools/mixin-targets.py` now checks `method = "*"` injectors (any method of the target,
  skipping static ones for non-static handlers) and reads `src/` for the active version. It would
  have caught SkyRendererMixin and MouseHandlerScaleMixin, which only failed at start-up; it still
  does not check @Shadow / @Accessor (`AbstractArrow.isInGround` was found with `javap`).
- Smoke tip: the run folder of 1.21.11 is `run/`, which is also where the reference config comes
  from; a script that refreshes `run-<mc>/config/shard` must skip `run/` itself. `.smoke-server/`
  (1.21.11) has RCON off; enable it for the pass and restore the file afterwards.
- Vanilla differences in the screenshots: same trial-chambers panorama as 1.21.2; whether the killed
  zombie is still on screen in `smoke-step7-recap` (and whether its drop was picked up) depends on
  tick timing and varies between runs of the same version.

Verified (2026-10-09): build + all JUnit tests on every node 1.21 to 26.3 (`./gradlew build
--continue`). 1.21.1: default (`-PcountInjections`), features, screens (`-PfakeBridge`), cosmetics
and drop2 give the same summaries as 1.21.2, no injection failures; screenshots compared side by
side with 1.21.2 and 1.21.3. 1.21: default, features and drop2 with `-PcountInjections`, same
summaries as 1.21.1. After the shared changes the generated sources of every node from 1.21.2 on
differ only in unused new classes, comments and two inlined method-descriptor constants, and
1.21.11 (default), 1.21.2 and 26.3 (default, `-PcountInjections`) were run again: same summaries
and screenshots.
