# Decisions

- **Legit only.** The first draft of the product brief listed hacked-client modules; the owner rejected them. Shard is a Lunar/Badlion-style client: HUD, visuals, vanilla-legal input helpers, performance. The fair-play rule in README.md is the acceptance test for every new module.
- **Mojang official mappings**, not Yarn: `ResourceLocation` is `Identifier` in 1.21.11 and the GUI stack uses `Matrix3x2fStack`; signatures were read from the mapped jar with `javap` before writing code, which is why the first compile passed.
- **Pure-Java core.** `Module`, settings, `ConfigManager`, `LauncherInfo`, `Keys`, `Colors` and `ClickTracker` have no Minecraft imports so they run under plain JUnit. Minecraft-facing work lives in `HudModule`, screens and mixins.
- **Settings drive everything.** Adding a `Setting` field to a module yields the GUI widget, config persistence and the `.set` chat command with no extra code.
- **Mixins check `ShardClient.isReady()`** because render mixins can fire before `onInitializeClient` finishes. Hooks whose vanilla method names are not compile-checked (`startAttack`, `startUseItem`, `renderItemActivationAnimation`) use `require = 0` so a mapping change degrades a feature instead of crashing the game.
- **Totem pops come from `EntityEvent.PROTECTED_FROM_DEATH`** (client entity event 35), the same signal vanilla uses for the animation, so the counter and messages are exact.
- **Particle thinning happens in `ParticleEngine.createParticle`** rather than per-particle-type mixins: one hook, every type, and returning `null` is a path vanilla already handles.
- **No Fabric `HudRenderCallback`.** 1.21.11 Fabric API routes HUD drawing through `HudElementRegistry`; Shard registers one layer and draws all HUD modules inside it so z-order and scaling stay under its control.
- **Two-row click GUI at large GUI scales.** When five panels do not fit side by side, panels wrap to a second row and each row's bodies scroll within half the screen, instead of shrinking panels until names truncate.
- **HUD positions are stored as fractions** of the GUI size so layouts survive window resizes and GUI-scale changes.
- **Dev-only smoke test.** `SmokeTest` (armed by `-PsmokeDir`, dev environment only) connects to an offline test server, screenshots the HUD, the click GUI and the HUD editor, and quits. It found the first-run accessibility onboarding screen blocking quick-play and the hotbar overlap of the default HUD positions.
- **Deferred:** cosmetics rendering and the emote wheel, remaining visual modules, per-server blacklist UI, and the benchmark table (method in BENCHMARKS.md). These ship after the owner reviews the first release.
