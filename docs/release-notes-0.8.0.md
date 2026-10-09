- **New title screen and server list** in Shard's look (switch back to vanilla in Settings → Menus).
  The server list keeps everything vanilla does: ping, players, LAN, add/edit/delete, direct connect.
- **In-game account switcher** on both screens: pick any account signed in to Shard Launcher, or add
  one. Needs Shard Launcher 0.4.0; tokens stay in memory and are never written to disk.
- **Mod menu:** Shard logo, a Profiles tab for your own saved setups, credits ("Made by OhMarker with
  the help of swxyzx2") and the MIT License in Settings → About.
- **Accent colour works again:** it colours switches, enabled mods, sliders and the main buttons.
- **Fixed settings you could not click:** the two- and three-option switches (like WASD / Arrows).
- **New menu options:** click sounds, mod descriptions on hover, open where you left off.

- **Faster start-up and resource pack changes:** the start-up resource reload went from about 10-12 s
  to about 1.5 s on the dev machine (fonts 4.2 s to 0.4 s), so the Mojang screen no longer hangs
  ("Not Responding"). Shard's fonts are now built the first time they are used.
- **No second reload at start-up:** the custom fire texture is no longer a resource pack ("Shard fire"
  is gone from Options > Resource Packs); switching it is instant and never reloads resources.
- **Sharp HUD at every HUD scale:** text is rasterised at exactly the size it is shown and placed on
  whole pixels, so small HUD scales no longer blur or lose thin strokes, and menu text is pixel-exact.
- **Small Items looks exactly like the Small Items mod by default** (everything in your hands, and
  your empty hand, at 60%); the old sliders are under Look: Custom.

Minecraft 1.21.11, Fabric Loader 0.19+, Fabric API. Shard Launcher installs it automatically; account switching needs Shard Launcher 0.4.0.
