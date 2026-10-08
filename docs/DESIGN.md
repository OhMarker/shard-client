# Shard design system (0.4.0)

The menu and HUD are tools for competitive crystal PvP players. They should feel quiet and
precise, like a good esports utility or Linear/Raycast, not like a gaming RGB menu or a
template. Every rule below exists because of something wrong in
`screenshots/0.3.0-gui-owner-feedback.png`.

All sizes are **design units** (see `gui/Scale.java`): one unit is 2 physical pixels at 100%
interface size, so 1920x1080 is 960x540 units and the page looks the same at every GUI scale.

## Personality
- **Quiet by default, loud only for state.** Neutral graphite surfaces; the accent marks what is
  on, selected or focused, and nothing else.
- **Readable at a glance.** Names are never cut off. Hierarchy comes from size, weight and
  contrast, not from boxes around everything.
- **Dense where pros want density.** List view is a first-class view, not an afterthought.
- **No template tells:** no glow, no gradient borders, no coloured icon tiles, no emoji, no
  counters next to every label, no identical heavy cards in a grid.

## Colour tokens
Surfaces are near-neutral (a hint of blue, not navy) so any accent works on them.

| Token | Value | Use |
|---|---|---|
| `scrim` | `#0A0B0E` at 72% | over the blurred world, under everything (darker than 0.3.0 so text reads over snow and sky) |
| `surface` | `#111317` at 96% | rail, detail panel |
| `surface-raised` | `#171A1F` | rows, cards, inputs |
| `surface-hover` | `#1D2026` | hover |
| `surface-active` | `#22262D` | pressed / selected row |
| `line` | white 7% | dividers, 1 unit |
| `line-strong` | white 14% | input borders, focus-less outlines |
| `text` | `#ECEEF2` | names, values (15.8:1 on surface) |
| `text-secondary` | `#A3A9B5` | descriptions (7.4:1 on surface, readable over bright worlds) |
| `text-tertiary` | `#6B717D` | captions, disabled (3.9:1, never for anything you must read) |
| `accent` | user pick, default `#3DD6F5` (Shard cyan) | on-state, selection, focus ring |
| `accent-muted` | accent at 14% | selected row fill, enabled edge |
| `success` / `warning` / `danger` | `#4ADE80` / `#FACC15` / `#F87171` | ping, durability, totems, low health |

Accent presets in Appearance: Shard cyan, Mono (white), Crimson `#F43F5E`, Violet `#A78BFA`,
Lime `#A3E635`, Gold `#FBBF24`, plus Custom. Text on an accent fill uses black or white by
contrast (`Colors.contrastText`).

## Type
Inter 4.1 (already bundled). At most three sizes on one screen.

| Role | Size / weight | Line |
|---|---|---|
| Page title | 18 semibold | 22 |
| Section label | 10 medium, `text-tertiary`, letter-spaced caps | 14 |
| Row / card title | 12 semibold | 16 |
| Body / description | 11 regular, `text-secondary` | 15 |
| Caption, values | 10 medium | 13 |
| HUD | 10 medium (scale from HUD settings) | 12 |

Text must be rasterised at the real on-screen pixel size (see "Sharp text" in DECISIONS.md
0.4.0), positioned on whole physical pixels, and numbers in the HUD use fixed-width digit
slots so values never jitter.

## Spacing, shape, elevation
- Spacing scale: 2, 4, 8, 12, 16, 24, 32. Rows have 12 horizontal and 8 vertical padding;
  panels have 16; sections are 24 apart.
- Radii: 4 (chips, toggles' track ends are full), 8 (rows, inputs, cards), 12 (rail, panel,
  popovers). Nothing else.
- Elevation: level 0 flat; level 1 (popovers, detail panel) = 1-unit `line` border + soft
  shadow `#000` 35%, 12 blur, 4 down. No other shadows.

## Motion
- Hover 120 ms linear. Toggle knob and fill 180 ms ease-out. Panel slide / screen open 220 ms
  ease-out (fade + 0.98 → 1.0 scale). Lists scroll with momentum and fade 12 units at the edges.
- Nothing bounces or overshoots. Settings → Appearance → Reduce motion snaps all of it.

## Iconography
- One family: **Lucide** (ISC licence), 1.75 stroke at 24 px, drawn at 16 units in rows and 14
  in the rail. Rasterised from SVG at build time to 64 px and 128 px mip levels, linear
  filtered, tinted at draw time: `text-secondary` when off, `accent` when on/selected.
- No Minecraft item sprites in the menu. HUD elements still show real item textures (that is
  game information), drawn at integer scales.
- Each icon means the function: gem for crystal modules, anchor for Anchor Optimizer, shield,
  flame, crosshair, gauge (FPS), signal (ping), keyboard, mouse-pointer, heart, timer, etc.

## Controls (one family)
- **Toggle:** 26x14 track, full radius. Off: `surface-active` track, `text-tertiary` knob.
  On: `accent` track, knob in `accent-text`. Never the brightest thing in a row.
- **Slider:** 2-unit track, 10-unit knob, value in an editable field at the right; Shift = fine.
- **Segmented control** for 2-4 options; **dropdown** for more.
- **Colour:** swatch opens HSV + hex + alpha + saved swatches.
- **Keybind chip:** monospace-ish label, "Press a key…", Esc cancels, Backspace clears,
  conflict shown in `warning`.
- Every control: hover, pressed, focused (2-unit accent ring, keyboard only), disabled.

## Layout of the menu
See the chosen mockup in `design/`. Rules any direction must follow:
- Content never leaves the window, from 1280x720 to 3840x2160, at any GUI scale.
- Full module names always fit; descriptions wrap or move to the detail panel, never "…" on a name.
- Grid and List views; List is the dense pro view.
- Search focuses on any typed letter or Ctrl+F; matches names, descriptions and setting names.
- Keyboard: arrows move, Space toggles, Enter opens, Esc backs out.
