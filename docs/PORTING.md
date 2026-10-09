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
