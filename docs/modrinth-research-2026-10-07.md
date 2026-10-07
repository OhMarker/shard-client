# Modrinth research: Fabric 1.21.11 mods used by crystal-PvP players

Date of research: 2026-10-07. All Modrinth data was pulled live from `https://api.modrinth.com/v2` with User-Agent `shard-client-research/0.1 (github.com/OhMarker/shard-client)`. Feature names quoted in this report are taken verbatim from each project's Modrinth `body` (long description) unless marked otherwise. Where a description does not say something, it is marked **not stated**; nothing below is inferred from source code.

Raw JSON kept in the scratchpad: `search_top50.json`, `search_crystal.json`, `search_pvp.json`, `projects/*.json`, `versions/*.json`, `projects_batch.json`, `projects_batch2.json`, `jars.json`, `all_projects_by_slug.json`.

---

## 1. Modrinth searches (facets: `categories:fabric`, `versions:1.21.11`, `project_type:mod`; `index=downloads`; `limit=50`)

### 1a. Top 50 Fabric mods for 1.21.11 by downloads

Query: none. total_hits = 17,753.

| # | slug | title | downloads | client | server | description |
|---|---|---|---|---|---|---|
| 1 | `fabric-api` | Fabric API | 269,978,217 | optional | optional | Lightweight and modular API providing common hooks and intercompatibility measures utilized by mods using the Fabric toolchain. |
| 2 | `sodium` | Sodium | 238,232,231 | required | unsupported | A high-performance rendering engine replacement for Minecraft, which greatly improves frame rates and reduces micro-stutter. |
| 3 | `iris` | Iris Shaders | 185,291,365 | required | unsupported | A modern shader pack loader for Minecraft intended to be compatible with existing OptiFine shader packs |
| 4 | `entityculling` | Entity Culling | 176,090,462 | required | unsupported | Using async path-tracing to hide Block-/Entities that are not visible |
| 5 | `cloth-config` | Cloth Config API | 175,101,530 | optional | optional | Configuration Library for Minecraft Mods |
| 6 | `ferrite-core` | FerriteCore | 156,741,206 | optional | optional | Memory usage optimizations |
| 7 | `modmenu` | Mod Menu | 150,914,349 | required | unsupported | Adds a mod menu to view the list of mods you have installed. |
| 8 | `lithium` | Lithium | 133,130,467 | optional | optional | No-compromises game logic optimization mod, useful for both single-player games and multi-player servers. |
| 9 | `immediatelyfast` | ImmediatelyFast | 128,790,544 | required | unsupported | Speed up immediate mode rendering in Minecraft |
| 10 | `yacl` | YetAnotherConfigLib (YACL) | 127,759,346 | optional | optional | A builder-based configuration library for Minecraft! |
| 11 | `fabric-language-kotlin` | Fabric Language Kotlin | 124,220,015 | optional | optional | This is a mod that enables usage of the Kotlin programming language for Fabric mods. |
| 12 | `xaeros-minimap` | Xaero's Minimap | 114,773,201 | required | optional | Displays a map of the nearby world terrain, players, mobs, entities in the corner of your screen. Lets you create waypoints which help you find the locations yo |
| 13 | `entitytexturefeatures` | [ETF] Entity Texture Features | 104,841,396 | required | unsupported | Emissive, Random & Custom texture support for entities in resourcepacks just like Optifine but for Fabric |
| 14 | `architectury-api` | Architectury API | 102,994,208 | required | required | An intermediary api aimed to ease developing multiplatform mods. |
| 15 | `xaeros-world-map` | Xaero's World Map | 100,361,533 | required | optional | Adds a full screen world map which shows you what you have explored in the world. Works great together with Xaero's Minimap. |
| 16 | `sodium-extra` | Sodium Extra | 100,203,377 | required | unsupported | A Sodium addon that adds features that shouldn't be in Sodium. |
| 17 | `entity-model-features` | [EMF] Entity Model Features | 99,528,740 | required | unsupported | EMF is an, OptiFine format, Custom Entity Model replacement mod available for Fabric and Forge. |
| 18 | `appleskin` | AppleSkin | 93,048,742 | optional | optional | Food/hunger-related HUD improvements |
| 19 | `veinminer` | VeinMiner | 90,618,866 | optional | required | Mine the whole vein on mining a single ore/block. Make the tedious mining experience to something satisfying and fun! |
| 20 | `not-enough-animations` | Not Enough Animations | 89,854,424 | required | unsupported | Bringing first-person animations to the third-person |
| 21 | `reeses-sodium-options` | Reese's Sodium Options | 83,668,113 | required | unsupported | Alternative Options Menu for Sodium |
| 22 | `jei` | Just Enough Items (JEI) | 80,855,867 | optional | optional | View Items and Recipes |
| 23 | `3dskinlayers` | 3D Skin Layers | 80,009,038 | required | unsupported | Render the player skin layer in 3d! |
| 24 | `continuity` | Continuity | 75,727,838 | required | unsupported | A Minecraft mod that allows for efficient connected textures |
| 25 | `geckolib` | Geckolib | 72,573,410 | required | optional | A 3D animation library for entities, blocks, items, armor, and more! |
| 26 | `simple-voice-chat` | Simple Voice Chat | 72,143,070 | optional | optional | A working voice chat in Minecraft! |
| 27 | `jade` | Jade 🔍 | 70,131,824 | optional | optional | Shows information about what you are looking at. (Hwyla/Waila fork for Minecraft 1.16+) |
| 28 | `placeholder-api` | Text Placeholder API | 68,675,217 | optional | optional | Placeholder and Text manipulation library for your Minecraft mods. |
| 29 | `moreculling` | More Culling | 68,433,833 | required | unsupported | A mod that changes how multiple types of culling are handled in order to improve performance |
| 30 | `zoomify` | Zoomify (Zoom) | 68,310,737 | required | unsupported | A zoom mod with infinite customizability. |
| 31 | `fancymenu` | FancyMenu | 68,007,619 | required | optional | Customize Minecraft's menus with ease! |
| 32 | `dynamic-fps` | Dynamic FPS | 67,969,445 | required | unsupported | Reduce resource usage while Minecraft is in the background, idle, or on battery. |
| 33 | `forge-config-api-port` | Forge Config API Port | 67,528,488 | optional | optional | NeoForge's & Forge's config systems provided to other modding ecosystems. Designed for a multiloader architecture. |
| 34 | `collective` | Collective | 66,700,651 | optional | optional | 🎓 Collective is a shared library with common code for all of Serilum's mods. |
| 35 | `veinminer-client` | VeinMiner Hotkey | 64,147,738 | required | unsupported | Veinminer Addon - Use a hotkey to veinmine with a dynamic crosshair & block highlighting |
| 36 | `puzzles-lib` | Puzzles Lib | 63,548,547 | optional | optional | Why is it called Puzzles? That's the puzzle. |
| 37 | `konkrete` | Konkrete | 62,888,609 | optional | optional | Just another boring library mod. |
| 38 | `balm` | Balm | 61,047,012 | optional | optional | Abstraction Layer for Multi-Loader Mods |
| 39 | `mouse-tweaks` | Mouse Tweaks | 60,587,813 | required | unsupported | Enhances inventory management by adding various functions to the mouse buttons. |
| 40 | `lambdynamiclights` | LambDynamicLights - Dynamic Lights | 60,298,253 | required | unsupported | Adds dynamic lights to Minecraft as the most feature-complete and optimized dynamic lighting mod. |
| 41 | `no-chat-reports` | No Chat Reports | 59,202,828 | optional | optional | Makes chat unreportable (where possible) |
| 42 | `sound-physics-remastered` | Sound Physics Remastered | 54,195,264 | required | optional | A Minecraft mod that provides realistic sound attenuation, reverberation, and absorption through blocks. |
| 43 | `creativecore` | CreativeCore | 53,010,082 | required | optional | A core mod |
| 44 | `melody` | Melody | 50,816,182 | required | unsupported | OpenAL-based library mod for playing background music. |
| 45 | `chat-heads` | Chat Heads | 50,309,898 | required | unsupported | See who you're chatting with! |
| 46 | `owo-lib` | oωo (owo-lib) | 49,140,393 | required | required | A general utility, GUI and config library for modding on Fabric and Quilt |
| 47 | `essential` | Essential Mod | 45,962,962 | required | unsupported | Enhance your Minecraft with one simple mod. Host worlds for free, chat with friends, and so much more! |
| 48 | `badoptimizations` | BadOptimizations | 44,610,157 | required | unsupported | Optimization mod that focuses on things other than rendering |
| 49 | `krypton` | Krypton | 42,887,033 | optional | optional | A mod to optimize the Minecraft networking stack |
| 50 | `language-reload` | Language Reload | 41,678,860 | required | unsupported | Reduces load times and adds fallbacks for languages |

### 1b. Query `crystal`

Query: `crystal`. total_hits = 107. Sorted by downloads. Entries also present in the top-50 are marked.

| # | slug | title | downloads | client | server | description |
|---|---|---|---|---|---|---|
| 1 | `cobweb` | Cobweb | 5,977,047 | required | required | Crystal Nest API |
| 2 | `marlow-crystal-optimizer` | Marlow's Crystal Optimizer | 4,866,257 | required | unsupported | Optimization that fixes waiting on the server after crystal break before end crystal entities are cleaned. |
| 3 | `harvest-with-ease` | Harvest with ease | 4,086,186 | unsupported | required | Don't break crops, right click them! |
| 4 | `magic-vibe-decorations` | Magic Vibe Decorations | 1,794,769 | required | required | Mod adding decorations and crystals with magical aesthetics. Some crystals provide protective effects. |
| 5 | `soul-fire-d` | Soul Fire'd | 1,726,652 | required | required | Let Soul Fire burn! |
| 6 | `pyrotechnic-elytra` | Pyrotechnic Elytra | 1,428,926 | unsupported | required | Create a show everywhere you fly! |
| 7 | `torch-hit` | Torch hit! | 1,035,235 | unsupported | required | Attack with torches! |
| 8 | `clientsidecrystals` | Client Side Crystals | 515,112 | required | unsupported | Makes your crystals show instantly client side  when crystaling to look look like zero ping Works with Marlow's optimizer Allowed on VTL and mcpvpclub |
| 9 | `clickcrystals` | ClickCrystals | 311,866 | required | unsupported | Crystal PvP tweaks client mod. Your ultimate cpvp assistance. Various tweaks that improve your performance without ruining the cpvp experience. |
| 10 | `pvp-optmizer` | PVP Optimizer | 268,288 | required | optional | This mod removes the Delay you got from swinging a sword axe pickaxe or any item Including crystal pvp anchor pvp cart pvp optimizing bows eating and ender pear |
| 11 | `crystalcarpetaddition` | Crystal Carpet Addition | 201,951 | optional | optional | Crystal0404's Carpet(fabric-carpet) Extension! |
| 12 | `hybrid-beta` | Cascades | 183,715 | optional | required | Make Minecraft more inspiring! |
| 13 | `prometheus-api` | Prometheus | 165,864 | required | required | 🔥 Backbone of modded fire 🔥 |
| 14 | `autototem+` | Auto Totem+ | 147,538 | required | unsupported | A great way to dominate in Minecraft pvp, especially with crystals! |
| 15 | `kinds-crystal-optimizer` | Kind’s Crystal Optimizer | 144,129 | required | optional | Kind Crystal Optimizer reduces delay when placing and breaking end crystals by handling them client side, making actions feel instant and eliminating lag. |
| 16 | `ore-growth` | Ore Growth | 143,523 | required | required | Ore Growth adds crystals which can grow on ores! |
| 17 | `custom-end-crystals` | Custom End Crystals | 134,156 | required | unsupported | Customize how your end crystals are rendered! |
| 18 | `fancy-entity-renderer` | Fancy Entity Renderer | 125,833 | required | unsupported | I'm a fancy GUI! |
| 19 | `lunartweaks` | LunarTweaks | 96,185 | required | unsupported | LunarTweaks is a versatile client-side mod featuring a customizable HUD, crosshair tweaks, crystal optimizer, and more — built for PvP players and developers. |
| 20 | `g1axcrystaloptimizer` | G1ax Crystal Optimizer | 88,126 | required | unsupported | G1axCrystalOptimizer optimizes end crystal placement and breaking with efficient client-side processing, reducing perceived delay and delivering a faster, smoot |
| 21 | `cpvp-macros` | CPvP Macros | 69,745 | required | unsupported | This mod contains crystal and anchor macros |
| 22 | `crystal-anchor-counter` | Crystal Anchor Counter | 66,618 | required | unsupported | HUD Counter Displays real-time CPS (Crystals per Second) and APS (Anchors per Second) shows anchor ticks and More!, |
| 23 | `auto-crystal-switcher` | Auto Crystal Switcher | 58,958 | required | unsupported | After you place obsidian if you have cyrstal in your hotbar it automatically switches to it and BOOM |
| 24 | `crystal-glow` | Crystal Glow | 53,083 | required | unsupported | adds crystal bloom to end crystals as well as other things like end gateway and a nether portal effect to the crystals. optimized for thousands of fps |
| 25 | `fastcrystal` | FastCrystal | 47,180 | required | optional | Fastest possible crystal optimizer |
| 26 | `shikarus-crystal-optimizer` | Shikaru's Crystal Optimizer | 45,179 | required | unsupported | Optimizes client-sided packets related to Crystal PvP |
| 27 | `crystals-overhauled` | Crystals Overhauled | 37,315 | required | required | Adds beautiful crystals to the game, each with its own set of tools and armor. |
| 28 | `fastcrystalspin` | FastCrystalSpin | 36,191 | required | unsupported | A lightweight Fabric client-side visual tweak that speeds up End Crystal display and spin animations without changing game mechanics. |
| 29 | `totemhelper` | Totemhelper | 31,955 | required | unsupported | TotemHelper is a mod designed for players who want to maximize their efficiency when using Totems, especially in Crystal PvP. |
| 30 | `crystal-speed` | Crystal Speed | 31,840 | required | unsupported | Allows you to change the crystal rotation speed and add little orbit crystals that can also spin fast works with crystal opacity mod |
| 31 | `safecrystal` | Safe Crystals | 27,559 | required | unsupported | Prevents punching obsidian while holding an end crystal (client-side). |
| 32 | `pvp-essentials-refined` | PVP essentials Refined | 27,275 | required | unsupported | A high-performance PvP HUD mod for Fabric 1.21.11 with crystal & anchor optimizations, built-in armor HUD, FPS display, and multiple configurable in combat syas |
| 33 | `server-sided-portals` | Server Sided Portals | 25,449 | unsupported | required | Create custom portals and dimensions compatible with any client! |
| 34 | `nightworld` | Nightworld | 23,041 | unsupported | required | Explore a parallel world! |
| 35 | `cpvp_utils` | Crystal PVP Utilities | 21,654 | required | unsupported | Adds useful crystal pvp features. Available for Fabricmc! |
| 36 | `explosive-party` | Explosive Party (Colored Explosions/TNT) | 19,834 | required | optional | Blow everything up in Style with custom Particle Colours for TNT, Beds, End Crystals & other large explosions |
| 37 | `fleckycrystaloptimizer` | Crystal Optimizer | 18,830 | required | unsupported | Instant crystals and anchors with zero server delay — a lightweight, client-side mod that makes Crystal & Anchor PvP feel smooth and responsive. 100% legit, no  |
| 38 | `essential-pvp-utilities` | Essential PVP Utilities | 14,425 | required | unsupported | This mod contains many features to give you an advantage when fighting. Contains Fast Block Placements, Wind Charge Optimizer, [Beta] Crystal Optimizer, Faster  |
| 39 | `ryuu-crystal-optimizer` | Ryuu Crystal Optimizer | 13,879 | required | unsupported | Ryuu Crystal Optimizer is a lightweight client-side mod that enhances the responsiveness of End Crystal combat by optimizing crystal interactions on the client. |
| 40 | `hcscr` | HCsCR | 11,760 | required | unsupported | Remove your end crystals before the server even knows you hit 'em! (a "crystal optimizer") |
| 41 | `crystal-opacity` | Crystal Opacity | 8,953 | required | unsupported | Allows you to change how see through a crystal is with lots of settings. or how transparent they are |
| 42 | `antighosttotem` | AntiGhostTotem | 8,878 | required | unsupported | Tired of eating ghost totems? Then here's the anti-ghost totem! Crystal PvP is a mod that will improve your skills; you can think of it like having water with y |
| 43 | `kohs-crystal-tweaks` | Crystal Tweaks KoHs | 8,778 | required | unsupported | Customize crystal glow, colors, sounds, and other-player crystal profiles. |
| 44 | `fastcrystal-lyrenxh` | Fast Crystal | 8,286 | required | unsupported | High-performance CPvP utility that automatically places and detonates End Crystals with customizable delay. |
| 45 | `td-end-crystal` | 3D End Crystal | 8,195 | required | unsupported | 3D and Animated End Crystal! |
| 46 | `rawnetcrystaloptimizer` | Rawnet's Crystal Optimizer | 7,348 | required | unsupported | Client-side Crystal optimizer for Crystal PvP. NOT ALLOWED ON VTL. |
| 47 | `xp-crystals` | XP Crystals | 6,921 | optional | required | Store your XP in craftable crystals. Survival-friendly and tier-based: store 3, 10, 30, 50 levels—or infinite XP. Very Customizable |
| 48 | `optipluscrystal` | OptiPlus-Crystal | 6,553 | required | unsupported | Removes the round-trip wait between clicking obsidian and seeing an end crystal appear. |
| 49 | `lets-better-cpvp` | Lets Better CPvP | 6,466 | required | unsupported | PvP QoL Fabric is a client-side Fabric mod for Minecraft that improves Crystal and Anchor PvP with cleaner visuals, reduced effects, customizable settings, and  |
| 50 | `d-hand-mod-fork` | D-hand mod fork | 4,637 | required | unsupported | Crystal pvp utillity that acts as a double keybind for totemslot and inventory |

### 1c. Query `pvp`

Query: `pvp`. total_hits = 349. Sorted by downloads. Duplicates of earlier lists are marked.

| # | slug | title | downloads | client | server | description |
|---|---|---|---|---|---|---|
| 1 | `tiertagger` | Tier Tagger | 3,128,262 | required | unsupported | Display player's PvP Tiers in their nametag! (MCTiers and SubTiers) |
| 2 | `tiers` | Tiers | 1,405,131 | required | unsupported | Official tiertagger mod for PvPTiers |
| 3 | `soup-api` | Soup Visual (PvP Visual) | 1,122,674 | required | unsupported | Visuals for the best PvP experience and playing with friends. Have fun :D |
| 4 | `clear-waterlavapowdersnow` | Clear Water/Lava/PowderSnow | 981,139 | required | unsupported | Clears the view while in any liquid e.g. water, lava and powder snow, so you are able to see through which is useful in PvP fights or similar. |
| 5 | `betterhitreg` | Better Hitreg | 848,109 | required | unsupported | Clientside mod that registers hits before the server to make hitreg feel faster, includes various pvp utilities mainly designed for sword |
| 6 | `axolotlclient` | AxolotlClient - Customizable HUD & Client QoL Features | 632,459 | required | unsupported | The most configurable HUD mod combined with everything else you might need to play - Your mod for the optimal PvP experience. |
| 7 | `mas-effects` | Mas Effects | 492,497 | required | unsupported | A mod focused on enhancing Minecraft PVP visually! Mace shockwave, Custom totem particles and more! |
| 8 | `universetiertagger` | Universal TierTagger | 407,270 | required | unsupported | Displays a players Tiers across the following Networks: MCTiers PVPTiers SubTiers Vox Tiers PVPHQ |
| 9 | `ravenclaws-ping-equalizer` | Ravenclaw's Ping Equalizer | 390,750 | required | unsupported | A mod dedicated to equalizing ping for PvP, doing this by adding arbitrary delays before packets are processed and sent out by the client. |
| 10 | `cookeymod` | CookeyMod | 355,741 | required | unsupported | A mod tweaking various smaller settings similar to what can be found in "PvP-clients" |
| 11 | `optimal-aim` | Optimal Aim | 326,089 | required | unsupported | Optimal aim renders a cube on other entities to aid in aiming and best utilizing your reach. Aimed towards pvp. |
| 12 | `clickcrystals` | ClickCrystals *(dup: in crystal list)* | 311,866 | required | unsupported | Crystal PvP tweaks client mod. Your ultimate cpvp assistance. Various tweaks that improve your performance without ruining the cpvp experience. |
| 13 | `pvp-bot-fabric` | PvP BOT | 286,866 | unsupported | required | A mod for Minecraft Fabric that adds smart PvP bots |
| 14 | `pvp-optmizer` | PVP Optimizer *(dup: in crystal list)* | 268,288 | required | optional | This mod removes the Delay you got from swinging a sword axe pickaxe or any item Including crystal pvp anchor pvp cart pvp optimizing bows eating and ender pear |
| 15 | `carpet-pvp-practice` | Carpet PvP Practice | 188,913 | unsupported | required | Carpet PvP is a mod with unique features for players to practice PvP efficiently. |
| 16 | `petprotect` | PetProtect | 179,320 | unsupported | required | Protect tamed mobs from PvP. |
| 17 | `vexbot` | VexBot | 172,980 | unsupported | required | Super smart Minecraft bots For PvP In any gamemode. |
| 18 | `autototem+` | Auto Totem+ *(dup: in crystal list)* | 147,538 | required | unsupported | A great way to dominate in Minecraft pvp, especially with crystals! |
| 19 | `impact-frames-pvp` | Impact Frames | 134,319 | required | unsupported | Adds flashing impact frames that plays on mace/spear hits, shield breaks, ... Or just any hits at all! Fully client-sided, configurable and customizable. BIG EP |
| 20 | `drypted-pvp-essentials` | PVP Essentials | 132,486 | required | unsupported | Essential client side PvP features, including armor, potion, damage indicators, low fire & shield, no explosion particles and much more! |
| 21 | `lt-betterpvp` | LT-BetterPVP | 117,001 | required | unsupported | Simple modification to add better animations and features to pvp in minecraft |
| 22 | `fps-overlay` | FPS Overlay | 106,572 | required | unsupported | A fully customizable, zero-bloat overlay. Tweak the layout to show exactly what you need to see for PvP and performance testing: FPS, 1% lows, ping, server TPS, |
| 23 | `macebot` | MaceBot | 98,651 | required | required | MaceBot is your personal mace PvP trainer. Practice aim, learn fight mechanics, swap attributes on the fly, and test your skills in a full simulated duel — all  |
| 24 | `lunartweaks` | LunarTweaks *(dup: in crystal list)* | 96,185 | required | unsupported | LunarTweaks is a versatile client-side mod featuring a customizable HUD, crosshair tweaks, crystal optimizer, and more — built for PvP players and developers. |
| 25 | `g1axcrystaloptimizer` | G1ax Crystal Optimizer *(dup: in crystal list)* | 88,126 | required | unsupported | G1axCrystalOptimizer optimizes end crystal placement and breaking with efficient client-side processing, reducing perceived delay and delivering a faster, smoot |
| 26 | `fpsmaster` | FPSMaster | 84,033 | required | unsupported | A pvp client, which provides common pvp utilities, like sprint, motionblur, autogg and etc. Along with fancy user-interface and music player. |
| 27 | `knockbacksync` | KnockbackSync | 73,183 | unknown | unknown | Say goodbye to lag-induced knockback woes, delivering fair and consistent PvP battles for all players, no matter their ping! |
| 28 | `jello-pvp-optimizer` | Jello's pvp optimizer | 70,638 | required | unsupported | This mod makes right‑click actions feel quicker on high ping by retrying them once or twice. It helps pearls/snowballs/eggs, fishing rod, crossbow fire, and bas |
| 29 | `extrapvp` | ExtraPvP | 69,004 | required | unsupported | A client-sided PvP mod that contains quality of life features |
| 30 | `shieldbreaker` | Shield Breaker | 62,504 | required | optional | Don't waste time in PvP. When your opponent raises their shield, Shield Breaker will think for you. |
| 31 | `motor-assistance` | Motor Assistance | 60,555 | required | unsupported | Aim assistance for blocks and mobs. A must-have with controllers or players with disabilities. (no PvP advantage) |
| 32 | `better-pvp-(client-site)` | Better-PvP | 60,050 | required | unsupported | This Mod allows players to create fully customizable teams with features like changing player armor colors and chat names, all client-side. |
| 33 | `tiertests` | Tier Tagger | 58,265 | required | unsupported | Display PvP tiers in nametags, up to two tier lists at once. Supports Tier Tests, MCTiers, PVPTiers and SubTiers |
| 34 | `mace-hitboxes` | Mace Hitboxes | 57,424 | required | unsupported | Automatically turn on hitboxes when holding a mace, good for PvP |
| 35 | `optixclient` | Optix Client | 56,560 | optional | optional | Optix Client is a cutting-edge, Fabric-based utility client built for players who demand performance and precision. Designed with a focus on PvP and minimalist  |
| 36 | `mace_pvb-traning-bot` | Mace_PVP Traning Bot | 45,426 | required | required | This mod provides the player to train their mace |
| 37 | `shikarus-crystal-optimizer` | Shikaru's Crystal Optimizer *(dup: in crystal list)* | 45,179 | required | unsupported | Optimizes client-sided packets related to Crystal PvP |
| 38 | `pvp-toggle-mod` | PvP Toggle | 42,315 | unsupported | required | Combat flagging and persistent PvP preferences to pet protection and more! |
| 39 | `reach-circles` | Reach Circles | 39,843 | required | unsupported | Circles around every player that show how much hit range/reach every player has. Calculates effective reach based on ping and velocity. For PvP use. |
| 40 | `better-hitreg+` | Better Hitreg+ | 38,154 | required | unsupported | Clientside mod that registers hits before the server to make hitreg feel faster, includes various pvp utilities mainly designed for sword |
| 41 | `pvphitsound` | PvP Hit Sound | 37,979 | required | unsupported | Adds a “pling” sound when you land a charged sword hit on another player. |
| 42 | `player-tracking` | Player Tracking | 35,211 | unsupported | required | 🔎 [PvP Mod] Allows the tracking of players via special trackers. |
| 43 | `fancy-hitbox-practice` | Fancy Hitbox Practice | 34,490 | required | unsupported | Fancy Hitbox Practice is a client-side Fabric mod that lets you visually shrink player hitboxes (even invisibles) for PvP training, with dynamic colors — withou |
| 44 | `totemhelper` | Totemhelper *(dup: in crystal list)* | 31,955 | required | unsupported | TotemHelper is a mod designed for players who want to maximize their efficiency when using Totems, especially in Crystal PvP. |
| 45 | `pvptweak` | PVP Tweaks | 29,256 | required | unsupported | PvP Tweaks is a mod in development that will give you the ability to Tweak every PVP sound, visual & Textures to your exact preference. |
| 46 | `pvp-trainer` | PVP Trainer | 28,635 | required | unsupported | Visualize hotbar keybinds and useful tools for learning PVP. |
| 47 | `sword-bot` | Sword Bot | 28,013 | required | required | A bot to practice your sword pvp skills on! |
| 48 | `crosshair-attack-indicator` | Crosshair Attack Indicator | 27,504 | required | unsupported | "A highly responsive indicator that tints the crosshair red when aiming at a living entity within reach. Optimized for PvP with frame-perfect hitbox interpolati |
| 49 | `pvp-essentials-refined` | PVP essentials Refined *(dup: in crystal list)* | 27,275 | required | unsupported | A high-performance PvP HUD mod for Fabric 1.21.11 with crystal & anchor optimizations, built-in armor HUD, FPS display, and multiple configurable in combat syas |
| 50 | `flexhud` | Flex HUD | 26,372 | required | unsupported | This mod adds a fully customizable HUD system and useful utilities for Minecraft. Whether you're playing survival, building, or PvP, it provides all the informa |

Observations on the search lists (facts only):

- The crystal-PvP "optimizer" niche on Modrinth is crowded. Besides Marlow's and ClientSideCrystals, the 1.21.11 crystal list contains `kinds-crystal-optimizer`, `g1axcrystaloptimizer`, `fastcrystal`, `shikarus-crystal-optimizer`, `fleckycrystaloptimizer`, `ryuu-crystal-optimizer`, `hcscr`, `rawnetcrystaloptimizer`, `optipluscrystal`, `lunartweaks` (bundles a crystal optimizer), `pvp-essentials-refined` and `essential-pvp-utilities`.
- Several results advertise automation that a legit client should not replicate: `autototem+`, `cpvp-macros` ("crystal and anchor macros"), `auto-crystal-switcher`, `fastcrystal-lyrenxh` ("automatically places and detonates End Crystals"), `d-hand-mod-fork`, `shieldbreaker`, `jello-pvp-optimizer` (retries right-click actions), `betterhitreg` / `better-hitreg+` (registers hits before the server). `ravenclaws-ping-equalizer` adds real network delay. They are listed only because they appear in the search output.
- `clientsidecrystals`, `anchoroptimizer` (cutebow) and `anchor` (Hero's) state server allow-lists in their descriptions (VTL / mctiers, mcpvp.club, etc.); see section 2.

---

## 2. Named projects: per-project detail

Slug resolution (the task listed guessed slugs; actual resolution below):

| Requested | Resolved slug | Notes |
|---|---|---|
| Marlow's Crystal Optimizer | `marlow-crystal-optimizer` | `marlows-crystal-optimizer` -> 404 |
| ClientSideCrystals | `clientsidecrystals` | OK |
| AnchorOptimizer | `anchoroptimizer` (cutebow) **and** `anchor-optimizer` (Walksy) | Both exist and are different projects. The installed jar (`AnchorOptimizer-1.21.x.jar`, mod id `client_side_anchors`) is cutebow's `anchoroptimizer`. A third installed anchor mod, Hero's Anchor Optimizer (`anchor`), is also covered. |
| ConsumableOptimizer | `consumableoptimizer` | OK |
| BadOptimizations | `badoptimizations` | OK |
| Ixeris | `ixeris` | OK |
| NoDeathAnimation | `no-death-animation` | `nodeathanimation` -> 404 |
| Controlling | `controlling` | OK |
| In-Game Account Switcher | `in-game-account-switcher` | `ias` -> 404 |
| Ambience | `ambience-v2` | The slug `ambience` resolves to an **unrelated** project (ID `Ge9LhP6v`, "A selection of completely client-side mods and resource packs to make Minecraft feel more immersive and cozy", MIT, no source). The installed jar `Ambience-2.1.1-1.21.11.jar` (mod id `ambience`, by Walksy) is Modrinth project `ambience-v2` (ID `BykykDiv`) per the profile's `.index/ambience-v2.pw.toml`. |
| Force Lower Case Commands | `force-lowercase-commands-maintained` | `force-lowercase-commands` (ID `CwxVUnPT`) exists but does **not** list 1.21.11 (max 1.21.8). The installed jar is the "Maintained" fork (ID `j6ChDYVD`, author tostraight). |

### 2.1 Marlow's Crystal Optimizer

- Slug `marlow-crystal-optimizer`, Modrinth ID `ozpC8eDC`, title "Marlow's Crystal Optimizer". Mod id (from jar `fabric.mod.json`): `marlowcrystal`.
- client_side `required`, server_side `unsupported`. Loaders: fabric. Downloads 4,869,763.
- 1.21.11 in `game_versions`: **yes**. 1.21.11 Fabric versions: `1.1.0` (2026-05-05, file `Marlow Crystal Optimizer.jar`, no listed Modrinth dependencies), `1.0.5` (2026-01-06, Fabric API required), `1.0.4` (2025-12-09). Installed jar is `1.1.0` (current). Jar `depends`: fabricloader >=0.17.3, minecraft >=1.21.11 <=1.21.11, fabric-api *.
- License MIT. Source https://github.com/Bram1903/MarlowsCrystalOptimizer.
- What it does (body): "optimizes the handling of using end crystals by removing the crystal client side, instead of waiting for the server to remove it. This can especially be useful when you are on higher ping." Supported platforms table: "Fabric | 1.19 - 26.2". Jar description: "Optimization that fixes waiting on the server after crystal break before end crystal entities are cleaned."
- Options/config: **not stated** (the body lists no settings).
- Packets / automation / hidden info: the body does not state any packet changes, automation, or revealed information. Behaviour when the server rejects the hit (whether the crystal reappears): **not stated**.

### 2.2 Client Side Crystals (cutebow)

- Slug `clientsidecrystals`, ID `37mwLSsz`, title "Client Side Crystals". Mod id: `clientsidecrystals` (the distributed jar is a multi-version bundle with mod id `clientsidecrystals_bundle` that nests one `clientsidecrystals` jar per Minecraft version 1.21 .. 1.21.11 and 26.1 .. 26.2; each nested jar also bundles `org_quiltmc_parsers_json`/`gson` 0.3.0).
- client_side `required`, server_side `unsupported`. Downloads 515,731. Modrinth categories: decoration, equipment.
- 1.21.11 in `game_versions`: **yes**. Only one 1.21.11 version exists: `1.21.X-26.X` (2026-08-14, file `ClientSideCrystals-1.21.X-26.X.jar`), dependencies: YACL (`1eAoo2KR`) required, Mod Menu optional. Installed jar matches. Nested 1.21.11 jar `depends`: fabricloader >=0.16.0, minecraft 1.21.11, fabric-api *, java >=21, yet_another_config_lib_v3 >=3.5.0. Body says "0.19.3 Fabric loader and newer" and "Requires: Silicon, YACL" (Silicon is linked as a separate Modrinth mod; the nested jar's `depends` does not list it).
- License `LicenseRef-All-Rights-Reserved` (jar: "AAR"). Source https://github.com/cutebow/Client-Side-Crystals.
- What it does (body): "Makes end crystals appear instantly on your screen the moment you place them no matter your ping, giving better looking crystaling and better timing feedback in crystalpvp. no packets are changed crystaling is still handled like normal." Mechanism: "when you place a crystal it will put a fake client side crystal that looks real and depending on ur setting it will either seemlessly switch from the fake client side visual only crystal to the real one that was placed server side but the real crystal is fully unaffected. like placement and breaking is fully unaffected". "it snaps back to the real crystal right as the server confirms the real one but it fills the gap of the crystal not existing so it looks instant." Nested jar description: "Instant client-side crystal visuals while real crystals remain fully server-authoritative."
- Options named: the "tint" setting ("the tint option") which highlights the fake crystal "to show like when it changes" (red highlight in the gallery). "depending on ur setting" implies other switching modes, but they are **not enumerated**.
- Allowed-on list in body: VTL (mctiers.com), mcpvp.club, pvptiers.com (Lurrns tierlist), napvp.us, westshop.org, catpvp, crystalranked, crackedpvp.club, applepvp.com, nova tiers, "and more".
- Packets / automation / hidden info: body explicitly says "no packets are changed" and "It's only visual and doesn't change how crystals behave on the server". No automation stated. Entry points: `me.clientsidecrystals.core.ClientHooks` (client), `me.clientsidecrystals.util.ModMenuIntegration` (modmenu).

### 2.3 Anchor Optimizer (cutebow) -- the installed one

- Slug `anchoroptimizer`, ID `Lpf6Rujk`, title "Anchor Optimizer". Mod id: `client_side_anchors`. Installed jar `AnchorOptimizer-1.21.x.jar` version `1.0.6+1.21.X`, jar description "Instant client-side anchoring. Server remains authoritative - meaning to others it looks normal." Jar `depends`: fabricloader >=0.16.0, minecraft >=1.21 <=1.21.11, fabric-api *. Bundles quilt parsers json/gson. Entry points `me.cutebow.client_side_anchors.ClientSideAnchors`, modmenu `...config.ClientSideAnchorsModMenuIntegration`.
- client_side `required`, server_side `unsupported`. Downloads 1,426,819.
- 1.21.11 in `game_versions`: **yes**. One 1.21.11 version: `1.0.6+1.21.X` (2026-08-11), YACL required. Body: "Requires YACL and Mod Menu" and "Requires: Silicon".
- License All Rights Reserved. Source https://github.com/cutebow/Anchor-Optimizer. "Mod creation date: December 9, 2025".
- What it does (body): "Instantly hides the anchor once you explode it so that it looks like zero ping, collision for that block is still there until the server confirmed the anchor explodes just you can't see it." "the anchor is replaced with a barrier block that is treated like a anchor that has already exploded so all interactions are treated as such keep in mind this is all client sided". Rationale for the barrier: "this was the compromise that had to be made for it to be allowed on vtl"; replacing with air "could be very badly abused in unintentional ways like hitting through anchors" at 500+ ms. "It removes your ping penalty visually / It only changes what your client shows while waiting for the server / It doesn't change outcomes: no faster damage, no earlier server explosion, no desync other players can see. its only client-side prediction."
- Options: a config exists (YACL / Mod Menu) but individual options are **not stated**.
- Allowed-on list: VTL, mcpvp.club, DonutSMP, westshop.org, pvptiers.com, napvp.us, crystalchaos, catpvp, crystalranked, crackedpvp.club, applepvp.com, nova tiers.
- Notable side behaviours stated in the body: "adds westshop.org to serverlist with an animated effect. (can be removed) also adds an effect for napvp but that isnt added to the serverlist." and "as well as an updater to stay up to date on the latest version of the mod" (i.e. it modifies the multiplayer server list and performs update checks; the update endpoint is **not stated**).
- Packets: not stated beyond "all client sided". Automation: none stated.

### 2.4 Anchor Optimizer (Walksy) -- NOT installed, different project

- Slug `anchor-optimizer`, ID `KvhelBPU`, title "Anchor Optimizer". Mod id: **not stated** (not installed; jar not inspected).
- client_side `required`, server_side `unsupported`. Downloads 205,624.
- 1.21.11 in `game_versions`: **yes**; one 1.21.11 version `1.2.0+1.21.11` (2026-02-17, file `AnchorOptimizer-1.2.0-1.21.11.jar`, no listed dependencies).
- License MIT. Source https://github.com/Walksy/AnchorOptimizer. Modrinth short description: "Handles anchor interactions client side - Allowed on VTL".
- What it does (body): "removes the anchor client-side instantly when interacted along with removing any nearby glowstone, ensuring you don't accidently interact with glowstone blocks which shouldn't actually be there anymore." "This mod does not remove the ability to air place. While it does remove the respawn anchor completely, it stores the interacted position until the server eventually does respond, ensuring the player can still air place."
- A struck-through section "~~Client-sided explosion~~": "~~This feature is disabled by default and must be enabled manually via the config menu. This feature creates an explosion client-side, allowing the player to fall into holes much faster. Unfortunately this feature is unlikely to be allowed on servers and could also flag anti cheats... Use with caution.~~" (presented struck through in the body; whether it still exists in 1.2.0 is **not stated**).
- Packets: not stated. Automation: none stated.

### 2.5 Hero's Anchor Optimizer -- installed (manually added, no `.index` entry)

- Slug `anchor`, ID `5J1t3PV8`, title "Hero's Anchor Optimizer". Mod id `herosanchoroptimizer`, installed jar version `1.1.4`. Jar description: "Optimization that fixes waiting on the server after anchor explosion before anchor block is removed. Replaces it with translucent ghost anchor to not change functionality of air-placing." Jar `depends`: fabricloader >=0.15.11, fabric *, minecraft ~1.21.11.
- client_side `required`, server_side `unsupported`. Downloads 2,270,313. 1.21.11 listed: **yes**. License MIT. Source https://github.com/HerobaneNair/herosanchoroptimizer.
- What it does (body): "Replaces it with a visibly different 'fake' anchor (that can be replaced like a fern) to not change functionality of airplacing. This allows you to not have to wait for the server to be able to place another block in the same place." Fake anchor texture/model is resource-pack overridable (`assets/herosanchoroptimizer/models/block/fake_anchor.json`, `fake_anchor_side.png`, `fake_anchor_vertical.png`). Allowed on: Vanilla+, PVP Legacy, USPVP, MCPVP, DonutSMP, LeoneMC, Vanilla (Crystal PVP) Tier list. "Make sure to have fabric api (for versions <1.21.11)".
- Options: **not stated**. Packets/automation: **not stated**.

### 2.6 Consumable Optimizer (Walksy)

- Slug `consumableoptimizer`, ID `4alTd2ag`, title "Consumable Optimizer". Mod id `consumableoptimizer`. Installed jar `ConsumableOptimizer-2.2.1-1.21.5.jar` version `2.2.1-1.21.5` (the version string says 1.21.5 but it is the version published for 1.21.11). Jar `depends`: fabricloader *, minecraft *, java >=17, fabric-api *, walksylib >=0.9.95.
- client_side `required`, server_side `unsupported`. Downloads 955,048.
- 1.21.11 in `game_versions`: **yes**. 1.21.11 versions: `2.2.1-1.21.5` (2026-03-09, latest), `2.2.0-1.21.5`, `2.1.0-1.21.5`, `2.0.0-1.21.5`. Installed is current.
- License MIT. Source https://github.com/Walksy/ConsumableOptimizer.
- What it does (body): "enforces more client-sided processing when handling consumable items (any food item)." "By default, Minecraft requires the user to wait for a server response before consuming an item ... causes players to be stuck in an eating animation until the server eventually responds. This optimizer ensures all of that processing is done client-side rather than server-side, allowing items to be comsumed with no delay and on the exact tick it should be finished. The server still applies the item's effects (such as absorption or regeneration from a golden apple) while the client handles the actual eating process." "High ping can also occationaly causes desyncs when consuming an item, forcing the server to restart the eating process for the player ... This optimizer fixes this, by cancelling these recalls from the server when unneeded."
- Server opt-out protocol (body, with example Bukkit plugin): the client sends a plugin message on channel `consumable_optimizer:handshake_payload` (C2S); a server replies on `consumable_optimizer:disable_payload` (S2C, empty payload) to disable the mod for that player.
- Packets: **sends a custom plugin-message handshake** to every server; **ignores/cancels certain server "recall" packets** that restart eating. Automation: none stated. Options: **not stated** in body.

### 2.7 BadOptimizations

- Slug `badoptimizations`, ID `g96Z4WVZ`, title "BadOptimizations". Mod id `badoptimizations`, installed `2.4.1`. Jar `depends`: minecraft >=1.21.10, fabricloader >=0.15.0; jar environment `client`.
- client_side `required`, server_side `unsupported`. Loaders fabric/forge/neoforge. Downloads 44,658,799.
- 1.21.11 listed: **yes**; one 1.21.11 Fabric version `2.4.1` (2025-12-16). Installed is current.
- License MIT. Source https://github.com/ItsThosea/BadOpitmizations.
- What it does (body): "A collection of mostly micro optimizations that add up to something bigger!" Named optimizations: "Avoid updating lightmap" (cancels lightmap texture updates "if nothing that changes block brightness (e.g. gamma slider, potion effects, dimension) has changed"); "Don't do debug logic if we don't need to" (the four debug renderers -- bees, game events, game test, villager AI -- only run "if debug is enabled and there is data from the server to process"); "Sky color calculation optimizations (1.21.10 and below)" (cubic colour sampler only used between biomes with different sky colours, cached per tick). "That's a few of them, at least. You can disable any optimization in the config file if you need to." Other mods can mark themselves incompatible with specific options, or add lightmap/sky-colour caching hooks. "Dependencies? None."
- Packets / automation / hidden info: none stated.

### 2.8 Ixeris

- Slug `ixeris`, ID `p8RJPJIC`, title "Ixeris". Mod id `ixeris`. Installed `4.2.0+1.21.11-fabric` (**outdated**: latest 1.21.11 is `4.6.8+1.21.11-fabric`, 2026-09-21; 39 Fabric releases exist for 1.21.11). Jar `depends`: minecraft >=1.21.9 <=1.21.11, fabricloader >=0.16.0.
- client_side `required`, server_side `unsupported`. Loaders fabric/forge/neoforge/quilt. Downloads 12,097,547.
- License LGPL-3.0-only. Source https://github.com/decce6/Ixeris.
- What it does (body): "optimizes event polling to improve client performance." Two measures: "Threaded Event Polling" ("performs event polling on the main thread and kicks rendering to a separate render thread") and "Buffered Raw Input (Windows-only)" (switches from `GetRawInputData` to `GetRawInputBuffer`, batch reads, eliminates JNI upcall overhead; the benchmark shows a "Non-buffered" mode, i.e. this option can be disabled). Additional technical features: "GLFW State Caching" (caches frequently used GLFW state for cross-thread access) and "Enhanced FPS Limiter" (rewrites the vanilla limiter "in a hybrid way, sleeping precisely and starting spin waiting when the wait time is very low"; vanilla limiter "prior to 26.1" is called flawed). Benchmarks: at 8000 Hz polling 12 FPS -> 121 FPS; at 2000 Hz 76 -> 135; 500 Hz 134 -> 151. "As of version 3.1.0, the requirements of thread safety in the GLFW documentation are strictly obeyed."
- Packets / automation / hidden info: none stated.

### 2.9 No Death Animation (Walksy)

- Slug `no-death-animation`, ID `r6VxNyu0`, title "No Death Animation". Mod id `nodeathanimation`, installed `2.0.0-1.21.11` (current; only 1.21.11 version, 2025-12-12, WalksyLib required). Jar `depends`: fabricloader *, minecraft *, java >=17, fabric-api *, walksylib >=0.9.95.
- client_side `required`, server_side `unsupported`. Downloads 558,708. License MIT. Source https://github.com/Walksy/NoDeathAnimation.
- What it does (body): "removes the death animation tilt which occurs on dead players." Option: "Red Death Overlay -- This mod also allows you to customise the duration of the red death overlay." (Jar description says "on entities"; Modrinth says "on players".)
- Packets / automation / hidden info: none stated.

### 2.10 Controlling

- Slug `controlling`, ID `xv94TkTM`, title "Controlling". Mod id `controlling`, installed `29.0.1` (current; 2025-12-18; requires Fabric API and Searchables `fuuu3xnx`). Jar `depends`: fabricloader >=0.18.2, java >=21, minecraft 1.21.11, searchables >=1.0.1, fabric-api *.
- client_side `required`, server_side `unsupported`. Loaders fabric/forge/neoforge. Downloads 40,095,488. License MIT. Source https://github.com/jaredlll08/Controlling.
- What it does (body): "Adds the ability to search for keybinds using their name in the KeyBinding menu"; "Adds a button that will only show KeyBindings that conflict with each other"; "You also have the ability to see what keys are available to be bound." No other options stated.
- Packets / automation / hidden info: none stated.

### 2.11 In-Game Account Switcher

- Slug `in-game-account-switcher`, ID `cudtvDnd`, title "In-Game Account Switcher". Mod id `ias`, installed `9.0.8+1.21.11-fabric` (current; 2026-09-24; Fabric API required, Mod Menu optional). Jar `depends`: minecraft 1.21.11, fabric-lifecycle-events-v1, fabric-resource-loader-v1, fabric-screen-api-v1.
- client_side `required`, server_side `unsupported`. Loaders fabric/forge/neoforge/quilt. Downloads 5,463,619. License LGPL-3.0-or-later. Source https://github.com/The-Fireplace-Minecraft-Mods/In-Game-Account-Switcher.
- What it does (body): "allows you to change which account you are logged in to in-game, without having to restart Minecraft." The body is mostly an FAQ. Relevant statements: documentation exists for "Terms and Privacy", "Crypt", "Stolen Accounts", "Common Errors", "Secure Log Sharing"; "Can I import/export my accounts from/into a file? Can I use access tokens/cookies to add Microsoft accounts? -- No. This kind of behavior is either shady or prohibited by Minecraft EULA."; closed-source clients (Feather/Lunar) are unsupported; credits the minecraft.wiki "Microsoft_authentication" page, i.e. the mod performs Microsoft account authentication itself. The body's "Which versions are supported" list stops at 1.21.5 (stale; Modrinth `game_versions` includes 1.21.11).
- Packets / automation / hidden info: it necessarily handles account credentials/tokens and talks to Microsoft/Mojang authentication services (storage/encryption details are in the linked "Crypt" doc, **not stated** in the body). No game-packet behaviour stated.

### 2.12 Ambience V2 (Walksy)

- Slug `ambience-v2`, ID `BykykDiv`, title "Ambience V2". Mod id `ambience`, installed `2.1.1-1.21.11` (current; 2026-03-29; WalksyLib required). Jar `depends`: fabricloader *, minecraft *, java >=17, fabric-api *, walksylib >=0.9.95.
- client_side `required`, server_side `unsupported`. Downloads: see step-4 table. License MIT. Source https://github.com/Walksy/Ambience-V2.
- What it does (body): "changes the color of the environment, such as the sky, foliage (plants), lava, fire, water, and clouds." Config, verbatim: General Settings: "Enable Mod", "Override in game time"; Sky Settings: "Override overworld, nether & end sky color", "Overworld sky gradient", "Brightness & height controller for the gradient"; Clouds, Water, Lava & Fire Settings: "Override color"; Foliage Settings: "Override leaves, grass block, short grass & tall grass color".
- Packets / automation / hidden info: none stated. "Override in game time" changes the displayed time client-side.
- Note: the profile also contains `ambience-v3-1.0.0.jar` (mod id `ambience_v3`, "Ambience V3", "Client-side ambience configuration controls for sky, clouds, water, lava, fire, foliage, and time.", entry points in package `tostraight.*`, All-Rights-Reserved, depends YACL >=3.8.1 and Mod Menu). It is **not on Modrinth** (search for "Ambience V3" returned nothing; it is not among tostraight's listed projects) and has no `.index` entry.

### 2.13 Force Lowercase Commands Maintained (tostraight)

- Slug `force-lowercase-commands-maintained`, ID `j6ChDYVD`, title "Force Lowercase Commands Maintained". Mod id `force_lowercase_commands`, installed `1.2.1` (current; 2026-04-11; earlier `1.0.0` 2026-03-13). Jar `depends`: fabricloader >=0.18.4, fabric-api *, minecraft >=1.21.9 <=1.21.11. Jar description: "Automatically lowercases only the command name before sending, while preserving arguments exactly as typed."
- client_side `required`, server_side `unsupported`. Loaders fabric. License All Rights Reserved. Source: **not stated**.
- What it does (body): "automatically converts the command name you type in chat to lowercase before sending it. It only affects the actual command, not the arguments after it." Examples: `/SPAWN -> /spawn`, `/HELP SPAWN -> /help SPAWN`, `/Help SPawn -> /help SPawn`.
- Packets: rewrites the outgoing chat-command text before it is sent (no other packet behaviour stated). Automation: none. Options: **not stated**.
- The original project `force-lowercase-commands` (ID `CwxVUnPT`, MIT, no source, "Forces Minecraft client to use lowercase letters in commands.") lists only up to 1.21.8.

---

## 3. Local profile folder: `C:\Users\OhMar\AppData\Roaming\ModrinthApp\profiles\1.21.11 9_11\mods`

The folder exists and contains 85 files (83 enabled `.jar`, 2 `.jar.disabled`) plus the `.index/` directory of Modrinth App `*.pw.toml` metadata. Every jar was opened as a zip and its `fabric.mod.json` parsed (Python `zipfile`; `unzip`, `tar` and `7z` are also available on this machine). All jars contain a `fabric.mod.json`.

Stale `.index` entries (file referenced but not present in folder): `CustomHitboxes-2.0.7-1.21.11.jar` -> `custom-hitboxes.pw.toml` (project y7bA0M36).

Jars present but with no `.index` entry (added manually, not via the Modrinth App): `ambience-v3-1.0.0.jar`, `AnchorOptimizer-1.21.x.jar`, `ClientSideCrystals-1.21.X-26.X.jar`, `exordium-1.4.3-1.21.11.jar`, `herosanchoroptimizer-1.1.4.jar`, `hitbox-studio-1.0.0.jar`, `IAS-9.0.8+1.21.11-fabric.jar`, `litematica-fabric-1.21.11-0.26.16.jar`, `malilib-fabric-1.21.11-0.27.20.jar`, `microdurability-1.21.11-Fabric-1.0.0.jar.disabled`, `naturalmotionblur-1.4.5+mc1.21.11.jar`, `shard-0.1.0.jar`, `shield-extensions-1.1.0+mc1.21.11.jar`, `voicechat-fabric-1.21.11-2.6.24.jar`.

Nested jars: `ClientSideCrystals-1.21.X-26.X.jar` is a bundle (`clientsidecrystals_bundle`) that nests 16 per-version `clientsidecrystals` jars (1.21 .. 1.21.11, 26.1 .. 26.2) under `META-INF/jars/`; the 1.21.11 one is `ClientSideCrystals-1.21.11.jar` (121,814 bytes). `AnchorOptimizer-1.21.x.jar` and the nested ClientSideCrystals jars each also nest `org_quiltmc_parsers_json` / `gson` 0.3.0.

`shard-0.1.0.jar` is the owner's own mod (id `shard`, "Sharpen your crystal PvP. Legit HUD, visuals and performance tweaks for crystal PvP.", contact https://github.com/OhMarker/shard-client, MIT).

| jar file | size (bytes) | mod `id` | `version` | `name` | `description` | `environment` | `depends` | Modrinth project ID (from .index) |
|---|---|---|---|---|---|---|---|---|
| Ambience-2.1.1-1.21.11.jar | 115,182 | `ambience` | 2.1.1-1.21.11 | Ambience | Allows you to change the colors of the environment | client | `{"fabricloader": "*", "minecraft": "*", "java": ">=17", "fabric-api": "*", "walksylib": ">=0.9.95"}` | BykykDiv |
| ambience-v3-1.0.0.jar | 54,322 | `ambience_v3` | 1.0.0 | Ambience V3 | Client-side ambience configuration controls for sky, clouds, water, lava, fire, foliage, and time. | client | `{"fabricloader": ">=0.18.4", "fabric-api": "*", "minecraft": "1.21.11", "yet_another_config_lib_v3": ">=3.8.1", "modmenu": ">=17.0.0-beta.2"}` | (none: not in .index) |
| AnchorOptimizer-1.21.x.jar | 169,522 | `client_side_anchors` | 1.0.6+1.21.X | Anchor Optimizer | Instant client-side anchoring. Server remains authoritative - meaning to others it looks normal. | client | `{"fabricloader": ">=0.16.0", "minecraft": ">=1.21 <=1.21.11", "fabric-api": "*"}` | (none: not in .index) |
| appleskin-fabric-mc1.21.11-3.0.8.jar | 180,107 | `appleskin` | 3.0.8+mc1.21.11 | AppleSkin | Adds various food-related HUD improvements | * | `{"fabricloader": ">=0.15.10", "fabric-api": "*"}` | EsAfCjCV |
| BadOptimizations-2.4.1-1.21.11.jar | 269,545 | `badoptimizations` | 2.4.1 | BadOptimizations | Optimization mod that focuses on things other than rendering | client | `{"minecraft": ">=1.21.10", "fabricloader": ">=0.15.0"}` | g96Z4WVZ |
| bbe-fabric-1.3.3+mc1.21.11.jar | 228,056 | `betterblockentities` | 1.3.3+mc1.21.11 | BBE | Optimize and customize block entity rendering with a more modern approach. | client | `{"fabricloader": ">=0.16.7", "minecraft": ">=1.21.11", "sodium": ">=0.8.7"}` | ONZm0H7Y |
| betterchathide-1.21.11.jar | 6,875 | `better_chat_hide` | 1.21.11 | Better Chat Hide | Hides in-game chat when the player is not actively focused on it. | client | `{"fabricloader": ">=0.18.3", "fabric-api": "*", "minecraft": "1.21.11"}` | AsEJlE2q |
| betterhurtcam-1.12.0+mc1.21.11.jar | 33,720 | `betterhurtcam` | 1.12.0+mc1.21.11 | BetterHurtCam | nohurtcam but better | client | `{"minecraft": ">=1.21.11", "fabricloader": ">=0.17", "ukulib": "^1.10.0"}` | o4y0N2hu |
| betterscreens-2.0.11+1.21.11-fabric.jar | 182,732 | `betterscreens` | 2.0.11 | Better Screens | Improvements to some screen mechanics! | client | `{"fabricloader": ">=0.18.4", "minecraft": "1.21.11", "java": ">=21", "fabric-api": "*", "yet_another_config_lib_v3": "*"}` | PBbfPJZN |
| bobby-5.2.11+mc1.21.11.jar | 871,882 | `bobby` | 5.2.11+mc1.21.11 | Bobby | Allows for render distances greater than the server's view-distance setting. | client | `{"fabricloader": ">=0.16.9", "minecraft": "~1.21.9"}` | M08ruV16 |
| clientsidecosmetics-1.21.11-2.0.2+mc1.21.11.jar | 230,829 | `clientsidecosmetics` | 2.0.0+mc1.21.11 | Client Side Cosmetics | A client-side cosmetics mod for Fabric with custom armor trims, shields, and capes. | client | `{"fabricloader": ">=0.18.4", "minecraft": "~1.21.11", "java": ">=21", "fabric-api": "*"}` | Wo8uYUHh |
| ClientSideCrystals-1.21.X-26.X.jar | 1,912,250 | `clientsidecrystals_bundle` | 1.21.X-26.X | Client Side Crystals | Multi-version Client Side Crystals bundle for Minecraft 1.21 through 1.21.11 and 26.1 through 26.2. | client | `{"fabricloader": ">=0.16.0", "minecraft": ">=1.21 <=26.2", "clientsidecrystals": "*", "java": ">=21"}` | (none: not in .index) |
| cloth-config-21.11.153-fabric.jar | 1,148,427 | `cloth-config` | 21.11.153 | Cloth Config v20 | An API for config screens. | (absent) | `{"fabricloader": ">=0.14.0", "minecraft": ">=1.21.9-"}` | 9s6osm5g |
| commandkeys-fabric-2.4.0+1.21.11.jar | 267,248 | `commandkeys` | 2.4.0+1.21.11 | CommandKeys | Send pre-written chat messages and commands using hotkeys. | client | `{"java": [">=21"], "minecraft": [">1.21.10"], "fabricloader": [">=0.17.0"], "fabric-api": ["*"]}` | 65UyswbY |
| ConsumableOptimizer-2.2.1-1.21.5.jar | 148,531 | `consumableoptimizer` | 2.2.1-1.21.5 | Consumable Optimizer | Handles the consumption of items client-side | client | `{"fabricloader": "*", "minecraft": "*", "java": ">=17", "fabric-api": "*", "walksylib": ">=0.9.95"}` | 4alTd2ag |
| Controlling-fabric-1.21.11-29.0.1.jar | 83,259 | `controlling` | 29.0.1 | Controlling | Adds the ability to search for keybinds using their name in the KeyBinding menu, this allows players to easily find a key binding in the menu. | client | `{"fabricloader": ">=0.18.2", "java": ">=21", "minecraft": "1.21.11", "searchables": ">=1.0.1", "fabric-api": "*"}` | xv94TkTM |
| cursorcenteredfix-1.2.1-fabric.jar | 9,845 | `cursorcenteredfix` | 1.2.1 | CursorCentered Fix | Fix the cursor centering issue that occasionally happens when opening a GUI. | * | `{"fabricloader": "*", "minecraft": ">=1.14.4", "java": ">=8"}` | 3Vf97jLY |
| custom-crosshair-mod-v1.6.4-fabric-mc1.21.11.jar | 143,299 | `custom-crosshair-mod` | 1.6.4-fabric | Custom Crosshair Mod | A dynamic and customisable crosshair. | client | `{"fabricloader": ">=0.18.2", "minecraft": "~1.21.11", "java": ">=21", "fabric-api": "*"}` | o1tyE5vJ |
| cwb-fabric-3.0.0-build.14+mc1.21.11.jar | 78,535 | `cubes-without-borders` | 3.0.0-build.14+mc1.21.11 | Cubes Without Borders | Allows you to play Minecraft in a borderless fullscreen window. | client | `{"fabricloader": ">=0.18.2", "minecraft": ">=1.21.11 <26.1", "java": ">=21"}` | ETlrkaYF |
| debugify-1.21.11+1.1.jar | 376,878 | `debugify` | 1.21.11+1.1 | Debugify | Fixes Minecraft bugs found on the bug tracker  Credits: j-Tai's TieFix - Code used licensed under LGPLv3 FlashyReese's Sodium Extra - Code used licens | * | `{"fabricloader": ">=0.18.0", "minecraft": "~1.21.11", "java": ">=21", "fabric-resource-loader-v0": "*"}` | QwxR6Gcd |
| effecttimerplus-fabric-2.2.7+1.21.11.jar | 47,633 | `effecttimerplus` | 2.2.7+1.21.11 | EffectTimerPlus | Adds a potency indicator and countdown overlay to status effect icons. | client | `{"java": [">=21"], "minecraft": [">1.21.10"], "fabricloader": [">=0.17.0"], "fabric-api": ["*"]}` | JIUF2Wb5 |
| emoji-type-3.0.0-1.21.10-fabric.jar | 373,914 | `emojitype` | 3.0.0-1.21.10 | Emoji Type | A mod that lets you type Minecraft emojis easier! | client | `{"fabricloader": "^0.18.1", "minecraft": "~1.21.9"}` | q7vRRpxU |
| entityculling-fabric-1.10.1-mc1.21.11.jar.disabled | 1,585,638 | `entityculling` | 1.10.1 | EntityCulling | This mod uses async path-tracing to hide Tiles/Entities that are not visible. | (absent) | `{"minecraft": "1.21.x", "fabric-api": "*"}` | NNAgCjsB |
| exordium-1.4.3-1.21.11.jar | 2,719,916 | `exordium` | 1.4.2+unknown | Exordium | Render the ingame gui on its own FPS limiter | client | `{"fabric-api": "*", "minecraft": "1.21.x"}` | (none: not in .index) |
| fabric-api-0.141.3+1.21.11.jar | 2,412,693 | `fabric-api` | 0.141.3+1.21.11 | Fabric API | Core API module providing key hooks and intercompatibility features. | * | `{"fabricloader": ">=0.17.3", "java": ">=21", "minecraft": ">=1.21.11- <1.21.12-"}` | P7dR8mSH |
| ferritecore-8.2.0-fabric.jar | 80,138 | `ferritecore` | 8.2.0 | FerriteCore | Reduces memory usage | * | `{"fabricloader": ">=0.14.21", "minecraft": ">=1.21.11 <1.22"}` | uXXizFIs |
| ForceLowerCaseCommandsFabric-1.2.1-1.21.11.jar | 9,403 | `force_lowercase_commands` | 1.2.1 | Force Lowercase Commands | Automatically lowercases only the command name before sending, while preserving arguments exactly as typed. | client | `{"fabricloader": ">=0.18.4", "fabric-api": "*", "minecraft": ">=1.21.9 <=1.21.11"}` | j6ChDYVD |
| ForgeConfigAPIPort-v21.11.1-mc1.21.11-Fabric.jar | 598,009 | `forgeconfigapiport` | 21.11.1 | Forge Config API Port | Provides NeoForge's & Forge's config systems to other modding ecosystems. Designed for a multi-loader architecture. | * | `{"minecraft": ">=1.21.11- <1.21.12-", "fabricloader": ">=0.18.0", "fabric-api": ">=0.139.0"}` | ohNO6lps |
| herosanchoroptimizer-1.1.4.jar | 42,422 | `herosanchoroptimizer` | 1.1.4 | HerosAnchorOptimizer | Optimization that fixes waiting on the server after anchor explosion before anchor block is removed. Replaces it with translucent ghost anchor to not  | client | `{"fabricloader": ">=0.15.11", "fabric": "*", "minecraft": "~1.21.11"}` | (none: not in .index) |
| highlights-1.2.jar | 1,180,403 | `highlights` | 1.2 | Highlights | Highlights Totems of Undying in the inventory. | client | `{"fabricloader": ">=0.18.4", "modmenu": "*", "cloth-config": "*", "minecraft": "~1.21.11", "java": ">=21"}` | QBMRH7wB |
| hitbox-studio-1.0.0.jar | 109,360 | `hitboxstudio` | 1.0.0 | Hitbox Studio | A fully customizable replacement for the F3+B hitbox renderer. Colors, line width, opacity, fill, per-entity sections and crystal highlights, all conf | client | `{"fabricloader": ">=0.19.5", "minecraft": "~1.21.11", "java": ">=21", "fabric-api": "*", "modmenu": ">=17.0.0-beta.1"}` | (none: not in .index) |
| IAS-9.0.8+1.21.11-fabric.jar | 320,896 | `ias` | 9.0.8+1.21.11-fabric | In-Game Account Switcher | Allows you to change which account you are signed in to in-game without restarting Minecraft. | client | `{"minecraft": "1.21.11", "fabric-lifecycle-events-v1": "*", "fabric-resource-loader-v1": "*", "fabric-screen-api-v1": "*"}` | (none: not in .index) |
| inventoryhud.fabric.1.21.11-3.4.29.jar | 226,047 | `inventoryhud` | 3.4.29 | Inventory HUD + | This mod will show you your inventory while playing. Enjoy! | client | `{"fabricloader": ">=0.14.11", "fabric": ">=0.134.1", "minecraft": ">=1.21.10", "java": ">=21"}` | Kp2uclYl |
| iris-fabric-1.10.7+mc1.21.11.jar | 2,807,547 | `iris` | 1.10.7+mc1.21.11 | Iris | A modern shaders mod for Minecraft intended to be compatible with existing OptiFine shader packs | client | `{"fabricloader": ">=0.12.3", "sodium": ["0.8.x"]}` | YL57xq9U |
| Ixeris-4.2.0+1.21.11-fabric.jar | 714,282 | `ixeris` | 4.2.0+1.21.11-fabric | Ixeris | Buffered raw input and threaded event polling | client | `{"minecraft": ">=1.21.9 <=1.21.11", "fabricloader": ">=0.16.0"}` | p8RJPJIC |
| konkrete_fabric_1.9.18_MC_1.21.11.jar | 771,623 | `konkrete` | 1.9.18 | Konkrete | Core library for Keksuccino's mods. | * | `{"fabricloader": ">=0.18.3", "fabric": ">=0.140.2", "minecraft": ">=1.21.11", "java": ">=21"}` | J81TRJWm |
| krypton-0.2.10.jar | 270,195 | `krypton` | 0.2.10 | Krypton | A Fabric mod that optimizes the Minecraft networking stack and entity tracker. | * | `{"fabricloader": ">=0.11.3", "minecraft": ">=1.21"}` | fQEb0iXm |
| litematica-fabric-1.21.11-0.26.16.jar | 1,966,804 | `litematica` | 0.26.16 | Litematica | A modern schematic mod for LiteLoader, Rift, Fabric etc. | client | `{"minecraft": "1.21.11", "malilib": ">=0.27.19- <0.28.0-"}` | (none: not in .index) |
| lithium-fabric-0.21.4+mc1.21.11.jar | 900,462 | `lithium` | 0.21.4+mc1.21.11 | Lithium | Lithium is a free and open-source optimization mod for Minecraft which makes a wide range of performance improvements to the game. | * | `{"fabricloader": ">=0.15.1", "minecraft": "1.21.11"}` | gvQqBUqZ |
| malilib-fabric-1.21.11-0.27.20.jar | 2,161,348 | `malilib` | 0.27.20 | MaLiLib | A library mod required by masa's client-side mods | client | `{"minecraft": "1.21.11", "fabric-networking-api-v1": ">=5.1.4", "fabric-resource-loader-v1": "*"}` | (none: not in .index) |
| Marlow Crystal Optimizer.jar | 114,134 | `marlowcrystal` | 1.1.0 | Marlow's Crystal Optimizer | Optimization that fixes waiting on the server after crystal break before end crystal entities are cleaned. | client | `{"fabricloader": ">=0.17.3", "minecraft": ">=1.21.11 <=1.21.11", "fabric-api": "*"}` | ozpC8eDC |
| melody_fabric_1.0.15_MC_1.21.11.jar | 227,886 | `melody` | 1.0.15 | Melody | OpenAL-based library mod to play background music. | * | `{"fabricloader": ">=0.18.3", "fabric": ">=0.140.2", "minecraft": ">=1.21.11", "java": ">=17"}` | CVT4pFB2 |
| microdurability-1.21.11-Fabric-1.0.0.jar.disabled | 28,061 | `microdurability` | 1.0.0 | MicroDurability | A very minimal durability viewer | client | `{"fabricloader": ">=0.18.2", "fabric-api": "*", "minecraft": "~1.21.11", "java": ">=21"}` | (none: not in .index) |
| modmenu-17.0.0.jar | 1,115,809 | `modmenu` | 17.0.0 | Mod Menu | Adds a mod menu to view the list of mods you have installed. | client | `{"fabric-resource-loader-v1": "*", "fabric-screen-api-v1": "*", "fabric-key-binding-api-v1": "*", "fabric-lifecycle-events-v1": "*", "fabricloader": ">=0.17.2", "minecraft": ">=1.21.11 <26"}` | mOgUt4GM |
| morechathistory-1.3.1.jar | 3,573 | `morechathistory` | 1.3.1 | More Chat History | Increases the length of the chat history. | client | `{"minecraft": ">=1.20.5", "fabricloader": ">=0.15.0"}` | 8qkXwOnk |
| moreculling-fabric-1.21.11-1.6.2.jar | 340,975 | `moreculling` | 1.6.2 | More Culling | Processes main resources. | client | `{"fabricloader": ">=0.15.0", "minecraft": ">=1.21", "java": ">=21", "cloth-config": ">=16.0.0"}` | 51shyZVL |
| naturalmotionblur-1.4.5+mc1.21.11.jar | 405,889 | `naturalmotionblur` | 1.4.5+mc1.21.11 | Natural Motion Blur | Natural Motion Blur Mod. | client | `{"fabricloader": ">=0.18.4", "minecraft": "~1.21", "java": ">=21 <22", "fabric-api": "*", "yet_another_config_lib_v3": "*"}` | (none: not in .index) |
| no-resource-pack-warnings-1.4.0.jar | 5,012 | `no-resource-pack-warnings` | 1.4.0 | No Resource Pack Warnings | Disable warnings for outdated resource packs | * | `{}` | 6xKUDQcB |
| NoDeathAnimation-2.0.0-1.21.11.jar | 11,493 | `nodeathanimation` | 2.0.0-1.21.11 | No Death Animation | Removes the death animation tilt on entities | client | `{"fabricloader": "*", "minecraft": "*", "java": ">=17", "fabric-api": "*", "walksylib": ">=0.9.95"}` | r6VxNyu0 |
| noisium-fabric-2.8.3+mc1.21.11.jar | 225,560 | `noisium` | 2.8.3+mc1.21.11 | Noisium | Optimises worldgen performance for a better gameplay experience. | * | `{"fabricloader": ">=0.18.4", "minecraft": "1.21.11"}` | hasdd01q |
| nowheel-1.3.2+mc1.21.2.jar | 386,076 | `nowheel` | 1.3.2+mc1.21.2 | NoWheel | Disables the scrolling wheel to move in your hotbar | client | `{"minecraft": ">=1.21.2", "fabricloader": ">=0.16", "ukulib": "^1.5.0"}` | GNhuDBbS |
| pack-folder-1.0.0.jar | 22,411 | `pack_folder` | 1.0.0 | Pack folder |  | client | `{"fabricloader": ">=0.18.4", "fabric-api": "*", "minecraft": ">=1.21.8 <=1.21.11"}` | U8LaJjly |
| packetfixer-fabric-3.3.5-1.21.11.jar | 31,237 | `packetfixer` | 3.3.5 | Packet Fixer | A simple mod to solve various problems with packets/NBT's. | * | `{"minecraft": ">=1.21.9"}` | c7m1mi73 |
| placeholder-api-2.8.2+1.21.10.jar | 268,662 | `placeholder-api` | 2.8.2+1.21.10 | Placeholder API | Simple api for mods allowing for user friendly formatting and universal placeholders! | * | `{"fabricloader": ">=0.16.10", "minecraft": ">=1.21.5-beta.3"}` | eXts2L7r |
| ProfilePresets-1.21.11v1.2.jar | 204,395 | `profile_presets` | 1.21.11 | Profile Presets |  | client | `{"fabricloader": ">=0.18.6", "fabric-api": "*", "minecraft": "1.21.11", "modmenu": "*"}` | iVf24pD3 |
| ravenclawspingequalizer-1.4.2-obf.jar | 63,611 | `ravenclawspingequalizer` | 1.4.2 | RavenclawsPingEqualizer |  | client | `{"fabricloader": ">=0.15.11", "fabric": "*", "minecraft": ">=1.21 <1.22"}` | pWxTsF2R |
| reeses-sodium-options-fabric-2.0.3+mc1.21.11.jar | 104,577 | `reeses-sodium-options` | 2.0.3+mc1.21.11 | Reese's Sodium Options | Replaces Sodium's Options Screen | client | `{"minecraft": ">=1.21.11", "sodium": ">=0.8.3"}` | Bh37bMuy |
| reflex-fabric-1.0.4+mc1.21.5.jar | 22,576 | `reflex` | 1.0.4+mc1.21.5 | Reflex AntiLag | This mod implements Nvidia Reflex in Minecraft to reduce rendering latency. | client | `{"fabricloader": ">=0.16.10", "minecraft": "~1.21.5", "java": ">=21", "fabric-api": "*", "modmenu": "*", "cloth-config": "*"}` | WTVzusYD |
| rrlsFabric-5.1.15+mc.1.21.11.jar | 198,379 | `rrls` | 5.1.15+mc.1.21.11 | Remove Reloading Screen | Makes resource packs load in the background, allowing you to do other things while waiting! | client | `{"fabricloader": ">=0.18.4", "minecraft": ">=1.21.11", "forgeconfigapiport": ">=21.11.1"}` | ZP7xHXtw |
| ScalableLux-0.1.6+fabric.c25518a-all.jar | 182,846 | `scalablelux` | 0.1.6+fabric.c25518a | ScalableLux | Rewrites the light engine to fix lighting performance and lighting errors | * | `{"fabricloader": ">=0.17.2", "minecraft": ">1.21.1"}` | Ps1zyz6x |
| Searchables-fabric-1.21.11-1.0.4.jar | 79,919 | `searchables` | 1.0.4 | Searchables | A library mod to facilitate adding search bars with auto complete and search types. | client | `{"fabricloader": ">=0.18.2", "java": ">=21", "minecraft": "1.21.11"}` | fuuu3xnx |
| servercore-fabric-1.5.15+1.21.11.jar | 932,539 | `servercore` | 1.5.15+1.21.11 | ServerCore | A Fabric mod that aims to optimize the minecraft server. | * | `{"fabricloader": ">=0.17.0", "minecraft": ">=1.21.11-", "fabric-api-base": "*", "fabric-command-api-v2": "*", "fabric-lifecycle-events-v1": "*"}` | 4WWQxlQP |
| shard-0.1.0.jar | 246,417 | `shard` | 0.1.0 | Shard | Sharpen your crystal PvP. Legit HUD, visuals and performance tweaks for crystal PvP. | client | `{"fabricloader": ">=0.19.5", "minecraft": "~1.21.11", "java": ">=21", "fabric-api": "*"}` | (none: not in .index) |
| shield-extensions-1.1.0+mc1.21.11.jar | 14,427 | `shield_extensions` | 1.1.0+mc1.21.11 | Shield Extensions | A simple shield HUD and sound helper for Fabric. | client | `{"fabricloader": ">=0.18.1", "fabric-api": "*", "minecraft": "1.21.11", "java": ">=21", "walksylib": "*"}` | (none: not in .index) |
| ShieldFixes-2.0.2-1.21.11.jar | 20,725 | `shieldfixes` | 2.0.2-1.21.11 | Shield Fixes | Resolves issues with shield sounds and corrects the animation glitch where the player's shield doesn't display blocking properly | client | `{"fabricloader": "*", "minecraft": "*", "java": ">=17", "fabric-api": "*", "walksylib": ">=0.9.95"}` | HfLFMeJe |
| ShieldStatus-4.1.2-1.21.11.jar | 37,034 | `shieldstatus` | 4.1.2-1.21.11 | Shield Status | Represents shield states using colors & textures | client | `{"fabricloader": "*", "minecraft": "*", "java": ">=17", "fabric-api": "*", "walksylib": ">=0.9.95"}` | GfoMZ9Ly |
| skyboxify-2.8+1.21.11-fabric.jar | 294,723 | `skyboxify` | 2.8 | Skyboxify | A skybox mod that allows you to use OptiFine skies in Fabric 1.21+ | client | `{"fabricloader": ">=0.18.4", "minecraft": "1.21.11", "fabric-resource-loader-v1": "*", "fabric-command-api-v2": "*", "yet_another_config_lib_v3": "*"}` | DWuwk8aA |
| sodium-extra-fabric-0.8.3+mc1.21.11.jar | 394,219 | `sodium-extra` | 0.8.3+mc1.21.11 | Sodium Extra | Features that shouldn't be in Sodium. | client | `{"fabricloader": ">=0.18", "sodium": ">=0.8.1", "minecraft": ">=1.21.11"}` | PtjYWJkn |
| sodium-fabric-0.8.7+mc1.21.11.jar | 1,870,358 | `sodium` | 0.8.7+mc1.21.11 | Sodium | Sodium is a powerful rendering engine for Minecraft which greatly improves frame rates and micro-stutter, while fixing many graphical issues | client | `{"fabricloader": ">=0.16.0", "fabric-block-view-api-v2": "*", "fabric-rendering-fluids-v1": ">=2.0.0", "fabric-resource-loader-v0": "*"}` | AANobbMI |
| sodium-fullbright-1.1.0.jar | 6,623 | `sodium-fullbright` | 1.1.0 | Sodium Fullbright |  | client | `{"sodium": ">=0.8.0", "minecraft": "*"}` | iVSjpXol |
| SprintByDefault-1.1.jar | 14,517 | `sprintbydefault` | 1.1 | SprintByDefault | Always have sprint enabled | client | `{"fabricloader": "*", "fabric-api": "*", "minecraft": "*"}` | JK2UFU1k |
| status-effect-bars-1.0.10.jar | 47,761 | `status-effect-bars` | 1.0.10 | Status Effect Bars | Adds small customizable bars to the status effects overlay and in the inventory to show the remaining duration of effects. | client | `{"fabricloader": ">=0.18.2", "minecraft": ">=1.21.11- <26", "java": ">=21", "cloth-config2": "*"}` | x02cBj9Y |
| totemcounter-1.11.2+mc1.21.11.jar | 70,668 | `totemcounter` | 1.11.2+mc1.21.11 | TotemCounter | counts the amount of skill issues (totem pops) | client | `{"minecraft": ">=1.21.11", "fabricloader": ">=0.17", "fabric-resource-loader-v1": "*", "fabric-command-api-v2": "*", "ukulib": "^1.10.0"}` | T9R7YTnA |
| TotemPopChams-1.0.1+1.21.11.jar | 30,749 | `walksypopchams` | 1.0.1+1.21.11 | Pop Chams | Captures entity states and displays a ghosted version | client | `{"fabricloader": "*", "minecraft": "*", "java": ">=17", "fabric-api": "*"}` | P5CL1LUJ |
| totemtweaks-1.21.11-1.1.0.jar | 12,264 | `totemtweaks` | 1.1.0 | Totem Tweaks | Changeable totem sizes and extensive customizability of the pop animation. | * | `{"fabricloader": ">=0.16.0", "fabric-api": "*", "modmenu": "*", "cloth-config": "*", "minecraft": "~1.21.11", "java": ">=21"}` | 1cfO6J6t |
| ukulib-1.10.2+1.21.11.jar | 464,276 | `ukulib` | 1.10.2+1.21.11 | ukulib | small utility library used in my mods | client | `{"minecraft": "~1.21.11", "fabricloader": ">=0.17", "fabric-resource-loader-v1": "*", "fabric-command-api-v2": "*", "fabric-key-binding-api-v1": "*"}` | Y8uFrUil |
| visual-tweaks-1.21.11.jar | 411,743 | `tostraights-customization` | 1.21.11 | Visual-Tweaks | Client-side CPvP/performance focused player rendering customization. | client | `{"fabricloader": ">=0.16.14", "minecraft": "1.21.11", "fabric-api": "*", "yet_another_config_lib_v3": "*", "modmenu": "*"}` | F64xDQC0 |
| vmp-fabric-mc1.21.11-0.2.0+beta.7.227-all.jar | 416,338 | `vmp` | 0.2.0+beta.7.227+1.21.11 | Very Many Players | A Fabric mod designed to improve server performance at high playercounts | * | `{"fabricloader": ">=0.14.13", "minecraft": ">=1.20.2-beta.2", "java": ">=17"}` | wnEe9KBa |
| voicechat-fabric-1.21.11-2.6.24.jar | 5,608,482 | `voicechat` | 1.21.11-2.6.24 | Simple Voice Chat | A working voice chat in Minecraft! | * | `{"fabricloader": ">=0.18.1", "minecraft": "1.21.11", "java": ">=21"}` | (none: not in .index) |
| WalksyLib-0.9.992+1.21.11.jar | 258,188 | `walksylib` | 0.9.992+1.21.11 | Walksy Lib | A config library for Walksy's mods | client | `{"fabricloader": "*", "minecraft": "*", "java": ">=17", "fabric-api": "*"}` | 7P86n6Vg |
| WI-Zoom-1.7-MC1.21.11.jar | 151,047 | `wi_zoom` | 1.7-MC1.21.11 | WI Zoom | The zoom from the Wurst Client as a standalone mod. | client | `{"fabricloader": ">=0.17.3", "fabric-api": ">=0.135.1", "minecraft": "~1.21.11-alpha.25.41.a", "java": ">=21"}` | o7DitHWP |
| yet_another_config_lib_v3-3.8.2+1.21.11-fabric.jar | 1,126,948 | `yet_another_config_lib_v3` | 3.8.2+1.21.11-fabric | YetAnotherConfigLib | YetAnotherConfigLib (yacl) is just that. A builder-based configuration library for Minecraft. | * | `{"fabricloader": ">=0.17.0", "minecraft": "~1.21.11", "java": ">=17", "fabric-api": ">=0.140.0+1.21.11"}` | 1eAoo2KR |
| zfastnoise-1.0.29+1.21.11.jar | 455,559 | `zfastnoise` | 1.0.29+1.21.11 | Fast Noise | Vanilla worldgen optimization mod | * | `{"fabricloader": ">=0.19.1", "minecraft": "~1.21.11", "java": ">=21"}` | OnlVIpq5 |

---

## 4. Other mods in the profile folder: Modrinth project pages

Metadata table (every profile mod not covered in section 2; `installed jar version` is from the local `fabric.mod.json`):

| slug | title | Modrinth ID | client / server | 1.21.11 listed | license | source | downloads | installed jar version |
|---|---|---|---|---|---|---|---|---|
| `anchor` | Hero's Anchor Optimizer | 5J1t3PV8 | required / unsupported | yes | MIT | https://github.com/HerobaneNair/herosanchoroptimizer | 2,271,889 | 1.1.4 |
| `appleskin` | AppleSkin | EsAfCjCV | optional / optional | yes | Unlicense | https://github.com/squeek502/AppleSkin | 93,130,633 | 3.0.8+mc1.21.11 |
| `better-block-entities` | Better Block Entities | ONZm0H7Y | required / unsupported | yes | LGPL-3.0-or-later | https://github.com/ceeden/betterblockentities | 9,955,408 | 1.3.3+mc1.21.11 |
| `better-chat-hide` | Better Chat Hide | AsEJlE2q | required / unsupported | yes | LicenseRef-All-Rights-Reserved | not stated | 37,918 | 1.21.11 |
| `better-hitbox` | Better Hitbox | cL7A0I0X | required / unsupported | yes | MIT | not stated | 14,570 | (not installed) |
| `better-screens` | Better Screens | PBbfPJZN | required / unsupported | yes | GPL-3.0-only | https://codeberg.org/MicrocontrollersDev/Better-Screens | 388,934 | 2.0.11 |
| `betterhurtcam` | BetterHurtCam | o4y0N2hu | required / unsupported | yes | MIT | https://github.com/uku3lig/betterhurtcam | 3,826,660 | 1.12.0+mc1.21.11 |
| `bobby` | Bobby | M08ruV16 | required / unsupported | yes | LGPL-3.0-only | https://github.com/Johni0702/bobby | 21,145,013 | 5.2.11+mc1.21.11 |
| `client-side-cosmetics` | Client Side Cosmetics | Wo8uYUHh | required / unsupported | yes | LicenseRef-All-Rights-Reserved | not stated | 43,053 | 2.0.0+mc1.21.11 |
| `cloth-config` | Cloth Config API | 9s6osm5g | optional / optional | yes | LGPL-3.0-only | https://github.com/shedaniel/ClothConfig/ | 175,238,774 | 21.11.153 |
| `commandkeys` | Command Keys | 65UyswbY | required / unsupported | yes | Apache-2.0 | https://github.com/TerminalMC/CommandKeys/ | 2,986,776 | 2.4.0+1.21.11 |
| `complete-shield-fixes` | Shield Fixes | HfLFMeJe | required / unsupported | yes | MIT | https://github.com/Walksy/ShieldFixes | 3,425,642 | 2.0.2-1.21.11 |
| `cpvp` | Totem Tweaks | 1cfO6J6t | required / unsupported | yes | LicenseRef-All-Rights-Reserved | https://github.com/eleqz/totem-tweaks/ | 2,784,049 | 1.1.0 |
| `cubes-without-borders` | Cubes Without Borders | ETlrkaYF | required / unsupported | yes | MIT | https://github.com/Kira-NT/cubes-without-borders | 28,307,855 | 3.0.0-build.14+mc1.21.11 |
| `cursorcentered-fix` | CursorCentered Fix | 3Vf97jLY | required / unsupported | yes | GPL-3.0-only | https://github.com/LitnhJacuzzi/CursorCentered-Fix | 570,348 | 1.2.1 |
| `custom-crosshair-mod` | Custom Crosshair Mod | o1tyE5vJ | required / unsupported | yes | LicenseRef-All-Rights-Reserved | not stated | 1,478,110 | 1.6.4-fabric |
| `custom-hitboxes` | Custom Hitboxes | y7bA0M36 | unknown / unknown | yes | MIT | https://github.com/Walksy/Custom-Hitboxes | 161,080 | (not installed) |
| `debugify` | Debugify | QwxR6Gcd | optional / optional | yes | LGPL-3.0-only | https://github.com/isXander/Debugify/ | 37,087,642 | 1.21.11+1.1 |
| `effecttimerplus` | Effect Timer Plus | JIUF2Wb5 | required / unsupported | yes | LGPL-3.0-only | https://github.com/TerminalMC/EffectTimerPlus | 1,949,184 | 2.2.7+1.21.11 |
| `emoji-type` | Emoji Type | q7vRRpxU | required / unsupported | yes | GPL-3.0-or-later | https://github.com/Norbiros/emojitype | 3,807,943 | 3.0.0-1.21.10 |
| `entityculling` | Entity Culling | NNAgCjsB | required / unsupported | yes | LicenseRef-tr7zw-Protective-License | https://github.com/tr7zw/EntityCulling | 176,266,605 | 1.10.1 |
| `exordium` | Exordium | DynYZEae | required / unsupported | yes | GPL-3.0-only | https://github.com/tr7zw/Exordium | 6,036,046 | 1.4.2+unknown |
| `fabric-api` | Fabric API | P7dR8mSH | optional / optional | yes | Apache-2.0 | https://github.com/FabricMC/fabric | 270,208,288 | 0.141.3+1.21.11 |
| `ferrite-core` | FerriteCore | uXXizFIs | optional / optional | yes | MIT | https://github.com/malte0811/FerriteCore | 156,869,343 | 8.2.0 |
| `forge-config-api-port` | Forge Config API Port | ohNO6lps | optional / optional | yes | MPL-2.0 | https://github.com/Fuzss/forgeconfigapiport | 67,583,234 | 21.11.1 |
| `highlights` | Highlights | QBMRH7wB | required / unsupported | yes | MIT | not stated | 36,256 | 1.2 |
| `inventoryhudplus` | InventoryHUD+ | Kp2uclYl | required / unsupported | yes | LicenseRef-All-Rights-Reserved | not stated | 10,118,731 | 3.4.29 |
| `iris` | Iris Shaders | YL57xq9U | required / unsupported | yes | LGPL-3.0-only | https://github.com/IrisShaders/Iris | 185,435,135 | 1.10.7+mc1.21.11 |
| `konkrete` | Konkrete | J81TRJWm | optional / optional | yes | Apache-2.0 | https://github.com/Keksuccino/Konkrete | 62,937,236 | 1.9.18 |
| `krypton` | Krypton | fQEb0iXm | optional / optional | yes | LGPL-3.0-only | https://github.com/astei/krypton | 42,918,893 | 0.2.10 |
| `litematica` | Litematica | bEpr0Arc | required / unsupported | yes | LGPL-3.0-only | https://github.com/sakura-ryoko/litematica | 26,355,262 | 0.26.16 |
| `lithium` | Lithium | gvQqBUqZ | optional / optional | yes | LGPL-3.0-only | https://github.com/caffeinemc/lithium-fabric | 133,241,771 | 0.21.4+mc1.21.11 |
| `malilib` | MaLiLib | GcWjdA9I | required / unsupported | yes | LGPL-3.0-only | https://github.com/maruohon/malilib/ | 36,987,306 | 0.27.20 |
| `melody` | Melody | CVT4pFB2 | unknown / unknown | yes | MIT | https://github.com/Keksuccino/Melody | 50,858,325 | 1.0.15 |
| `microdurability` | microDurability | JW3bGnLO | required / unsupported | NO | MPL-2.0 | https://github.com/ReviversMC/microDurability | 46,936 | 1.0.0 |
| `microdurability-maintained` | MicroDurability Maintained | bYzs56g2 | required / unsupported | yes | MPL-2.0 | https://github.com/Leclowndu93150/MicroDurability | 27,596 | (not installed) |
| `modmenu` | Mod Menu | mOgUt4GM | required / unsupported | yes | MIT | https://github.com/TerraformersMC/ModMenu | 151,042,441 | 17.0.0 |
| `morechathistory` | More Chat History | 8qkXwOnk | required / unsupported | yes | CC0-1.0 | https://github.com/JackFredMods/MoreChatHistory | 28,515,190 | 1.3.1 |
| `moreculling` | More Culling | 51shyZVL | required / unsupported | yes | GPL-3.0-only | https://github.com/fxmorin/moreculling | 68,494,247 | 1.6.2 |
| `natural-motion-blur` | Natural Motion Blur | RlEyqCCv | required / unsupported | yes | LGPL-3.0-only | https://github.com/ItsPasi/natural-motionblur-fabric | 1,094,260 | 1.4.5+mc1.21.11 |
| `no-resource-pack-warnings` | No Resource Pack Warnings | 6xKUDQcB | required / unsupported | yes | MIT | https://github.com/SpaceWalkerRS/no-resource-pack-warnings | 9,322,371 | (not installed) |
| `noisiumforked` | NoisiumForked | hasdd01q | unsupported / required | yes | LGPL-3.0-only | https://github.com/coredex-source/noisium | 2,555,537 | 2.8.3+mc1.21.11 |
| `nowheel` | NoWheel | GNhuDBbS | required / unsupported | yes | MIT | https://github.com/uku3lig/nowheel | 570,690 | 1.3.2+mc1.21.2 |
| `pack-folders` | Texture Pack Folders (Like Recursive Resources) | U8LaJjly | required / unsupported | yes | LicenseRef-All-Rights-Reserved | not stated | 185,208 | 1.0.0 |
| `packet-fixer` | Packet Fixer | c7m1mi73 | optional / optional | yes | MIT | https://github.com/TonimatasDEV/PacketFixer | 29,688,928 | 3.3.5 |
| `placeholder-api` | Text Placeholder API | eXts2L7r | optional / optional | yes | LGPL-3.0-only | https://github.com/Patbox/TextPlaceholderAPI | 68,753,391 | 2.8.2+1.21.10 |
| `profile-presets` | Profile Presets | iVf24pD3 | required / unsupported | yes | LicenseRef-All-Rights-Reserved | not stated | 30,668 | 1.21.11 |
| `ravenclaws-ping-equalizer` | Ravenclaw's Ping Equalizer | pWxTsF2R | required / unsupported | yes | MIT | https://github.com/ravineclaw/ping-equalizer | 391,350 | 1.4.2 |
| `reeses-sodium-options` | Reese's Sodium Options | Bh37bMuy | required / unsupported | yes | MIT | https://github.com/FlashyReese/reeses-sodium-options | 83,734,727 | 2.0.3+mc1.21.11 |
| `reflex-antilag` | Reflex AntiLag | WTVzusYD | required / unsupported | yes | LicenseRef-All-Rights-Reserved | https://github.com/Tythee/Minecraft-Reflex | 576,002 | 1.0.4+mc1.21.5 |
| `rrls` | Remove Reloading Screen | ZP7xHXtw | required / unsupported | yes | OSL-3.0 | https://github.com/dima-dencep/rrls | 26,911,982 | 5.1.15+mc.1.21.11 |
| `scalablelux` | ScalableLux | Ps1zyz6x | optional / optional | yes | LGPL-3.0-only | https://github.com/RelativityMC/ScalableLux | 14,751,895 | 0.1.6+fabric.c25518a |
| `searchables` | Searchables | fuuu3xnx | required / unsupported | yes | MIT | https://github.com/jaredlll08/searchables | 40,234,656 | 1.0.4 |
| `servercore` | ServerCore | 4WWQxlQP | unsupported / required | yes | MIT | https://github.com/Wesley1808/ServerCore | 17,604,137 | 1.5.15+1.21.11 |
| `shield-extensions` | Shield Extensions | J2upEK4n | required / unsupported | NO | MIT | https://github.com/Noryea/shield-extensions | 2,980 | 1.1.0+mc1.21.11 |
| `shield-statuses` | Shield Statuses | GfoMZ9Ly | required / unsupported | yes | MIT | https://github.com/Walksy/ShieldStatus | 3,968,140 | 4.1.2-1.21.11 |
| `simple-voice-chat` | Simple Voice Chat | 9eGKb6K1 | optional / optional | yes | LicenseRef-All-Rights-Reserved | https://github.com/henkelmax/simple-voice-chat | 72,191,587 | 1.21.11-2.6.24 |
| `skyboxify` | Skyboxify | DWuwk8aA | required / unsupported | yes | GPL-3.0-only | https://github.com/Legacy-Visuals-Project/Skyboxify | 14,290,112 | 2.8 |
| `sodium` | Sodium | AANobbMI | required / unsupported | yes | LicenseRef-Polyform-Shield-1.0.0 | https://github.com/CaffeineMC/sodium | 238,422,500 | 0.8.7+mc1.21.11 |
| `sodium-extra` | Sodium Extra | PtjYWJkn | required / unsupported | yes | LGPL-3.0-only | https://github.com/FlashyReese/sodium-extra-fabric | 100,283,035 | 0.8.3+mc1.21.11 |
| `sodium-fullbright` | Sodium Fullbright | iVSjpXol | required / unsupported | yes | Apache-2.0 | not stated | 572,556 | 1.1.0 |
| `sprintbydefault` | SprintByDefault | JK2UFU1k | required / unsupported | yes | MIT | not stated | 102,265 | 1.1 |
| `status-effect-bars` | Status Effect Bars | x02cBj9Y | required / unsupported | yes | LGPL-3.0-only | https://github.com/A5b84/status-effect-bars | 7,556,799 | 1.0.10 |
| `totem-pop-chams` | Totem Pop Chams | P5CL1LUJ | required / unsupported | yes | MIT | https://github.com/Walksy/TotemPopChams | 95,853 | 1.0.1+1.21.11 |
| `totemcounter` | TotemCounter | T9R7YTnA | required / unsupported | yes | MIT | https://github.com/uku3lig/totemcounter | 4,613,454 | 1.11.2+mc1.21.11 |
| `ukulib` | ukulib | Y8uFrUil | required / unsupported | yes | MPL-2.0 | https://github.com/uku3lig/ukulib | 13,667,831 | 1.10.2+1.21.11 |
| `visual-tweaks` | Visual Tweaks (Player Customization) | F64xDQC0 | required / unsupported | yes | LicenseRef-All-Rights-Reserved | not stated | 26,494 | 1.21.11 |
| `vmp-fabric` | Very Many Players (Fabric) | wnEe9KBa | optional / required | yes | MIT | https://github.com/RelativityMC/VMP-fabric | 17,072,775 | 0.2.0+beta.7.227+1.21.11 |
| `walksylib` | WalksyLib | 7P86n6Vg | required / unsupported | yes | MIT | https://github.com/Walksy/WalksyLib | 5,226,238 | 0.9.992+1.21.11 |
| `yacl` | YetAnotherConfigLib (YACL) | 1eAoo2KR | optional / optional | yes | LGPL-3.0-or-later | https://github.com/isXander/YetAnotherConfigLib | 127,862,676 | 3.8.2+1.21.11-fabric |
| `zfastnoise` | Fast Noise | OnlVIpq5 | unsupported / required | yes | MPL-2.0 | https://codeberg.org/ZenXArch/FastNoise | 7,797,141 | 1.0.29+1.21.11 |

Two installed jars could not be matched to a Modrinth project:

- `hitbox-studio-1.0.0.jar` (mod id `hitboxstudio`, "Hitbox Studio", "A fully customizable replacement for the F3+B hitbox renderer. Colors, line width, opacity, fill, per-entity sections and crystal highlights, all configured through Mod Menu.", MIT, no authors/contact, entry point `dev.hitboxstudio.HitboxStudio`). Slugs `hitbox-studio` / `hitboxstudio` -> 404; name search returns only "Better Hitbox" (`better-hitbox`, a different mod). It appears to have replaced Walksy's Custom Hitboxes (whose `.index` entry is stale).
- `WI-Zoom-1.7-MC1.21.11.jar` (mod id `wi_zoom`, "The zoom from the Wurst Client as a standalone mod.", GPL-3.0-or-later, author Alexander01998, sources https://github.com/Wurst-Imperium/WI-Zoom). The `.index` entry points at Modrinth project `o7DitHWP`, which now returns 404, and a search for "WI Zoom" returns nothing -- the project is no longer listed on Modrinth.

Per-project summaries (from the Modrinth `body`; 3-6 lines each), grouped by relevance to a crystal-PvP client.

### 4a. Crystal-PvP / combat visual and feedback mods

- **Totem Pop Chams** (`totem-pop-chams`, Walksy, MIT): "captures the visual state of a player when they pop a totem of undying and displays a 'ghosted' version in that place." Config: "Disperse Settings" -- "make the model fly outwards from the player's pop position"; "You can control the distance (in blocks) it travels and the speed it travels at"; a ghost "lifetime" setting is referenced. Purely visual; no packets stated.
- **TotemCounter** (`totemcounter`, uku3lig, MIT): "shows the amount of totem pops for every player above their nametag, and shows how many totems you have in your inventory". Features: "Pop counter in nametag", "Easy reset via options or keybinding", "Automatic reset on death or on match end (on supported servers)", "Show the amount of totems in your inventory", "Colored XP bar according the totem count". Requires ukulib. How other players' pops are detected is not stated.
- **Totem Tweaks** (`cpvp`, eleqz, All Rights Reserved): "Customizable Pop Animation/Size/Position and Totem Size." Features: "Changing totem size for handheld", "Changing the totem pop size", "Disable Equip Animation of the totem", "Option to Disable the pop animation", "Configurable Totem Pop Animation Speed", "Lock Rotation Position", "Disable Rotations". Visual only.
- **Shield Fixes** (`complete-shield-fixes`, Walksy, MIT): fixes MC-105068 (shield sounds) and MC-238293 (blocking animation). "Shield Sounds" for "If a player's shield gets disabled" and "If a player's shield is being hit / attacked by any damage source. This includes explosions and projectiles." "Shield Blocking" fixes the animation where "a player is shielding but sometimes doesn't show them blocking"; "also factors in the 5-tick delay from shields" so it "will not show the player is blocking until they actually are blocking", with a config toggle to disable that delay handling.
- **Shield Statuses** (`shield-statuses`, Walksy, MIT): "renders color overlays on player's shields to determine whether that shield is disabled or not." Config: customizable "disabled and enabled color"; an "interpolation option" that lets "the disabled color gradually transition to the enabled color. This way, you can estimate when the shield will be reactivated." Renders as an overlay so resource-pack shield textures are preserved; fixes the original mod's bug where a shield "would incorrectly display as disabled (orange state) even after it had been re-enabled."
- **Shield Extensions** (`shield-extensions`, Noryea, MIT; Modrinth does **not** list 1.21.11, yet a 1.21.11 jar `1.1.0+mc1.21.11` is installed): "Shield Sounds like PvpLegacy. Also adds some tiny functions that shows the status of your shield(off by default)." Config: "Shield Disabling Sound: True / False", "Blocked Sound (attack&bow): True / False", "Cooling Down Showing Mode: Always / Offhand Only / Never", "Show Raising Delay (5gt): True / False". Depends on walksylib (jar).
- **Custom Hitboxes** (`custom-hitboxes`, Walksy, MIT; `.index` entry only, jar not present): "a complete overhaul of minecraft's hitboxes". Config sections: "Block Hovering Settings", "Team Settings" (usernames grouped into teams with per-team colors; "Player names are cap sensitive"), "Server-Side position settings" ("renders the hitbox where the entity is server side. This provides no advantage as entity interactions are determined by client-side calculations"), "Hitbox Vanishing" (hide at a distance, optional fading), "Hitbox Color Switching" (color transitions by distance), "Damage Colors" (color change while an entity is in a damage tick), plus gradient and rainbow colours.
- **Better Hitbox** (`better-hitbox`, afkz studios, MIT; not installed, surfaced by search): replaces F3+B hitboxes with "11 preset colors for both regular and crosshair-targeted entities", "Per-entity targeting", "Adjustable opacity & line width", F3+B toggle, quick-toggle keybind for "All Entities"/"Targeted Only", Mod Menu config.
- **Visual Tweaks (Player Customization)** (`visual-tweaks`, tostraight, All Rights Reserved; jar mod id `tostraights-customization`): "client-side Fabric mod for 1.21.11 ... CPvP-focused customization". Features: "Hide armor by slot", "Control enchantment glint", "Customize player model parts and skin layers", "Hide visual clutter like arrows, stingers, and fire overlay", "Edit hurt tint with custom RGBA colors", "Style nametags and show color-coded ping", "Block actionbar overlays client-side", "Spoof the local displayed name and skin", "Replace shield-break audio with your own .mp3" (reads the first alphabetically sorted `.mp3` from `.minecraft/shielddisablesound`, refreshable in-game).
- **Client Side Cosmetics** (`client-side-cosmetics`, All Rights Reserved): client-only armor trims ("Change trims on your armor freely", "live 3D player preview with full 360 rotation", "Save up to 9 presets", keybind `K` default, "Toggle trims on/off"), "Shield Banner Editor" (banner from command/NBT text, favorites, "HD shield rendering fix", experimental), "Cape Customization" (own PNG capes 64x32, separate normal/elytra cape, "Also includes capes that are normally unavailable"); visible only to the local player.
- **Highlights** (`highlights`, MIT): "rule-based item highlighting to Minecraft inventory screens ... outlining or highlighting them with custom colors, without changing item textures". Jar description: "Highlights Totems of Undying in the inventory." Depends on Cloth Config and Mod Menu (jar).
- **BetterHurtCam** (`betterhurtcam`, uku3lig, MIT): revamped NoHurtCam; disable or change hurt-camera intensity "via the integrated config screen, or via a keybinding"; "Use any multiplier (eg higher than 1 or lower than 0)", "Bring back the old non-direction-based damage tilt", "Disabling the blinking animation of the health bar". Requires ukulib.
- **Custom Crosshair Mod** (`custom-crosshair-mod`, All Rights Reserved): crosshair "Size!", "Colour!", "Rainbow Crosshair!", "Shape!", "Dynamic! (Try using a bow or sword!)", "Adaptive Colour!", "Works on servers!", "Draw your own!"; GRAVE key opens the menu.
- **Effect Timer Plus** (`effecttimerplus`, TerminalMC, LGPL-3.0): "potency indicator and countdown overlay to status effect icons". Options: "Fully adjustable text color and opacity", "Optional text shadow", "Optional and adjustable text background", "8 options for text position", "Optional low-time warning color and flash", "Option to disable either indicator separately", "Option to hide timer on ambient (beacon) effects", icon scaling, text scaling. Requires Fabric API, ModMenu, YACL.
- **Status Effect Bars** (`status-effect-bars`, A5b84, LGPL-3.0): "small customizable bars to the status effects overlay and in the inventory to show the remaining duration of effects"; "Config screen to edit colors and bar positions"; "Bars can be hidden automatically in some specific cases ... (e.g. beacon effects and effects with very long durations)". Requires Cloth Config.
- **InventoryHUD+** (`inventoryhudplus`, All Rights Reserved): "Inventory HUD" (mini/normal, horizontal/vertical, background transparency, animation toggle), "PotionHUD" (timer or duration bar, gap, transparency, horizontal mode), "ArmorHUD" ("durability of your armor and equipment", free inventory slots, arrow count; scale, damage indicator type, per-piece toggles). Drag-and-drop HUD positioning; keybind `O` opens config.
- **AppleSkin** (`appleskin`, Unlicense): food values in tooltips, saturation/exhaustion visualization on the HUD, preview of hunger/saturation restored while holding food. "it needs to be on the server in order to display accurate saturation/exhaustion values"; "provides information about some mechanics that are invisible by default".
- **microDurability** (`microdurability`, MPL-2.0; installed jar is `.disabled`; the Modrinth original lists no 1.21.11, `microdurability-maintained` does): "minimal durability viewer that shows the durability bars of your armor right above the hotbar"; "can also warn you when your tools or armor are about to break."
- **Hitbox Studio** (not on Modrinth; see above): jar description only -- "Colors, line width, opacity, fill, per-entity sections and crystal highlights, all configured through Mod Menu."

### 4b. Input / latency / performance mods

- **Ravenclaw's Ping Equalizer** (`ravenclaws-ping-equalizer`, MIT): "allows players to artificially increase their network latency ... introduces real packet delay at the network layer" ("The delay happens in the Netty event loop"). Commands: `/pe add <ms>`, `/pe total <ms>`, `/pe status`, `/pe off`. "Outgoing and incoming packets are intercepted and held in a scheduled task queue". Server control payloads: S2C `ravenclawspingequalizer:control` (boolean enabled), S2C `ravenclawspingequalizer:state_query`, C2S `ravenclawspingequalizer:state_response` (replies with `requestId`, `serverEnabled`, `mode`, `currentDelayMs`, `basePingMs`, `totalPingMs`). "All status changes (enabling/disabling delay) post a public chat message." Explicitly allowed on MCTiers "for the sole purpose of equalizing ping in high-tier testing". This mod delays and answers packets -- a network-layer manipulation.
- **Reflex AntiLag** (`reflex-antilag`, Tythee, All Rights Reserved; jar `reflex` 1.0.4+mc1.21.5, depends Cloth Config + Mod Menu): "paces the CPU frame start to wake up right when the GPU is ready, completely eliminating driver queue backlog". Highlights: "Zero Driver Queue", "Fresh Input Polling (Paces frame sleep strictly before input polling)", "Auto Adaptive Margin", "In-Game F3 Timeline Diagram", "Universal Compatibility" (NVIDIA/AMD/Intel, OpenGL & Vulkan). No configuration needed per the FAQ.
- **Exordium** (`exordium`, tr7zw, GPL-3.0; manually added): "Renders the GUI at a lower fixed framerate (configurable in the settings), freeing up CPU and GPU time for the world rendering." "This mod is not actively maintained"; incompatibilities listed with VulkanMod, MiniHUD, Canvas, ImmediatelyFast's Fast Crosshair, BetterF3 animations and "Many other mods that render their own GUI elements". (Jar version string is `1.4.2+unknown` inside `exordium-1.4.3-1.21.11.jar`.)
- **Natural Motion Blur** (`natural-motion-blur`, LGPL-3.0; manually added): frame blending to mimic human-vision motion blur. Features: "Blur Toggle - Including adjustable Keybind", "Refresh Rate Scaling", "Blur Strength Adjustment", "Multiple Blur Methods" ("Velocity Based (Default)", "Frame Blending", "Hybrid Blending", "Accumulation Max", "Accumulation Mix"), "OBS Recording Output" (Spout2 sender). Config via `/motionblur` or `/mb` (client-side commands).
- **NoWheel** (`nowheel`, uku3lig, MIT): "disables the scroll wheel to switch slots in the hotbar"; toggle via keybind or config (ukulib).
- **SprintByDefault** (`sprintbydefault`, Intallact, MIT): "makes it so you automatically sprint when clicking W." No options stated.
- **CursorCentered Fix** (`cursorcentered-fix`, GPL-3.0): "fixes the cursor centering issue that occasionally happens when opening a GUI ... which could be dangerous when you need to pull something from a chest or inventory rapidly." Notes vanilla moved to an SDL backend after 26.3, so the mod stops at 26.2. Wayland support via ydotool/wdotool.
- **Cubes Without Borders** (`cubes-without-borders`, MIT): adds a third "Borderless" option to the Fullscreen setting; "does not hinder your input latency"; `--borderless` startup flag; selectable modes `minecraft:windowed` (default, low-latency) vs `windows:windowed`.
- **Better Screens** (`better-screens`, GPL-3.0): "Prevent Closing Screens" ("Prevents the server from forcefully closing your screens"), "Don't Reset Cursor", "Click Out of Containers", "Container Background Opacity", "Container Texture Opacity", "Container Label Color", "Player Inventory Label Color", "Container GUI Scale" (unfinished), "Remove Recipe Book Shift". Note: "Prevent Closing Screens" ignores a server packet.
- **Ixeris**, **BadOptimizations**: see section 2.
- Rendering/engine performance (standard stack, summaries only): **Sodium** (`sodium`, Polyform Shield) rendering engine; **Sodium Extra** (`sodium-extra`, LGPL) OptiFine-style "Animations Settings", "Particles Settings", "Details Settings", "Render Settings", "Extra Settings (Display FPS, coordinates, toast notifications, clouds, ...)"; **Reese's Sodium Options** (`reeses-sodium-options`, MIT) alternative Sodium options screen with search, tabs, per-option reset/undo, option to return to the default screen; **Sodium Fullbright** (`sodium-fullbright`, Apache-2.0) "change the brightness option in Sodium from 100% to 1000%"; **Iris Shaders** (`iris`, LGPL) shader loader; **Entity Culling** (`entityculling`, tr7zw protective license; installed jar `.disabled`) async path-traced visibility culling of entities/block entities; **More Culling** (`moreculling`, GPL-3.0) additional culling, requires Cloth Config; **Better Block Entities** (`better-block-entities`, LGPL) meshes static block entities (chests, shulkers, signs, pots, beds, bells, banners, copper golem statues) into terrain, requires Sodium, per-block toggles; **FerriteCore** (`ferrite-core`, MIT) memory reduction; **Lithium** (`lithium`, LGPL) game-logic optimization, "not changing any vanilla mechanics"; **Krypton** (`krypton`, LGPL) networking-stack optimization, "best-effort" support; **ScalableLux** (`scalablelux`, LGPL) Starlight-based light engine; **Debugify** (`debugify`, LGPL) "fixes over 70 bugs found on the bug tracker" with per-fix config (lists superseded mods incl. BetterShields, No Telemetry, ToolTipFix, Entity Collision FPS Fix); **Bobby** (`bobby`, LGPL) caches server chunks in `.minecraft/.bobby` to render beyond server view-distance; **Remove Reloading Screen** (`rrls`, OSL-3.0) strips the resource reload screen to a progress bar.
- Server-side optimizers that are also installed client-side (server_side `required`; on a pure client they only affect the integrated server): **ServerCore** (`servercore`), **Very Many Players** (`vmp-fabric`), **NoisiumForked** (`noisiumforked`, "discontinued starting 26.3" in favour of FastNoise), **Fast Noise** (`zfastnoise`).

### 4c. Chat / UI / misc

- **Command Keys** (`commandkeys`, TerminalMC, Apache-2.0): "A powerful command macro mod." Features: "Multiple commands per macro", "Multiple macros per keybind", "Optional delay timing between commands", conflict strategies `Submit` / `Assert` / `Veto` / `Avoid`, send modes "Send", "Type", "Edit", "Cycle", "Random", "Repeat", "Profiles with automatic switching based on server address or world name", placeholders (`%lastsent%`, `%lastcmd%`, `%clipboard%`, `%myname%`, `%pmsender%`, `%pos%`, `%x%`, `%lpos%`, regex placeholders). This mod **sends chat messages/commands on keypress** (automation of chat only).
- **Better Chat Hide** (`better-chat-hide`, tostraight, All Rights Reserved): "ensures that your chat remains hidden unless you manually open it, and only functions when the mod is toggled" (keybind; Modrinth: "Command Is Toggleable").
- **More Chat History** (`morechathistory`, CC0): chat history "from the vanilla 100 messages to 16384."
- **Emoji Type** (`emoji-type`, GPL-3.0): shortcodes like `:skull:` -> Unicode emoji in chat/signs/books/anvils; autocomplete on `:`; custom YAML emoji packs.
- **Profile Presets** (`profile-presets`, tostraight, All Rights Reserved): save/load named client setups: `config/`, `options.txt`, active resource packs and pack files, optionally `mods/`; "Profiles with mods need a restart"; in-game screen with search/load/delete.
- **Texture Pack Folders** (`pack-folders`, tostraight, All Rights Reserved): lets resource packs inside sub-folders of `resourcepacks` be listed and used.
- **No Resource Pack Warnings** (`no-resource-pack-warnings`, MIT): removes the red outdated-pack borders and the confirmation pop-ups.
- **Skyboxify** (`skyboxify`, GPL-3.0): OptiFine-format custom skies for Fabric 1.21+.
- **Mod Menu** (`modmenu`, MIT): mod list and config-screen access; includes a Modrinth update checker (network).
- **Packet Fixer** (`packet-fixer`, MIT): raises packet/NBT size limits to avoid "Packet too big" / "VarInt too big" / timeout errors; "The mod is required in Client and Server sides."
- **Simple Voice Chat** (`simple-voice-chat`, All Rights Reserved; manually added): proximity voice chat; "requires special setup on the server".
- **Litematica** (`litematica`, LGPL; manually added) schematic mod (no printer in the base mod) with **MaLiLib** (`malilib`, LGPL) as its library.
- **Controlling** / **In-Game Account Switcher**: see section 2.

### 4d. Libraries (no user-facing behaviour of their own)

Fabric API (`fabric-api`), Cloth Config API (`cloth-config`), YetAnotherConfigLib (`yacl`), Forge Config API Port (`forge-config-api-port`), Konkrete (`konkrete`), Melody (`melody`, OpenAL background-music library), Text Placeholder API (`placeholder-api`), Searchables (`searchables`), ukulib (`ukulib`), WalksyLib (`walksylib`: "builder-based config library" with Boolean, Numerical, Color, Enum, "Sprite/Texture Option -- Drag and drop custom images", "Pixel Grid Option", String, String List options).

---

## 5. Summary of behaviours relevant to a "legit" client (facts from the descriptions only)

Client-side prediction with the server remaining authoritative (each states it does not change packets or outcomes):

1. Crystal break prediction -- Marlow's Crystal Optimizer: remove the end-crystal entity client-side on hit instead of waiting for the server.
2. Crystal place prediction -- Client Side Crystals: spawn a fake, visual-only crystal immediately on placement, swap to the real one when the server spawns it; optional "tint" to show which crystal is fake.
3. Anchor explode prediction -- cutebow Anchor Optimizer: replace the exploded anchor with a client-side barrier-like placeholder (collision kept) until the server confirms; Hero's: replace with a "fake" replaceable ghost anchor so another block can be placed in the same spot; Walksy's: remove the anchor and nearby glowstone outright while remembering the position for air-placing.
4. Eating prediction -- Consumable Optimizer: finish consuming on the exact client tick and cancel server "recall" packets; exposes an opt-out plugin channel (`consumable_optimizer:handshake_payload` / `consumable_optimizer:disable_payload`).

Pure visual/HUD behaviours stacked by this player: totem pop ghosts (Totem Pop Chams), totem pop counters on nametags and totem count in inventory (TotemCounter), totem size/pop animation tweaks (Totem Tweaks), shield state colours with cooldown interpolation (Shield Statuses) and shield sound/animation fixes with 5-tick-delay awareness (Shield Fixes, Shield Extensions), customizable hitboxes with team colours / distance fading / damage colours (Custom Hitboxes, Hitbox Studio), armor/clutter hiding, hurt-tint colour and ping-coloured nametags (Visual Tweaks), death-tilt removal and red-overlay duration (No Death Animation), hurt-cam intensity (BetterHurtCam), effect timers/bars, inventory/armor HUD, custom crosshair, environment colours (Ambience V2/V3), fullbright (Sodium Fullbright).

Input/latency: Ixeris (threaded polling + buffered raw input + FPS limiter), Reflex AntiLag (frame pacing), NoWheel, SprintByDefault, CursorCentered Fix, borderless fullscreen (CWB), lower GUI framerate (Exordium).

Behaviours that touch the network or automate actions (present in the profile; a legit client should treat these as out of scope or opt-in with care): Ravenclaw's Ping Equalizer (real packet delay, server control payloads, public chat announcements), Command Keys (sends chat/commands on keypress), Consumable Optimizer's handshake payload, cutebow Anchor Optimizer's server-list injection and updater, Force Lowercase Commands' rewrite of outgoing command text, Better Screens' "Prevent Closing Screens", In-Game Account Switcher's Microsoft authentication, Mod Menu's update checker.

Local staleness noticed: Ixeris installed 4.2.0 vs latest 4.6.8; `custom-hitboxes.pw.toml` references a jar that is no longer present; WI Zoom's Modrinth project is gone (404); Shield Extensions' Modrinth page does not list 1.21.11 although a 1.21.11 build is installed.