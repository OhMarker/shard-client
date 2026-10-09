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
   space-separated and all must match: `>=1.21.6 <26.1`). Remove the placeholder
   `ExampleOnlyOnSomeVersions` entry once a real one exists.
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

## Version notes
Record every non-obvious difference here (what changed, which version, how it was handled).
