# Status (handoff for a new session)

Last updated 2026-10-07.

- Fabric mod for Minecraft 1.21.11 (Loader 0.19.5, Fabric API 0.141.6, Mojang official mappings,
  Loom 1.18.2, Gradle 9.8, JDK 25 on this machine compiling for 21).
- First release complete: click GUI (Right Shift), HUD framework + editor, 21 legit modules,
  settings/config/profiles, `.` chat commands, launcher-info integration, Mod Menu entry.
  Module list and roadmap in README.md; design notes in DECISIONS.md; benchmark method in
  BENCHMARKS.md (numbers not yet measured).
- Verified: `./gradlew build` passes with 20 JUnit tests; in-game smoke test (dev client joined a
  local offline 1.21.11 server with Sodium/Iris/Lithium/Cloth/Mod Menu, screenshotted HUD, GUI
  and HUD editor, no mixin errors). Screenshots in smoke-out/.
- Jar: build/libs/shard-0.1.0.jar (+ .sha512, value
  62e7dec1d0db6bba64e7688e4e01a29185831e7aa1274f3960f594698c9fa8de95643c273918aaa9729ff2a8715cc3e0d552a566334d4b84609a12644fc0fe67).
  A copy was placed on the owner's Desktop (C:\Users\OhMar\OneDrive\Desktop) for the Modrinth App
  profile "1.21.11 9_11".
- Git: two local commits on `main`; NOT pushed yet. Claude's sandbox is not allowed to create
  public repositories, so the owner runs the publish steps below. The hosted manifest prepared
  in ../meta already points at the v0.1.0 release URL that these steps create.

## Publish (owner runs once, from this folder)

```bash
gh repo create OhMarker/shard-client --public --source=. --remote=origin --push --description "Shard Client: a legit, server-safe Fabric client for crystal PvP (Minecraft 1.21.11)"
```

```bash
gh release create v0.1.0 build/libs/shard-0.1.0.jar build/libs/shard-0.1.0.jar.sha512 --title "Shard Client 0.1.0" --notes "First public build for Minecraft 1.21.11. Click GUI (Right Shift), HUD editor and 21 legit modules. Requires Fabric Loader 0.19+ and Fabric API; Shard Launcher installs it automatically."
```

The jar URL must stay `https://github.com/OhMarker/shard-client/releases/download/v0.1.0/shard-0.1.0.jar`
(that is what ../meta/shard-manifest.json references). Then publish ../meta (see its STATUS.md).

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

## Releasing a new client version later
1. Bump `modVersion` in gradle.properties, `./gradlew build`, read build/libs/shard-<v>.jar.sha512.
2. `gh release create v<v> build/libs/shard-<v>.jar build/libs/shard-<v>.jar.sha512 --title ...`
3. In OhMarker/meta, add a build object to shard-manifest.json (url, sha512, minecraft list,
   changelog, releasedAt), bump `latest`, commit and push. Every launcher picks it up on the next
   launch; no launcher release is needed.

## Next steps the owner may ask for
- Roadmap items in README.md (cosmetics rendering + emote wheel, remaining visual modules,
  server blacklist UI, benchmark numbers).
- Later versions: add 1.21.x / 26.x targets (owner wants 1.21.11 first, others afterwards).
