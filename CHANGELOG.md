# Changelog

## 0.10.0

- **Shard on more Minecraft versions.** The same client, with every module, menu, HUD element and
  cosmetic, now also runs on Minecraft 26.3, 26.2, 26.1, 26.1.1, 26.1.2, 1.21.10, 1.21.9, 1.21.8, 1.21.7 and 1.21.6. More versions follow in this release
  line; Shard Launcher installs the right build for each instance automatically.

## 0.9.1

- Borderless fullscreen no longer hides the mouse cursor in menus: the window is one pixel taller
  than the screen so Windows does not treat it as exclusive fullscreen, and the pointer is forced
  visible whenever a menu is open.

## 0.9.0

- **OhMarker set: shield and bandana.** Two new cosmetics next to the cape, bought and equipped in
  Shard Launcher, visible to every Shard player:
  - **Shield:** every shield you hold uses the OhMarker shield art (riveted steel frame, ringed moon),
    in first person and for everyone else in third person, in either hand, blocking or not, even
    over banner patterns. It is smooth at any distance (mipmapped like the capes), and the Shield
    module's tint and see-through setting still apply on top.
  - **Bandana:** a night-sky bandana tied over your head, with the ringed-moon emblem on top, the
    moon-and-star border along its edges, the lower half of the art at the back and a knot with two
    short tails. It follows your head and sneaking, and hides while you wear a helmet or a head item.
- **Cosmetics settings:** "Show my shield" and "Show my bandana"; "Show Shard capes" is now
  "Show other players' cosmetics" (your old choice carries over).
- The client reads the new cosmetics-v2.json catalogue (falls back to cosmetics.json) and every
  equipped slot from the Shard API.

## 0.8.1

- Account switcher: a clear message when Shard Launcher has been closed (Shard Launcher 0.4.1 now
  stays in the tray while the game runs, so switching keeps working).
- Credits now read "Made by OhMarker with the help of SwxyzX2".

## 0.8.0

- **New title screen and server list** in Shard's look (switch back to vanilla in Settings → Menus).
  The server list keeps everything vanilla does: ping, players, LAN, add/edit/delete, direct connect.
- **In-game account switcher** on both screens: pick any account signed in to Shard Launcher, or add
  one. Needs Shard Launcher 0.4.0; tokens stay in memory and are never written to disk.
- **Mod menu:** Shard logo, a Profiles tab for your own saved setups, credits ("Made by OhMarker with
  the help of SwxyzX2") and the MIT License in Settings → About.
- **Accent colour works again:** it colours switches, enabled mods, sliders and the main buttons.
- **Fixed settings you could not click:** the two- and three-option switches (like WASD / Arrows).
- **New menu options:** click sounds, mod descriptions on hover, open where you left off.

- **Faster start-up and resource pack changes:** the start-up resource reload went from about 10-12 s
  to about 1.5 s on the dev machine (fonts 4.2 s to 0.4 s), so the Mojang screen no longer hangs
  ("Not Responding"). Shard's fonts are now built the first time they are used.
- **No second reload at start-up:** the custom fire texture is no longer a resource pack ("Shard fire"
  is gone from Options > Resource Packs); switching it is instant and never reloads resources.
- **Sharp HUD at every HUD scale:** text is rasterised at exactly the size it is shown and placed on
  whole pixels, so small HUD scales no longer blur or lose thin strokes, and menu text is pixel-exact.
- **Small Items looks exactly like the Small Items mod by default** (everything in your hands, and
  your empty hand, at 60%); the old sliders are under Look: Custom.

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
