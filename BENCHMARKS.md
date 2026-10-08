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

## Results (2026-10-07, owner's machine under load, dev environment, 0.2.0)

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

## 0.3.0 rendering check (2026-10-07, same machine, quiet)

0.3.0 replaces the HUD's vanilla font with Inter, draws HUD backgrounds with textured rounded
corners and scales the HUD by a fractional pose. To check that this costs nothing, the same
scenario ran twice back to back against the same server: once on 0.3.0 and once on the 0.2.0
commit (a separate git worktree). The machine was otherwise idle this time, so every run sits at
vanilla's 120 fps cap and the averages say little; the 1% lows and the worst frame are the
numbers that would move if the new rendering were slower. Raw results:
`docs/bench-2026-10-07-0.3.0.json` and `docs/bench-2026-10-07-0.2.0-baseline.json`.

| Build | Explosion Optimizer | Runs | Avg fps | 1% low fps | Worst frame |
| --- | --- | --- | --- | --- | --- |
| 0.3.0 | on | 6 | 118.0 | 99.2 | 10.9 ms |
| 0.3.0 | off | 6 | 118.2 | 96.6 | 11.1 ms |
| 0.2.0 | on | 6 | 117.7 | 100.1 | 30.4 ms (one 128 ms hitch in run 4; 10.8 ms without it) |
| 0.2.0 | off | 6 | 118.3 | 96.8 | 12.8 ms |

Reading: no frame-time regression. 0.3.0 and 0.2.0 are within run-to-run noise on every metric
(1% lows differ by under 1 fps, worst frames by about 1 ms once 0.2.0's single hitch is set
aside). With the frame cap reached, the optimizer's effect shrinks to about 3 fps on the 1% low;
the loaded-machine numbers above remain the better picture of what it does under pressure.

0.3.0 per run (alternating, warm-up excluded):

| Run | Optimizer | Frames in 3 s | Avg fps | 1% low fps | Worst ms |
| --- | --- | --- | --- | --- | --- |
| 1 | off | 354 | 118.6 | 97.1 | 11.2 |
| 2 | on | 353 | 118.2 | 94.3 | 11.1 |
| 3 | off | 354 | 118.3 | 97.5 | 11.0 |
| 4 | on | 353 | 118.1 | 98.9 | 10.4 |
| 5 | off | 354 | 118.2 | 99.0 | 11.0 |
| 6 | on | 352 | 117.7 | 95.3 | 13.2 |
| 7 | off | 353 | 118.0 | 90.0 | 11.7 |
| 8 | on | 353 | 117.9 | 100.4 | 10.4 |
| 9 | off | 353 | 118.0 | 97.2 | 11.1 |
| 10 | on | 353 | 118.1 | 103.2 | 9.9 |
| 11 | off | 352 | 117.9 | 98.6 | 10.6 |
| 12 | on | 353 | 118.1 | 103.0 | 10.2 |

## Re-running

1. Start the smoke server: `cd .smoke-server && java -Xmx2G -jar server.jar nogui`.
2. `./gradlew runClient -PquickPlay=localhost:25599 -PsmokeDir="$PWD/bench-out" -PsmokeBench`.
3. Read `bench-out/bench.json` (`summary` holds the averages per configuration) and update the
   tables above. Keep the warm-up run excluded.

## 0.4.0 (2026-10-08): the HUD's own cost, with a busy HUD

Same scenario, plus a deliberately busy HUD on every run: the Crystal PvP Pro setup, the "Crystal
PvP full" layout, Target HUD, Combo, Reach, Fight Recap, Cooldowns, Compass, Speed, TPS,
Keystrokes, CPS, and the FPS and ping graphs. The harness now also times Shard's whole HUD layer
every frame (CPU time to build it; `HudManager.LAYER_MS`) and, per element, while sampling.
The dev window runs at vanilla's 120 fps cap, so frame rates are capped; the HUD numbers are the
point here.

| Change | HUD layer avg | HUD layer p99 |
| --- | --- | --- |
| Before (step 7 start) | 0.31 ms | 0.44 ms |
| Text drawn as plain left-to-right sequences, widths cached | 0.30 ms | 0.49 ms |
| Module lookups cached by class (helps every mixin, not the HUD) | 0.30 ms | 0.45 ms |
| Small rounded boxes drawn as one cached texture instead of 4 corners + 3 fills | **0.18 ms** | **0.29 ms** |

The box was the cost: a one-line element spent 8.5 us of about 11 us drawing its background
(seven draw calls); it now spends 1.3 us. All three changes are kept (the first two are cheap and
correct). Final run, Explosion Optimizer on vs off:

| Explosion Optimizer | Runs | Avg fps | 1% low fps | Worst frame | HUD avg | HUD p99 |
| --- | --- | --- | --- | --- | --- | --- |
| on | 6 | 117.99 | 97.63 | 13.17 ms | 0.181 ms | 0.3 ms |
| off | 6 | 118.03 | 97.17 | 11.17 ms | 0.179 ms | 0.284 ms |

| Run | Optimizer | Frames | Avg fps | 1% low fps | Worst ms | HUD avg ms | HUD p99 ms |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | off | 353 | 118.03 | 93.32 | 13.11 | 0.201 | 0.34 |
| 2 | on | 351 | 117.45 | 93.41 | 16.81 | 0.225 | 0.417 |
| 3 | off | 351 | 117.68 | 94.51 | 10.82 | 0.195 | 0.314 |
| 4 | on | 353 | 117.95 | 96.37 | 10.89 | 0.186 | 0.272 |
| 5 | off | 354 | 118.24 | 97.7 | 10.61 | 0.177 | 0.268 |
| 6 | on | 353 | 118.1 | 99.7 | 10.31 | 0.177 | 0.301 |
| 7 | off | 353 | 118.01 | 99.04 | 10.45 | 0.171 | 0.244 |
| 8 | on | 353 | 118.15 | 98.26 | 10.53 | 0.167 | 0.264 |
| 9 | off | 353 | 118.15 | 100.88 | 10.71 | 0.171 | 0.244 |
| 10 | on | 353 | 118.07 | 97.59 | 19.73 | 0.174 | 0.309 |
| 11 | off | 353 | 118.09 | 97.59 | 11.34 | 0.159 | 0.292 |
| 12 | on | 354 | 118.2 | 100.43 | 10.73 | 0.154 | 0.238 |

Raw: `docs/bench-0.4.0.json`. Most expensive elements after the change (per frame): Item
Counter about 28 us (eight item renders), Session about 24 us (five lines), FPS with its graph
about 23 us (one fill per bar), Compass about 19 us.
