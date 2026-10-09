# Decisions

## Foundations (0.1.0)

- **Legit only.** The first draft of the product brief listed hacked-client modules; the owner rejected them. Shard is a legit PvP client: HUD, visuals, vanilla-legal input helpers, performance. The fair-play rule in README.md is the acceptance test for every new module.
- **Mojang official mappings**, not Yarn: `ResourceLocation` is `Identifier` in 1.21.11 and the GUI stack uses `Matrix3x2fStack`; signatures were read from the mapped jar with `javap` before writing code, which is why the first compile passed.
- **Pure-Java core.** `Module`, settings, `ConfigManager`, `LauncherInfo`, `Keys`, `Colors`, `ClickTracker`, `ServerBlacklist`, `CrystalPredictor` and `AnchorPredictor` have no Minecraft imports so they run under plain JUnit. Minecraft-facing work lives in `HudModule`, screens, modules and mixins.
- **Settings drive everything.** Adding a `Setting` field to a module yields the settings-panel row, config persistence and the `.set` chat command with no extra code. `Setting.group("…")` adds a heading above a run of settings.
- **Mixins check `ShardClient.isReady()`** because render mixins can fire before `onInitializeClient` finishes. Hooks whose vanilla method names are not compile-checked use `require = 0` so a mapping change degrades a feature instead of crashing the game.
- **Particle thinning happens in `ParticleEngine.createParticle`** rather than per-particle-type mixins: one hook, every type, and returning `null` is a path vanilla already handles.
- **No Fabric `HudRenderCallback`.** 1.21.11 Fabric API routes HUD drawing through `HudElementRegistry`; Shard registers one layer and draws all HUD modules (plus the crosshair and totem flash) inside it so z-order and scaling stay under its control.
- **HUD positions are stored as fractions** of the GUI size so layouts survive window resizes and GUI-scale changes.

## 0.2.0: settings page

- **The click GUI was replaced, not restyled.** Draggable per-category panels with coloured bars read as a hacked client. The new `ClickGuiScreen` (same class name so the keybind, Mod Menu entry and `.gui` keep working) is one full-screen page: sidebar with categories and search, a grid of mod cards with toggle switches, a settings panel that slides in from the right. One accent colour (the launcher's), 1px low-contrast lines, radius about 14 screen pixels (`Theme.radius()` divides by the GUI scale so it looks the same at scale 2 and 3), and a real blur behind the page via `renderBlurredBackground` from an overridden `renderBackground` (vanilla blurs there before `render`, and blurring twice in one frame throws).
- **Hits are the single source of truth for input.** Every control registers its rectangle (and current scissor clip) while it renders; clicks, hover and keyboard focus all consult that list, so a control can never be clickable where it is not drawn. Popovers (dropdown list, colour picker) are drawn last in a new stratum and get input first.
- **Narrow layout below 560 GUI px.** The sidebar becomes a tab strip (short labels when the full ones would collide with the HUD button) and the panel replaces the grid with a back button. This covers GUI scale 3 at 720p and Auto at 1080p and 1440p.
- **Rounded rectangles are per-row fills** (replaced by textured corners in 0.3.0, kept as a fallback). `Render2D.roundedRect` computes a circular inset per row, so corners are real curves at any scale without textures or shaders. Outlines follow the same shape.
- **Icons are vanilla items or 16x16 glyph bitmaps.** `Module.icon()` returns `item:<id>` or `glyph:<name>`; glyphs are drawn as horizontal-run fills in the theme colour so they stay crisp and recolour with the accent.
- **Modules can be on yet inactive.** `Module.isEnabled()` is the effective state (switch on, no conflicting mod, not blacklisted here); `isToggledOn()` is the user's switch. Mixins and the HUD only ever read `isEnabled()`.
- **Mod conflicts come from `FabricLoader.isModLoaded`** against each module's `conflictingMods()` list of mod ids (read from the actual jars in the owner's Modrinth profile, see `docs/modrinth-research-2026-10-07.md`). A blocked module keeps its switch state but stays off, and the card/panel say which mod is installed.
- **Server rules are a per-host blacklist of module keys** (`ServerBlacklist`, config key `servers`), applied on `ClientPlayConnectionEvents.JOIN` from `handler.getServerData().ip`. The settings panel exposes it as a "Disable on <server>" switch, which is how players actually think about it; wildcard patterns (`*.hypixel.net`) and ports are supported in the data model and tests but only editable in the file for now.
- **"Background" became "Show background"** on every HUD module; `Module.load` maps the old key so existing configs keep their value. The three merged modules (Particle Multiplier, Pop Messages, No Totem Animation) do not migrate their keys; their defaults are the sensible crystal-PvP ones and the config version moved to 2.

## 0.2.0: what the research changed

- **The totem event never reached `LivingEntity` in 1.21.11.** `ClientPacketListener.handleEntityEvent` handles id 35 itself (particles, sound, item activation) and returns without calling `entity.handleEntityEvent`, so 0.1.0's totem hooks never fired. All totem behaviour now hangs off the packet listener (hook placed after `ensureRunningOnSameThread` so it never runs on the network thread), and the sound volume, pitch and the full-screen animation are wrapped there with MixinExtras `@WrapOperation`.
- **Modrinth mods mirrored, and how** (each is server-authoritative and sends nothing extra; credits in the module Javadoc):
  - Marlow's Crystal Optimizer (`marlowcrystal`) → Crystal Optimizer "Remove when hit": `ClientLevel.removeEntity` after vanilla has sent the attack packet.
  - Client Side Crystals (`clientsidecrystals`) → Crystal Optimizer "Show placed instantly": after vanilla's own client-side `EndCrystalItem.useOn` succeeds, a client-only `EndCrystal` with a negative entity id is added; it is swapped out when the server's crystal arrives at the same block (`handleAddEntity`), dropped by a nearby explosion, or expired after 1.5 s. Attacking a stand-in is cancelled before the attack packet is built, because vanilla would have clicked air there.
  - cutebow's Anchor Optimizer (`client_side_anchors`), Hero's (`herosanchoroptimizer`), Walksy's (`anchoroptimizer`) → Anchor Optimizer. The three mods disagree on the placeholder (barrier-like with collision, replaceable ghost anchor, plain air). Shard uses air plus the explosion sound/particles, because the next step in a fight is placing a new anchor into that spot and both the ghost and the air variants are allowed on the major servers; the server's block update always wins if it disagrees. Charging needs no prediction: `RespawnAnchorBlock.charge` already runs on the client.
  - Walksy's No Death Animation (`nodeathanimation`) → No Death Animation (tilt and red tint cleared in the extracted render state; optional instant removal on the client's first death tick).
  - Visual Tweaks' hurt tint → Hit Color (repaints the top half of the 16x16 overlay texture, which vanilla fills with `0xB2FF0000`, and restores it when off).
  - BetterHurtCam, WI Zoom, SprintByDefault, Custom Crosshair Mod, TotemCounter, Sodium Fullbright → conflict ids on No Hurt Cam, Zoom, Toggle Sprint, Crosshair, Totem Counter and Fullbright respectively.
- **Mods deliberately not mirrored, and why:**
  - Consumable Optimizer: finishes eating on the client and cancels server "recall" packets, and sends a plugin-message handshake to every server. Dropping or answering packets is outside the fair-play rule.
  - Force Lowercase Commands: rewrites outgoing command text. Harmless, but it changes what the client sends.
  - Ravenclaw's Ping Equalizer (network-layer packet delay), Command Keys (chat macros), In-Game Account Switcher (accounts are the launcher's job), Controlling (keybind search UI, not crystal-related), Ambience V2/V3 (environment colours, roadmap).
  - BadOptimizations: its remaining 1.21.11 items are "avoid updating the lightmap" (a small GPU buffer write since 1.21.5) and "skip debug renderers when debug is off" (micro); the sky-colour optimisation only applied to 1.21.10 and below. Nothing there would show in the benchmark on top of the bundled set, and the owner can keep the mod installed alongside Shard.
  - Ixeris: moves event polling to its own thread and rewrites raw input and the FPS limiter. Real gains at high polling rates, but it restructures the render loop; reproducing it inside Shard is not worth the risk when the mod itself is MIT-free to install. Recommendation for the launcher: consider adding Ixeris to the bundled set.
- **Explosion "screen shake"** has no vanilla equivalent beyond the hurt-camera tilt, which No Hurt Cam already removes, so the Explosion Optimizer covers particles, the emitter and the per-tick sound cap only; knockback is never touched.
- **Fullbright hooks the second `Double.floatValue()` in `LightTexture.updateLightTexture`** (darkness scale first, gamma second, read from the bytecode) with `require = 0`; if the hook never runs, the module falls back to the vanilla slider maximum after one second and restores it afterwards.
- **Hitboxes toggle vanilla's debug entry** (`DebugScreenEntries.ENTITY_HITBOXES`, `ALWAYS_ON`) and restore the previous status; nothing is rendered that vanilla cannot render itself.
- **Nametags append health, armour and pops** by decorating `EntityRenderState.nameTag` after vanilla decided to show one; health is the same synced entity data vanilla uses, so servers that hide it hide it here too.
- **The benchmark is scripted** (`Benchmark`, `-PsmokeBench`): the dev username is fixed to `ShardSmoke` and `.smoke-server/ops.json` ops it so `/fill`, `/attribute` and `/kill` work; frame times are sampled from the HUD layer for 60 ticks after one hit on a corner crystal, five runs each way, alternating.
- **Smoke items come from `/give`, not `handleCreativeModeItemAdd`:** the creative-slot packet updated the server but the client's hotbar stayed empty, so client-side placement returned PASS and nothing could be predicted. Server commands keep both sides identical.

## 0.3.0: smooth text, fixed density, detailed settings

- **Fixed-density rendering ("design units").** The settings page, its popovers and the HUD editor chrome are laid out in design units and drawn under one pose scale, `pageScale = 2 / guiScale × interface size` (`gui/Scale.java`, tested). One unit is always two physical pixels at 100%, which is what GUI scale 2 on a 1080p monitor looks like, so the page is identical at GUI scale 1, 2, 3, 4 and Auto: the smoke test records the layout at each scale and it matches (640×360 units on the 1280×720 dev window, same columns, same card size). Mouse coordinates are divided by the same scale. `javap` on 1.21.11 shows `GuiGraphics.enableScissor` builds a `ScreenRectangle` and calls `transformAxisAligned(pose)`, so scissor rectangles are written in design units too; no manual conversion is needed. The only user knob is Settings → Appearance → Interface size (75–150%).
- **GUI scale 4 does not exist on a 720p window.** Vanilla caps the GUI scale at what fits (3 at 1280×720), so the "scale 4" pass of the smoke test renders at 3; the 4x crop is still written from it.
- **Narrow layout only for small windows.** The tab-strip layout now depends on the window's width in design units (below 460, about 920 px), never on the GUI scale. Below 420 units of height the sidebar tightens its row gaps from 12 to 4 so every category, Settings and the HUD editor button stay visible on a 720p window; the 4/8/12/16/24 spacing applies everywhere else. At 1280×720 the content area is 392 units wide, so the grid shows one card per row and the panel replaces the grid; at 1080p-equivalent widths it shows three columns, at 1440p four, never five.
- **Inter as Minecraft TTF fonts.** The launcher's font, Inter 4.1 (official rsms/inter release, OFL-1.1, licence in `assets/shard/font/LICENSE-Inter.txt`), ships as regular, medium and semibold TTFs. Minecraft fixes a TTF provider's size per font definition, so there is one definition per weight and size (`ui-<weight>-<size>.json`, sizes 10, 11, 12, 13, 14, 18; `ui.json`, `ui-medium.json` and `ui-semibold.json` are the size-13 aliases). Each provider is followed by a `minecraft:default` reference so ❤, 🛡 and arrows fall back to vanilla glyphs. `oversample` is 2.0, exactly the on-screen density at 100%, so one value suffices; the 4× zoom crops in the smoke output show smooth edges. Vanilla's `font.lineHeight` (9) is never used for layout: `Fonts` derives line height (1.25 em), cap height (0.7275 em) and the baseline from Inter's metrics, and compensates for vanilla placing every glyph's baseline 7 units below the draw position (`GlyphBitmap.getTop` is `7 - bearingTop`).
- **No tabular figures.** Minecraft's FreeType provider rasterises glyphs one by one and applies no OpenType features, so Inter's `tnum` cannot be reached. Numbers in slider fields are right-aligned instead.
- **Vanilla font stays one click away.** Settings → Appearance → Font: Vanilla draws the bitmap font (2× for titles) inside the same line boxes, so nothing moves; HUD modules follow the same option.
- **Anti-aliased corners from a 9-slice texture.** `CornerMask` (tested) computes 4×4 supersampled coverage for a disc or a ring; `RoundedTextures` registers one `DynamicTexture` per physical pixel radius; `Render2D` blits the four quadrants as corners and fills the edges and centre with quads. Switches, slider knobs, swatches and the colour picker's markers use the same shapes. The 0.2.0 per-row path remains behind Settings → Appearance → Smooth corners.
- **Motion is time-based.** Hover and selection fade over 150 ms, switch knobs travel in 180 ms, the panel slides and popovers fade in 150–200 ms with ease-out or smoothstep. Settings → Appearance → Reduce motion snaps everything.
- **Focus rings only for the keyboard.** A click no longer leaves an accent ring on the clicked control; Tab and the arrow keys bring the ring back. This keeps the accent on the active switch, the focused control, the selected category and the primary button only.
- **Details on hover, after 600 ms, at the pointer.** Settings can carry a second "details" line; it shows as a tooltip next to the pointer after it rests on the row, so it never covers the control beside it. Row descriptions wrap to up to three lines instead of being cut off; every module has an About paragraph in the panel header (the prediction modules state that the server stays authoritative).
- **Settings page.** Appearance and HUD defaults are hidden, always-on modules (`modules/settings`), so they get persistence, rows and `.set appearance …` for free. Keybinds lists every module keybind with capture buttons and warns when a key is shared with another module or a vanilla binding. Profiles carry an optional description (stored inside the profile JSON). Server rules can be added as patterns (`*.example.net`, `host:port`), renamed in place and given a per-rule module list. Export copies the whole config JSON to the clipboard; Import replaces it after a confirmation and leaves everything untouched if the JSON is not a Shard config. Reset all clears modules, keybinds and HUD positions but keeps server rules and profiles.
- **HUD style group.** Every HUD module declares the same `HudStyle` settings, in the same order: Custom style, Style preset (Card, Minimal, Outlined), text colour, value colour, background (alpha is opacity), corner radius, padding, text shadow, alignment where it applies, and the label text. Settings → HUD holds the global defaults; an element uses them until Custom style is on, and the inherited rows are hidden meanwhile. HUD elements are drawn in HUD units normalised against the GUI scale exactly like the page (`Scale.hudScale`), times Settings → HUD → HUD scale, times the element's own scale from the editor; positions stay fractions. HUD text is Inter 10 on a 10-unit line with padding 2, so a one-line element is 14 units tall and existing 0.2.0 layouts do not start overlapping.
- **Config schema 3.** `Module.load(json, version)` hands keys the current build does not know (or keys whose meaning changed, via `migratesKey`) to `migrateSetting`. Migrations: every HUD module's `show-background` / `background` = false becomes a custom Minimal style; FPS `color` becomes the custom text and value colour; FPS `label` (a switch in 0.2.0) becomes the label text "FPS" or empty. All other keys kept their names. A loaded older file is marked dirty so it is rewritten as schema 3 on the next flush. `MigrationTest` loads a schema-2 file and checks enabled state, keybind, values, server rules and GUI state survive.
- **Module options added.** Crosshair: dynamic gap after your own attacks and a hit marker on living targets (both react to attacks vanilla already sent). Nametags: scale and background opacity (patched in `NameTagFeatureRenderer.Storage.add`, which every name tag goes through, so they apply to all tags while the module is on) and hide own. Hit Color: flash duration, trimming how many of vanilla's 10 hurt ticks keep the tint. Low Fire: a live preview in the panel. Crystal Optimizer: a glow outline colour on crystals you just placed (your own crystals, so nothing hidden is shown). Totem Pop Tweaks: message format with `{name}`, `{count}`, `{ordinal}` and `{s}` (`Placeholders`, tested). Explosion Optimizer: any extra particle types by id with their own keep percentage. Zoom: the wheel adjusts the zoom while held (the scroll is consumed so the hotbar slot does not change) and an optional vanilla cinematic camera while zoomed. Keystrokes: key labels, key size, gap and a WASD/Arrows layout. Item Counter: a custom item list typed as ids (`ItemIds`, tested).
- **Smoke harness.** The cursor is parked in the page margin after every screen opens (opening a screen recentres it, which used to leave hover state in screenshots), and the crystal kit gets 30 ticks to arrive before placement.

## 0.4.0: design system, sharp text, one icon set

- **Design system first.** `docs/DESIGN.md` fixes the tokens, type scale, spacing, motion and control family; `docs/design/mockups.html` showed three directions (rail + list + detail, wide cards, command window). The owner picked **A: rail + list + detail**, because the full module name always fits in a list row and settings stay visible without opening anything.
- **Why 0.3.0 text stair-stepped.** `javap` on 1.21.11 shows `FontTexture`'s constructor asks `SamplerCache.getRepeat(FilterMode.NEAREST)` for every glyph atlas. With the raster at exactly 2 px per unit and the page at 100% that is pixel-exact, but at any other density (interface size 125%, a HUD element scaled 1.65x, Windows display scaling) nearest sampling repeats or drops whole texel rows, which is the blocky "Frames per second." in the owner's screenshot. Two fixes together: `FontTextureMixin` gives `shard:ui*` atlases LINEAR filtering (vanilla fonts keep NEAREST; the packer already leaves a 1-texel gap so neighbours never bleed), and `Fonts` picks one of four raster densities (2, 3, 4, 6 px per unit; `tools/fonts/gen_fonts.py` writes the 120 definitions) at or just above the real on-screen density, so linear filtering only ever smooths a small rest and never magnifies. `HudManager.renderOne` now reports each element's own scale as the density, which 0.3.0 ignored. Proof: `docs/screenshots/0.4.0/text-before-after.png` (the smoke test's new density pass shoots 125%, 150% and a 1.65x HUD element).
- **SDF/MSDF text was considered and not built.** It would need a custom shader pipeline, its own glyph atlas and layout (Minecraft's splitter, wrapping and styles would no longer apply). Per-density TTF rasters with linear filtering give the same visible result at the sizes Shard uses for a fraction of the code.
- **One icon set: Lucide.** 0.3.0 mixed 16x16 pixel glyphs with Minecraft item sprites, which read as random. `tools/icons/build_icons.py` renders the names in `tools/icons/icons.txt` (93 icons, Lucide 0.460.0, stroke 1.75, plus the Shard logo) with headless Edge into white-on-transparent atlases at 32, 64 and 128 px per cell; `.mcmeta` "blur" makes them linear-filtered, and `Icons.draw` tints them and picks the smallest atlas at or above the on-screen size. Item sprites are no longer used as module icons; HUD elements still show real item textures, since those are game information. `Glyphs.java` is gone.
- **The menu is rail + list + detail (step 3).** `ClickGuiScreen` keeps 0.3.0's plumbing (hits as the single source of input truth, popovers in their own stratum, design units, time-based motion) and replaces the layout: a 176-unit rail (brand, search with a Ctrl F hint, Favorites, Enabled, categories, then Edit HUD and Settings pinned to the bottom); a list where every row is icon, full name, one line of description and a 28x16 switch; and a 300-unit detail column. From 820 design units (1640 px at 100%, so 1080p and up) the detail column is always visible and something is always selected (the last module you looked at, else the first in the list); below that it slides over the list with a back button; below 460 the rail becomes the tab strip as before. Grid view (Grid/List toggle, remembered) puts the full name on its own line under the icon and switch, so it never shares a line with the toggle.
- **Search covers settings.** `SearchMatcher` (tested) needs every query word somewhere in the fields and ranks name matches first; "colour" and "color" are the same word. Results list modules, then up to 12 settings with their module and group; picking a setting opens its module, scrolls the row into view and flashes it. Typing any letter or digit with nothing focused starts a search; Esc clears it first, then closes.
- **Keyboard.** Up/Down move through rows (the detail column follows on wide windows), Space toggles, Enter jumps into the module's first control, right-click on a row toggles it. Favorites are a star in the detail header, stored with the view and selection in the config's `gui` object.
- **Controls.** Toggles are smaller and quieter (neutral track off, accent on, no outline when on); sliders have a 2-unit track and 12-unit knob and Shift drags at a quarter speed; enums with two or three short options are segmented controls; rows with a switch give the label the full width (0.3.0 reserved a 150-unit column for a 28-unit switch, which cut labels off); the keybind row warns when the key is shared.
- **Smoke test at 1080p.** `-PwindowSize=1920x1080` sets the dev window; a menu pass shoots list, grid, a settings search and the jump. The layout check now allows the 1-2 units of page height that vanilla's rounding adds when the window height does not divide by the GUI scale (1061 px on a 1080p monitor); every width and column count must still match.
- **HUD positions are an anchor plus an offset (step 4).** 0.3.0 stored the top-left corner as a fraction of the GUI size, so an element hugging the right edge drifted inward or off-screen when the window or GUI scale changed. Each axis now has an anchor (start, centre or end, by which third of the screen the element's centre is in) and an offset in HUD units from that anchor (`HudGeometry`, tested): a Totem Counter 10 units from the right edge stays 20 physical pixels from it at every GUI scale and window size. Old fraction configs (and module defaults) are converted the first time the element is drawn, after its real size is known; no schema bump was needed because the loader tells the two formats apart by their keys.
- **Editor.** Drags snap within 6 physical pixels to screen edges, the centre lines and other elements' edges and centres, with accent guide lines (Alt places freely); ties prefer the top/left edge. Shift-click or a drag box selects several; two or more get align buttons, three or more distribute. Arrows nudge one physical pixel (Shift: ten), positions are kept fractional and the HUD layer rounds the translation to whole physical pixels so text stays sharp. Scroll or the toolbar's -/+ scales around the element's anchor. Undo/redo (`UndoStack`, tested) snapshots every element's state before each change; arrow nudges and scroll steps within 700 ms merge into one step. G toggles an 8-unit grid (remembered). Delete hides the selection, right-click resets.
- **Settings in a side panel.** A click without a drag opens the element's settings next to the HUD. It is the menu's own detail column in an "embedded" mode (`ClickGuiScreen.embedded`), so every control, popover and keybind capture behaves exactly as in the menu; the editor forwards input to it while the pointer is over it or it is capturing keys, and the toolbar centres itself in the space left over (icon-only title when tight).
- **Presets.** Built in: Crystal PvP minimal (FPS, ping, totems left of the offhand slot, armour right of the hotbar, kit counter above the hearts, cooldown under the crosshair), Crystal PvP full (adds CPS, coordinates, keystrokes, effects, session, clock) and Streamer (full without coordinates or server). Positions are tuned for GUI scale 2 at 1080p. Players save their own from the Presets menu (stored in the config's `gui.hudPresets`); applying any preset is one undo step.

### Module audit (step 5)

| Module (0.4.0 name, key) | What was weak in 0.3.0 | Changed in step 5 | Left for step 6/7 |
|---|---|---|---|
| FPS (`fps`) | Average only; stutters invisible | 1% low (yellow when under half the average), optional frame-time graph, numbers update 4x a second so they are readable | — |
| Ping (`ping`) | Number only | Spike warning (1.5x and 40 ms over the last 30 s), optional 30 s graph | — |
| CPS (`cps`) | Fine | — | small graph (7) |
| Coords (`coordinates`) | Long name | Renamed | biome option (7) |
| Keystrokes (`keystrokes`) | Labels read "Wasd"; mouse keys only said LMB/RMB; presses snapped | WASD label; CPS on the mouse keys (number only when the key is too narrow); presses fade over 120 ms | styles (6, HUD looks) |
| Armor (`armor-status`) | Grey placeholders for empty slots; no bars; long name | Hides empty slots, durability bars, low pieces pulse; renamed | — |
| Totem Counter (`totem-counter`) | Fixed thresholds, no warning | "Warn at" count with a red flash and a bell when you drop to it; offhand dot (green: totem in offhand, red: none) | — |
| Item Counter (`item-counter`) | Only zero was highlighted | Low-stock yellow per item (16 crystals/obsidian/XP, 8 glowstone, 4 anchors/gapples, 3 totems/pearls) | — |
| Effects (`potion-effects`) | Colour bar instead of the effect icon; blinked only under 5 s | Vanilla effect icons, blink under 10 s; renamed | compact mode (7) |
| Attack Cooldown (`hit-delay`) | Name said "Hit Delay"; colours fixed | Renamed; charging and ready colours | ring style (7) |
| Server (`server-address`), Session (`session-stats`) | Long names | Renamed | — |
| Clock, Memory | Fine | — | — |
| Crystal Optimizer (`crystal-optimizer`) | No way to see whether the prediction matches the server | Prediction readout (off by default): predicted vs confirmed breaks, missed breaks, average confirm time, from the server's own remove packets | — |
| Anchor Optimizer, Explosion Optimizer, Toggle Sprint | Fine | — | — |
| Totem Pops (`totem-pop-tweaks`) | Counts never reset in a session; long name | "Reset on death" (default on) so counts mean this fight; renamed | animation size (6) |
| Nametags (`nametags`) | No ping | Optional ping from the tab list, coloured by latency | — |
| Crystal Visuals (`crystal-size`) | Scale only | Renamed ahead of step 6 | colours, glow, spin, bounce (6) |
| Low Fire, Low Shield, Crosshair, Hit Color | — | — | rebuilt in step 6 (owner's must-have list) |
| Fullbright, Hitboxes, No Hurt Cam, No Death Animation, Zoom | Fine | — | — |

- **Renames keep their keys.** `Module.key()` was derived from the display name, so renaming would have orphaned configs, keybinds, server rules, favorites and HUD presets. `Module.legacyKey()` pins the old key for every renamed module (`MigrationTest.renamedModuleKeepsItsSettingsKeybindAndServerRules`).
- **Enum labels.** Enums can implement `Labeled` to show "WASD" instead of the derived "Wasd".
- **1% low** comes from `FrameStats` (tested): the HUD layer records every frame's time; the 1% low is the frame rate of the slowest 1% of the last 1000 frames. It includes hitches from opening screens; in the smoke test it reads low because of the screenshot captures themselves.

### Step 6: the owner's must-have list

- **Low Fire covers three fires.** Your screen (height, opacity down to hidden, tint), fire and soul fire blocks on the ground (height, opacity, separate tints) and the flames on burning players and mobs (height, opacity, tint). Ground fire is part of the world mesh, which Sodium (bundled by the launcher) builds itself, so a vanilla mixin would not reach it. Instead the fire and soul fire block models are wrapped through Fabric's model loading API (`LowFireModels`): each quad is scaled down from the block's floor, multiplied by the tint and moved to the translucent layer when it is see-through. Both Indigo and Sodium honour the Fabric rendering API. Meshes are built on worker threads, so they read an immutable snapshot; when it changes (slider, switch, server rule) the world is re-meshed once the value has been still for 250 ms. Entity flames are squashed in `FlameFeatureRenderer.renderFlame` and drawn with the translucent item sheet when needed. Blocks, hitboxes, burning and damage are untouched.
- **Tints multiply.** Vertex colour can only multiply the fire texture, so a blue tint on orange fire comes out yellow-green, not blue. True recolouring needs a greyscale fire sprite in the block atlas; shipping one derived from Mojang's texture is not allowed and generating one at runtime needs a custom atlas source. Deferred; the panel preview shows the real result, and white keeps vanilla.
- **Crosshair: every shape is a pixel mask.** `CrosshairShape` (tested) turns each style (gap plus, plus, plus and dot, dot, circle, T, X, custom) into a centred mask; the outline is the mask grown by its width, so it follows any shape, including one the player draws; rows are merged into runs for drawing (cached until a setting changes). One crosshair pixel is one GUI unit, like vanilla's. New: outline colour and width, a crystal colour, dimming while the hit recharges (the attack strength vanilla's indicator shows). The panel preview shows it on sky, grass, stone, netherrack or end stone at real size and 4x, becomes a 15x15 pixel editor for Custom (click paints, right-click erases, drags work), and copies or pastes share codes (`SHARD-X1-` plus base64 of the settings). Previews now receive clicks and drags (`PanelPreview.previewInput`).
- **Shield (was Low Shield, key `low-shield`).** Two sets of values, while blocking and while just holding: width, size, lower, sideways. Position is applied right after vanilla pushes the hand's pose, so "lower" is down the screen; scale is applied just before the item is drawn, in the item's own space, so "width" thins the shield's face around its own centre instead of pulling it towards the middle of the screen. 0.3.0's "Lower by" migrates to the blocking value (and to holding when "Only while blocking" was off). An opacity slider was in the brief but is not built: the shield draws with an opaque entity render type plus banner layers, and making it see-through needs a translucent copy of that pipeline.
- **Anchor Glow.** Every 4 ticks the module looks at block states within a set range (4-16 blocks) of the player for respawn anchors and draws their outline after entities (Fabric `WorldRenderEvents.AFTER_ENTITIES`) with vanilla's depth-tested `lines()` render type, coloured per charge (0-4, empty off by default), full ones pulsing. Depth testing means an anchor behind a wall is not drawn; the charge is the same block state vanilla shows as glowstone on the anchor's top. A general "highlight all anchors through walls" was never on the table.
- **Crystal Visuals.** Spin speed (the rotation vanilla derives from age, scaled), bounce off (vanilla's bob evaluated at age 0), base off (render state), separate core and frame colours and opacity, and an outline on the crystal under your crosshair. Separate colours need two draws: two extra copies of the crystal model are baked from vanilla's layer, one that hides the cube and one that skips drawing the glass while keeping its pose, and the renderer submits both with their tint (translucent render type below 100%). The aimed-crystal outline uses vanilla's outline effect, which shows through walls, so it is limited to the crystal vanilla picked under the crosshair (line of sight); a general crystal glow was left out for that reason.
- **GUI Scales (new Utility category).** Inventory scale: a container screen is laid out at a virtual size (`ScreenScale`, tested) when it opens, the factor is stored on the screen, the screen is drawn under that pose scale, and every mouse event the handler gives it (click, release, drag, move, scroll) is divided by the same stored factor, so drawing and input can never disagree. 1.21.11 draws the deferred subtitles and, through them, Fabric's mod HUD layers (Shard's HUD) from inside `Screen.renderBackground`; that call is wrapped to cancel the scale, otherwise the HUD grew with the inventory. The scale is capped at what fits the window. Proof: the smoke test moves the pointer through the mouse handler's own move path to the centre of all 47 slots at inventory scale 3 over game scale 2 and reads the hovered slot back: 0 wrong (`inventoryScaleSlotMisses` in the summary). Hotbar (with hearts, food, armour, air, mount, XP and held-item name), scoreboard, tab list, titles and action bar, and boss bar are vanilla HUD elements wrapped through Fabric's `HudElementRegistry.replaceElement` and scaled around their own anchor. Chat scale writes vanilla's own chat scale option, so chat clicks stay correct.
- **Display.** Borderless fullscreen is a GLFW window with no decoration placed over the chosen monitor (or the one holding the window's centre) at the monitor's own video mode, so Windows never changes the resolution and alt-tab is instant; F11 goes borderless instead of exclusive when "F11 fullscreen" is Borderless (a hook on `Window.toggleFullScreen`), leaving exclusive first if it was on, and restores the previous window position and size on the way back. The smoke test switches it on and off and checks the window covers the monitor and is restored (`borderlessCoversMonitor`, `borderlessRestoresWindow`); alt-tab itself needs a real desktop and was not automated. Background FPS caps `FramerateLimitTracker`'s limit while the window is not focused; Menu FPS replaces vanilla's 60 outside a world. The window title comes from `Minecraft.createTitle`. Raw mouse input and VSync write vanilla's own options. A resolution and refresh-rate picker for exclusive fullscreen is vanilla's (Video Settings), so it was not duplicated.
- **HUD looks.** A fourth style, Pill (fully rounded, wider side padding), a label position ("FPS 240" or "240 FPS") and optional brackets join the shared style group (`HudStyle`, tested; new keys only, so old configs load unchanged). Every HUD element's settings panel previews the element itself in all four styles at real data (the module renders once per style under a preview override; its measured size is restored afterwards so the live layout does not move); clicking one switches to it, copying the inherited look first so only the style changes. "Use this look for every HUD element" writes the look into Settings → HUD and turns off every custom style, so all elements follow it; label texts stay per element.
- **Smoke world reset.** The fire pass once switched the player to survival, which made it fall from where it hovers and die, and the next run joined dead. The smoke test now respawns the player, sets creative and teleports to the usual spot on join.

### Step 7: more crystal PvP modules, measured performance

- **One fight log for everything.** `FightLog` (pure, tested) records your hits and their reach (eye to hitbox, like vanilla), the combo (reset when your health drops), damage you take, pops on both sides, crystals you place, kills (a target dying within 5 s of your hit, from the death entity event every client receives; hooked next to the totem event), deaths, streaks and a recap when a fight ends (kill, death, or 15 s of quiet with at least three hits). `CombatTracker` feeds it from your own attacks after vanilla sent them, your own health, your crystal placements and those events; nothing is sent.
- **New HUD elements** (all off by default; Crystal PvP Pro turns some on): Target HUD (head, name, health and absorption, armour durability, pops, distance; hides a few seconds after your last hit), Combo, Reach, Fight Recap, Cooldowns (vanilla's item cooldowns as a strip), Compass, Speed, TPS (`TpsEstimator`, tested, from the time packet every server sends once a second). Session gained kills, K/D and streak.
- **New modules:** Low Health Warning (pulsing edge glow, optional heartbeat), Sounds (explosions, hurt, hit and crystal-break volumes via `SoundEngine.calculateVolume`), Block Outline (colour, thickness, and a crystal-spot colour on obsidian or bedrock with air above and nothing in the way; the looked-at block is the one vanilla picked), Clean Screen (pumpkin, powder snow, portal, vignette), Weather and Time (client-side rain and thunder off and a pinned time of day; the client level's getters only), Chat (timestamps, repeats stacked as (x3), joins and leaves hidden, mentions and keywords highlighted). New categories: Chat and Utility.
- **Quick setup and welcome.** `QuickSetup` applies Crystal PvP Pro, Minimal or Recording (modules on plus a HUD layout). A three-step welcome (setup, accent colour, menu key) opens the first time you join a world and never again (`gui.welcomed`); Settings -> Quick setup offers the setups any time.
- **Considered and not built:** Auto GG (it would send chat on its own; the fair-play rule says no extra packets), Freelook (banned on many servers and needs camera rotation decoupled from the player's; better as an explicit per-server feature later), copy-a-chat-message (needs click handling in vanilla's chat screen), screenshot-to-clipboard (Minecraft runs AWT headless, so an image clipboard needs native code), Motion Blur (a shader pass; worth it only with a measured cost), custom hand position and swing speed, waypoints, hotbar slot numbers, per-server HUD layouts, scoreboard and tab list restyles beyond their scale. Nausea and portal wobble are vanilla's Distortion Effects slider; static FOV is vanilla's FOV Effects slider; exclusive fullscreen resolution is vanilla's Video Settings.
- **Performance, measured.** The benchmark now runs with a busy HUD and times Shard's HUD layer per frame and per element (BENCHMARKS.md, 0.4.0). The background box of every element was seven draw calls; small rounded boxes are now one cached texture per size (least recently used sizes released), which took the busy HUD from 0.31 ms to 0.18 ms per frame. Text is drawn as plain left-to-right sequences (no per-draw bidirectional reordering) with widths cached, and `ModuleManager.get` is cached by class because mixins call it many times per frame.

### Step 8: final verification

- **Three window sizes.** The smoke test ran at 1280x720, 1920x1080 and 2560x1440 (this
  machine's monitor is 1920x1080, so Windows keeps the 1440 window 2560 wide but only 1061
  tall; widths are tested for real, heights only up to 1061). Every PNG was looked at.
- **Cut-off text is now measured.** `Fonts.clip` records every text it had to shorten while the
  smoke test runs, and the summary lists them (`clippedTexts`). No module name appears at any
  size; what is shortened is one-line descriptions in list rows and cards (the full text is in
  the detail column), placeholders and long hints.
- **Fixed in step 8:**
  - Below 1640 px, typing a search while a module was open left the results hidden behind the
    detail column. A query now slides the detail column away and leaves the Settings page; the
    search field keeps focus (`searchEdited`).
  - The page width came from vanilla's GUI width, which is rounded up (2560 px at GUI scale 3 is
    854 GUI units), so the page was one design unit wider at some scales and
    `layoutIdenticalAcrossScales` failed at 2560 wide. The design size now comes from the
    window's physical pixels (`Scale.designSize(gui, page, physical, guiScale)`, tested), which
    are the same at every scale and never exceed the GUI size.
  - The compact top bar (small windows or a large interface size) cut "Edit HUD" to "Edit H…"
    and let the last tabs run under it (the button was measured at the description size and
    drawn at the label size). It now steps down: full names; short names with an icon-only
    Edit HUD button; icon-only category tabs.
  - The HUD editor toolbar slid under the settings side panel at 720p, hiding Done. When one
    bar does not fit beside the panel, the toolbar wraps onto a second bar.
  - The "Crystal PvP full" and "Streamer" presets put Keystrokes in the bottom-left corner,
    which is vanilla's chat; it now sits on the left edge just below the middle.
  - The welcome screen cut the Quick setup descriptions with "…"; they wrap to two lines.
  - The Settings page's line explaining the three setups was cut at 720p; it wraps now and uses
    the secondary text colour (the tertiary one is for things you need not read).
  - Smoke harness: `parkCursor` moved the OS cursor, but the unfocused dev window never
    reports that move, so hover tooltips leaked into screenshots; it now also tells the mouse
    handler. The inventory check converts slot positions with the window size (what mouse
    events use) instead of the framebuffer size; they differ when the window is bigger than
    the monitor, which made all 47 slots "miss" at 2560 wide. The menu pass also shoots the
    HUD category in grid and list view (the owner's 0.3.0 feedback view) and, below 1640 px,
    closes the detail column so the list itself is checked.
- **Left as is:** on a 1280x720 window chat (vanilla's fixed width) can reach the kit counter
  above the hearts while chat lines are showing; the known limits above (fire tints multiply,
  no shield opacity, alt-tab not automated, a wide toolbar can cover top-left elements) still
  apply.

## 0.5.0: the launcher cape in-game

- **Only your own player.** The Cosmetics module swaps the cape (and, by default, the elytra
  texture) in `AbstractClientPlayer.getSkin()` for the local player only. equipped.json is a local
  file, so there is no way to know what other people wear without a Shard server; showing capes to
  others is not built. Nothing is sent to the server; other players see your Minecraft cape.
- **Where the texture comes from.** launcher-info.json's `equippedPath`; the launcher caches every
  equipped back texture as `textures/<id>.png` next to it (same file-name rule as the launcher's
  `CosmeticAssets.cachePath`). Launcher 0.2.1 also copies bundled textures there, so the bundled
  catalogue fallback works in-game too. Dev runs use `-PequippedPath=<equipped.json>`.
- **Sharp without flicker.** Vanilla uploads capes as one level sampled NEAREST. `CapeTexture`
  creates the GPU texture with a full mip chain (4096 down to 64 wide, 7 levels; `MipChain`, tested,
  averages 2x2 blocks weighted by alpha so the transparent parts of the layout never darken the
  painted edges) and samples it with trilinear filtering through `SamplerCache.getSampler(..., true)`;
  RenderSetup binds a texture with its own sampler, so the entity render type picks it up.
- **Loaded once, off the render thread.** The PNG is decoded and the mip chain built on a worker
  thread on the first tick in a world; only the upload runs on the render thread.
- **Smoke test.** A cape pass shoots the player from behind, from above, close up, from the front,
  with an elytra and with the module off, in daylight above the arena's roof (under it the whole
  player is in shadow); `capeStatus` in the summary. The inventory-scale check now gives each slot
  three ticks: with two, a tick that ran without a frame in between read a stale hovered slot and
  0-3 of 47 slots "missed" at random, with or without the cape.

## 0.6.0: Shard API (capes for everyone, tokens, friends)

- **A real service, not a list.** Showing purchases to everyone and paying tokens for play time
  needs a shared, trusted record, so there is now a small API (`shard-api`, a Cloudflare Worker
  with a D1 database; contract in its API.md). The address is read from meta's `services.json`, so
  it can move without a client release.
- **Identity without passwords.** First built as Mojang's server-join check (`joinServer` +
  `hasJoined`), but Mojang answers 403 to every request from Cloudflare (hasJoined, profiles,
  public keys), so the API could never confirm anyone. 0.6.1 signs the API's one-time challenge
  with the account's Mojang-signed chat key (`ProfileKeyPairManager`) instead; the API checks
  Mojang's signature with Mojang's published keys and never contacts Mojang. The access token only
  goes to Mojang.
- **Tokens for active play only.** A heartbeat every two minutes while in a world; it is "active"
  when the player turned or moved in the last five minutes. The API credits at most 150 s per
  heartbeat, never more than wall-clock time, 10 tokens per 10 minutes (API.md).
- **Your own cape must be owned.** The launcher's local choice still shows offline, but once
  signed in the client only shows it if the API says you own it.
- **Everyone's capes.** Players in the world are looked up in batches (new faces at once, everyone
  again every minute); textures come from the meta catalogue once per cape. A failure anywhere in
  the skin hook turns the module off instead of crashing the game (a null lookup did crash the dev
  build once; it is fixed and guarded now).

## 0.7.0: compact menu, 1:1 text, visual mods

- **Menu at one design unit per physical pixel.** The owner's mockup is 960x580 CSS pixels at
  1080p. `ClickGuiScreen.pageScaleFor` draws the menu at max(1, window height / 1080) physical
  pixels per unit times Interface size, so the panel keeps the mockup's proportions at every GUI
  scale (the smoke test's `layoutIdenticalAcrossScales` still holds). The HUD editor's embedded
  column keeps the 2-px-per-unit density. Rail, list view and slide-in panel are gone; the
  settings view replaces the grid inside the panel.
- **Why text was still choppy.** At 1 px per unit the smallest raster was density 2, so every glyph
  was downsampled 2:1 by bilinear filtering, which skips texels and breaks 1-px stems (the "F" in
  FPS lost its bar). A density-1 raster (`-x1` definitions, `tools/fonts/gen_fonts.py`) is drawn
  1:1 and FreeType hints it at that size; zoomed crops show solid strokes.
- **Smooth scrolling.** Grid, settings view and pages keep a target and a drawn position that
  eases toward it with frame-time exponential smoothing (instant with Reduce motion).
- **Removed Target HUD and Nametags** (health on name tags): commonly banned. Old config entries
  are ignored because modules are loaded by registered key.
- **Cosmetics always on** (`Module.alwaysOn`): no tile, no keybind; options on the Cosmetics tab.
- **Fire texture is a built-in resource pack** (`resourcepacks/shard_fire`, Fabric
  `ResourceLoader.registerBuiltinPack`). It follows only its own setting: tying it to the module
  switch reloaded every resource each time Fire was toggled (seen in the smoke run).
- **Neutral controls.** Switches use the green "on" token, sliders a light grey fill, primary
  buttons white; the accent only marks keyboard focus.
- Fire / Shield / Hit Color / Small Items / Totem size and Crosshair / Sky / Hitboxes / Zoom /
  Entity Optimizer details: CHANGELOG.md 0.7.0. New mixins use `require = 0` and were checked with
  javap against the 1.21.11 jar; the full smoke run logs no injection failures.

## Fonts, fire and Small Items (branch `fonts`, after 0.7.1)

- **Why launches hung.** 0.7.1 shipped 330 Inter font definitions. While resources load, Minecraft
  opens every TTF provider (a 400 KB face each) and then, on the render thread, builds every font
  set's width table by loading the metrics of every Inter glyph and every fallback glyph (the
  `minecraft:default` reference pulls in unifont). Measured with the new "Shard: resource reload
  took N ms" / "fonts loaded in N ms" log lines at 1920x1080: 12.3 s and 10.0 s reloads, fonts 4.2 s.
  Now there are no definitions: `LazyFonts` + `FontManagerMixin` build `shard:ui-<weight>-<size>-d<density x 100>`
  on first use (1.5-3 ms; the very first one about 40 ms while FreeType warms up), register it in
  the font manager so `updateOptions` and the next reload treat it like any font, and
  `FontSetMixin` skips the width table for these sets (only obfuscated text uses it). After: 1.2-1.7 s
  reloads, fonts 0.35-0.5 s. `tools/fonts/gen_fonts.py` is gone.
- **Exact densities.** With fonts free until used, `Fonts.densityFor` returns the on-screen density
  rounded to 0.01 (0.5..12). 0.7.1 rounded up to the next of 11 densities: a 0.5 px/unit HUD drew a
  0.75 raster downsampled by bilinear filtering, which drops thin strokes (the "cut off" letters).
  `Fonts.draw` also snaps the baseline origin to a whole physical pixel (`Fonts.pixelSnap`): FreeType's
  hinted advances and bearings are whole raster pixels, so the whole line is pixel-exact. The menu's
  4-unit grid at 1.25 px/unit was already aligned for most text; tabs and previews moved.
- **Fire without a resource pack.** The built-in pack was not in the first reload's list on some
  launches; `syncFirePack` then selected it and reloaded everything a second time. The frames now
  ship as `shard:block/fire_0/1` (the block atlas's `block/` directory source stitches every
  namespace) and "Custom fire texture" swaps sprites: ground fire UVs are remapped in
  `LowFireModels` (re-mesh on change), burning entities through `AtlasManager.get` in
  `FlameFeatureRendererMixin`, the first-person overlay through `ScreenEffectRendererMixin`. One
  "Reloading ResourceManager" per launch with the setting on and off.
- **Small Items = Smallhands 1.0.0.** The owner's `Small-Items-1.21.11.jar` is a code mod, not a
  resource pack: javap shows `@Inject(HEAD)` into `ItemInHandRenderer.renderItem` and `renderPlayerArm`
  that push and `scale(0.6, 0.6, 0.6)` (config defaults: scale 0.6, offsets 0, affectItem and affectArm
  true; the owner's `config/smallhands.json` has exactly those). No item model or per-item transforms.
  Look "Small Items mod" (default) reproduces that: 0.6 around the hand's pivot for every item,
  shields included, and the empty arm; Look "Custom" keeps the 0.7.1 sliders.

## Deferred

Cosmetics rendering and the emote wheel, environment colours, shield state colours, totem pop ghosts, and additional Minecraft targets. The wildcard server-rule editor shipped in 0.3.0.
