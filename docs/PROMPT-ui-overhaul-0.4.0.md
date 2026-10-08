# Shard Client 0.4.0: the pro crystal PvP client, rebuilt clean

You are working on Shard Client, my Fabric mod for Minecraft 1.21.11 (Mojang mappings), in this
folder. Its users are **competitive crystal PvP players**: they play on high-ping and low-ping
servers, they use 144-360 Hz monitors, and they judge a client on three things: how clean it
looks, how fast it feels, and whether every millisecond and pixel helps them in fights.
0.4.0 has to feel like the best legit crystal PvP client that exists: clean, quiet, fast and
made by a person with taste, not a template.

Read STATUS.md, DECISIONS.md, README.md and BENCHMARKS.md before you change anything.

## Hard rule: stay legit (not negotiable)
Follow the fair-play rule in STATUS.md word for word. Allowed: HUD, visuals, vanilla-legal input
helpers, server-authoritative prediction (like the existing Crystal/Anchor Optimizer) and
measured performance work. **Not allowed:** auto crystal, auto totem, auto armor, auto anything,
inventory automation, any movement or timing change, extra/dropped/delayed/reordered packets,
reach or hitbox changes that affect gameplay, rendering info the server hides (no ESP, no
through-wall rendering, no hidden-player info) and anything that answers or spoofs server
checks. If a module idea is near the line, give it a per-server rule (the existing server-rule
system), default it off and write the reasoning in DECISIONS.md. If it is over the line, don't
build it; list it under "rejected" with the reason. Never weaken this rule to add a feature.

---

## Part 1: What's wrong with the current GUI (my feedback on 0.3.0)

Look at `docs/screenshots/0.3.0-gui-owner-feedback.png`. That's the current HUD page at 1920x1080
and **I don't like it**. Specifically:

1. **Everything gets cut off.** Names show as "Coordina…", "Armor St…", "Totem C…",
   "Potion Ef…", "Item Cou…", "Server A…", "Session…". Every description is cut off after 2-3
   words ("Your latency to the serv…"). A pro looking for a module can't read the grid. The cards
   are too narrow for name + description + a big toggle side by side.
2. **The grid runs off the right edge.** The fourth column is cut by the screen edge, and the
   last row is ragged (2 cards then empty space).
3. **The icons don't match each other.** Thin line icons (FPS clock, keyboard, mouse, server,
   memory chip) sit next to pixel-art Minecraft items (diamond chestplate, totem, enchanted
   book, clock, potion, sword). Some tiles have a coloured background (Ping) and most don't. It
   looks random and cheap.
4. **It looks generic / AI-made.** Every card is the same dark rounded box with the same weight,
   the same bright cyan toggle, the same padding. Nothing stands out, no hierarchy, no brand.
   The sidebar is a copy of every template sidebar. The "3/14" counters are noise.
5. **The text still isn't crisp enough** compared with the launcher, and the description text is
   too low-contrast to read on top of the blurred world.
6. **The toggles are too big and too bright.** The cyan pill toggles are the loudest thing on
   the screen, more than the module names.
7. **Too much empty space** at the bottom, while the content at the top is cramped and cut off.
8. **The search box and the HUD editor button** look like default text fields/buttons.

Fix all of these, and then go further.

---

## Part 2: Design direction

### Step 0: write the design system before writing code
Create `docs/DESIGN.md` with:
- **Personality:** sharp, quiet, competitive. Think of a pro esports tool or Linear/Raycast rather
  than a gaming RGB menu. Calm neutrals, **one** accent colour used sparingly (active state,
  focus, small highlights), plus semantic colours for good/warn/bad (ping, durability, totems).
- **Colour tokens:** background layers (base, raised, overlay), border, text primary/secondary/
  tertiary with real contrast numbers (secondary text at least 4.5:1 against the panel on top of a
  bright world, so test on both a dark cave and a snowy daytime background), accent, accent-muted,
  success/warning/danger. Let the user pick the accent in Appearance, with presets (Shard cyan,
  white/mono, crimson, violet, lime, gold) and a custom colour.
- **Type scale:** Inter (already bundled). E.g. 22 page title, 15 card title, 12.5 body,
  11 caption, plus a tabular-number style for HUD values so digits don't jump around.
  Weights: semibold titles, regular body, medium labels. Never more than 3 sizes on one screen.
- **Spacing:** 4-pt grid (4, 8, 12, 16, 24, 32). **Radii:** 6 small, 10 card, 14 panel; the same
  everywhere. **Elevation:** at most 2 shadow levels, soft and low-opacity.
- **Motion:** 120 ms for hover, 180 ms ease-out for toggles/expand, 220 ms for screen open
  (fade + 0.98→1.0 scale). Nothing bounces. Add an option for reduced motion.
- **Iconography rules** (see Part 3).
Show me DESIGN.md plus 2-3 mock screenshots of directions before building the whole thing.

### The main menu (module browser)
- **Layout:** a slim left rail (logo, categories, Settings at the bottom), a content area with a
  page header, and a **right-side detail panel** that opens when a module is selected. Content
  must never run off screen at any resolution from 1280x720 to 3840x2160 or at any GUI scale.
- **Module rows/cards that fit:** pick a layout where the full name always fits (e.g. responsive
  columns that drop from 3 to 2 to 1 as width shrinks, or a clean list view). The description
  should show in full on hover or in the detail panel, never "…" on the name. Offer a
  **Grid / List toggle**; List view is the dense pro view (name, one-line description, small toggle,
  settings chevron).
- **Toggles:** smaller, subtle. Off = neutral outline; on = accent fill with a soft animated knob.
  Enabled modules also get a small accent dot/edge so you can scan what's on.
- **Categories that make sense for crystal PvP:** Combat (crystal/anchor/totem/hit stuff),
  HUD, Visuals, Performance, Chat, Utility. Show "Enabled" and "Favorites" as filters at the top,
  not counters next to every category.
- **Search:** ctrl+F or just start typing to focus it, fuzzy matching on names, descriptions and
  **setting names** (typing "outline colour" finds Block Outline → Colour), results highlighted,
  Enter toggles the top result, Esc clears.
- **Detail panel:** module name, icon, About paragraph, all settings grouped into sections, a
  live preview where it helps (crosshair, HUD element, hit colour, block outline), a
  "Reset module" link and the keybind for toggling it.
- **Keyboard first:** arrows to move, Space to toggle, Enter to open settings, Tab through
  controls, every control has a visible focus ring. The menu keybind (Right Shift by default)
  opens/closes instantly, with no frame hitch on open (measure it).
- **Background:** the current blur is fine but add a darker, slightly desaturated scrim so text is
  readable over bright worlds. One blur pass per frame, as now.
- Kill everything that looks like a template: no gradient borders, no glow, no emojis, no random
  coloured tiles.

### Settings controls (one consistent family)
Slider (with an editable value field and fine adjustment while holding Shift), segmented
control for 2-4 options, dropdown for longer lists, colour picker (HSV + hex + alpha + saved
swatches + eyedropper from the HUD preview), keybind chip (shows "Press a key…", Esc cancels,
Backspace clears, conflict warning when a key is already used), text input, and a toggle row.
All of them get hover, pressed, focused and disabled states and look identical at every GUI scale.

### HUD editor
- Drag with snapping to screen edges, the centre lines and other elements (show guide lines).
- Multi-select and align/distribute, arrow keys to nudge by 1 px, Shift + arrow for 10 px.
- Per-element scale, plus the global HUD scale that already exists. Anchor to a corner so the
  layout survives a resolution change.
- Undo/redo (Ctrl+Z / Ctrl+Y), "Reset position", and a grid toggle.
- Clicking an element opens its settings in a side panel, without leaving the editor.
- **Layout presets:** "Crystal PvP minimal", "Crystal PvP full", "Streamer" (clean for
  recording) and saving your own.

---

## Part 3: Icons (fix them properly)
- Throw out the mixed set. Use **one** open-source vector icon family (Lucide or Phosphor,
  check the licence, credit it in README) with one stroke width and optical size across every
  icon. No Minecraft item sprites as module icons in the menu.
- Rasterise the SVGs at build time (or ship them at 4x the largest size) with linear filtering and
  mipmaps so they are crisp at GUI scale 1 through 4 and on 4K screens. Tint them with the theme
  colours (secondary text colour when off, accent when on).
- Every module and category gets an icon that actually means its function (e.g. a gem/shard
  shape for the crystal modules, an anchor for Anchor Optimizer, a shield, a heart, a crosshair,
  a gauge for FPS, a signal for ping).
- The in-HUD elements (armor, totems, potions) **should** still show the real item textures since
  that's game information, but rendered at clean integer scales with no blur.
- Redo the Shard logo mark in the rail and the mod icon (`assets/shard/icon.png`) so they match
  the launcher's branding.

## Part 4: Text that is actually sharp
- The current font JSONs use one TTF size with `"oversample": 2.0`, which gets upscaled at
  higher GUI scales and looks pixely/soft. Fix the root cause: render glyphs at the real on-screen
  pixel size for the current GUI scale and window size (rebuild the font atlas when either changes),
  or implement an SDF/MSDF text renderer. Pick one, benchmark it and justify it in DECISIONS.md.
- Snap text positions to whole pixels; no fractional offsets that blur glyphs.
- Use tabular numbers in every HUD value (FPS, ping, CPS, coords, counts, timers).
- Optional text shadow/outline styles for the HUD that look clean (soft 1 px shadow by default,
  not the vanilla heavy offset shadow).
- Prove it: 4x zoom crops of menu text and HUD text at GUI scale 1, 2, 3, 4 and Auto, at
  1920x1080 and 2560x1440, put side by side with 0.3.0 in docs/screenshots/.

---

## Part 5: Modules: built for crystal PvP

### 5a. Audit first
Write a table in DECISIONS.md of every existing module: what it does, what's weak (missing
options, ugly HUD, bad defaults, poor name), and what you'll change. Rename modules to short,
clear names that fit (e.g. "Coordinates" → "Coords", "Server Address" → "Server",
"Session Stats" → "Session", "Potion Effects" → "Effects", "Hit Delay Indicator" →
"Attack Cooldown").

### 5b. Upgrade the existing ones
- **Crystal Optimizer / Anchor Optimizer:** keep them server-authoritative. Add a clear explanation
  in the About text, a ping-aware status readout, and a debug overlay (off by default) showing
  predicted vs confirmed so pros can trust it. Measure again on a 0 ms and a simulated 150 ms
  setup.
- **Totem Pop:** per-player pop counter shown next to nametags and in a compact HUD list,
  chat message styles (minimal/detailed/off), optional sound, pop counts reset on death, and
  a "pops this fight" vs "pops this session" choice. Totem animation scale/duration/opacity, or off.
- **Totem Counter:** big clean count with an offhand indicator, colour change at a threshold
  (e.g. ≤2 = red), optional low-totem warning sound/flash.
- **Armor Status:** durability as bars or %, per-piece warning thresholds, flash when a piece is
  below X%, horizontal/vertical layouts, show the elytra too.
- **Item Counter:** a configurable list (defaults: end crystals, obsidian, respawn anchors,
  glowstone, totems, golden apples, XP bottles, ender pearls) with icons, compact layout,
  warning thresholds per item.
- **Keystrokes:** styles (minimal, outline, filled), CPS under LMB/RMB, a jump/space bar, key press
  fade animation, custom colours.
- **CPS:** left/right/both, a small graph option.
- **Ping:** colour by value, a small ping graph over the last 30 s, and a spike indicator.
- **FPS:** current, 1% low, and frame-time graph option (pros care about stutters, not averages).
- **Effects:** icons + timers, blinking under 10 s, sorted by time left, compact mode.
- **Attack Cooldown:** a clean bar or ring under the crosshair, colour when fully charged.
- **Crosshair:** presets, a pixel editor, dynamic colour when targeting an entity/crystal
  (vanilla-visible info only), outline, gap, thickness, a dot, hide in third person/F1/GUI.
- **Nametags:** show health (from vanilla entity data the server sends), pop count, armor
  durability summary, ping (from the tab list), scale, background opacity, through-walls **off**
  (vanilla behaviour only).
- **Hitboxes, Hit Color, Low Fire, Low Shield, Fullbright, No Hurt Cam, Zoom, Toggle Sprint,
  Death Animation, Explosion Optimizer, Clock, Memory, Server, Session:** review defaults,
  options and naming, and make their HUD styles match the new design.

### 5c. New modules for pro crystal PvP (all legit, all must pass the rule)
Research what top crystal PvP players use (Lunar, Feather, Badlion, popular Fabric PvP mods on
Modrinth, and what pro players run in their videos) and build the best ones. Candidates:

**Combat feedback (visual/info only)**
- **Target HUD:** the player you last hit or are looking at: name, head, health, absorption,
  armor durability summary, totem pops, distance. Only vanilla-visible data. Fades out after
  N seconds out of combat.
- **Combo / Hit counter:** hits in a row, resets when you get hit.
- **Reach Display:** distance of your last hit, calculated from positions the client already
  has. Display only.
- **Kill / Death tracking:** kills, deaths, K/D for the session, kill streak; optional kill
  effects (client-side particles/sound, visible only to you).
- **Fight Recap:** after a death or kill, a small summary: damage taken by type, pops on both
  sides, crystals placed, fight duration. Toggleable, auto-hides.
- **Low Health Warning:** vignette/pulse when under X hearts (configurable), optional sound.
- **Damage Tint / Hurt Flash:** a cleaner replacement for the red hurt overlay.
- **Pop / Crystal sounds:** custom volume per sound (crystal explode, totem pop, anchor
  explode, hit), useful to hear pops over explosions.

**Crystal visuals**
- **Crystal Tweaks** (exists, extend it): crystal scale, spin speed, bounce on/off, render
  the base on/off, colour tint, render distance cap.
- **Explosion visuals:** particle amount slider, smaller explosion particles, flash on/off.
- **Block Outline:** colour, thickness, fill opacity, rounded/sharp, "highlight obsidian/
  bedrock" style option (looked-at block only, no scanning).
- **Placement Highlight:** when looking at obsidian/bedrock, show a subtle overlay if a crystal
  could legally go there (the block above is air and there are no entities in the way, using the
  client's own visible data). Visual only, never places anything. Make it a per-server rule in
  case a server dislikes it.
- **Hand/Item view:** low/custom hand position, item scale, swing animation speed (visual only,
  doesn't change attack timing), hide offhand, low offhand totem.
- **No View Bobbing per-item** / **Static FOV** (no FOV change from speed effects/sprinting).
- **Clean Screen:** remove pumpkin overlay, powder snow, nausea/portal wobble (where purely
  visual), fire overlay (Low Fire), totem overlay size.
- **Hit Particles:** multiplier, always show crit/sharpness particles.
- **Custom Sky / Time / Weather** (client-side only), plus **Motion Blur** (shader, off by default).

**HUD utilities**
- **Cooldowns:** ender pearl, wind charge, shield, chorus fruit cooldowns from the vanilla item
  cooldown data, as a compact strip.
- **Hotbar upgrades:** durability bars on items, item counts for stacked crystals/obsidian
  shown clearly, slot numbers, a clean hotbar skin.
- **Direction / Compass** strip, **Speed** (blocks/s), **TPS estimate**, **Packet-loss/lag
  indicator** (from timing the client already measures; no extra packets).
- **Scoreboard / Bossbar / Tab list restyle:** scale, opacity, hide numbers, show ping as
  numbers in the tab list.

**Chat**
- Timestamps, compact repeated messages (×3), copy a message by click, chat background opacity,
  hide join/leave spam, highlight your name and keywords, filter pop/kill messages into a
  separate tab.
- **Auto GG:** only as an option that sends a message the player configures after a kill. Default
  off, disabled on servers whose rules say so (server-rule system), rate-limited. Write in
  DECISIONS.md whether this is in or out.

**Quality of life**
- **Profiles per server** (already partly exists): auto-switch the HUD layout/module set when
  joining a specific server.
- **Screenshot tools:** copy to clipboard, open folder, toast with preview.
- **Freelook:** only behind a per-server rule, default off, since many servers ban it.
- **Waypoints:** visual only, no through-wall rendering of anything other than the user's own
  waypoint markers.
- **Pro preset ("Crystal PvP Pro")**: one-click setup with the best defaults: minimal HUD (FPS
  with 1% low, ping, totems, armor, item counter for crystals/obi/anchors/glowstone/totems/gaps/xp,
  attack cooldown, target HUD, pops), clean screen, low fire, low shield, crystal optimizer
  on, tuned crystal visuals and a clean crosshair. Also a "Minimal" and a "Recording" preset.
- **First-run welcome:** a 3-step setup on first open: pick a preset, pick an accent colour,
  set the menu key. Skippable.

**Performance (measured, not guessed)**
- Profile a heavy crystal fight (the 100-crystal benchmark plus many particles and players) and
  fix the real hotspots: entity culling for crystals/items, particle limits during explosions,
  avoiding allocations in HUD rendering every frame, caching text widths and layouts.
- A **Performance** page with a simple FPS-first preset, and a frame-time overlay to prove it.
- Target: the menu open/close and every HUD element add < 0.2 ms per frame combined; no GC spikes
  in a 2-minute fight. Record before/after in BENCHMARKS.md. If a change doesn't move the numbers,
  don't ship it.

### 5d. MUST-HAVE list from me (these are required, not suggestions)
Build every item in this section. Each one gets a live preview in its detail panel so you
can see the change before closing the menu.

**Low Fire (rebuild it, much better)**
- First-person fire overlay: height slider (0-100%), opacity slider, **colour/tint picker**,
  and an "off" option. Separate settings for normal fire and soul fire.
- **Fire on the ground:** make fire *blocks* in the world lower and/or see-through: height
  slider, opacity slider, colour tint, separate for fire and soul fire. Visual only; the block
  and its hitbox don't change. Crystal fights are full of fire, so this matters a lot.
- Fire on other entities (players burning): lower/transparent/tint, separate option.
- Live preview in the detail panel showing the overlay and a fire block at the current settings.

**Crosshair (with a real preview)**
- Big **live preview** in the detail panel, drawn on a sample game background (switch
  between sky, grass, stone, nether and end so you can check visibility), updating while you
  drag sliders.
- Presets (dot, plus, gap plus, circle, T-shape, vanilla) plus a pixel editor (grid you click to
  draw your own), size, thickness, gap, outline colour/thickness, opacity, colour per state
  (normal, looking at a player, looking at a crystal, attack ready), and **import/export as a
  short code** so players can share crosshairs.
- Option to hide the vanilla crosshair blend/inverted look, and hide in third person and F1.

**Shield (skinnier + smaller, always)**
- Make the held shield **thinner/skinnier** (width slider) and **smaller** (scale slider) and move
  it (X/Y/Z position, rotation) so it blocks less of the screen.
- Two sets of settings: **while blocking** and **while just holding it, not blocking**. The shield
  is smaller even when you aren't using it.
- Opacity slider and an option to lower it in the offhand only. Visual only; blocking and
  hitboxes stay vanilla. Replace or merge with the existing Low Shield module.
- Live preview showing the first-person shield in both states.

**Anchor Glow / Anchor visuals**
- Respawn anchors you can see get a **glow/outline and colour by charge level** (0, 1, 2, 3, 4
  charges each with its own colour you pick), so you can see at a glance which anchors are loaded.
- Options: outline, fill, glow strength, pulse when fully charged, colour picker per charge.
- **Only anchors in normal line of sight**: no through-wall rendering, no scanning hidden blocks.
  Uses the block state the client already has. Add a per-server rule toggle.

**Crystal visuals (colours + glow)**
- End crystal **colour/tint picker** (core and outer frame separately), glow/outline option,
  opacity, scale, spin speed, bounce on/off, base on/off.
- Optional outline colour when your crosshair is on a crystal. Only crystals you can see
  normally, never through walls.
- Live preview of a spinning crystal in the detail panel.

**Separate GUI scales (scale things independently)**
- **Inventory Scale:** a separate GUI scale only for inventory and container screens (your
  inventory, chests, shulkers, ender chest, crafting, anvil). E.g. play with GUI scale 2 for the
  HUD but open the inventory at scale 3 so it's big and fast to click when refilling totems.
- The same idea for: **hotbar scale**, **chat scale**, **scoreboard scale**, **tab list scale**,
  **title/action bar scale**, **boss bar scale** and **Shard menu scale**, each "Auto / follow
  game / 1-6" plus a fine slider where possible.
- Item tooltips follow the inventory scale. Mouse clicks must land exactly where items are drawn
  at every combination (test this carefully; write tests for the coordinate maths).

**Display & window**
- **Borderless fullscreen** (windowed fullscreen): no border, covers the monitor, instant
  alt-tab with no black screen or resolution switch. Toggle with the vanilla fullscreen key (F11)
  if the user picks "Borderless" as the fullscreen mode. Choose which monitor on multi-monitor
  setups. Make sure it works on Windows 10/11 with NVIDIA and AMD, and doesn't break
  exclusive fullscreen for people who want it.
- **Fullscreen mode** dropdown: Windowed / Borderless / Exclusive (with resolution and
  refresh-rate picker).
- **Unfocused FPS limit** (e.g. 30 when alt-tabbed) and **Menu FPS limit**, to keep the PC cool.
- **Custom window title** (e.g. "Shard Client 1.21.11"), with the option to hide the server
  name for streamers.
- **Raw mouse input** shortcut and a note about the vanilla option; **VSync / frame-cap** shortcuts
  in the Performance page.

**HUD element looks (every module looks good, not just works)**
- Every HUD module gets a **style picker** with 3-4 clean styles (e.g. Minimal text, Pill,
  Card, Outline), using the shared style group: background colour/opacity, text colour,
  accent colour, corner radius, padding, shadow on/off, brackets on/off, label on/off, and
  "show label" text (e.g. "FPS: 240" vs "240 FPS" vs "240").
- A **"match all"** button that copies one element's style to every HUD element.
- Previews of each style in the picker, rendered at real size.

For each new module: About paragraph, sensible defaults (most new modules default **off**
except what the Pro preset turns on), icon, search keywords, config migration (bump the config
schema, migrate 0.3.0 files without losing settings) and JUnit tests for any logic. Record
everything you considered and rejected (with the reason) in DECISIONS.md.

---

## Part 6: Clean code and quality bar
- Keep the pure-Java core testable without Minecraft (as now). New logic (pop tracking, combo
  counting, reach maths, recap aggregation, search matching, snapping, undo stack, config
  migration) gets JUnit tests.
- One theme/token source; no hard-coded colours or sizes in screens.
- No per-frame allocations in render paths; no new mixins where an event or existing hook works.
- Compatibility: still works next to Sodium, Iris, Lithium, Entity Culling, ImmediatelyFast
  and the other bundled mods in the launcher. Update the conflict detection if a new module
  overlaps another mod.
- Nothing breaks the existing smoke test; extend it for the new screens.

## Part 7: Verification (do it all, actually look at the results)
1. `./gradlew build`: all tests pass.
2. The smoke flow from STATUS.md: screenshot the main menu (grid and list view), search with
   results, the detail panel, every settings control, the colour picker, the keybind chip, the
   HUD editor with guides, each HUD preset and each new HUD element at GUI scale 1, 2, 3, 4 and
   Auto, at 1280x720, 1920x1080 and 2560x1440. Keep `layoutIdenticalAcrossScales: true`.
3. **Check every screenshot yourself:** no "…" on any module name, nothing off-screen, no
   mismatched icons, text crisp in 4x crops, readable on both a dark and a bright background.
4. Must-have checks: screenshots of ground fire and overlay fire at several height/opacity/colour
   settings; the crosshair preview on each background; the shield blocking and not blocking,
   before and after; anchors at 0-4 charges with glow; tinted crystals; the inventory at a
   different scale from the HUD with a click-accuracy test (click every slot, confirm the right
   slot is picked); borderless fullscreen on, alt-tab out and back, F11 toggle, no black flash.
5. Benchmark: the 100-crystal scenario plus a menu open/close test, no regression vs 0.3.0, and
   show the improvements in BENCHMARKS.md.
6. Before/after screenshots in `docs/screenshots/0.4.0/`, including the same view as
   `0.3.0-gui-owner-feedback.png` so I can compare directly.
7. Update STATUS.md, DECISIONS.md, README.md and the changelog. Bump to 0.4.0 and build the jar.
   **Ask me before** creating a GitHub release or pushing the manifest to meta.

## How to work
Go in this order and stop for my feedback at each ★:
1. Design system (DESIGN.md) + 2-3 direction mockups. ★
2. Sharp text + new icon set (screenshots of 4x crops). ★
3. New main menu, detail panel and controls. ★
4. HUD editor + presets.
5. Module audit + upgrades to existing modules. ★
6. Everything in 5d (my must-have list): Low Fire, Crosshair preview, Shield, Anchor Glow,
   Crystal visuals, separate GUI scales, borderless fullscreen, HUD styles. ★
7. Other new crystal PvP modules + performance work.
8. Full verification, docs, version bump. ★ (final review before release)

Use subagents for the research parts if it helps. Don't stop until each step is
verified with screenshots you have actually looked at.
