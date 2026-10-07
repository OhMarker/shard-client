# Status (handoff for a new session)

Last updated 2026-10-07 by the session that built the project.

- Fabric mod for Minecraft 1.21.11 (Loader 0.19.5, Fabric API 0.141.6, Mojang official mappings,
  Loom 1.18.2, Gradle 9.8, JDK 25 on this machine compiling for 21).
- First release complete: click GUI (Right Shift), HUD framework + editor, 21 legit modules,
  settings/config/profiles, `.` chat commands, launcher-info integration, Mod Menu entry.
  Module list and roadmap in README.md; design notes in DECISIONS.md; benchmark method in
  BENCHMARKS.md (numbers not yet measured).
- Verified: `./gradlew build` passes with 20 JUnit tests; in-game smoke test (dev client joined a
  local offline 1.21.11 server with Sodium/Iris/Lithium/Cloth/Mod Menu, screenshotted HUD, GUI
  and HUD editor, no mixin errors). Screenshots in smoke-out/.
- Jar: build/libs/shard-0.1.0.jar (+ .sha512). A copy was placed on the owner's Desktop
  (C:\Users\OhMar\OneDrive\Desktop) for use in the Modrinth App profile "1.21.11 9_11".
- Git: one local commit on `main`; NOT pushed yet. Intended remote: OhMarker/shard-client.

## Fair-play rule
Only HUD, visuals, vanilla-legal input helpers and measured performance tweaks. No combat or
inventory automation, no movement/timing changes, no extra packets, no hidden-info rendering.
The owner explicitly rejected hacked-client modules.

## How to verify changes
1. Start the offline server: `cd .smoke-server && java -Xmx2G -jar server.jar nogui`
   (server.properties has online-mode=false, port 25599).
2. `./gradlew runClient -PquickPlay=localhost:25599 -PsmokeDir="$PWD/smoke-out"` joins, screenshots
   smoke-hud/gui/editor.png and quits. Delete run/config/shard/config.json first to test defaults.
3. `./gradlew build` for the jar + tests.

## Next steps the owner may ask for
- Push to GitHub; publish shard-manifest.json in OhMarker/meta so the launcher installs it.
- Roadmap items in README.md (cosmetics rendering + emote wheel, remaining visual modules,
  server blacklist UI, benchmark numbers).
- Later versions: add 1.21.x / 26.x targets (owner wants 1.21.11 first, others afterwards).
