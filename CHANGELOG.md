# Changelog

## 0.7.1

- **Bigger mod menu:** 1200x725 on a 1080p screen (it grows on bigger screens and always fits the
  window). An older Interface size below 100% no longer shrinks it.
- **Crisp menu text at every size:** text rasters now come in quarter-pixel steps, so the menu's
  letters are always drawn 1:1 and never break up.
- **Inventory scale:** the player model now stays inside its black box.
- **Settings → Window:** borderless fullscreen, frame caps, window title, raw input and VSync moved
  here from the Display tile and are always in effect.
- **Small Items:** smaller items are lifted toward the screen so they stay visible.

## 0.7.0

A compact new mod menu, sharper text and a round of visual mods.

- **New mod menu:** one compact centred panel with Mods / Settings / Cosmetics / Friends tabs,
  search (Ctrl+F), category tabs (All, HUD, Visuals, Combat, Performance, Chat, Utility,
  Favorites, Enabled) and a six-column grid of mod tiles. Click a tile to toggle it; the gear or a
  right-click opens its settings in the same panel. Neutral greys, no glows or gradients.
- **Smooth scrolling** everywhere in the menu: the wheel glides instead of jumping.
- **Sharper text:** the menu's text is now rasterised at the exact screen size, so thin strokes no
  longer break up.
- **Removed Target HUD and Nametags** (health on name tags): many servers ban them.
- **Cosmetics are always on.** Their options moved to the Cosmetics tab.
- **Fire:** Low Fire is now "Fire" and ships a new animated fire texture (switchable).
- **Shield:** opacity and colour tint, with presets.
- **Hit Color** now tints armour too.
- **Small Items (new):** smaller held items, 50–100 % per hand, with position offsets.
- **Totem Pops:** animation size (25–150 %).
- **Crosshair:** pixel-perfect mode (true 1-pixel lines at any GUI scale), much wider ranges and
  five new styles (circle, circle and plus, square, X and dot, chevron).
- **Sky (new):** sky and horizon colour presets (Sunset, Night, Pastel, Ocean, Mint, Lavender,
  Cherry blossom, Golden hour) or your own colours, following day and night.
- **Hitboxes:** your own line colour, opacity and width, optional fill, target highlight, eye line
  and look direction.
- **Zoom:** frame-rate independent easing, gliding scroll-to-adjust and smooth sensitivity.
- **Entity Optimizer (new):** caps how many XP orbs, dropped items and stuck arrows are drawn,
  thins ender pearl particles and can cap new particles per tick. Nothing is removed from the world.

## 0.6.1

- Fixed signing in to Shard: Mojang blocks the Shard server's checks, so the game now proves your
  account with the key Mojang gives every account for chat signing. Tokens and capes work again.

## 0.6.0

- **Everyone's capes:** every Shard player now sees the capes other Shard players equipped.
- **Shard tokens:** earn 10 tokens for every 10 minutes you play (standing idle for five minutes
  pauses it) and spend them on capes in Shard Launcher. A small pop-up tells you when you earn
  some; the new Tokens HUD element shows your balance and the minutes to the next 10.
- Sign-in to Shard uses Mojang's own check, the same one servers use: your account details never
  reach Shard. Capes show on online-mode servers (offline-mode servers use different player ids).

## 0.5.0 (2026-10-08)

- **Cosmetics:** the cape you equip in Shard Launcher now shows in-game on your own player, in
  third person and on your elytra (switchable). High-resolution
  capes are drawn smooth and sharp, with no flicker. Only you see it; nothing is sent to the
  server.

## 0.4.0 (2026-10-08)

A new menu, sharp text, one icon set, a real HUD editor and the crystal PvP modules players
asked for.

- **New menu:** a rail with search, Favorites, Enabled and six categories (HUD, Visuals, Combat,
  Performance, Chat, Utility); a list or grid of modules where full names are never cut off; a
  detail column with every setting and live previews. Search finds settings too and jumps to
  them. Keyboard: arrows, Space, Enter, Esc.
- **Sharp text everywhere:** Inter is rasterised at the real on-screen size and filtered
  smoothly, so it stays crisp at any interface size, HUD scale or Windows display scaling.
- **One icon set:** Lucide icons replace the mix of pixel glyphs and item sprites.
- **HUD editor:** snapping with guide lines, multi-select, align and distribute, 1-pixel nudges,
  undo and redo, a grid, scaling with the scroll wheel, settings in a side panel and layout
  presets (Crystal PvP minimal, Crystal PvP full, Streamer, plus your own). HUD positions now
  keep their distance from the nearest edge at any window size or GUI scale.
- **HUD styles:** a fourth style (Pill), label before or after the value, optional brackets,
  live previews, and "use this look for every HUD element".
- **Better HUD elements:** FPS with 1% low and a graph, Ping spike warnings and a graph,
  Keystrokes with CPS and fades, Armor durability bars, Totem Counter warning and offhand dot,
  Item Counter low-stock colours, Effects with vanilla icons, Attack Cooldown colours, Totem
  Pops reset on death, Nametags ping, a Crystal Optimizer prediction readout.
- **New HUD elements:** Target HUD, Combo, Reach, Fight Recap, Cooldowns, Compass, Speed, TPS;
  Session now shows kills, K/D and streaks.
- **Low Fire** for your screen, fire blocks on the ground (also with Sodium) and burning
  entities, each with height, opacity and tint.
- **Crosshair:** pixel-mask shapes, an outline that follows any shape, a pixel editor, live
  previews on five backgrounds, share codes, dimming while the hit recharges.
- **Shield:** separate size and position while blocking and while holding.
- **Crystal Visuals** (colours, spin, bounce, base, aimed-crystal outline) and **Anchor Glow**
  (nearby anchors outlined by charge, never through walls).
- **GUI Scales:** the inventory at its own scale with exact clicks; separate scales for the
  hotbar, scoreboard, tab list, titles, boss bar and chat.
- **Display:** borderless fullscreen (also for F11), background and menu FPS caps, window title.
- **New modules:** Block Outline with a crystal-spot hint, Low Health Warning, Clean Screen,
  Weather and Time, Sounds, Chat (timestamps, stacked repeats, hidden joins, highlights).
- **Quick setup:** Crystal PvP Pro, Minimal or Recording in one click, offered by a short
  welcome the first time you join a world.
- **Faster HUD:** a busy HUD now costs 0.18 ms per frame instead of 0.31 ms (BENCHMARKS.md).
- Renamed modules keep their settings, keybinds, server rules and favorites; 0.3.0 configs
  load unchanged.

## 0.3.0 (2026-10-08)

- Smooth text: Shard's settings page and HUD use Inter, the launcher's font (Vanilla font is one
  click away).
- The settings page looks the same at every GUI scale, with roomier spacing, anti-aliased
  corners and an Interface size slider.
- HUD styles: Card, Minimal or Outlined, colours, background opacity, radius, padding, shadow,
  alignment and label text, with global defaults and a HUD scale.
- Settings: Appearance, Keybinds with conflict warnings, profile descriptions, a wildcard
  server-rule editor, Export/Import and Reset all.
- New options for Crosshair, Nametags, Hit Color, Low Fire, Crystal Optimizer, Totem Pop
  Tweaks, Explosion Optimizer, Zoom, Keystrokes and Item Counter.
- 0.2.0 configs migrate automatically.

## 0.2.0 (2026-10-07)

- Settings page: sidebar, mod cards with toggle switches, sliding settings panel,
  dropdowns, colour picker, keybind capture, keyboard navigation.
- Crystal Optimizer and Anchor Optimizer: instant client-side feedback, server stays
  authoritative.
- Explosion Optimizer, Totem Pop Tweaks, No Death Animation, Hit Color, Low Fire, Low Shield,
  Fullbright, Hitboxes, Nametags, Crosshair.
- Per-server rules ("Disable on <server>") and automatic conflict detection for installed mods.
