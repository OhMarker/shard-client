Continue Shard Client 0.4.0 in this folder (C:\Users\OhMar\New folder\shard-client). Steps 1-7 of
docs/PROMPT-ui-overhaul-0.4.0.md are done and committed on branch `v0.4.0`. Only step 8 is left.

Read first, in this order: STATUS.md (top section "0.4.0 in progress"), DECISIONS.md (the 0.4.0
sections), docs/DESIGN.md, BENCHMARKS.md (0.4.0 section), README.md. Follow the fair-play rule in
STATUS.md exactly. I am not comfortable with terminals: if I must run something, put it in a
double-click .cmd file on my Desktop (C:\Users\OhMar\OneDrive\Desktop).

## Step 8: final verification, docs, version bump

1. `./gradlew build`: every test must pass.
2. Start the offline server (`cd .smoke-server && java -Xmx2G -jar server.jar nogui`, port 25599)
   and run the smoke test at three window sizes:
   `./gradlew runClient -PquickPlay=localhost:25599 -PsmokeDir="$PWD/smoke-out" -PwindowSize=1280x720`
   then `1920x1080` and `2560x1440`. Look at every PNG yourself. Check: no module name cut off
   with "...", nothing off-screen, icons consistent, text crisp in 4x crops, readable on bright and
   dark backgrounds, `layoutIdenticalAcrossScales: true`, `inventoryScaleSlotMisses: 0`,
   `borderlessCoversMonitor: true`, `fightLogKills: 1` in smoke-summary.json. Fix anything wrong.
3. Run the benchmark (`-PsmokeBench`, see BENCHMARKS.md) and confirm the HUD layer is still about
   0.18 ms per frame and frame rates did not regress; add the result to BENCHMARKS.md.
4. Put a final before/after set in docs/screenshots/0.4.0/, including the same view as
   docs/screenshots/0.3.0-gui-owner-feedback.png (HUD category of the menu at 1920x1080).
5. Update README.md (features, new modules, categories, credits for Inter and Lucide), STATUS.md
   (0.4.0 built and verified, publish steps), DECISIONS.md, and add a 0.4.0 changelog entry.
6. Bump `modVersion` in gradle.properties to 0.4.0, `./gradlew build`, read
   build/libs/shard-0.4.0.jar.sha512.
7. Merge `v0.4.0` into `main` only after asking me.
8. STOP and ask me before creating a GitHub release or pushing ../meta. When I say yes, follow
   "Releasing a new client version later" in STATUS.md (the automation sandbox can push to existing repos
   but may not create releases; if that is refused, write a Desktop .cmd that runs the `gh release
   create` command for me).

Known limits already recorded (do not try to fix unless you have a clean way): fire tints multiply
instead of recolouring, no shield opacity, alt-tab speed in borderless is not automated, the HUD
editor toolbar can cover top-left elements when it is wide.

When done, tell me in plain words what changed, what you checked, and what I need to do.
