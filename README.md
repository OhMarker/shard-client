# Shard Client

**Sharpen your crystal PvP.** A legit, server-safe Fabric client for Minecraft 1.21.11: a click
GUI, HUD and visual modules tuned for crystal fights, and performance tweaks that are measured
rather than assumed. It is the client half of Shard; the launcher lives at
[OhMarker/shard-launcher](https://github.com/OhMarker/shard-launcher) and defines the data
contract (`CONTRACT.md` there).

## Fair-play stance

Shard never automates combat or inventory, never changes movement or timing, never sends packets
the vanilla client would not send, and never reveals information the vanilla client hides. Every
feature is visual, informational, cosmetic or performance-related. Features that some servers
still ban (Freelook, toggle-sprint on a few anticheats) carry a per-server blacklist so they turn
themselves off there.

## Modules (first release)

| Category | Modules |
| --- | --- |
| HUD | FPS, Ping, Coordinates, CPS, Keystrokes, Armor Status, Totem Counter, Potion Effects, Item Counter, Server Address, Session Stats, Clock, Memory, Hit Delay |
| Visuals | No Hurt Cam, Particle Multiplier, Crystal Size, Zoom, Pop Messages |
| Combat QoL | Toggle Sprint |
| Performance | No Totem Animation |

Open the menu with **Right Shift**. Left-click a module toggles it, right-click expands its
settings, middle-click sets a keybind. Type `/` or Ctrl+F to search. The HUD editor (button in the
menu, or `.hud` in chat) lets you drag, scale, snap and nudge every HUD element.

Chat commands use the `.` prefix and never reach the server: `.toggle <module>`,
`.bind <module> <key|none>`, `.set <module> <setting> <value>`, `.reset <module>`,
`.config save|load|list [name]`, `.gui`, `.hud`, `.list`, `.help`.

## Launcher integration

- Reads `launcher-info.json` from the game directory on start: the GUI takes the launcher's accent
  colour and theme; the launcher version and instance are logged. Without the file everything
  falls back to defaults, so the mod also works from any other launcher.
- Config lives in `config/shard/` so the launcher's shared-config layer can sync it across versions.
- Cosmetics (`equipped.json`) and the Shard-client hosted manifest are the next milestones; see
  "Roadmap".

## Building

```bash
./gradlew build          # build/libs/shard-<version>.jar plus its .sha512
./gradlew test           # JUnit tests for settings, config, launcher-info, keys, colours
./gradlew runClient      # dev client with Fabric API, Mod Menu, Sodium, Iris, Lithium, Cloth Config
```

Requires JDK 21 or newer. Versions of Minecraft, Fabric and the companion mods are pinned in
`gradle.properties`.

### In-game verification

The dev client has a headless smoke test. Start an offline-mode server (the repo's
`.smoke-server` folder shows the configuration used), then:

```bash
./gradlew runClient -PquickPlay=localhost:25599 -PsmokeDir="$PWD/smoke-out"
```

The client skips onboarding, connects, waits for the world, writes `smoke-hud.png`,
`smoke-gui.png` and `smoke-editor.png`, and quits. The hook only exists in development builds.

## Releasing to the launcher

1. Bump `modVersion` in `gradle.properties` and run `./gradlew build`.
2. Upload `build/libs/shard-<version>.jar` to a GitHub release of this repository.
3. Add a build entry to `shard-manifest.json` in the `meta` repository with the jar URL, the
   sha512 from `build/libs/shard-<version>.jar.sha512`, `"minecraft": ["1.21.11"]` and a
   changelog, then bump `latest`. Every launcher installs it on the next launch.

## Roadmap

- Cosmetics rendering (capes, cloaks, wings, hats, bandanas, back-bling) from the launcher's
  `equipped.json`, and the emote wheel.
- Remaining visual modules: fire overlay lowering, fullbright, hit colour, nametag and
  scoreboard restyles, crosshair customiser, motion blur, weather/time changer, item physics.
- Per-server blacklist UI and profiles per gamemode.
- Benchmarked optimizers beyond particles and the totem animation (see `BENCHMARKS.md`).
