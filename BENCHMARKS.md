# Benchmarks

Optimizers only stay in Shard if they show a measurable gain in a repeatable scenario. This file
records the method and the results. Anything listed as "not measured" ships as a visual option,
not as a performance claim.

## Scenario

- Flat world, offline-mode 1.21.11 server (`.smoke-server/server.properties`), creative mode,
  the smoke player (`ShardSmoke`) is an operator.
- 100 end crystals on a 20x20 obsidian floor, one crystal every two blocks (a crystal's box is
  two blocks wide, so a solid 10x10 grid can only hold about 25). The player hovers six blocks
  above the centre in creative flight, so explosions neither hurt nor move it.
- One hit on the corner crystal starts the chain; frame times are sampled from Shard's HUD layer
  for the next 60 ticks (3 s), which covers the chain and the particle tail.
- Dev client with the launcher's bundled set as far as the dev run ships it (Sodium, Iris,
  Lithium, Mod Menu, Cloth Config), 1280x720, vsync off, particles "All", inactivity FPS limit
  "Minimized", vanilla's 120 fps cap left in place. One unrecorded warm-up run, then six runs with
  the Explosion Optimizer on and six with it off, alternating.
- Metrics per run: average frame time, p99 frame time (reported as "1% low fps"), worst frame.

Everything is scripted: `./gradlew runClient -PquickPlay=localhost:25599 -PsmokeDir=... -PsmokeBench`
writes `bench.json` into the smoke directory. The harness lives in `src/main/java/gg/shard/client/dev/Benchmark.java`.

## Results (2026-10-07, owner's machine, dev environment)

Explosion Optimizer defaults: skip the explosion emitter, keep 25% of explosion particles, 50%
smoke, 100% crits and damage hearts, at most 3 explosion sounds per tick.

| Explosion Optimizer | Runs | Avg fps | 1% low fps | Worst frame |
| --- | --- | --- | --- | --- |
| on | 6 | 51.9 | 17.6 | 70.7 ms |
| off | 6 | 51.9 | 14.3 | 79.4 ms |

Per run (alternating, warm-up excluded):

| Run | Optimizer | Frames in 3 s | Avg fps | 1% low fps | Worst ms |
| --- | --- | --- | --- | --- | --- |
| 1 | off | 197 | 67.3 | 16.9 | 63.0 |
| 2 | on | 187 | 62.7 | 23.7 | 46.1 |
| 3 | off | 151 | 50.6 | 13.9 | 81.1 |
| 4 | on | 170 | 56.7 | 20.0 | 58.8 |
| 5 | off | 140 | 46.8 | 12.0 | 89.6 |
| 6 | on | 179 | 60.1 | 19.3 | 56.5 |
| 7 | off | 156 | 52.3 | 15.2 | 66.1 |
| 8 | on | 120 | 40.5 | 14.2 | 78.7 |
| 9 | off | 149 | 50.1 | 14.0 | 79.3 |
| 10 | on | 143 | 48.4 | 16.3 | 97.4 |
| 11 | off | 131 | 44.1 | 13.7 | 97.2 |
| 12 | on | 126 | 42.9 | 12.1 | 86.5 |

Reading: the optimizer does not change the average (the chain itself, entity removal and chunk
re-meshing dominate) but it lifts the worst 1% of frames by about 23% and trims the single worst
frame by about 11%. Four of the six pairs improve, two are within noise. The spread between runs
is large because the machine also ran the test server, Gradle and the tooling that drove the
test; treat the numbers as a direction, not a promise, and re-run the scenario on a quiet machine
before quoting them.

| Optimizer | Default | Status |
| --- | --- | --- |
| Explosion Optimizer (emitter skip, 25% explosion particles, sound cap) | on | measured above |
| Totem Pop Tweaks: hide animation | on | not measured in this scenario (no pops occur); the vanilla animation draws a full-screen item model for 30 ticks, so it is offered as a visual option |
| Totem Pop Tweaks: 50% pop particles | on | not measured; same particle hook as the explosion thinning |
| No Death Animation: remove instantly | off | not measured; visual option |
| Crystal Size | on | visual only, not a performance claim |

## Re-running

1. Start the smoke server: `cd .smoke-server && java -Xmx2G -jar server.jar nogui`.
2. `./gradlew runClient -PquickPlay=localhost:25599 -PsmokeDir="$PWD/bench-out" -PsmokeBench`.
3. Read `bench-out/bench.json` (`summary` holds the averages per configuration) and update the
   tables above. Keep the warm-up run excluded.
