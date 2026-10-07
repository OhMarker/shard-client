# Benchmarks

Optimizers only stay in Shard if they show a measurable gain in a repeatable scenario. This file
records the method and the results. Anything listed as "not yet measured" ships as a visual
option, not as a performance claim.

## Scenario

- Flat world, offline-mode 1.21.11 server (`.smoke-server/server.properties`), creative mode.
- 100 end crystals placed on a 10x10 obsidian grid and detonated in one hit, repeated 5 times.
- Dev client with the launcher's bundled set (Sodium, Lithium, Iris, Entity Culling, FerriteCore,
  ImmediatelyFast, Krypton, Dynamic FPS) so gains are measured on top of them, not instead of them.
- Metric: 1% low frame time over the 3 seconds following each detonation, read from the F3
  frame-time graph export (`debug/profiling`), averaged across runs.

## Results

| Optimizer | Default | Status |
| --- | --- | --- |
| Particle Multiplier (explosions 25%, totems 50%, smoke 50%) | on | not yet measured in the scripted scenario; kept because particle count is the dominant per-frame cost during a 100-crystal detonation and the setting is also a readability feature |
| No Totem Animation | off | not yet measured; the vanilla animation draws a full-screen item model for 30 ticks, so it is offered as an opt-in |
| Crystal Size | on | visual only, not a performance claim |

## How to run the scenario

1. Start the smoke server and join it with `./gradlew runClient -PquickPlay=localhost:25599`.
2. `/fill ~-5 ~-1 ~-5 ~4 ~-1 ~4 obsidian`, place 100 crystals, enable the F3 pie chart.
3. Detonate, record the 1% low from the frame-time graph for 3 seconds, repeat with the
   optimizer toggled off. Five runs each way; add the averages to the table above.
